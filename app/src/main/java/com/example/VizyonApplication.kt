package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.notifications.VizyonNotificationHelper
import com.example.security.PasscodeSecurityHelper
import com.example.util.DevicePerfHelper
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VizyonApplication : Application(), ImageLoaderFactory {
  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    if (FirebaseApp.getApps(this).isEmpty()) {
      runCatching { FirebaseApp.initializeApp(this) }
    }
    VizyonNotificationHelper.ensureChannel(this)
    appScope.launch {
      DevicePerfHelper.pruneLegacyCachesIfNeeded(applicationContext)
    }
  }

  override fun newImageLoader(): ImageLoader {
    val constrained = DevicePerfHelper.isConstrainedDevice(this)
    val memPercent = if (constrained) 0.10 else 0.20
    val diskBytes = if (constrained) 20L * 1024L * 1024L else 56L * 1024L * 1024L
    return ImageLoader.Builder(this)
      .allowRgb565(constrained)
      .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !constrained)
      .crossfade(if (constrained) 0 else 160)
      .memoryCache {
        MemoryCache.Builder(this)
          .maxSizePercent(memPercent)
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve("coil_image_cache"))
          .maxSizeBytes(diskBytes)
          .build()
      }
      .build()
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
      trimVolatileMemoryCaches(aggressive = level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
    }
  }

  override fun onLowMemory() {
    super.onLowMemory()
    trimVolatileMemoryCaches(aggressive = true)
  }

  private fun trimVolatileMemoryCaches(aggressive: Boolean) {
    runCatching {
      val imageLoader = Coil.imageLoader(this)
      if (aggressive) {
        imageLoader.memoryCache?.clear()
      } else {
        imageLoader.memoryCache?.trimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
      }
    }
    if (aggressive) {
      PasscodeSecurityHelper.clearBitmapCache()
    }
  }
}
