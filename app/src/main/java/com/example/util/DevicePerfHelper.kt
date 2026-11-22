package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.io.File

/**
 * Detects legacy Android versions (API 24–25) or low-RAM / 2GB-class constrained hardware
 * so Vizyon Music can automatically reduce GPU/Canvas overdraw, cap image/disk caches
 * for 32GB storage devices, and keep transitions responsive.
 */
object DevicePerfHelper {
  private const val LOW_RAM_TOTAL_BYTES_THRESHOLD = 2_700L * 1024L * 1024L
  private const val MAX_DISK_CACHE_BYTES_CONSTRAINED = 20L * 1024L * 1024L

  @Volatile private var cachedSdk: Int = -1
  @Volatile private var cachedLowRam: Boolean? = null
  @Volatile private var cachedTotalMemBytes: Long = -1L

  fun totalDeviceRamBytes(context: Context): Long {
    if (cachedTotalMemBytes > 0L) return cachedTotalMemBytes
    val total =
      runCatching {
        val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo()
        if (am != null) {
          am.getMemoryInfo(info)
          info.totalMem
        } else {
          0L
        }
      }.getOrDefault(0L)
    if (total > 0L) {
      cachedTotalMemBytes = total
    }
    return total
  }

  fun isLowRamDevice(context: Context): Boolean {
    if (cachedSdk == Build.VERSION.SDK_INT) {
      cachedLowRam?.let { return it }
    }
    val am = runCatching {
      context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }.getOrNull()
    val totalMem = totalDeviceRamBytes(context)
    val lowRam =
      (am?.isLowRamDevice == true) ||
        (totalMem in 1L..LOW_RAM_TOTAL_BYTES_THRESHOLD)
    cachedSdk = Build.VERSION.SDK_INT
    cachedLowRam = lowRam
    return lowRam
  }

  fun isConstrainedDevice(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    if (isLowRamDevice(context)) return true
    return Runtime.getRuntime().availableProcessors() <= 4 && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
  }

  /**
   * Keeps disk storage lean on 32GB devices by removing expired generated story PNGs
   * and trimming oversized Coil disk caches in background.
   */
  fun pruneLegacyCachesIfNeeded(context: Context) {
    runCatching {
      val cacheRoot = context.applicationContext.cacheDir ?: return
      val now = System.currentTimeMillis()

      val storyDir = File(cacheRoot, "story_cards")
      if (storyDir.exists()) {
        val storyFiles = storyDir.listFiles()?.sortedByDescending { it.lastModified() }.orEmpty()
        storyFiles.forEachIndexed { index, file ->
          if (index >= 4 || (now - file.lastModified() > 12L * 60L * 60L * 1000L)) {
            runCatching { file.delete() }
          }
        }
      }

      if (isConstrainedDevice(context)) {
        val coilDir = File(cacheRoot, "coil_image_cache")
        if (coilDir.exists()) {
          val files = coilDir.walkTopDown().filter { it.isFile }.toList()
          var totalSize = files.sumOf { it.length() }
          if (totalSize > MAX_DISK_CACHE_BYTES_CONSTRAINED) {
            files.sortedBy { it.lastModified() }.forEach { f ->
              if (totalSize <= MAX_DISK_CACHE_BYTES_CONSTRAINED * 3 / 4) return@forEach
              val len = f.length()
              if (runCatching { f.delete() }.getOrDefault(false)) {
                totalSize -= len
              }
            }
          }
        }
      }
    }
  }
}
