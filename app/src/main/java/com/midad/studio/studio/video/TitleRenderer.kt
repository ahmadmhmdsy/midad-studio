package com.midad.studio.studio.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils

/**
 * Draws the title into a transparent frame-sized bitmap that Media3 composites on top of the
 * video. StaticLayout gives us correct bidi/RTL shaping and wraps long titles safely.
 */
internal object TitleRenderer {

    private const val SHADOW_ALPHA = 0x88000000.toInt()

    fun render(title: VideoTitle, width: Int, height: Int): Bitmap? {
        val text = title.text.trim()
        if (text.isEmpty() || width <= 0 || height <= 0) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = title.color.argb
            textSize = (width * title.size.fractionOfWidth).coerceAtLeast(12f)
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer((width * 0.006f).coerceAtLeast(1.5f), 0f, 0f, SHADOW_ALPHA)
        }

        val horizontalPadding = width * 0.08f
        val layoutWidth = (width - horizontalPadding * 2).toInt().coerceAtLeast(1)
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, layoutWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.05f)
            .setMaxLines(4)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

        val margin = height * 0.07f
        val top = when (title.position) {
            TitlePosition.TOP -> margin
            TitlePosition.CENTER -> (height - layout.height) / 2f
            TitlePosition.BOTTOM -> height - layout.height - margin
        }.coerceAtLeast(0f)

        canvas.save()
        canvas.translate(horizontalPadding, top)
        layout.draw(canvas)
        canvas.restore()
        return bitmap
    }
}
