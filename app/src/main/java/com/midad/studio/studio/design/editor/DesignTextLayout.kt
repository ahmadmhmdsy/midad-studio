package com.midad.studio.studio.design.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.midad.studio.studio.design.model.DesignDocument
import com.midad.studio.studio.design.model.LayerContent
import com.midad.studio.studio.design.model.TextAlignChoice
import com.midad.studio.studio.design.model.TextFont
import com.midad.studio.studio.design.model.designColor
import kotlin.math.roundToInt

/** Stable inputs of one text layout inside the preview, used to reuse measurements. */
internal data class DesignTextLayoutKey(
    val text: String,
    val size: Float,
    val font: TextFont,
    val color: Long,
    val align: TextAlignChoice,
    val bold: Boolean,
    val opacity: Float,
    val scale: Float,
    val widthPx: Int,
)

/**
 * Lays out text layers at the current zoom level so hit testing and painting agree.
 *
 * A drag changes the document on every pointer move, but only the selected layer's geometry
 * changes. [cache] keeps the unchanged layers' layouts so a drag no longer re-measures every
 * text layer on every frame. Keys not present in this document are dropped each pass.
 */
internal fun measureDesignText(
    document: DesignDocument,
    scale: Float,
    measurer: TextMeasurer,
    density: Density,
    cache: MutableMap<DesignTextLayoutKey, TextLayoutResult>? = null,
): Map<Long, TextLayoutResult> {
    val result = LinkedHashMap<Long, TextLayoutResult>()
    val keys = HashSet<DesignTextLayoutKey>()
    document.layers.forEach { layer ->
        val content = layer.content
        if (content !is LayerContent.Text) return@forEach
        val widthPx = (layer.width * scale).roundToInt().coerceAtLeast(1)
        val opacity = layer.opacity.coerceIn(0f, 1f)
        val key = DesignTextLayoutKey(
            text = content.text,
            size = content.size,
            font = content.font,
            color = content.color,
            align = content.align,
            bold = content.bold,
            opacity = opacity,
            scale = scale,
            widthPx = widthPx,
        )
        keys += key
        val cached = cache?.get(key)
        if (cached != null) {
            result[layer.id] = cached
            return@forEach
        }
        val baseColor = designColor(content.color)
        val style = TextStyle(
            color = baseColor.copy(alpha = baseColor.alpha * opacity),
            fontSize = with(density) { (content.size * scale).toSp() },
            fontFamily = if (content.font == TextFont.SERIF) FontFamily.Serif else FontFamily.SansSerif,
            fontWeight = if (content.bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = when (content.align) {
                TextAlignChoice.START -> TextAlign.Start
                TextAlignChoice.CENTER -> TextAlign.Center
                TextAlignChoice.END -> TextAlign.End
            },
        )
        val layout = measurer.measure(
            text = AnnotatedString(content.text),
            style = style,
            overflow = TextOverflow.Visible,
            softWrap = true,
            maxLines = Int.MAX_VALUE,
            constraints = Constraints.fixedWidth(widthPx),
            layoutDirection = designTextDirection(content.text),
        )
        cache?.put(key, layout)
        result[layer.id] = layout
    }
    cache?.keys?.retainAll(keys)
    return result
}

/** Follows the first strong character, matching the export renderer's direction heuristic. */
fun designTextDirection(text: String): LayoutDirection {
    for (char in text) {
        if (isRtlCharacter(char)) return LayoutDirection.Rtl
        if (char.isLetter()) return LayoutDirection.Ltr
    }
    return LayoutDirection.Ltr
}

fun isRtlDesignText(text: String): Boolean = designTextDirection(text) == LayoutDirection.Rtl

private fun isRtlCharacter(char: Char): Boolean {
    val code = char.code
    return (code in 0x0590..0x08FF) || (code in 0xFB1D..0xFDFF) || (code in 0xFE70..0xFEFF)
}
