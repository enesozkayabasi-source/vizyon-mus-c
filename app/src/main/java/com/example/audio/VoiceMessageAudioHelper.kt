package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Play-compliant AAC (.m4a) voice message recorder and player capped at 30 seconds
 * (~32 kbps -> <= 125 KB raw / <= 170K Base64 chars) for DM voice messages.
 */
object VoiceMessageAudioHelper {
  const val MAX_DURATION_MS = 30_000L
  private const val MAX_RAW_AUDIO_BYTES = 160_000
  private const val MAX_BASE64_LENGTH = 220_000

  data class RecordedVoiceClip(
    val filePath: String,
    val base64: String,
    val durationMs: Long
  )

  private val playerLock = Any()
  private var activeRecorder: MediaRecorder? = null
  private var activeRecordFile: File? = null
  private var activePlayer: MediaPlayer? = null
  @Volatile private var activePlayingKey: String? = null

  fun startRecording(context: Context): Result<File> =
    runCatching {
      stopPlayback()
      discardActiveRecording()

      val appCtx = context.applicationContext
      val outFile = File(appCtx.cacheDir, "voice_rec_${UUID.randomUUID().toString().take(8)}.m4a")
      val recorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          MediaRecorder(appCtx)
        } else {
          @Suppress("DEPRECATION")
          MediaRecorder()
        }
      recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(32_000)
        setAudioSamplingRate(22_050)
        setAudioChannels(1)
        setMaxDuration(MAX_DURATION_MS.toInt())
        setOutputFile(outFile.absolutePath)
        prepare()
        start()
      }
      activeRecorder = recorder
      activeRecordFile = outFile
      outFile
    }.onFailure {
      discardActiveRecording()
    }

  suspend fun stopRecordingAndEncode(elapsedMs: Long): Result<RecordedVoiceClip> =
    withContext(Dispatchers.IO) {
      runCatching {
        val recorder = activeRecorder
        val file = activeRecordFile
        activeRecorder = null
        activeRecordFile = null

        if (recorder != null) {
          runCatching { recorder.stop() }
          runCatching { recorder.reset() }
          runCatching { recorder.release() }
        }
        require(file != null && file.exists() && file.length() > 256L) {
          "Ses kaydı alınamadı. Lütfen en az 1 saniye konuşun."
        }
        val bytes = file.readBytes()
        require(bytes.size <= MAX_RAW_AUDIO_BYTES) {
          "Ses kaydı boyutu sınırı aşıyor (maks 30 sn)."
        }
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        require(b64.length <= MAX_BASE64_LENGTH) {
          "Sesli mesaj boyutu limiti aşıyor."
        }
        val safeDuration = elapsedMs.coerceIn(1_000L, MAX_DURATION_MS)
        RecordedVoiceClip(
          filePath = file.absolutePath,
          base64 = b64,
          durationMs = safeDuration
        )
      }
    }

  fun discardActiveRecording(clipPath: String? = null) {
    runCatching { activeRecorder?.stop() }
    runCatching { activeRecorder?.reset() }
    runCatching { activeRecorder?.release() }
    activeRecorder = null
    runCatching { activeRecordFile?.delete() }
    activeRecordFile = null
    if (!clipPath.isNullOrBlank()) {
      runCatching { File(clipPath).delete() }
    }
  }

  fun playVoiceClip(
    context: Context,
    key: String,
    filePath: String? = null,
    audioBase64: String? = null,
    onCompleted: () -> Unit = {},
    onError: (String) -> Unit = {}
  ) {
    synchronized(playerLock) {
      stopPlaybackInternal()
      try {
        val appCtx = context.applicationContext
        val playableFile =
          when {
            !filePath.isNullOrBlank() && File(filePath).exists() -> File(filePath)
            !audioBase64.isNullOrBlank() -> {
              val decoded = Base64.decode(audioBase64, Base64.DEFAULT)
              val cacheFile = File(appCtx.cacheDir, "voice_play_${key.hashCode()}.m4a")
              cacheFile.writeBytes(decoded)
              cacheFile
            }
            else -> null
          }
        if (playableFile == null || !playableFile.exists()) {
          onError("Ses dosyası açılamadı.")
          return
        }

        val player = MediaPlayer()
        player.setDataSource(playableFile.absolutePath)
        player.setOnCompletionListener {
          synchronized(playerLock) {
            if (activePlayingKey == key) {
              activePlayingKey = null
            }
            runCatching { it.reset() }
            runCatching { it.release() }
            if (activePlayer === it) activePlayer = null
          }
          onCompleted()
        }
        player.setOnErrorListener { mp, _, _ ->
          synchronized(playerLock) {
            if (activePlayingKey == key) activePlayingKey = null
            runCatching { mp.reset() }
            runCatching { mp.release() }
            if (activePlayer === mp) activePlayer = null
          }
          onError("Sesli mesaj oynatılamadı.")
          true
        }
        player.prepare()
        player.start()
        activePlayer = player
        activePlayingKey = key
      } catch (_: Throwable) {
        stopPlaybackInternal()
        onError("Sesli mesaj oynatılırken bir hata oluştu.")
      }
    }
  }

  fun currentPlayingKey(): String? = activePlayingKey

  fun stopPlayback() {
    synchronized(playerLock) {
      stopPlaybackInternal()
    }
  }

  private fun stopPlaybackInternal() {
    activePlayingKey = null
    activePlayer?.let { mp ->
      runCatching {
        if (mp.isPlaying) mp.stop()
      }
      runCatching { mp.reset() }
      runCatching { mp.release() }
    }
    activePlayer = null
  }

  fun formatDurationMmSs(durationMs: Long): String {
    val totalSec = ((durationMs + 499L) / 1000L).coerceIn(0L, 30L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "$mins:${secs.toString().padStart(2, '0')}"
  }
}
