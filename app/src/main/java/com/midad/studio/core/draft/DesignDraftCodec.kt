package com.midad.studio.core.draft

import com.midad.studio.studio.design.DesignScreen
import com.midad.studio.studio.design.collage.CollageAspect
import com.midad.studio.studio.design.model.CanvasPreset
import com.midad.studio.studio.design.model.DesignBackground
import com.midad.studio.studio.design.model.DesignDocument
import com.midad.studio.studio.design.model.DesignLayer
import com.midad.studio.studio.design.model.GradientDirection
import com.midad.studio.studio.design.model.ImageFit
import com.midad.studio.studio.design.model.LayerContent
import com.midad.studio.studio.design.model.PatternKind
import com.midad.studio.studio.design.model.ShapeKind
import com.midad.studio.studio.design.model.TextAlignChoice
import com.midad.studio.studio.design.model.TextFont
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

/**
 * Binary codec for [DesignDraft]. The sealed design model is flattened with explicit
 * tags so an older app version can never silently misread a newer file.
 */
object DesignDraftCodec {

    private const val MAGIC = 0x4D444431 // "MDD1"
    private const val VERSION = 1

    private const val MAX_LAYERS = 500
    private const val MAX_TEXT = 8192
    private const val MAX_URI = 8192
    private const val MAX_COLLAGE = 20

    private const val BG_SOLID = 0
    private const val BG_GRADIENT = 1
    private const val BG_PATTERN = 2
    private const val BG_TRANSPARENT = 3

    private const val CONTENT_TEXT = 0
    private const val CONTENT_IMAGE = 1
    private const val CONTENT_SHAPE = 2

    fun encode(draft: DesignDraft): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(MAGIC)
            out.writeInt(VERSION)
            out.writeInt(draft.screen.ordinal)
            val document = draft.document
            out.writeBoolean(document != null)
            if (document != null) out.writeDocument(document)
            val collage = draft.collage
            out.writeBoolean(collage != null)
            if (collage != null) {
                out.writeInt(collage.images.size)
                collage.images.forEach { uri -> out.writeString(uri) }
                out.writeString(collage.layoutId)
                out.writeInt(collage.aspect.ordinal)
                out.writeInt(collage.spacing)
                out.writeLong(collage.background)
            }
        }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): DesignDraft? = try {
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != MAGIC) return null
            if (input.readInt() != VERSION) return null
            val screenOrdinal = input.readInt().coerceIn(0, DesignScreen.entries.size - 1)
            val document = if (input.readBoolean()) input.readDocument() else null
            val collage = if (input.readBoolean()) {
                CollageDraft(
                    images = buildList {
                        val count = input.readCount(MAX_COLLAGE)
                        repeat(count) { add(input.readString() ?: "") }
                    },
                    layoutId = input.readString() ?: "",
                    aspect = CollageAspect.entries[
                        input.readInt().coerceIn(0, CollageAspect.entries.size - 1),
                    ],
                    spacing = input.readInt().coerceIn(0, 48),
                    background = input.readLong(),
                )
            } else {
                null
            }
            DesignDraft(DesignScreen.entries[screenOrdinal], document, collage)
        }
    } catch (eof: EOFException) {
        null
    } catch (throwable: Throwable) {
        null
    }

    private fun DataOutputStream.writeDocument(document: DesignDocument) {
        writeInt(document.preset.ordinal)
        writeBackground(document.background)
        writeInt(document.layers.size)
        document.layers.forEach { layer ->
            writeLong(layer.id)
            writeFloat(layer.x)
            writeFloat(layer.y)
            writeFloat(layer.width)
            writeFloat(layer.height)
            writeFloat(layer.rotation)
            writeFloat(layer.opacity)
            when (val content = layer.content) {
                is LayerContent.Text -> {
                    writeInt(CONTENT_TEXT)
                    writeString(content.text)
                    writeFloat(content.size)
                    writeInt(content.font.ordinal)
                    writeLong(content.color)
                    writeInt(content.align.ordinal)
                    writeBoolean(content.bold)
                }
                is LayerContent.Image -> {
                    writeInt(CONTENT_IMAGE)
                    writeString(content.uri)
                    writeInt(content.fit.ordinal)
                    writeFloat(content.cornerRadius)
                }
                is LayerContent.Shape -> {
                    writeInt(CONTENT_SHAPE)
                    writeInt(content.kind.ordinal)
                    writeLong(content.color)
                    writeBoolean(content.filled)
                    writeFloat(content.strokeWidth)
                }
            }
        }
    }

    private fun DataInputStream.readDocument(): DesignDocument {
        val preset = CanvasPreset.entries[readInt().coerceIn(0, CanvasPreset.entries.size - 1)]
        val background = readBackground()
        val layers = buildList {
            val count = readCount(MAX_LAYERS)
            repeat(count) {
                val id = readLong()
                val x = finite(readFloat())
                val y = finite(readFloat())
                val width = finite(readFloat())
                val height = finite(readFloat())
                val rotation = finite(readFloat())
                val opacity = finite(readFloat()).coerceIn(0f, 1f)
                val content = when (readInt()) {
                    CONTENT_IMAGE -> LayerContent.Image(
                        uri = readString()?.takeIf { it.length <= MAX_URI } ?: "",
                        fit = ImageFit.entries[readInt().coerceIn(0, ImageFit.entries.size - 1)],
                        cornerRadius = finite(readFloat()),
                    )
                    CONTENT_SHAPE -> LayerContent.Shape(
                        kind = ShapeKind.entries[readInt().coerceIn(0, ShapeKind.entries.size - 1)],
                        color = readLong(),
                        filled = readBoolean(),
                        strokeWidth = finite(readFloat()),
                    )
                    else -> LayerContent.Text(
                        text = readString()?.takeIf { it.length <= MAX_TEXT } ?: "",
                        size = finite(readFloat()),
                        font = TextFont.entries[readInt().coerceIn(0, TextFont.entries.size - 1)],
                        color = readLong(),
                        align = TextAlignChoice.entries[
                            readInt().coerceIn(0, TextAlignChoice.entries.size - 1)
                        ],
                        bold = readBoolean(),
                    )
                }
                add(DesignLayer(id, x, y, width, height, rotation, opacity, content))
            }
        }
        return DesignDocument(preset, background, layers)
    }

    private fun DataOutputStream.writeBackground(background: DesignBackground) {
        when (background) {
            is DesignBackground.Solid -> {
                writeInt(BG_SOLID)
                writeLong(background.color)
            }
            is DesignBackground.Gradient -> {
                writeInt(BG_GRADIENT)
                writeLong(background.from)
                writeLong(background.to)
                writeInt(background.direction.ordinal)
            }
            is DesignBackground.Pattern -> {
                writeInt(BG_PATTERN)
                writeLong(background.base)
                writeLong(background.ink)
                writeInt(background.kind.ordinal)
            }
            DesignBackground.Transparent -> writeInt(BG_TRANSPARENT)
        }
    }

    private fun DataInputStream.readBackground(): DesignBackground = when (val tag = readInt()) {
        BG_SOLID -> DesignBackground.Solid(readLong())
        BG_GRADIENT -> DesignBackground.Gradient(
            from = readLong(),
            to = readLong(),
            direction = GradientDirection.entries[
                readInt().coerceIn(0, GradientDirection.entries.size - 1)
            ],
        )
        BG_PATTERN -> DesignBackground.Pattern(
            base = readLong(),
            ink = readLong(),
            kind = PatternKind.entries[readInt().coerceIn(0, PatternKind.entries.size - 1)],
        )
        BG_TRANSPARENT -> DesignBackground.Transparent
        else -> throw EOFException("Unknown background tag $tag")
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
        if (length > MAX_URI * 4) throw EOFException()
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
