package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Ultra-soft organic matte water droplet & gentle "pıt" sound engine:
 * - Zero white noise / static hiss and zero shrill metallic upper harmonics.
 * - Focused in the warm 250-640 Hz smartphone micro-speaker sweet spot.
 * - Uses SoundPool (with serialized AudioTrack fallback) for zero-lag, crash-free playback.
 */
object StudioSoundEngine {
  private const val SAMPLE_RATE = 44_100
  private const val SEND_DURATION_SECONDS = 0.088
  private const val RECEIVE_DURATION_SECONDS = 0.116
  private const val NOTIF_DURATION_SECONDS = 0.360

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val audioMutex = Mutex()
  private val poolLock = Any()

  private val softSendPcm: ShortArray by lazy { synthesizeSoftSendPit() }
  private val softReceivePcm: ShortArray by lazy { synthesizeSoftWaterDropReceive() }
  private val softNotifPcm: ShortArray by lazy { synthesizeSoftDoubleDropletNotification() }

  @Volatile private var soundPool: SoundPool? = null
  @Volatile private var sendSoundId: Int = 0
  @Volatile private var receiveSoundId: Int = 0
  @Volatile private var notifSoundId: Int = 0
  @Volatile private var sendReady: Boolean = false
  @Volatile private var receiveReady: Boolean = false
  @Volatile private var notifReady: Boolean = false

  fun warmup(context: Context) {
    ensureSoundPool(context.applicationContext)
  }

  fun playMessageSendChime(context: Context? = null) {
    if (isSilentMode(context)) return
    if (context != null) {
      val sp = ensureSoundPool(context.applicationContext)
      if (sp != null && sendSoundId != 0 && sendReady) {
        val stream = runCatching { sp.play(sendSoundId, 0.78f, 0.78f, 1, 0, 1.0f) }.getOrDefault(0)
        if (stream != 0) return
      }
    }
    scope.launch {
      playPcmSerialized(softSendPcm, SEND_DURATION_SECONDS)
    }
  }

  fun playMessageReceiveChime(context: Context? = null) {
    if (isSilentMode(context)) return
    if (context != null) {
      val sp = ensureSoundPool(context.applicationContext)
      if (sp != null && receiveSoundId != 0 && receiveReady) {
        val stream = runCatching { sp.play(receiveSoundId, 0.78f, 0.78f, 1, 0, 1.0f) }.getOrDefault(0)
        if (stream != 0) return
      }
    }
    scope.launch {
      playPcmSerialized(softReceivePcm, RECEIVE_DURATION_SECONDS)
    }
  }

  fun playNotificationPreviewChime(context: Context? = null) {
    if (isSilentMode(context)) return
    if (context != null) {
      val sp = ensureSoundPool(context.applicationContext)
      if (sp != null && notifSoundId != 0 && notifReady) {
        val stream = runCatching { sp.play(notifSoundId, 0.80f, 0.80f, 1, 0, 1.0f) }.getOrDefault(0)
        if (stream != 0) return
      }
    }
    scope.launch {
      playPcmSerialized(softNotifPcm, NOTIF_DURATION_SECONDS)
    }
  }

  private fun isSilentMode(context: Context?): Boolean {
    if (context == null) return false
    val audioManager =
      runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
    return audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT
  }

  private fun ensureSoundPool(appCtx: Context): SoundPool? {
    synchronized(poolLock) {
      soundPool?.let { return it }
      return runCatching {
        val pool =
          SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            .build()
        pool.setOnLoadCompleteListener { _, sampleId, status ->
          if (status == 0) {
            if (sampleId == sendSoundId) sendReady = true
            if (sampleId == receiveSoundId) receiveReady = true
            if (sampleId == notifSoundId) notifReady = true
          }
        }
        val sendFile = ensureCachedWavFile(appCtx, "vizyon_soft_send_v3.wav", softSendPcm)
        val recvFile = ensureCachedWavFile(appCtx, "vizyon_soft_recv_v3.wav", softReceivePcm)
        val notifFile = ensureCachedWavFile(appCtx, "vizyon_soft_notif_v3.wav", softNotifPcm)
        if (sendFile != null) sendSoundId = pool.load(sendFile.absolutePath, 1)
        if (recvFile != null) receiveSoundId = pool.load(recvFile.absolutePath, 1)
        if (notifFile != null) notifSoundId = pool.load(notifFile.absolutePath, 1)
        soundPool = pool
        pool
      }.getOrNull()
    }
  }

  private fun ensureCachedWavFile(context: Context, fileName: String, pcm: ShortArray): File? =
    runCatching {
      val file = File(context.cacheDir, fileName)
      if (file.exists() && file.length() > 44L) return@runCatching file
      writeWavFile(file, pcm, SAMPLE_RATE)
      file
    }.getOrNull()

  private fun writeWavFile(file: File, pcm: ShortArray, sampleRate: Int) {
    val byteRate = sampleRate * 2
    val dataSize = pcm.size * 2
    val totalDataLen = dataSize + 36
    val header = ByteArray(44)
    header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
    header[4] = (totalDataLen and 0xff).toByte(); header[5] = ((totalDataLen shr 8) and 0xff).toByte()
    header[6] = ((totalDataLen shr 16) and 0xff).toByte(); header[7] = ((totalDataLen shr 24) and 0xff).toByte()
    header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
    header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
    header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
    header[20] = 1; header[21] = 0
    header[22] = 1; header[23] = 0
    header[24] = (sampleRate and 0xff).toByte(); header[25] = ((sampleRate shr 8) and 0xff).toByte()
    header[26] = ((sampleRate shr 16) and 0xff).toByte(); header[27] = ((sampleRate shr 24) and 0xff).toByte()
    header[28] = (byteRate and 0xff).toByte(); header[29] = ((byteRate shr 8) and 0xff).toByte()
    header[30] = ((byteRate shr 16) and 0xff).toByte(); header[31] = ((byteRate shr 24) and 0xff).toByte()
    header[32] = 2; header[33] = 0
    header[34] = 16; header[35] = 0
    header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
    header[40] = (dataSize and 0xff).toByte(); header[41] = ((dataSize shr 8) and 0xff).toByte()
    header[42] = ((dataSize shr 16) and 0xff).toByte(); header[43] = ((dataSize shr 24) and 0xff).toByte()

    FileOutputStream(file).use { out ->
      out.write(header)
      val payload = ByteArray(dataSize)
      for (i in pcm.indices) {
        val v = pcm[i].toInt()
        payload[i * 2] = (v and 0xff).toByte()
        payload[i * 2 + 1] = ((v shr 8) and 0xff).toByte()
      }
      out.write(payload)
    }
  }

  internal fun synthesizeAppleSwooshPop(): ShortArray = synthesizeSoftSendPit()

  internal fun synthesizeAppleMessagesSend(): ShortArray = synthesizeSoftSendPit()

  internal fun synthesizeAppleMessagesReceive(): ShortArray = synthesizeSoftWaterDropReceive()

  /**
   * Soft matte outgoing "pıt" droplet (~88ms):
   * velvety warm upward pitch bloom (255 -> 530 Hz), zero noise, zero harsh overtones.
   */
  internal fun synthesizeSoftSendPit(): ShortArray {
    val totalSamples = (SAMPLE_RATE * SEND_DURATION_SECONDS).toInt()
    val raw = DoubleArray(totalSamples)
    var phase = 0.0
    var warm2Phase = 0.0
    for (i in 0 until totalSamples) {
      val t = i / SAMPLE_RATE.toDouble()
      val glide = 1.0 - exp(-t * 36.0)
      val freq = 255.0 + 275.0 * glide
      phase += 2.0 * PI * freq / SAMPLE_RATE
      warm2Phase += 2.0 * PI * (freq * 2.0) / SAMPLE_RATE
      val attack = 0.5 * (1.0 - cos(PI * (t / 0.007).coerceAtMost(1.0)))
      val decay = exp(-t * 42.0)
      raw[i] = attack * decay * (0.96 * sin(phase) + 0.04 * sin(warm2Phase))
    }
    return normalizeAndWindow(raw, targetPeak = 0.46, fadeInSec = 0.006, fadeOutSec = 0.022)
  }

  /**
   * Soft matte incoming water droplet (~116ms):
   * organic liquid bubble rise (310 -> 595 Hz) with cushioned bell envelope and velvety tail.
   */
  internal fun synthesizeSoftWaterDropReceive(): ShortArray {
    val totalSamples = (SAMPLE_RATE * RECEIVE_DURATION_SECONDS).toInt()
    val raw = DoubleArray(totalSamples)
    var phase = 0.0
    var warm2Phase = 0.0
    for (i in 0 until totalSamples) {
      val t = i / SAMPLE_RATE.toDouble()
      val bubbleCurve = 1.0 - exp(-t * 31.0)
      val freq = 310.0 + 285.0 * bubbleCurve
      phase += 2.0 * PI * freq / SAMPLE_RATE
      warm2Phase += 2.0 * PI * (freq * 2.0) / SAMPLE_RATE
      val attack = 0.5 * (1.0 - cos(PI * (t / 0.010).coerceAtMost(1.0)))
      val decay = exp(-t * 29.0)
      raw[i] = attack * decay * (0.95 * sin(phase) + 0.05 * sin(warm2Phase))
    }
    return normalizeAndWindow(raw, targetPeak = 0.44, fadeInSec = 0.008, fadeOutSec = 0.028)
  }

  /**
   * Calm double water-drop notification chime (~360ms):
   * two soft, matte low-mid droplets (392->485 Hz, then 523->630 Hz), zero metallic glare.
   */
  internal fun synthesizeSoftDoubleDropletNotification(): ShortArray {
    val totalSamples = (SAMPLE_RATE * NOTIF_DURATION_SECONDS).toInt()
    val raw = DoubleArray(totalSamples)

    var p1 = 0.0
    for (i in 0 until totalSamples) {
      val t = i / SAMPLE_RATE.toDouble()
      val f1 = 392.0 + 93.0 * (1.0 - exp(-t * 26.0))
      p1 += 2.0 * PI * f1 / SAMPLE_RATE
      val attack = 0.5 * (1.0 - cos(PI * (t / 0.012).coerceAtMost(1.0)))
      val decay = exp(-t * 19.0)
      raw[i] += attack * decay * 0.90 * sin(p1)
    }

    val drop2Start = (0.130 * SAMPLE_RATE).toInt().coerceIn(0, totalSamples)
    var p2 = 0.0
    for (i in drop2Start until totalSamples) {
      val t2 = (i - drop2Start) / SAMPLE_RATE.toDouble()
      val f2 = 523.25 + 106.75 * (1.0 - exp(-t2 * 28.0))
      p2 += 2.0 * PI * f2 / SAMPLE_RATE
      val attack = 0.5 * (1.0 - cos(PI * (t2 / 0.012).coerceAtMost(1.0)))
      val decay = exp(-t2 * 15.0)
      raw[i] += attack * decay * 0.74 * sin(p2)
    }

    return normalizeAndWindow(raw, targetPeak = 0.48, fadeInSec = 0.010, fadeOutSec = 0.055)
  }

  private fun normalizeAndWindow(
    raw: DoubleArray,
    targetPeak: Double,
    fadeInSec: Double,
    fadeOutSec: Double
  ): ShortArray {
    var peak = 0.0
    for (v in raw) {
      val av = abs(v)
      if (av > peak) peak = av
    }
    val norm = if (peak > 0.0) targetPeak / peak else 1.0
    val totalSamples = raw.size
    val fadeInEnd = (SAMPLE_RATE * fadeInSec).toInt().coerceAtLeast(1)
    val fadeOutStart = (totalSamples - (SAMPLE_RATE * fadeOutSec).toInt()).coerceAtLeast(fadeInEnd)
    val pcm = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val inGain =
        if (i < fadeInEnd) 0.5 * (1.0 - cos(PI * i.toDouble() / fadeInEnd)) else 1.0
      val outGain =
        if (i >= fadeOutStart) {
          val u = (i - fadeOutStart).toDouble() / (totalSamples - fadeOutStart).coerceAtLeast(1)
          0.5 * (1.0 + cos(PI * u))
        } else {
          1.0
        }
      val sample = (raw[i] * norm * inGain * outGain * Short.MAX_VALUE).toInt()
      pcm[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return pcm
  }

  private suspend fun playPcmSerialized(pcm: ShortArray, durationSec: Double) {
    audioMutex.withLock {
      runCatching {
        val minBufferBytes =
          AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
          )
        if (minBufferBytes <= 0) return

        val pcmByteCount = pcm.size * 2
        val track =
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(maxOf(minBufferBytes, pcmByteCount))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
          if (track.state == AudioTrack.STATE_INITIALIZED) {
            track.write(pcm, 0, pcm.size)
            track.setNotificationMarkerPosition(pcm.size)
            track.play()
            kotlinx.coroutines.delay(((durationSec * 1000.0) + 40.0).toLong())
            runCatching { track.stop() }
          }
        } finally {
          runCatching { track.release() }
        }
      }
    }
  }
}
