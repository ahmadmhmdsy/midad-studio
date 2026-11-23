package com.midad.studio.render

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.midad.studio.studio.photo.editor.EditorStage
import com.midad.studio.studio.photo.editor.PhotoEditorState
import com.midad.studio.studio.photo.editor.PhotoEditorViewModel
import com.midad.studio.studio.photo.editor.PhotoStage
import com.midad.studio.studio.photo.editor.panels.AdjustPanel
import com.midad.studio.studio.photo.engine.Adjustments
import com.midad.studio.studio.photo.engine.GeometryEngine
import com.midad.studio.studio.photo.engine.GeometryState
import com.midad.studio.studio.photo.ui.ColorSwatchRow
import com.midad.studio.studio.photo.ui.SliderRow
import com.midad.studio.ui.theme.MidadColors
import com.midad.studio.ui.theme.MidadTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Semantics-level checks for the direct-manipulation controls that the UI review flagged as
 * unlabelled with sub-48 dp targets. These assertions read the accessibility tree the same way
 * TalkBack does, so they fail if a swatch loses its name, role or touch size.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h914dp-420dpi")
class EditorAccessibilityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun photoSwatchesExposeNameRoleStateAnd48DpTarget() {
        val saffron = MidadColors.Saffron.toArgb()
        val terracotta = MidadColors.Terracotta.toArgb()
        var picked = 0
        rule.setContent {
            MidadTheme(darkTheme = false) {
                ColorSwatchRow(
                    colors = listOf(saffron, terracotta),
                    selected = saffron,
                    onSelect = { picked = it },
                )
            }
        }

        val selectedNode = rule.onNodeWithContentDescription("Saffron")
        selectedNode.assertWidthIsEqualTo(48.dp)
        selectedNode.assertHeightIsAtLeast(48.dp)
        selectedNode.assertIsSelected()
        selectedNode.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.RadioButton),
        )

        val other = rule.onNodeWithContentDescription("Terracotta")
        other.assertIsNotSelected()
        other.performClick()
        assertEquals(terracotta, picked)
    }

    @Test
    fun designSwatchesExposeNameAnd48DpTarget() {
        val paper = com.midad.studio.studio.design.model.DesignPalette.PAPER
        val ink = com.midad.studio.studio.design.model.DesignPalette.INK
        rule.setContent {
            MidadTheme(darkTheme = false) {
                com.midad.studio.studio.design.ui.ColorSwatchRow(
                    colors = listOf(paper, ink),
                    selected = paper,
                    onPick = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Paper").assertWidthIsEqualTo(48.dp)
        rule.onNodeWithContentDescription("Paper").assertIsSelected()
        rule.onNodeWithContentDescription("Ink black").assertIsNotSelected()
    }

    @Test
    fun sliderAnnouncesItsNameAndValue() {
        rule.setContent {
            MidadTheme(darkTheme = false) {
                SliderRow(
                    label = "Brightness",
                    valueText = "+12",
                    value = 0.6f,
                    valueRange = -1f..1f,
                    onValueChange = {},
                )
            }
        }
        val slider = rule.onNodeWithContentDescription("Brightness")
        slider.assertWidthIsAtLeast(48.dp)
        slider.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "+12"),
        )
    }

    @Test
    fun photoStageIsNamedAndAnnouncesRendering() {
        val viewModel = PhotoEditorViewModel()
        val frame = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val plan = GeometryEngine.plan(64, 64, GeometryState())
        var state by mutableStateOf(
            PhotoEditorState(stage = EditorStage.READY, previewFrame = frame, plan = plan),
        )
        rule.setContent {
            MidadTheme(darkTheme = false) {
                PhotoStage(state = state, viewModel = viewModel)
            }
        }
        // The canvas is now a named node instead of a semantics void.
        val stage = rule.onNodeWithContentDescription("Photo being edited")
        stage.assertExists()
        stage.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))

        state = state.copy(renderingPreview = true)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Photo being edited").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Rendering…"),
        )
    }

    @Test
    fun adjustPanelHasThreeNamedGroupResetsAndAllTwelveSliders() {
        val viewModel = PhotoEditorViewModel()
        rule.setContent {
            MidadTheme(darkTheme = false) {
                AdjustPanel(
                    state = PhotoEditorState(adjustments = Adjustments(brightness = 20, saturation = 10)),
                    viewModel = viewModel,
                )
            }
        }
        // Four reset buttons would all read "Reset" without the group names.
        listOf("Reset Light", "Reset Colour", "Reset Detail").forEach { description ->
            rule.onNodeWithContentDescription(description).assertExists()
        }
        listOf(
            "Exposure", "Brightness", "Contrast", "Highlights", "Shadows",
            "Saturation", "Warmth", "Tint",
            "Sharpen", "Blur", "Vignette", "Grain",
        ).forEach { label ->
            rule.onNodeWithContentDescription(label).assertExists()
        }
    }
}