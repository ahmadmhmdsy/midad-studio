package com.midad.studio.studio.design.model

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import com.midad.studio.R

/**
 * The starting library. Every template is plain layer data: background, type, rules and
 * shapes positioned in canvas pixels, with all copy written for this app.
 */
object DesignTemplates {

    fun all(context: Context): List<DesignTemplate> {
        val res = context.resources
        val rtl = res.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
        return listOf(
            DesignTemplate("manifesto", R.string.d_tpl_manifesto, manifesto(res)),
            DesignTemplate("sale", R.string.d_tpl_sale, sale(res)),
            DesignTemplate("event", R.string.d_tpl_event, event(res)),
            DesignTemplate("recipe", R.string.d_tpl_recipe, recipe(res)),
            DesignTemplate("label", R.string.d_tpl_label, label(res)),
            DesignTemplate("countdown", R.string.d_tpl_countdown, countdown(res)),
            DesignTemplate("chapter", R.string.d_tpl_chapter, chapter(res)),
        ).map { template ->
            if (rtl) template.copy(document = template.document.mirroredForRtl()) else template
        }
    }

    private fun manifesto(res: Resources): DesignDocument {
        val doc = DocBuilder(CanvasPreset.SQUARE, DesignBackground.Solid(DesignPalette.PAPER))
        val m = doc.margin
        doc.rule(m, m + 74f, 128f, 8f, DesignPalette.SAFFRON)
        doc.text(
            res.getString(R.string.d_tpl_manifesto_eyebrow),
            x = m, y = m + 116f, width = doc.width - m * 2f, size = 30f,
            font = TextFont.SANS, color = DesignPalette.MUTED,
        )
        doc.text(
            res.getString(R.string.d_tpl_manifesto_title),
            x = m, y = 236f, width = doc.width - m * 2f, size = 124f,
            font = TextFont.SERIF, color = DesignPalette.INK,
        )
        doc.text(
            res.getString(R.string.d_tpl_manifesto_body),
            x = m, y = 742f, width = 610f, size = 36f,
            font = TextFont.SANS, color = DesignPalette.INK_SOFT,
        )
        doc.text(
            res.getString(R.string.d_tpl_manifesto_sign),
            x = m, y = 952f, width = 420f, size = 28f,
            font = TextFont.SANS, color = DesignPalette.MUTED,
        )
        doc.shape(
            x = 812f, y = 150f, width = 132f, height = 132f,
            kind = ShapeKind.ELLIPSE, color = DesignPalette.SAFFRON,
            filled = false, strokeWidth = 7f,
        )
        return doc.build()
    }

    private fun sale(res: Resources): DesignDocument {
        val doc = DocBuilder(CanvasPreset.SQUARE, DesignBackground.Solid(DesignPalette.TERRACOTTA))
        val m = doc.margin
        doc.shape(
            x = 546f, y = 64f, width = 640f, height = 640f,
            kind = ShapeKind.ELLIPSE, color = DesignPalette.PAPER, opacity = 0.12f,
        )
        doc.text(
            res.getString(R.string.d_tpl_sale_kicker),
            x = m, y = m + 46f, width = 620f, size = 30f,
            font = TextFont.SANS, color = DesignPalette.SAFFRON_SOFT,
        )
        doc.text(
            res.getString(R.string.d_tpl_sale_amount),
            x = m - 12f, y = 150f, width = 980f, size = 360f,
            font = TextFont.SANS, color = DesignPalette.PAPER, bold = true,
        )
        doc.text(
            res.getString(R.string.d_tpl_sale_off),
            x = m + 8f, y = 618f, width = 520f, size = 120f,
            font = TextFont.SANS, color = DesignPalette.SAFFRON_SOFT, bold = true,
        )
        doc.rule(m + 8f, 800f, 440f, 8f, DesignPalette.SAFFRON_SOFT)
        doc.text(
            res.getString(R.string.d_tpl_sale_body),
            x = m + 8f, y = 846f, width = 780f, size = 36f,
            font = TextFont.SANS, color = DesignPalette.PAPER,
        )
        doc.text(
            res.getString(R.string.d_tpl_sale_code),
            x = m + 8f, y = 944f, width = 560f, size = 28f,
            font = TextFont.SANS, color = DesignPalette.SAFFRON_SOFT,
        )
        return doc.build()
    }

    private fun event(res: Resources): DesignDocument {
        val doc = DocBuilder(CanvasPreset.A4, DesignBackground.Solid(DesignPalette.INK))
        val m = doc.margin
        doc.shape(
            x = m, y = m + 96f, width = 10f, height = 176f,
            kind = ShapeKind.RECTANGLE, color = DesignPalette.SAFFRON,
        )
        doc.text(
            res.getString(R.string.d_tpl_event_kicker),
            x = m + 46f, y = m + 92f, width = 1800f, size = 56f,
            font = TextFont.SANS, color = DesignPalette.SAFFRON,
        )
        doc.text(
            res.getString(R.string.d_tpl_event_title),
            x = m, y = 470f, width = 2180f, size = 296f,
            font = TextFont.SERIF, color = DesignPalette.PAPER,
        )
        doc.text(
            res.getString(R.string.d_tpl_event_date),
            x = m + 6f, y = 1352f, width = 1500f, size = 92f,
            font = TextFont.SANS, color = DesignPalette.SAFFRON,
        )
        doc.text(
            res.getString(R.string.d_tpl_event_place),
            x = m + 6f, y = 1490f, width = 1500f, size = 64f,
            font = TextFont.SANS, color = DesignPalette.PAPER,
        )
        doc.text(
            res.getString(R.string.d_tpl_event_note),
            x = m + 6f, y = 1620f, width = 1450f, size = 46f,
            font = TextFont.SANS, color = DesignPalette.PAPER, opacity = 0.62f,
        )
        doc.shape(
            x = 1600f, y = 1720f, width = 720f, height = 720f,
            kind = ShapeKind.ELLIPSE, color = DesignPalette.SAFFRON,
            filled = false, strokeWidth = 5f, opacity = 0.55f,
        )
        doc.shape(
            x = 1770f, y = 1890f, width = 380f, height = 380f,
            kind = ShapeKind.ELLIPSE, color = DesignPalette.SAFFRON, opacity = 0.9f,
        )
        doc.rule(m, 292f, 280f, 12f, DesignPalette.SAFFRON)
        doc.text(
            res.getString(R.string.d_tpl_event_footer),
            x = m, y = 3150f, width = 1900f, size = 40f,
            font = TextFont.SANS, color = DesignPalette.PAPER, opacity = 0.5f,
        )
        return doc.build()
    }

    private fun recipe(res: Resources): DesignDocument {
        val doc = DocBuilder(CanvasPreset.STORY, DesignBackground.Solid(DesignPalette.PAPER_HIGH))
        val m = doc.margin
        doc.text(
            res.getString(R.string.d_tpl_recipe_kicker),
            x = m, y = m + 26f, width = 800f, size = 30f,
            font = TextFont.SANS, color = DesignPalette.MUTED,
        )
        doc.text(
            res.getString(R.string.d_tpl_recipe_title),
            x = m, y = 168f, width = 920f, size = 148f,
            font = TextFont.SERIF, color = DesignPalette.INK,
        )
        doc.rule(m, 600f, 176f, 9f, DesignPalette.SAFFRON)
        doc.text(
            res.getString(R.string.d_tpl_recipe_meta),
            x = m, y = 652f, width = 900f, size = 30f,
            font = TextFont.SANS, color = DesignPalette.MUTED,
        )
        doc.text(
            res.getString(R.string.d_tpl_recipe_ingredients),
            x = m, y = 800f, width = 520f, size = 28f,
            font = TextFont.SANS, color = DesignPalette.SAFFRON, bold = true,
        )
        doc.text(
            res.getString(R.string.d_tpl_recipe_list),
            x = m, y = 856f, width = 520f, size = 40f,
            font = TextFont.SANS, color = DesignPalette.INK_SOFT,
        )
        doc.shape(
            x = 650f, y = 800f, width = 3f, height = 340f,
            kind = ShapeKind.RECTANGLE, color = DesignPalette.LINE,
        )
        doc.text(
            res.getString(R.string.d_tpl_recipe_step),
            x = 690f, y = 796f, width = 320f, size = 30f,
            font = TextFont.SANS, color = DesignPalette.INK_SOFT,
        )
        doc.shape(
            x = 828f, y = 168f, width = 156f, height = 156f,
            kind = ShapeKind.ELLIPSE, color = DesignPalette.SAFFRON,
            filled = false, strokeWidth = 5f,
        )
        doc.text(
            res.getString(R.string.d_tpl_recipe_footer),
            x = m, y = 1752f, width = 900f, size = 28f,
            font = TextFont.SANS, color = DesignPalette.MUTED,
        )
        return doc.build()
    }

    private fun label(res: Resources): DesignDocument {
        val doc = DocBuilder(
            CanvasPreset.LINK,
            DesignBackground.Gradient(
                from = DesignPalette.SAND,
                to = DesignPalette.PAPER_HIGH,
                direction = GradientDirection.VERTICAL,
            ),
        )
        doc.text(
            res.getString(R.string.d_tpl_label_brand),
            x = 70f, y = 126f, width = 720f, size = 148f,
            font = TextFont.SERIF, color = DesignPalette.INK,
        )
        doc.rule(74f, 330f, 128f, 7f, DesignPalette.TERRACOTTA)
        doc.text(
            res.getString(R.string.d_tpl_label_product),
            x = 74f, y = 372f, width = 540f, size = 52f,
            font = TextFont.SERIF, color = DesignPalette.INK_SOFT,
        )
        doc.text(
            res.getString(R.string.d_tpl_label_size),
            x = 74f, y = 540f, width = 300f, size = 32f,
            font = TextFont.SANS, color = DesignPalette.MUTED,
        )
        doc.text(
            res.getString(R.string.d_tpl_label_note),
            x = 700f, y = 372f, width = 430f, size = 26f,
            font = TextFont.SANS, color = DesignPalette.MUTED,
        )
        doc.shape(
            x = 980f, y = 116f, width = 176f, height = 176f,
            kind = ShapeKind.ELLIPSE, color = DesignPalette.TERRACOTTA,
            filled = false, strokeWidth = 4f,
        )
        doc.shape(
            x = 1026f, y = 162f, width = 84f, height = 84f,
            kind = ShapeKind.ELLIPSE, color = DesignPalette.SAGE,
        )
        return doc.build()
    }

    private fun countdown(res: Resources): DesignDocument {
        val doc = DocBuilder(
            CanvasPreset.STORY,
            DesignBackground.Gradient(
                from = DesignPalette.SAFFRON,
                to = DesignPalette.TERRACOTTA,
                direction = GradientDirection.VERTICAL,
            ),
        )
        val m = doc.margin
        doc.text(
            res.getString(R.string.d_tpl_countdown_kicker),
            x = m, y = 198f, width = 940f, size = 32f,
            font = TextFont.SANS, color = DesignPalette.PAPER,
        )
        doc.text(
            res.getString(R.string.d_tpl_countdown_days),
            x = 46f, y = 268f, width = 960f, size = 528f,
            font = TextFont.SERIF, color = DesignPalette.PAPER,
        )
        doc.text(
            res.getString(R.string.d_tpl_countdown_unit),
            x = m + 8f, y = 986f, width = 460f, size = 84f,
            font = TextFont.SANS, color = DesignPalette.PAPER,
        )
        doc.rule(m + 8f, 1120f, 620f, 9f, DesignPalette.PAPER, opacity = 0.85f)
        doc.text(
            res.getString(R.string.d_tpl_countdown_title),
            x = m, y = 1196f, width = 900f, size = 76f,
            font = TextFont.SERIF, color = DesignPalette.PAPER,
        )
        doc.text(
            res.getString(R.string.d_tpl_countdown_meta),
            x = m, y = 1430f, width = 840f, size = 36f,
            font = TextFont.SANS, color = DesignPalette.PAPER, opacity = 0.85f,
        )
        doc.shape(
            x = 700f, y = 1600f, width = 300f, height = 300f,
            kind = ShapeKind.ELLIPSE, color = DesignPalette.PAPER,
            filled = false, strokeWidth = 3f, opacity = 0.5f,
        )
        return doc.build()
    }

    private fun chapter(res: Resources): DesignDocument {
        val doc = DocBuilder(CanvasPreset.LANDSCAPE, DesignBackground.Solid(DesignPalette.INK))
        val m = doc.margin
        doc.rule(m, 118f, 92f, 6f, DesignPalette.SAFFRON)
        doc.text(
            res.getString(R.string.d_tpl_chapter_kicker),
            x = m, y = 146f, width = 900f, size = 26f,
            font = TextFont.SANS, color = DesignPalette.SAFFRON,
        )
        doc.text(
            res.getString(R.string.d_tpl_chapter_title),
            x = 40f, y = 196f, width = 1080f, size = 122f,
            font = TextFont.SERIF, color = DesignPalette.PAPER,
        )
        doc.text(
            res.getString(R.string.d_tpl_chapter_meta),
            x = m, y = 556f, width = 840f, size = 30f,
            font = TextFont.SANS, color = DesignPalette.PAPER, opacity = 0.7f,
        )
        doc.rule(m, 652f, 1194f, 2f, DesignPalette.SAFFRON, opacity = 0.3f)
        doc.shape(
            x = 1030f, y = 132f, width = 220f, height = 220f,
            kind = ShapeKind.ELLIPSE, color = DesignPalette.SAFFRON,
            filled = false, strokeWidth = 2f, opacity = 0.4f,
        )
        doc.shape(
            x = 1082f, y = 184f, width = 116f, height = 116f,
            kind = ShapeKind.ELLIPSE, color = DesignPalette.SAFFRON,
        )
        return doc.build()
    }
}

/**
 * Mirrors an authored template for right-to-left reading. Boxes are reflected across the
 * canvas so decorations move to the left and RTL text (which starts at the box's right edge)
 * keeps the same breathing room. Rotation is negated to preserve the visual angle.
 */
internal fun DesignDocument.mirroredForRtl(): DesignDocument = copy(
    background = when (val background = background) {
        is DesignBackground.Gradient -> when (background.direction) {
            GradientDirection.VERTICAL -> background
            GradientDirection.HORIZONTAL, GradientDirection.DIAGONAL ->
                background.copy(from = background.to, to = background.from)
        }
        else -> background
    },
    layers = layers.map { layer ->
        layer.copy(
            x = width - layer.x - layer.width,
            rotation = -layer.rotation,
        )
    },
)

/** Small authoring helper so templates read as a list of design decisions. */
private class DocBuilder(
    private val preset: CanvasPreset,
    private val background: DesignBackground,
) {
    val width: Float = preset.width.toFloat()
    val height: Float = preset.height.toFloat()
    val margin: Float = minOf(preset.width, preset.height) * DesignDocument.SAFE_MARGIN

    private val layers = mutableListOf<DesignLayer>()
    private var nextId = 1L

    fun text(
        value: String,
        x: Float,
        y: Float,
        width: Float,
        size: Float,
        font: TextFont = TextFont.SERIF,
        color: Long = DesignPalette.INK,
        align: TextAlignChoice = TextAlignChoice.START,
        bold: Boolean = false,
        opacity: Float = 1f,
    ) {
        val lines = value.count { it == '\n' } + 1
        layers += DesignLayer(
            id = nextId++,
            x = x,
            y = y,
            width = width,
            height = size * 1.35f * lines,
            opacity = opacity,
            content = LayerContent.Text(
                text = value,
                size = size,
                font = font,
                color = color,
                align = align,
                bold = bold,
            ),
        )
    }

    fun shape(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        kind: ShapeKind,
        color: Long,
        filled: Boolean = true,
        strokeWidth: Float = 12f,
        opacity: Float = 1f,
    ) {
        layers += DesignLayer(
            id = nextId++,
            x = x,
            y = y,
            width = width,
            height = height,
            opacity = opacity,
            content = LayerContent.Shape(
                kind = kind,
                color = color,
                filled = filled,
                strokeWidth = strokeWidth,
            ),
        )
    }

    fun rule(x: Float, y: Float, width: Float, height: Float, color: Long, opacity: Float = 1f) {
        shape(
            x = x, y = y, width = width, height = height,
            kind = ShapeKind.RULE, color = color, opacity = opacity,
        )
    }

    fun build(): DesignDocument = DesignDocument(
        preset = preset,
        background = background,
        layers = layers.toList(),
    )
}
