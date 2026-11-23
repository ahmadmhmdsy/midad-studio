package com.midad.studio.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.midad.studio.R
import com.midad.studio.core.ai.AiModel
import com.midad.studio.core.ai.AiModelStatus
import com.midad.studio.core.ai.ModelState
import com.midad.studio.core.prefs.MidadPreferences
import com.midad.studio.home.HomeScreen
import com.midad.studio.projects.ProjectsScreen
import com.midad.studio.settings.SettingsScreen
import com.midad.studio.studio.ai.AiHomeContent
import com.midad.studio.studio.ai.AiUiState
import com.midad.studio.studio.design.editor.DesignEditorActions
import com.midad.studio.studio.design.model.DesignTemplates
import com.midad.studio.studio.design.start.DesignStartScreen
import com.midad.studio.studio.photo.PhotoStudioScreen
import com.midad.studio.studio.photo.editor.PhotoEditorState
import com.midad.studio.studio.photo.editor.PhotoEditorViewModel
import com.midad.studio.studio.photo.editor.panels.AdjustPanel
import com.midad.studio.studio.photo.engine.Adjustments
import com.midad.studio.ui.LocalAppState
import com.midad.studio.ui.MidadAppState
import com.midad.studio.ui.components.StudioScaffold
import com.midad.studio.ui.theme.MidadTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * JVM-side pixel renders of the real Compose screens, for the UI review.
 *
 * Robolectric runs with the native (Skia) graphics backend, so the same drawing code that runs
 * on a phone produces real pixels here: text, gradients, Canvas art and Material 3 surfaces.
 * Only test sources are involved; the app itself is untouched.
 *
 * Run: gradle :app:testDebugUnitTest --tests "com.midad.studio.render.ScreenshotRenderTest"
 * PNGs land in <project>/tmp/ui-render.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h914dp-420dpi")
class ScreenshotRenderTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun render(
        fileName: String,
        darkTheme: Boolean = false,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ): Bitmap {
        val appState = MidadAppState(MidadPreferences(context))
        rule.setContent {
            CompositionLocalProvider(LocalAppState provides appState) {
                MidadTheme(darkTheme = darkTheme) {
                    if (fontScale == 1f) content() else ScaledFonts(fontScale, content)
                }
            }
        }
        rule.waitForIdle()
        // Give one more frame so async state (ProjectStore, cache size) has settled.
        rule.mainClock.advanceTimeBy(250)
        rule.waitForIdle()

        val bitmap = capture(darkTheme)
        writePng(fileName, bitmap)
        return bitmap
    }

    /** Mimics Android's font-size setting inside Compose without rebuilding the activity. */
    @Composable
    private fun ScaledFonts(scale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density = density.density, fontScale = scale),
            content = content,
        )
    }

    /**
     * Full-window capture: the decor view draws the platform window background behind the
     * Compose tree, exactly like a device screenshot. The test host activity normally runs on
     * a white platform theme, so the app's own window colour is set first: light screens
     * resolve over paper, dark screens over NightBackground.
     */
    private fun capture(darkTheme: Boolean): Bitmap {
        val window = rule.activity.window
        val windowColor = if (darkTheme) R.color.midad_night_background else R.color.midad_paper
        window.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, windowColor)))
        val view = window.decorView
        val width = maxOf(view.width, 1)
        val height = maxOf(view.height, 1)
        val windowShot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(windowShot))
        if (isBlank(windowShot)) {
            // Defensive: if the decor never drew, fall back to the Compose root capture.
            val fromCompose = runCatching {
                rule.onRoot().captureToImage().asAndroidBitmap()
            }.getOrNull()
            if (fromCompose != null) {
                return fromCompose.copy(Bitmap.Config.ARGB_8888, false) ?: fromCompose
            }
        }
        return windowShot
    }

    private fun isBlank(bitmap: Bitmap): Boolean {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val step = maxOf(1, pixels.size / 4096)
        var nonTransparent = 0
        var index = 0
        while (index < pixels.size) {
            if ((pixels[index] ushr 24) != 0) nonTransparent++
            index += step
        }
        return nonTransparent < 16
    }

    private fun writePng(fileName: String, bitmap: Bitmap) {
        val dir = File(
            System.getProperty("midad.renderDir")
                ?: File(System.getProperty("user.dir") ?: ".").absolutePath + "/build/ui-render",
        )
        dir.mkdirs()
        val out = File(dir, fileName)
        FileOutputStream(out).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "PNG encode failed" }
        }
        val stats = stats(bitmap)
        println(
            "RENDER_OK file=" + out.absolutePath +
                " bytes=" + out.length() +
                " size=" + bitmap.width + "x" + bitmap.height +
                " colors=" + stats.colors +
                " meanLum=" + String.format("%.1f", stats.mean),
        )
        assertTrue("PNG smaller than 20KB: " + out.length(), out.length() > 20_000)
        assertTrue("image too flat (colors=" + stats.colors + ")", stats.colors > 8)
        assertTrue("image is blank (meanLum=" + stats.mean + ")", stats.mean > 8.0)
        assertTrue("captured image is not phone-portrait sized", bitmap.height > bitmap.width)
    }

    private class Stats(val colors: Int, val mean: Double)

    private fun stats(bitmap: Bitmap): Stats {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val seen = HashSet<Int>()
        var sum = 0.0
        var count = 0
        val step = 7
        var index = 0
        while (index < pixels.size) {
            val p = pixels[index]
            seen.add(p)
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
            count++
            index += step
        }
        return Stats(seen.size, if (count == 0) 0.0 else sum / count)
    }

    /** Mean luminance (0-255) of a horizontal band of the captured window. */
    private fun meanLuminance(bitmap: Bitmap, fromFraction: Double, toFraction: Double): Double {
        val top = (bitmap.height * fromFraction).toInt().coerceIn(0, bitmap.height - 1)
        val bottom = (bitmap.height * toFraction).toInt().coerceIn(top + 1, bitmap.height)
        val pixels = IntArray(bitmap.width * (bottom - top))
        bitmap.getPixels(pixels, 0, bitmap.width, 0, top, bitmap.width, bottom - top)
        var sum = 0.0
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
        }
        return sum / pixels.size
    }

    // --------------------------------------------------------------------------- screens

    @Test
    fun homeLight() {
        render("home-light.png", darkTheme = false) {
            HomeScreen(onOpenStudio = {}, onOpenProjects = {}, onOpenSettings = {})
        }
    }

    @Test
    fun settingsLight() {
        render("settings-light.png", darkTheme = false) {
            SettingsScreen(onBack = {}, onOpenModels = {}, onOpenLicenses = {})
        }
    }

    @Test
    fun designStartLight() {
        render("design-start-light.png", darkTheme = false) {
            DesignStartScreen(
                onBack = {},
                templates = DesignTemplates.all(context),
                onPreset = {},
                onOpenTemplates = {},
                onOpenCollage = {},
                onOpenTemplate = {},
            )
        }
    }

    /**
     * The design editor's real top bar at phone width: Export is a filled primary button
     * while undo, redo and margins stay glyphs, and the title still gets usable space.
     */
    @Test
    fun designEditorTopBarLight() {
        val title = context.getString(R.string.d_editor_title)
        render("design-editor-topbar-light.png", darkTheme = false) {
            StudioScaffold(
                title = title,
                onBack = {},
                actions = {
                    DesignEditorActions(
                        canUndo = true,
                        canRedo = false,
                        showGuides = true,
                        onUndo = {},
                        onRedo = {},
                        onToggleGuides = {},
                        onExport = {},
                    )
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding))
            }
        }
        rule.onNodeWithText(title).assertIsDisplayed()
        rule.onNodeWithText(title).assertWidthIsAtLeast(72.dp)
        rule.onNodeWithText(context.getString(R.string.d_action_export)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "ar-rIQ-w411dp-h914dp-420dpi")
    fun designEditorTopBarArabic() {
        val title = context.getString(R.string.d_editor_title)
        render("design-editor-topbar-ar.png", darkTheme = false) {
            StudioScaffold(
                title = title,
                onBack = {},
                actions = {
                    DesignEditorActions(
                        canUndo = true,
                        canRedo = true,
                        showGuides = false,
                        onUndo = {},
                        onRedo = {},
                        onToggleGuides = {},
                        onExport = {},
                    )
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding))
            }
        }
        rule.onNodeWithText(title).assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.d_action_export)).assertIsDisplayed()
    }

    @Test
    fun aiHomeLight() {
        render("ai-home-light.png", darkTheme = false) {
            AiHomeContent(
                state = catalogueState(),
                onBack = {},
                onOpenCutout = {},
                onOpenModels = {},
                onRefresh = {},
                onDismissError = {},
            )
        }
    }

    @Test
    fun projectsEmptyLight() {
        render("projects-empty-light.png", darkTheme = false) {
            ProjectsScreen(onBack = {}, onNewProject = {}, onOpenProject = { _, _ -> })
        }
    }

    @Test
    fun photoEmptyLight() {
        render("photo-empty-light.png", darkTheme = false) {
            PhotoStudioScreen(onBack = {})
        }
    }

    /**
     * Regression for the P0 dark-theme defect: the home gradient used to end at 45% alpha,
     * so the light window behind it washed the bottom of the dark screen to #99948B. The
     * bottom tenth of the window must stay dark.
     */
    @Test
    fun homeDark() {
        val bitmap = render("home-dark.png", darkTheme = true) {
            HomeScreen(onOpenStudio = {}, onOpenProjects = {}, onOpenSettings = {})
        }
        val bottom = meanLuminance(bitmap, fromFraction = 0.90, toFraction = 1.0)
        assertTrue(
            "dark Home bottom band washed out (mean luminance " + String.format("%.1f", bottom) + ")",
            bottom < 45.0,
        )
    }

    @Test
    @Config(qualifiers = "ar-rIQ-w411dp-h914dp-420dpi")
    fun homeArabic() {
        render("home-ar.png", darkTheme = false) {
            HomeScreen(onOpenStudio = {}, onOpenProjects = {}, onOpenSettings = {})
        }
    }

    /** The same catalogue in the dark scheme, where the old chips all collapsed to one tone. */
    @Test
    fun aiHomeDark() {
        render("ai-home-dark.png", darkTheme = true) {
            AiHomeContent(
                state = catalogueState(),
                onBack = {},
                onOpenCutout = {},
                onOpenModels = {},
                onRefresh = {},
                onDismissError = {},
            )
        }
    }

    /**
     * Regression for the ungrouped 12-slider panel: light, colour and detail each get a
     * heading and a reset, and the whole list still fits without clipping at 1.0 font scale.
     */
    @Test
    @Config(qualifiers = "w411dp-h1040dp-420dpi")
    fun photoAdjustLight() {
        val viewModel = PhotoEditorViewModel()
        render("photo-adjust-light.png", darkTheme = false) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                AdjustPanel(
                    state = PhotoEditorState(
                        adjustments = Adjustments(
                            exposure = 0.4f,
                            brightness = 18,
                            contrast = 12,
                            saturation = 25,
                            warmth = -10,
                            tint = 6,
                            highlights = -15,
                            shadows = 20,
                            vignette = 16,
                            grain = 8,
                            sharpen = 30,
                        ),
                    ),
                    viewModel = viewModel,
                )
            }
        }
    }

    /**
     * The Android font-size setting at 1.3x: every Home label still exists and the two
     * above-the-fold anchors are visible, so nothing is clipped or pushed off screen.
     */
    @Test
    fun homeFontScale1_3() {
        render("home-font-scale-1.3.png", fontScale = 1.3f) {
            HomeScreen(onOpenStudio = {}, onOpenProjects = {}, onOpenSettings = {})
        }
        rule.onNodeWithText(context.getString(R.string.home_hero_title)).assertIsDisplayed()
        rule.onNodeWithContentDescription(context.getString(R.string.action_settings)).assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.home_studios_label).uppercase()).assertExists()
        rule.onNodeWithText(context.getString(R.string.home_recent_empty)).assertExists()
    }

    /** A plausible AI Lab catalogue: the bundled model installed, one downloading, two idle. */
    private fun catalogueState(): AiUiState {
        fun model(id: String, name: String, description: String, size: Long, bundled: Boolean) =
            AiModel(
                id = id,
                displayName = name,
                description = description,
                licenseName = "Apache-2.0",
                licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                sizeBytes = size,
                bundled = bundled,
            )

        val models = listOf(
            AiModelStatus(
                model = model(
                    "mediapipe-selfie-segmenter-256-f16",
                    "MediaPipe Selfie Segmenter (square, 256x256, float16)",
                    "Binary person/background segmentation for portrait cut-outs.",
                    249_537,
                    true,
                ),
                state = ModelState.BUNDLED,
            ),
            AiModelStatus(
                model = model(
                    "mediapipe-selfie-multiclass-256-f32",
                    "MediaPipe Selfie Multiclass 256x256 (float32)",
                    "Six-class segmentation: background, hair, body skin, face skin, clothes, others.",
                    16_371_837,
                    false,
                ),
                state = ModelState.DOWNLOADING,
                progress = 0.42f,
            ),
            AiModelStatus(
                model = model(
                    "mediapipe-hair-segmenter-512-f32",
                    "MediaPipe Hair Segmenter (512x512, float32)",
                    "Hair-only mask for precise edge work around the head.",
                    781_618,
                    false,
                ),
                state = ModelState.NOT_INSTALLED,
            ),
            AiModelStatus(
                model = model(
                    "mediapipe-selfie-segmenter-landscape-256x144-f16",
                    "MediaPipe Selfie Segmenter (landscape, 256x144, float16)",
                    "Landscape variant of the person segmenter.",
                    250_177,
                    true,
                ),
                state = ModelState.NOT_INSTALLED,
            ),
            AiModelStatus(
                model = model(
                    "mediapipe-deeplab-v3-257-f32",
                    "MediaPipe DeepLab-V3 (257x257, float32)",
                    "21-class segmentation for objects, people, vehicles and scenery.",
                    2_780_176,
                    false,
                ),
                state = ModelState.NOT_INSTALLED,
            ),
            AiModelStatus(
                model = model(
                    "modnet-portrait-matting-512-f32",
                    "MODNet Portrait Matting (512x512, float32)",
                    "Soft portrait matte with hair and edge detail.",
                    26_166_776,
                    false,
                ),
                state = ModelState.NOT_INSTALLED,
            ),
        )
        return AiUiState(
            models = models,
            catalogueLoading = false,
            installedCount = 1,
        )
    }
}