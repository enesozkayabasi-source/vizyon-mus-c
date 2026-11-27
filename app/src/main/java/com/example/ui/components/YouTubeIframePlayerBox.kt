package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * Creates a single shared foreground WebView for the active session so switching between
 * the MiniPlayer compact live video slot and fullscreen NowPlayingModal continues playback
 * with zero re-buffering or timestamp reset.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun rememberSharedYouTubeWebView(
  initialVideoId: String,
  onAttachWebView: (WebView?) -> Unit,
  onIFrameReady: (String) -> Unit,
  onIFrameStateChanged: (Int) -> Unit,
  onIFrameProgress: (Double, Double) -> Unit,
  onIFrameError: (Int) -> Unit,
  onIFrameLoadFailure: (String) -> Unit
): WebView {
  val context = LocalContext.current
  val latestReady by rememberUpdatedState(onIFrameReady)
  val latestState by rememberUpdatedState(onIFrameStateChanged)
  val latestProgress by rememberUpdatedState(onIFrameProgress)
  val latestError by rememberUpdatedState(onIFrameError)
  val latestLoadFailure by rememberUpdatedState(onIFrameLoadFailure)
  val latestAttach by rememberUpdatedState(onAttachWebView)

  val mainHandler = remember { Handler(Looper.getMainLooper()) }

  val destroyedRef = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

  val webView = remember(context) {
    val bridge =
      object {
        @JavascriptInterface
        fun onReady(vid: String) {
          if (!destroyedRef.get()) mainHandler.post { latestReady(vid) }
        }

        @JavascriptInterface
        fun onStateChange(stateCode: Int) {
          if (!destroyedRef.get()) mainHandler.post { latestState(stateCode) }
        }

        @JavascriptInterface
        fun onProgress(currentSec: Double, durationSec: Double) {
          if (!destroyedRef.get()) mainHandler.post { latestProgress(currentSec, durationSec) }
        }

        @JavascriptInterface
        fun onError(code: Int) {
          if (!destroyedRef.get()) mainHandler.post { latestError(code) }
        }

        @JavascriptInterface
        fun onLoadFailure(reason: String) {
          if (!destroyedRef.get()) mainHandler.post { latestLoadFailure(reason) }
        }
      }

    WebView(context).apply {
      setBackgroundColor(AndroidColor.BLACK)
      isVerticalScrollBarEnabled = false
      isHorizontalScrollBarEnabled = false
      overScrollMode = View.OVER_SCROLL_NEVER
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true
      settings.mediaPlaybackRequiresUserGesture = false
      settings.javaScriptCanOpenWindowsAutomatically = true
      settings.loadsImagesAutomatically = true
      settings.useWideViewPort = false
      settings.loadWithOverviewMode = false
      settings.setSupportZoom(false)
      settings.builtInZoomControls = false
      settings.displayZoomControls = false
      settings.textZoom = 100
      settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
      runCatching {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
      }
      addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
        val wPx = right - left
        val hPx = bottom - top
        if (wPx > 0 && hPx > 0) {
          val density = view.resources.displayMetrics.density.coerceAtLeast(1f)
          val cssW = wPx / density
          val cssH = hPx / density
          view.scrollTo(0, 0)
          view.post {
            runCatching {
              (view as? WebView)?.evaluateJavascript(
                "window.vizyonSyncStage && window.vizyonSyncStage(null, $cssW, $cssH);",
                null
              )
            }
          }
        }
      }
      webChromeClient = WebChromeClient()
      webViewClient =
        object : WebViewClient() {
          override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
          ): Boolean {
            val host = request?.url?.host.orEmpty()
            return !(
              host.contains("youtube.com") ||
                host.contains("youtu.be") ||
                host.contains("youtube-nocookie.com") ||
                host.contains("googlevideo.com")
            )
          }

          override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
          ) {
            val url = request?.url?.toString().orEmpty()
            val isCriticalFrame =
              request?.isForMainFrame == true ||
                url.contains("youtube.com/iframe_api", ignoreCase = true) ||
                url.contains("youtube.com/embed", ignoreCase = true)
            if (isCriticalFrame && error != null) {
              val desc = error.description?.toString().orEmpty().ifBlank { "Ağ bağlantı hatası" }
              mainHandler.post {
                latestLoadFailure("YouTube oynatıcı ağ hatası (Kod: ${error.errorCode}): $desc")
              }
            }
          }

          override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
          ) {
            val url = request?.url?.toString().orEmpty()
            val statusCode = errorResponse?.statusCode ?: return
            val isRelevant =
              (request?.isForMainFrame == true ||
                url.contains("youtube.com/embed", ignoreCase = true) ||
                url.contains("youtube.com/iframe_api", ignoreCase = true)) &&
                statusCode >= 400
            if (isRelevant) {
              mainHandler.post {
                latestLoadFailure("YouTube oynatıcı HTTP hatası (HTTP $statusCode).")
              }
            }
          }

          override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?
          ): Boolean {
            mainHandler.post {
              runCatching {
                (view?.parent as? ViewGroup)?.removeView(view)
                view?.removeJavascriptInterface("VizyonYtBridge")
                view?.destroy()
              }
              latestAttach(null)
              latestLoadFailure("Video oynatıcı bellek nedeniyle sıfırlandı. Tekrar dene.")
            }
            return true
          }
        }
      addJavascriptInterface(bridge, "VizyonYtBridge")
      val sanitizedInitial = sanitizeVideoId(initialVideoId)
      tag = sanitizedInitial
      loadDataWithBaseURL(
        "https://youtube.com",
        buildYouTubeIframeHtml(sanitizedInitial),
        "text/html",
        "UTF-8",
        null
      )
    }
  }

  DisposableEffect(webView) {
    destroyedRef.set(false)
    latestAttach(webView)
    onDispose {
      destroyedRef.set(true)
      mainHandler.removeCallbacksAndMessages(null)
      latestAttach(null)
      runCatching {
        (webView.parent as? ViewGroup)?.let { parent ->
          runCatching { parent.endViewTransition(webView) }
          parent.removeView(webView)
        }
        webView.removeJavascriptInterface("VizyonYtBridge")
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
      }
    }
  }

  return webView
}

/**
 * Visible YouTube IFrame viewport supporting both compact MiniPlayer cover slot mode
 * and fullscreen NowPlayingModal stage mode using the shared foreground WebView.
 */
@Composable
fun YouTubeIframePlayerBox(
  sharedWebView: WebView,
  videoId: String,
  isResolvingVideo: Boolean,
  isPlayerReady: Boolean,
  errorMessage: String?,
  onIFrameLoadFailure: (String) -> Unit,
  onRetryVideo: () -> Unit,
  modifier: Modifier = Modifier,
  compactMode: Boolean = false,
  onCompactClick: (() -> Unit)? = null
) {
  val latestLoadFailure by rememberUpdatedState(onIFrameLoadFailure)

  LaunchedEffect(videoId, isResolvingVideo, isPlayerReady, errorMessage) {
    if (!isResolvingVideo && videoId.isNotBlank() && !isPlayerReady && errorMessage == null) {
      delay(12_000L)
      latestLoadFailure(
        "YouTube IFrame oynatıcısı zaman aşımına uğradı (Video ID: $videoId). İnternet bağlantısını kontrol edip tekrar deneyin."
      )
    }
  }

  val shape = RoundedCornerShape(if (compactMode) 10.dp else 22.dp)
  val boxModifier =
    if (compactMode) {
      modifier
        .clip(shape)
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), shape)
        .testTag("youtube_mini_video_viewport")
    } else {
      modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(shape)
        .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), shape)
        .testTag("youtube_iframe_player_box")
    }

  Surface(
    color = Color.Black,
    shape = shape,
    tonalElevation = if (compactMode) 2.dp else 8.dp,
    modifier = boxModifier
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      AndroidView(
        factory = {
          runCatching {
            (sharedWebView.parent as? ViewGroup)?.let { parent ->
              runCatching { parent.endViewTransition(sharedWebView) }
              parent.removeView(sharedWebView)
            }
          }
          sharedWebView.layoutParams =
            ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
          sharedWebView.scrollTo(0, 0)
          sharedWebView
        },
        update = { wv ->
          val sanitized = sanitizeVideoId(videoId)
          val previous = wv.tag as? String
          if (sanitized.isBlank()) {
            wv.tag = ""
          } else if (sanitized != previous) {
            wv.tag = sanitized
            runCatching {
              wv.evaluateJavascript("window.vizyonLoadAndPlay && window.vizyonLoadAndPlay('$sanitized');", null)
            }
          }
          wv.scrollTo(0, 0)
          val density = wv.resources.displayMetrics.density.coerceAtLeast(1f)
          val cssW = if (wv.width > 0) wv.width / density else 0f
          val cssH = if (wv.height > 0) wv.height / density else 0f
          val compactFlag = if (compactMode) "true" else "false"
          val syncJsForView: (WebView) -> String = { web ->
            val d = web.resources.displayMetrics.density.coerceAtLeast(1f)
            val wCss = if (web.width > 0) web.width / d else 0f
            val hCss = if (web.height > 0) web.height / d else 0f
            if (wCss > 4f && hCss > 4f) {
              "window.vizyonSyncStage && window.vizyonSyncStage($compactFlag, $wCss, $hCss);"
            } else {
              "window.vizyonSyncStage && window.vizyonSyncStage($compactFlag);"
            }
          }
          runCatching { wv.evaluateJavascript(syncJsForView(wv), null) }
          listOf(90L, 220L, 380L).forEach { delayMs ->
            wv.postDelayed(
              { runCatching { wv.evaluateJavascript(syncJsForView(wv), null) } },
              delayMs
            )
          }
        },
        modifier = Modifier.fillMaxSize()
      )

      if (errorMessage != null || (!isResolvingVideo && videoId.isBlank())) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.fillMaxSize()
              .background(Color(0xE6050914))
              .padding(if (compactMode) 4.dp else 16.dp)
              .testTag("youtube_player_error_box")
        ) {
          if (compactMode) {
            Icon(
              imageVector = Icons.Rounded.ErrorOutline,
              contentDescription = errorMessage ?: "Video hatası",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp)
            )
          } else {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = "Oynatma hatası",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(34.dp)
              )
              Text(
                text = errorMessage ?: "Bu parça için resmi YouTube videosu henüz eşleştirilemedi.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
              )
              Spacer(modifier = Modifier.height(2.dp))
              StudioGlassButton(
                text = "Tekrar Dene / Yeniden Eşleştir",
                icon = Icons.Rounded.Refresh,
                onClick = onRetryVideo,
                tintColor = Color.White,
                modifier = Modifier.testTag("retry_youtube_video_btn")
              )
            }
          }
        }
      } else if (isResolvingVideo || (videoId.isNotBlank() && !isPlayerReady)) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.fillMaxSize()
              .background(Color(0xBB050914))
              .testTag("youtube_player_loading_box")
        ) {
          if (compactMode) {
            CircularProgressIndicator(
              color = MaterialTheme.colorScheme.primary,
              strokeWidth = 2.dp,
              modifier = Modifier.size(18.dp)
            )
          } else {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
              )
              Text(
                text =
                  if (isResolvingVideo) "Resmi YouTube videosu eşleştiriliyor…"
                  else "YouTube IFrame oynatıcısı hazırlanıyor…",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
              )
            }
          }
        }
      }

      if (compactMode && onCompactClick != null) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .clickable(onClick = onCompactClick)
        )
      }
    }
  }
}

private fun sanitizeVideoId(raw: String): String =
  raw.replace(Regex("[^a-zA-Z0-9_-]"), "")

private fun buildYouTubeIframeHtml(initialVideoId: String): String =
  """
  <!DOCTYPE html>
  <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no">
      <style>
        * {
          box-sizing: border-box;
        }
        html, body {
          margin: 0;
          padding: 0;
          width: 100%;
          height: 100%;
          background: #000000;
          overflow: hidden;
        }
        #stage {
          position: fixed;
          inset: 0;
          width: 100%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #000000;
          overflow: hidden;
        }
        #player-frame {
          position: relative;
          width: 100%;
          height: 100%;
          background: #000000;
          overflow: hidden;
          transform-origin: center center;
          flex-shrink: 0;
        }
        #player, #player iframe {
          position: absolute !important;
          inset: 0 !important;
          width: 100% !important;
          height: 100% !important;
          max-width: 100% !important;
          max-height: 100% !important;
          display: block;
          border: 0;
        }
      </style>
    </head>
    <body>
      <div id="stage">
        <div id="player-frame">
          <div id="player"></div>
        </div>
      </div>
      <script>
        var ytPlayer = null;
        var ytApiReady = false;
        var isYtReady = false;
        var currentVid = "$initialVideoId";
        var progressTimer = null;
        var lastHintW = 0;
        var lastHintH = 0;
        var lastCompact = null;

        function syncStageScale(isCompact, hintW, hintH) {
          var stage = document.getElementById('stage');
          var frame = document.getElementById('player-frame');
          if (!stage || !frame) return;

          if (typeof isCompact === 'boolean' && lastCompact !== isCompact) {
            lastCompact = isCompact;
            if (!(typeof hintW === 'number' && hintW > 4)) lastHintW = 0;
            if (!(typeof hintH === 'number' && hintH > 4)) lastHintH = 0;
          }

          if (typeof hintW === 'number' && hintW > 4) lastHintW = hintW;
          if (typeof hintH === 'number' && hintH > 4) lastHintH = hintH;

          var measuredW = Math.max(window.innerWidth || 0, document.documentElement.clientWidth || 0, stage.clientWidth || 0);
          var measuredH = Math.max(window.innerHeight || 0, document.documentElement.clientHeight || 0, stage.clientHeight || 0);
          var vw = (lastHintW > 4) ? lastHintW : Math.max(measuredW, 1);
          var vh = (lastHintH > 4) ? lastHintH : Math.max(measuredH, 1);

          stage.style.width = '100%';
          stage.style.height = '100%';

          if (lastCompact === true && vw > 4 && vh > 4) {
            var virtualW = 320;
            var virtualH = 180;
            var fitScale = Math.min(vw / virtualW, vh / virtualH);
            frame.style.width = virtualW + 'px';
            frame.style.height = virtualH + 'px';
            frame.style.transformOrigin = 'center center';
            frame.style.transform = 'scale(' + fitScale.toFixed(5) + ')';
            if (ytPlayer && isYtReady && typeof ytPlayer.setSize === 'function') {
              try {
                ytPlayer.setSize(virtualW, virtualH);
              } catch (e) {}
            }
          } else {
            frame.style.transform = 'none';
            frame.style.width = '100%';
            frame.style.height = '100%';
            if (ytPlayer && isYtReady && typeof ytPlayer.setSize === 'function' && vw > 4 && vh > 4) {
              try {
                ytPlayer.setSize(Math.round(vw), Math.round(vh));
              } catch (e) {}
            }
          }
          var iframe = document.querySelector('#player iframe');
          if (iframe) {
            iframe.style.width = '100%';
            iframe.style.height = '100%';
          }
        }

        window.vizyonSyncStage = function(isCompact, hintW, hintH) {
          syncStageScale(isCompact, hintW, hintH);
        };

        window.addEventListener('resize', function() {
          lastHintW = 0;
          lastHintH = 0;
          syncStageScale(null);
        });

        if (typeof ResizeObserver === 'function') {
          try {
            var ro = new ResizeObserver(function(entries) {
              if (entries && entries.length > 0 && entries[0].contentRect) {
                var r = entries[0].contentRect;
                if (r.width > 4 && r.height > 4) {
                  syncStageScale(null, r.width, r.height);
                  return;
                }
              }
              syncStageScale(null);
            });
            ro.observe(document.documentElement);
          } catch (e) {}
        }

        syncStageScale(null);

        function startProgressLoop() {
          if (progressTimer) clearInterval(progressTimer);
          progressTimer = setInterval(function() {
            if (ytPlayer && isYtReady && typeof ytPlayer.getCurrentTime === 'function') {
              try {
                var st = ytPlayer.getPlayerState();
                if (st === 1 || st === 3) {
                  var cur = ytPlayer.getCurrentTime() || 0;
                  var dur = ytPlayer.getDuration() || 0;
                  VizyonYtBridge.onProgress(cur, dur);
                }
              } catch (e) {}
            }
          }, 450);
        }

        function createOrLoadPlayer(vid) {
          if (!vid) return;
          currentVid = vid;
          if (!ytApiReady || typeof YT === 'undefined' || typeof YT.Player !== 'function') {
            return;
          }
          if (ytPlayer && isYtReady && typeof ytPlayer.loadVideoById === 'function') {
            try {
              ytPlayer.loadVideoById(currentVid);
              if (typeof ytPlayer.playVideo === 'function') {
                ytPlayer.playVideo();
              }
              syncStageScale(null);
              return;
            } catch (e) {
              VizyonYtBridge.onLoadFailure("YouTube video yükleme hatası: " + (e.message || e));
              return;
            }
          }
          if (ytPlayer && !isYtReady) {
            try { ytPlayer.destroy(); } catch (e) {}
            ytPlayer = null;
          }
          try {
            ytPlayer = new YT.Player('player', {
              width: '100%',
              height: '100%',
              videoId: currentVid,
              playerVars: {
                autoplay: 1,
                playsinline: 1,
                enablejsapi: 1,
                origin: 'https://youtube.com',
                rel: 0,
                modestbranding: 1,
                fs: 0,
                iv_load_policy: 3
              },
              events: {
                onReady: function() {
                  isYtReady = true;
                  syncStageScale(null);
                  startProgressLoop();
                  VizyonYtBridge.onReady(currentVid);
                  if (typeof ytPlayer.playVideo === 'function') {
                    ytPlayer.playVideo();
                  }
                },
                onStateChange: function(event) {
                  syncStageScale(null);
                  VizyonYtBridge.onStateChange(event.data);
                },
                onError: function(event) {
                  VizyonYtBridge.onError(event.data);
                }
              }
            });
            syncStageScale(null);
          } catch (e) {
            VizyonYtBridge.onLoadFailure("YouTube IFrame başlatılamadı: " + (e.message || e));
          }
        }

        function onYouTubeIframeAPIReady() {
          ytApiReady = true;
          syncStageScale(null);
          if (currentVid) {
            createOrLoadPlayer(currentVid);
          }
        }

        window.vizyonLoadAndPlay = function(vid) {
          if (!vid) return;
          createOrLoadPlayer(vid);
        };
        window.vizyonPlay = function() {
          if (ytPlayer && isYtReady && typeof ytPlayer.playVideo === 'function') ytPlayer.playVideo();
        };
        window.vizyonPause = function() {
          if (ytPlayer && isYtReady && typeof ytPlayer.pauseVideo === 'function') ytPlayer.pauseVideo();
        };
        window.vizyonSeek = function(sec) {
          if (ytPlayer && isYtReady && typeof ytPlayer.seekTo === 'function') ytPlayer.seekTo(sec, true);
        };
      </script>
      <script
        src="https://www.youtube.com/iframe_api"
        onerror="VizyonYtBridge.onLoadFailure('YouTube IFrame API betiği yüklenemedi. İnternet bağlantısını kontrol edin.');">
      </script>
    </body>
  </html>
  """.trimIndent()
