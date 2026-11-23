package com.midad.studio.studio.photo.editor

/**
 * Swatch values used by the text, sticker and brush palettes. Colours only; no assets.
 */
object PhotoPalette {
    const val White = 0xFFFFFFFF.toInt()
    const val Ink = 0xFF1C1915.toInt()
    const val Saffron = 0xFFC8923F.toInt()
    const val Terracotta = 0xFFB1552F.toInt()
    const val Sage = 0xFF66795F.toInt()
    const val Indigo = 0xFF344866.toInt()
    const val Sand = 0xFFE7DCC7.toInt()
    const val Paper = 0xFFF4EFE5.toInt()
    const val Rose = 0xFFC77B7B.toInt()
    const val Sky = 0xFF7FA3B5.toInt()

    val text: List<Int> = listOf(White, Ink, Saffron, Terracotta, Sage, Indigo, Sand, Rose, Sky)
    val stickers: List<Int> = listOf(White, Ink, Saffron, Terracotta, Sage, Indigo, Rose, Sky)
    val brush: List<Int> = listOf(White, Ink, Saffron, Terracotta, Sage, Indigo, Rose, Sky)
}
