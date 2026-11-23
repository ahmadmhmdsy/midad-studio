package com.midad.studio.studio.design.collage

import android.graphics.RectF
import androidx.annotation.StringRes
import com.midad.studio.R
import com.midad.studio.studio.design.model.CanvasPreset
import com.midad.studio.studio.design.model.DesignPalette
import kotlin.math.min

enum class CollageAspect(val preset: CanvasPreset, @StringRes val labelRes: Int) {
    SQUARE(CanvasPreset.SQUARE, R.string.d_collage_square),
    STORY(CanvasPreset.STORY, R.string.d_collage_story),
    WIDE(CanvasPreset.LANDSCAPE, R.string.d_collage_wide);

    val width: Int get() = preset.width
    val height: Int get() = preset.height
    val aspectRatio: Float get() = width.toFloat() / height.toFloat()
}

/** A cell in normalized 0..1 coordinates. */
data class CollageCell(val x: Float, val y: Float, val width: Float, val height: Float)

data class CollageLayout(val id: String, val cells: List<CollageCell>) {
    val cellCount: Int get() = cells.size
}

object CollageLayouts {

    private fun cell(x: Float, y: Float, width: Float, height: Float): CollageCell =
        CollageCell(x, y, width, height)

    val all: List<CollageLayout> = listOf(
        CollageLayout(
            "duo_columns",
            listOf(cell(0f, 0f, 0.5f, 1f), cell(0.5f, 0f, 0.5f, 1f)),
        ),
        CollageLayout(
            "duo_rows",
            listOf(cell(0f, 0f, 1f, 0.5f), cell(0f, 0.5f, 1f, 0.5f)),
        ),
        CollageLayout(
            "trio_columns",
            listOf(
                cell(0f, 0f, 1f / 3f, 1f),
                cell(1f / 3f, 0f, 1f / 3f, 1f),
                cell(2f / 3f, 0f, 1f / 3f, 1f),
            ),
        ),
        CollageLayout(
            "trio_hero",
            listOf(
                cell(0f, 0f, 0.62f, 1f),
                cell(0.62f, 0f, 0.38f, 0.5f),
                cell(0.62f, 0.5f, 0.38f, 0.5f),
            ),
        ),
        CollageLayout(
            "quad_grid",
            listOf(
                cell(0f, 0f, 0.5f, 0.5f),
                cell(0.5f, 0f, 0.5f, 0.5f),
                cell(0f, 0.5f, 0.5f, 0.5f),
                cell(0.5f, 0.5f, 0.5f, 0.5f),
            ),
        ),
        CollageLayout(
            "five_hero",
            listOf(
                cell(0f, 0f, 0.5f, 0.58f),
                cell(0.5f, 0f, 0.5f, 0.58f),
                cell(0f, 0.58f, 1f / 3f, 0.42f),
                cell(1f / 3f, 0.58f, 1f / 3f, 0.42f),
                cell(2f / 3f, 0.58f, 1f / 3f, 0.42f),
            ),
        ),
        CollageLayout(
            "six_grid",
            buildList {
                for (row in 0 until 2) {
                    for (column in 0 until 3) {
                        add(cell(column / 3f, row / 2f, 1f / 3f, 0.5f))
                    }
                }
            },
        ),
        CollageLayout(
            "seven_hero",
            listOf(cell(0f, 0f, 1f, 0.5f)) + buildList {
                for (row in 0 until 2) {
                    for (column in 0 until 3) {
                        add(cell(column / 3f, 0.5f + row * 0.25f, 1f / 3f, 0.25f))
                    }
                }
            },
        ),
        CollageLayout(
            "eight_grid",
            buildList {
                for (row in 0 until 4) {
                    for (column in 0 until 2) {
                        add(cell(column * 0.5f, row * 0.25f, 0.5f, 0.25f))
                    }
                }
            },
        ),
        CollageLayout(
            "nine_grid",
            buildList {
                for (row in 0 until 3) {
                    for (column in 0 until 3) {
                        add(cell(column / 3f, row / 3f, 1f / 3f, 1f / 3f))
                    }
                }
            },
        ),
    )

    fun layoutsForCount(count: Int): List<CollageLayout> = all.filter { it.cellCount == count }

    fun defaultFor(count: Int): CollageLayout =
        layoutsForCount(count).firstOrNull() ?: all.first()

    fun layoutFor(id: String, count: Int): CollageLayout =
        layoutsForCount(count).firstOrNull { it.id == id } ?: defaultFor(count)

    /**
     * Turns normalized cells into pixel rectangles with an outer margin and inner gutters.
     * The same function feeds the preview and the export, so both stay in step.
     */
    fun cellRects(
        layout: CollageLayout,
        canvasWidth: Float,
        canvasHeight: Float,
        spacing: Float,
    ): List<RectF> {
        val gap = spacing.coerceAtLeast(0f)
        val spanWidth = (canvasWidth - gap * 2f).coerceAtLeast(1f)
        val spanHeight = (canvasHeight - gap * 2f).coerceAtLeast(1f)
        return layout.cells.map { cell ->
            var left = gap + cell.x * spanWidth
            var top = gap + cell.y * spanHeight
            var right = gap + (cell.x + cell.width) * spanWidth
            var bottom = gap + (cell.y + cell.height) * spanHeight
            if (cell.x > 0.0001f) left += gap / 2f
            if (cell.y > 0.0001f) top += gap / 2f
            if (cell.x + cell.width < 0.9999f) right -= gap / 2f
            if (cell.y + cell.height < 0.9999f) bottom -= gap / 2f
            if (right <= left) right = left + 1f
            if (bottom <= top) bottom = top + 1f
            RectF(left, top, right, bottom)
        }
    }
}

data class CollageSpec(
    val aspect: CollageAspect = CollageAspect.SQUARE,
    val layout: CollageLayout = CollageLayouts.defaultFor(2),
    val spacing: Int = 12,
    val background: Long = DesignPalette.PAPER_HIGH,
) {
    val width: Int get() = aspect.width
    val height: Int get() = aspect.height

    fun spacingPx(): Float = spacing.toFloat() * min(width, height) / 1080f
}
