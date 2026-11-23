package com.example.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom

object PasscodeSecurityHelper {
  private val bitmapCache = object : LruCache<String, ImageBitmap>(12) {}

  fun clearBitmapCache() {
    runCatching { bitmapCache.evictAll() }
  }

  data class PasscodeDigest(
    val hashHex: String,
    val saltBase64: String
  )

  fun createPasscodeDigest(passcode: String): PasscodeDigest {
    val salt = generateSalt()
    val hash = hashPasscode(passcode, salt)
    return PasscodeDigest(hashHex = hash, saltBase64 = salt)
  }

  fun generateSalt(): String {
    val bytes = ByteArray(16)
    SecureRandom().nextBytes(bytes)
    return java.util.Base64.getEncoder().encodeToString(bytes)
  }

  fun hashPasscode(passcode: String, salt: String): String {
    val normalized = passcode.trim()
    val digest = MessageDigest.getInstance("SHA-256")
    var current = "$salt:$normalized".toByteArray(Charsets.UTF_8)
    repeat(512) {
      current = digest.digest(current)
    }
    return java.util.Base64.getEncoder().encodeToString(current)
  }

  fun verifyPasscode(input: String, expectedHash: String, salt: String): Boolean {
    if (input.isBlank() || salt.isBlank() || expectedHash.isBlank()) return false
    val candidate = hashPasscode(input, salt)
    if (candidate.length != expectedHash.length) return false
    var diff = 0
    for (i in candidate.indices) {
      diff = diff or (candidate[i].code xor expectedHash[i].code)
    }
    return diff == 0
  }

  fun passcodeStrengthLabel(passcode: String): Pair<String, Float> {
    val clean = passcode.trim()
    if (clean.length < 4) return "En az 4 karakter girin" to 0.15f
    var score = 0.35f
    if (clean.length >= 6) score += 0.25f
    if (clean.any { it.isLetter() } && clean.any { it.isDigit() }) score += 0.20f
    if (clean.any { !it.isLetterOrDigit() } || clean.length >= 8) score += 0.20f
    val clamped = score.coerceIn(0.2f, 1.0f)
    val label =
      when {
        clamped >= 0.85f -> "Çok Güçlü"
        clamped >= 0.60f -> "Güçlü"
        else -> "Orta"
      }
    return label to clamped
  }

  suspend fun compressAvatarToDataUrl(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
      try {
        val resolver = context.applicationContext.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return@withContext null

        val targetSize = 160
        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > targetSize * 2 && boundsOpts.outHeight / sampleSize > targetSize * 2) {
          sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
          inSampleSize = sampleSize
          inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded =
          resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            ?: return@withContext null

        val minDim = minOf(decoded.width, decoded.height).coerceAtLeast(1)
        val cropX = ((decoded.width - minDim) / 2).coerceAtLeast(0)
        val cropY = ((decoded.height - minDim) / 2).coerceAtLeast(0)
        val square = Bitmap.createBitmap(decoded, cropX, cropY, minDim, minDim)
        val scaled =
          if (square.width != targetSize || square.height != targetSize) {
            Bitmap.createScaledBitmap(square, targetSize, targetSize, true)
          } else {
            square
          }

        var quality = 78
        var dataUrl = ""
        while (quality >= 42) {
          val out = ByteArrayOutputStream()
          scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
          val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
          val candidate = "data:image/jpeg;base64,$b64"
          if (candidate.length <= 22_000) {
            dataUrl = candidate
            break
          }
          quality -= 10
        }
        if (scaled !== square && !scaled.isRecycled) scaled.recycle()
        if (square !== decoded && !square.isRecycled) square.recycle()
        if (!decoded.isRecycled) decoded.recycle()
        dataUrl.ifBlank { null }
      } catch (_: Throwable) {
        null
      }
    }

  fun decodeDataUrlBitmap(dataUrl: String): ImageBitmap? {
    if (!dataUrl.startsWith("data:image/")) return null
    val key = dataUrl.take(48) + "_" + dataUrl.length
    bitmapCache.get(key)?.let { return it }
    val commaIdx = dataUrl.indexOf(',')
    if (commaIdx <= 0 || commaIdx >= dataUrl.lastIndex) return null
    return try {
      val b64 = dataUrl.substring(commaIdx + 1)
      val bytes = Base64.decode(b64, Base64.DEFAULT)
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
      var sample = 1
      while (bounds.outWidth / (sample * 2) >= 160 && bounds.outHeight / (sample * 2) >= 160) {
        sample *= 2
      }
      val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
      }
      val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
      bmp.asImageBitmap().also { bitmapCache.put(key, it) }
    } catch (_: Throwable) {
      null
    }
  }
}
