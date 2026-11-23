package com.midad.studio.core.draft

import com.midad.studio.studio.photo.engine.Adjustments
import com.midad.studio.studio.photo.engine.CropAspect
import com.midad.studio.studio.photo.engine.CropRect
import com.midad.studio.studio.photo.engine.DrawStroke
import com.midad.studio.studio.photo.engine.GeometryState
import com.midad.studio.studio.photo.engine.StickerLayer
import com.midad.studio.studio.photo.engine.StickerShape
import com.midad.studio.studio.photo.engine.StrokePoint
import com.midad.studio.studio.photo.engine.TextLayer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

/**
 * Versioned binary codec for [PhotoDraft]. Deliberately not JSON: no reflection, no
 * unknown-field silent loss, and small enough to write after every edit burst.
 * Decoding rejects absurd sizes so a corrupt file can never allocate a huge list.
 */
object PhotoDraftCodec {

    private const val MAGIC = 0x4D445031 // "MDP1"
    private const val VERSION = 1

    private const val MAX_TEXT = 4096
    private const val MAX_LAYERS = 500
    private const val MAX_STROKES = 1000
    private const val MAX_POINTS = 20_000

    fun encode(draft: PhotoDraft): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(MAGIC)
            out.writeInt(VERSION)
            out.writeString(draft.sourceUri)
            out.writeBoolean(draft.baseFromOriginal)
            out.writeBoolean(draft.sourcePng)

            out.writeFloat(draft.adjustments.exposure)
            out.writeInt(draft.adjustments.brightness)
            out.writeInt(draft.adjustments.contrast)
            out.writeInt(draft.adjustments.saturation)
            out.writeInt(draft.adjustments.warmth)
            out.writeInt(draft.adjustments.tint)
            out.writeInt(draft.adjustments.highlights)
            out.writeInt(draft.adjustments.shadows)
            out.writeInt(draft.adjustments.vignette)
            out.writeInt(draft.adjustments.grain)
            out.writeInt(draft.adjustments.sharpen)
            out.writeInt(draft.adjustments.blur)

            out.writeInt(draft.filterIndex ?: -1)
            out.writeInt(draft.filterIntensity)

            out.writeInt(draft.geometry.quarterTurns)
            out.writeBoolean(draft.geometry.flipHorizontal)
            out.writeBoolean(draft.geometry.flipVertical)
            out.writeFloat(draft.geometry.straighten)
            out.writeFloat(draft.geometry.crop.left)
            out.writeFloat(draft.geometry.crop.top)
            out.writeFloat(draft.geometry.crop.right)
            out.writeFloat(draft.geometry.crop.bottom)
            out.writeInt(draft.geometry.aspect.ordinal)

            out.writeInt(draft.textLayers.size)
            draft.textLayers.forEach { layer ->
                out.writeLong(layer.id)
                out.writeString(layer.text)
                out.writeInt(layer.color)
                out.writeFloat(layer.sizeFraction)
                out.writeBoolean(layer.serif)
                out.writeFloat(layer.opacity)
                out.writeFloat(layer.x)
                out.writeFloat(layer.y)
                out.writeFloat(layer.rotation)
            }

            out.writeInt(draft.stickers.size)
            draft.stickers.forEach { layer ->
                out.writeLong(layer.id)
                out.writeInt(layer.shape.ordinal)
                out.writeInt(layer.color)
                out.writeFloat(layer.sizeFraction)
                out.writeFloat(layer.x)
                out.writeFloat(layer.y)
                out.writeFloat(layer.rotation)
                out.writeFloat(layer.opacity)
            }

            out.writeInt(draft.strokes.size)
            draft.strokes.forEach { stroke ->
                out.writeInt(stroke.points.size)
                stroke.points.forEach { point ->
                    out.writeFloat(point.x)
                    out.writeFloat(point.y)
                }
                out.writeInt(stroke.color)
                out.writeFloat(stroke.widthFraction)
                out.writeBoolean(stroke.erase)
            }
            out.writeLong(draft.nextLayerId)
        }
        return bytes.toByteArray()
    }

    /** Returns null for any corrupt, truncated or future-version file. */
    fun decode(bytes: ByteArray): PhotoDraft? = try {
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != MAGIC) return null
            if (input.readInt() != VERSION) return null
            val sourceUri = input.readString()
            val baseFromOriginal = input.readBoolean()
            val sourcePng = input.readBoolean()
            val adjustments = Adjustments(
                exposure = finite(input.readFloat()),
                brightness = input.readInt(),
                contrast = input.readInt(),
                saturation = input.readInt(),
                warmth = input.readInt(),
                tint = input.readInt(),
                highlights = input.readInt(),
                shadows = input.readInt(),
                vignette = input.readInt(),
                grain = input.readInt(),
                sharpen = input.readInt(),
                blur = input.readInt(),
            )
            val filterIndex = input.readInt().takeIf { it >= 0 }
            val filterIntensity = input.readInt().coerceIn(0, 100)
            val geometry = GeometryState(
                quarterTurns = ((input.readInt() % 4) + 4) % 4,
                flipHorizontal = input.readBoolean(),
                flipVertical = input.readBoolean(),
                straighten = finite(input.readFloat()).coerceIn(-45f, 45f),
                crop = CropRect(
                    left = finite(input.readFloat()),
                    top = finite(input.readFloat()),
                    right = finite(input.readFloat()),
                    bottom = finite(input.readFloat()),
                ).clamped(),
                aspect = CropAspect.entries[input.readInt().coerceIn(0, CropAspect.entries.size - 1)],
            )
            val textLayers = buildList {
                val count = input.readCount(MAX_LAYERS)
                repeat(count) {
                    add(
                        TextLayer(
                            id = input.readLong(),
                            text = input.readString()?.takeIf { it.length <= MAX_TEXT } ?: "",
                            color = input.readInt(),
                            sizeFraction = finite(input.readFloat()),
                            serif = input.readBoolean(),
                            opacity = finite(input.readFloat()),
                            x = finite(input.readFloat()),
                            y = finite(input.readFloat()),
                            rotation = finite(input.readFloat()),
                        ),
                    )
                }
            }
            val stickers = buildList {
                val count = input.readCount(MAX_LAYERS)
                repeat(count) {
                    add(
                        StickerLayer(
                            id = input.readLong(),
                            shape = StickerShape.entries[
                                input.readInt().coerceIn(0, StickerShape.entries.size - 1),
                            ],
                            color = input.readInt(),
                            sizeFraction = finite(input.readFloat()),
                            x = finite(input.readFloat()),
                            y = finite(input.readFloat()),
                            rotation = finite(input.readFloat()),
                            opacity = finite(input.readFloat()),
                        ),
                    )
                }
            }
            val strokes = buildList {
                val count = input.readCount(MAX_STROKES)
                repeat(count) {
                    val points = buildList {
                        val pointCount = input.readCount(MAX_POINTS)
                        repeat(pointCount) {
                            add(StrokePoint(finite(input.readFloat()), finite(input.readFloat())))
                        }
                    }
                    add(
                        DrawStroke(
                            points = points,
                            color = input.readInt(),
                            widthFraction = finite(input.readFloat()),
                            erase = input.readBoolean(),
                        ),
                    )
                }
            }
            val nextLayerId = input.readLong().coerceAtLeast(1L)
            PhotoDraft(
                sourceUri = sourceUri?.takeIf { it.isNotBlank() && it.length <= 8192 },
                baseFromOriginal = baseFromOriginal,
                sourcePng = sourcePng,
                adjustments = adjustments,
                filterIndex = filterIndex,
                filterIntensity = filterIntensity,
                geometry = geometry,
                textLayers = textLayers,
                stickers = stickers,
                strokes = strokes,
                nextLayerId = nextLayerId,
            )
        }
    } catch (eof: EOFException) {
        null
    } catch (throwable: Throwable) {
        null
    }

    private fun finite(value: Float): Float = if (value.isFinite()) value else 0f

    private fun DataOutputStream.writeString(value: String?) {
        if (value == null) {
            writeInt(-1)
        } else {
            val encoded = value.toByteArray(Charsets.UTF_8)
            writeInt(encoded.size)
            write(encoded)
        }
    }

    private fun DataInputStream.readString(): String? {
        val length = readInt()
        if (length < 0) return null
        if (length > MAX_TEXT * 4) throw EOFException()
        val bytes = ByteArray(length)
        readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun DataInputStream.readCount(max: Int): Int {
        val count = readInt()
        if (count < 0 || count > max) throw EOFException()
        return count
    }
}
