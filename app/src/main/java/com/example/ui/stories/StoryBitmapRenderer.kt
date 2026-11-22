package com.example.ui.stories

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.model.MusicStoryItem
import com.example.model.StoryCardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

object StoryBitmapRenderer {
  private const val CARD_WIDTH = 1080
  private const val CARD_HEIGHT = 1920

  suspend fun renderAndCacheStoryPngUri(
    context: Context,
    story: MusicStoryItem
  ): Uri? =
    withContext(Dispatchers.IO) {
      val artworkBitmap = loadBitmapSafe(context, story.albumArtUrl, 640)
      val avatarBitmap = loadBitmapSafe(context, story.userAvatarUrl, 200)
      val bitmap = drawStoryCardBitmap(story, artworkBitmap, avatarBitmap)

      runCatching {
        val dir = File(context.cacheDir, "story_cards")
        if (!dir.exists()) dir.mkdirs()
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { old ->
          if (now - old.lastModified() > 24L * 60L * 60L * 1000L) {
            runCatching { old.delete() }
          }
        }
        val outFile = File(dir, "vizyon_story_${now}.png")
        FileOutputStream(outFile).use { stream ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 96, stream)
        }
        bitmap.recycle()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
      }.getOrNull()
    }

  fun launchExternalStoryShareChooser(
    context: Context,
    imageUri: Uri,
    story: MusicStoryItem
  ) {
    val caption =
      buildString {
        append("Vizyon Music'te dinliyorum: ${story.trackTitle} — ${story.artistName}")
        if (story.note.isNotBlank()) {
          append("\n“${story.note}”")
        }
      }
    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val chooser = Intent.createChooser(shareIntent, "Hikaye Kartını Paylaş")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
  }

  private suspend fun loadBitmapSafe(context: Context, urlOrPath: String, targetSize: Int): Bitmap? {
    if (urlOrPath.isBlank()) return null
    return runCatching {
      val loader = ImageLoader(context)
      val model: Any =
        if (urlOrPath.startsWith("http", ignoreCase = true) || urlOrPath.startsWith("content://")) {
          urlOrPath
        } else {
          File(urlOrPath)
        }
      val request =
        ImageRequest.Builder(context)
          .data(model)
          .size(targetSize)
          .allowHardware(false)
          .build()
      val result = loader.execute(request)
      if (result is SuccessResult) {
        result.drawable.toBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
      } else {
        null
      }
    }.getOrNull()
  }

  private fun drawStoryCardBitmap(
    story: MusicStoryItem,
    artwork: Bitmap?,
    avatar: Bitmap?
  ): Bitmap {
    val theme = StoryCardTheme.fromRaw(story.cardTheme)
    val startColor = theme.startColorHex.toInt()
    val endColor = theme.endColorHex.toInt()
    val accentColor = theme.accentColorHex.toInt()

    val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 1. Background Studio Gradient
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(),
        intArrayOf(startColor, endColor, Color.rgb(6, 9, 18)),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP
      )
    }
    canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

    // 2. Ambient Accent Glow Circles
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(46, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
    }
    canvas.drawCircle(190f, 340f, 360f, glowPaint)
    glowPaint.color = Color.argb(34, Color.red(endColor), Color.green(endColor), Color.blue(endColor))
    canvas.drawCircle(900f, 1520f, 420f, glowPaint)

    // 3. Top Author Header Pill
    val headerTop = 120f
    val avatarSize = 112f
    val avatarRect = RectF(96f, headerTop, 96f + avatarSize, headerTop + avatarSize)
    if (avatar != null) {
      val path = Path().apply { addOval(avatarRect, Path.Direction.CW) }
      canvas.save()
      canvas.clipPath(path)
      canvas.drawBitmap(avatar, null, avatarRect, Paint(Paint.ANTI_ALIAS_FLAG))
      canvas.restore()
    } else {
      val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
      canvas.drawOval(avatarRect, circlePaint)
      val initPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 44f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
      }
      val initial = story.userName.firstOrNull()?.uppercaseChar()?.toString() ?: "V"
      canvas.drawText(initial, avatarRect.centerX(), avatarRect.centerY() + 15f, initPaint)
    }
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 5f
      color = accentColor
    }
    canvas.drawOval(avatarRect, ringPaint)

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = 42f
      typeface = Typeface.DEFAULT_BOLD
    }
    canvas.drawText(story.userName.ifBlank { "Vizyon Dinleyicisi" }, 236f, headerTop + 48f, namePaint)

    val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = accentColor
      textSize = 32f
      typeface = Typeface.DEFAULT
    }
    canvas.drawText(story.userHandle.ifBlank { "@vizyon" }, 236f, headerTop + 94f, handlePaint)

    // 4. Frosted Center Glass Card
    val cardRect = RectF(76f, 300f, CARD_WIDTH - 76f, 1540f)
    val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(145, 11, 18, 34)
    }
    canvas.drawRoundRect(cardRect, 56f, 56f, cardFill)
    val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 3.5f
      color = Color.argb(95, 255, 255, 255)
    }
    canvas.drawRoundRect(cardRect, 56f, 56f, cardBorder)

    // 5. Artwork Container inside Glass Card
    val artLeft = 148f
    val artTop = 368f
    val artSize = CARD_WIDTH - 296f
    val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)
    val artClip = Path().apply { addRoundRect(artRect, 42f, 42f, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(artClip)
    if (artwork != null) {
      canvas.drawBitmap(artwork, null, artRect, Paint(Paint.ANTI_ALIAS_FLAG))
    } else {
      val fallbackArtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
          artRect.left, artRect.top, artRect.right, artRect.bottom,
          accentColor, endColor, Shader.TileMode.CLAMP
        )
      }
      canvas.drawRect(artRect, fallbackArtPaint)
      val noteSymbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 255, 255, 255)
        textSize = 220f
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText("♪", artRect.centerX(), artRect.centerY() + 72f, noteSymbolPaint)
    }
    canvas.restore()
    canvas.drawRoundRect(artRect, 42f, 42f, cardBorder)

    // 6. Track Title & Artist Name
    val titleTop = artRect.bottom + 54f
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = 54f
      typeface = Typeface.DEFAULT_BOLD
    }
    val titleLayout = StaticLayout.Builder.obtain(
      story.trackTitle.ifBlank { "Vizyon Parçası" },
      0,
      story.trackTitle.ifBlank { "Vizyon Parçası" }.length,
      titlePaint,
      (CARD_WIDTH - 296).coerceAtLeast(300)
    )
      .setMaxLines(2)
      .setAlignment(Layout.Alignment.ALIGN_NORMAL)
      .build()
    canvas.save()
    canvas.translate(artLeft, titleTop)
    titleLayout.draw(canvas)
    canvas.restore()

    val artistY = titleTop + titleLayout.height + 36f
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = accentColor
      textSize = 38f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(story.artistName.ifBlank { "Vizyon Sanatçısı" }, artLeft, artistY, artistPaint)

    // 7. Equalizer Soundwave Decoration
    val eqTop = artistY + 46f
    val barCount = 26
    val barWidth = 14f
    val gap = 14f
    val eqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
    val seed = abs(story.trackTitle.hashCode())
    for (i in 0 until barCount) {
      val factor = (((seed shr (i % 12)) + i * 7) % 10 + 3) / 13f
      val barH = 18f + factor * 46f
      val left = artLeft + i * (barWidth + gap)
      val barRect = RectF(left, eqTop + (64f - barH) / 2f, left + barWidth, eqTop + (64f + barH) / 2f)
      canvas.drawRoundRect(barRect, 7f, 7f, eqPaint)
    }

    // 8. Mood Quote Note (if provided)
    if (story.note.isNotBlank()) {
      val quoteTop = eqTop + 92f
      val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 232, 240)
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
      }
      val quoteText = "“${story.note.take(140)}”"
      val quoteLayout = StaticLayout.Builder.obtain(
        quoteText,
        0,
        quoteText.length,
        quotePaint,
        (CARD_WIDTH - 296).coerceAtLeast(300)
      )
        .setMaxLines(3)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .build()
      canvas.save()
      canvas.translate(artLeft, quoteTop)
      quoteLayout.draw(canvas)
      canvas.restore()
    }

    // 9. Bottom Branding Badge
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(215, 255, 255, 255)
      textSize = 32f
      typeface = Typeface.DEFAULT_BOLD
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText("VİZYON MUSIC STUDIO • HİKAYE KARTI", CARD_WIDTH / 2f, 1740f, footerPaint)

    val subFooterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = accentColor
      textSize = 26f
      typeface = Typeface.DEFAULT
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText(theme.label.uppercase(), CARD_WIDTH / 2f, 1790f, subFooterPaint)

    return bitmap
  }
}
