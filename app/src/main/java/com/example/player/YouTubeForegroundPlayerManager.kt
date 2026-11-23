package com.example.player

import android.webkit.WebView
import com.example.model.MusicTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference
import java.util.Locale

enum class RepeatMode {
  NONE,
  ALL,
  ONE
}

data class PlaybackUiState(
  val currentTrack: MusicTrack? = null,
  val queue: List<MusicTrack> = emptyList(),
  val currentIndex: Int = -1,
  val isPlaying: Boolean = false,
  val isResolvingVideo: Boolean = false,
  val isPlayerReady: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val shuffleEnabled: Boolean = false,
  val repeatMode: RepeatMode = RepeatMode.NONE,
  val playbackMessage: String? = null,
  val activeVideoId: String = ""
)

/**
 * Foreground-only YouTube IFrame playback state controller.
 * Never runs a background service or audio extraction; pauses automatically
 * when the app goes to background or when the Full Player video surface is closed.
 */
class YouTubeForegroundPlayerManager {
  private val _state = MutableStateFlow(PlaybackUiState())
  val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

  private var attachedWebViewRef: WeakReference<WebView>? = null
  private val attachedWebView: WebView?
    get() = attachedWebViewRef?.get()
  private var onTrackStartedCallback: ((MusicTrack) -> Unit)? = null
  private var onRequestTrackPlay: ((MusicTrack, List<MusicTrack>) -> Unit)? = null

  fun attachWebView(webView: WebView?) {
    attachedWebViewRef = webView?.let { WeakReference(it) }
    if (webView == null) {
      _state.update { it.copy(isPlayerReady = false) }
    }
  }

  fun onHostResumed() {
    runCatching { attachedWebView?.onResume() }
  }

  fun setOnTrackStartedListener(listener: (MusicTrack) -> Unit) {
    onTrackStartedCallback = listener
  }

  fun setOnRequestTrackPlayListener(listener: (MusicTrack, List<MusicTrack>) -> Unit) {
    onRequestTrackPlay = listener
  }

  fun prepareLoading(track: MusicTrack, queue: List<MusicTrack> = listOf(track)) {
    val safeQueue = if (queue.any { it.trackId == track.trackId }) queue else listOf(track) + queue
    val idx = safeQueue.indexOfFirst { it.trackId == track.trackId }.coerceAtLeast(0)
    val previousVideoId = _state.value.activeVideoId
    _state.update {
      it.copy(
        currentTrack = track,
        queue = safeQueue,
        currentIndex = idx,
        positionMs = 0L,
        durationMs = track.durationMs.coerceAtLeast(1000L),
        isResolvingVideo = true,
        isPlayerReady =
          if (track.youtubeVideoId.isNotBlank() && track.youtubeVideoId == previousVideoId) {
            it.isPlayerReady
          } else {
            false
          },
        isPlaying = false,
        playbackMessage = null,
        activeVideoId = track.youtubeVideoId
      )
    }
    onTrackStartedCallback?.invoke(track)
  }

  fun onVideoResolved(track: MusicTrack, queue: List<MusicTrack>) {
    val safeQueue =
      queue.map { if (it.trackId == track.trackId) track else it }.ifEmpty { listOf(track) }
    val idx = safeQueue.indexOfFirst { it.trackId == track.trackId }.coerceAtLeast(0)
    _state.update {
      it.copy(
        currentTrack = track,
        queue = safeQueue,
        currentIndex = idx,
        durationMs = track.durationMs.coerceAtLeast(it.durationMs).coerceAtLeast(1000L),
        isResolvingVideo = false,
        activeVideoId = track.youtubeVideoId,
        playbackMessage = null
      )
    }
  }

  fun onVideoResolutionFailed(message: String) {
    _state.update {
      it.copy(
        isResolvingVideo = false,
        isPlayerReady = false,
        isPlaying = false,
        playbackMessage = message
      )
    }
  }

  fun onIFrameReady(videoId: String) {
    _state.update { it.copy(isPlayerReady = true, playbackMessage = null) }
    val active = _state.value.activeVideoId
    if (active.isNotBlank() && (videoId.isBlank() || videoId == active)) {
      evaluateJs("window.vizyonPlay && window.vizyonPlay();")
    } else if (active.isNotBlank()) {
      evaluateJs("window.vizyonLoadAndPlay && window.vizyonLoadAndPlay('${safeJs(active)}');")
    }
  }

  fun onIFrameStateChanged(ytStateCode: Int) {
    // YouTube IFrame Player states: -1 unstarted, 0 ended, 1 playing, 2 paused, 3 buffering, 5 cued
    when (ytStateCode) {
      1 -> _state.update { it.copy(isPlaying = true, isPlayerReady = true, isResolvingVideo = false, playbackMessage = null) }
      2 -> _state.update { it.copy(isPlaying = false, isPlayerReady = true) }
      3 -> _state.update { it.copy(isPlayerReady = true, isResolvingVideo = false) }
      5 -> _state.update { it.copy(isPlayerReady = true, isResolvingVideo = false) }
      0 -> handleTrackEnded()
    }
  }

  fun onIFrameProgress(currentSec: Double, durationSec: Double) {
    val posMs = (currentSec * 1000.0).toLong().coerceAtLeast(0L)
    val durMs = (durationSec * 1000.0).toLong()
    _state.update { st ->
      st.copy(
        positionMs = posMs,
        durationMs = if (durMs > 1000L) durMs else st.durationMs
      )
    }
  }

  fun onIFrameError(errorCode: Int) {
    val reason =
      when (errorCode) {
        2 -> "Geçersiz video kimliği veya istek parametresi (Hata Kodu: 2)."
        5 -> "HTML5 video oynatıcısı bu içeriği oynatamadı (Hata Kodu: 5)."
        100 -> "YouTube videosu bulunamadı, kaldırılmış veya gizli (Hata Kodu: 100)."
        101, 150, 152, 153 ->
          "Bu video sahibi tarafından gömülü (embed) oynatmaya kısıtlı (Hata Kodu: $errorCode)."
        else -> "Resmi YouTube oynatıcısı video yükleyemedi (Hata Kodu: $errorCode)."
      }
    _state.update {
      it.copy(
        isPlaying = false,
        isPlayerReady = false,
        isResolvingVideo = false,
        playbackMessage = reason
      )
    }
  }

  fun onIFrameLoadFailure(reason: String) {
    _state.update {
      it.copy(
        isPlaying = false,
        isPlayerReady = false,
        isResolvingVideo = false,
        playbackMessage = reason.ifBlank { "YouTube IFrame oynatıcısı yüklenemedi." }
      )
    }
  }

  fun togglePlayPause() {
    val current = _state.value
    if (current.activeVideoId.isBlank()) return
    if (current.isPlaying) {
      evaluateJs("window.vizyonPause && window.vizyonPause();")
      _state.update { it.copy(isPlaying = false) }
    } else {
      evaluateJs("window.vizyonPlay && window.vizyonPlay();")
      _state.update { it.copy(isPlaying = true) }
    }
  }

  fun seekTo(positionMs: Long) {
    val clampedMs = positionMs.coerceIn(0L, _state.value.durationMs.coerceAtLeast(1000L))
    val sec = clampedMs / 1000.0
    evaluateJs(String.format(Locale.US, "window.vizyonSeek && window.vizyonSeek(%.2f);", sec))
    _state.update { it.copy(positionMs = clampedMs) }
  }

  fun skipNext() {
    val cur = _state.value
    if (cur.queue.isEmpty()) return
    val nextIdx =
      if (cur.shuffleEnabled && cur.queue.size > 1) {
        cur.queue.indices.filter { it != cur.currentIndex }.randomOrNull() ?: 0
      } else {
        (cur.currentIndex + 1) % cur.queue.size
      }
    val nextTrack = cur.queue.getOrNull(nextIdx) ?: return
    onRequestTrackPlay?.invoke(nextTrack, cur.queue)
  }

  fun skipPrevious() {
    val cur = _state.value
    if (cur.queue.isEmpty()) return
    if (cur.positionMs > 4000L) {
      seekTo(0L)
      return
    }
    val prevIdx = if (cur.currentIndex - 1 < 0) cur.queue.lastIndex else cur.currentIndex - 1
    val prevTrack = cur.queue.getOrNull(prevIdx) ?: return
    onRequestTrackPlay?.invoke(prevTrack, cur.queue)
  }

  fun toggleShuffle() {
    _state.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
  }

  fun cycleRepeatMode() {
    _state.update {
      val next =
        when (it.repeatMode) {
          RepeatMode.NONE -> RepeatMode.ALL
          RepeatMode.ALL -> RepeatMode.ONE
          RepeatMode.ONE -> RepeatMode.NONE
        }
      it.copy(repeatMode = next)
    }
  }

  fun addToQueue(track: MusicTrack) {
    _state.update { st ->
      if (st.queue.any { it.trackId == track.trackId }) st
      else st.copy(queue = st.queue + track)
    }
  }

  /**
   * Enforces disabled background playback: stops YouTube IFrame when app leaves foreground.
   */
  fun pauseForBackground() {
    if (_state.value.isPlaying || attachedWebView != null) {
      evaluateJs("window.vizyonPause && window.vizyonPause();")
      runCatching { attachedWebView?.onPause() }
      _state.update { it.copy(isPlaying = false) }
    }
  }

  /**
   * Preserves live foreground playback inside the compact MiniPlayer video viewport
   * when minimizing NowPlayingModal; only explicit user pause or background transition stops video.
   */
  fun pauseWhenMinimized() {
    // Compact MiniPlayerBar renders a visible live video viewport in the album slot,
    // so foreground playback continues smoothly while browsing.
  }

  fun dismissMessage() {
    _state.update { it.copy(playbackMessage = null) }
  }

  fun stopAndClear() {
    evaluateJs("window.vizyonPause && window.vizyonPause();")
    runCatching { attachedWebView?.onPause() }
    _state.value = PlaybackUiState()
  }

  fun resetAllForAccountDeletion() {
    evaluateJs("window.vizyonPause && window.vizyonPause();")
    attachedWebViewRef = null
    _state.value = PlaybackUiState()
  }

  private fun handleTrackEnded() {
    val cur = _state.value
    when (cur.repeatMode) {
      RepeatMode.ONE -> {
        seekTo(0L)
        evaluateJs("window.vizyonPlay && window.vizyonPlay();")
      }
      RepeatMode.ALL -> {
        val nextIdx = (cur.currentIndex + 1) % cur.queue.size.coerceAtLeast(1)
        val nextTrack = cur.queue.getOrNull(nextIdx)
        if (nextTrack != null) {
          onRequestTrackPlay?.invoke(nextTrack, cur.queue)
        }
      }
      RepeatMode.NONE -> {
        val nextTrack = cur.queue.getOrNull(cur.currentIndex + 1)
        if (nextTrack != null) {
          onRequestTrackPlay?.invoke(nextTrack, cur.queue)
        } else {
          _state.update { it.copy(isPlaying = false, positionMs = it.durationMs) }
        }
      }
    }
  }

  private fun evaluateJs(script: String) {
    val wv = attachedWebView ?: return
    runCatching {
      wv.post {
        runCatching { attachedWebView?.evaluateJavascript(script, null) }
      }
    }
  }

  private fun safeJs(raw: String): String =
    raw.replace(Regex("[^a-zA-Z0-9_-]"), "")
}
