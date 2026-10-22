package au.howesthat.whirl.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Test
import kotlin.test.assertTrue

class ThemeContrastTest {
    @Test
    fun `light and dark themes keep text and actions readable`() {
        listOf(LightColors, DarkColors).forEach { scheme ->
            val surfaces = listOf(scheme.background, scheme.surface, scheme.surfaceContainerHigh, scheme.surfaceContainerHighest)
            surfaces.forEach { surface ->
                readable(scheme.onSurface, surface)
                readable(scheme.onSurfaceVariant, surface)
                readable(scheme.primary, surface)
            }
            readable(scheme.onPrimary, scheme.primary)
            readable(scheme.onPrimaryContainer, scheme.primaryContainer)
            readable(scheme.onSecondaryContainer, scheme.secondaryContainer)
        }
    }

    @Test
    fun `all neutral surface elevations stay grey`() {
        listOf(LightColors, DarkColors).forEach { scheme ->
            neutralSurfaces(scheme).forEach { color ->
                assertTrue(color.red == color.green && color.green == color.blue, "Surface must remain neutral: $color")
            }
        }
    }

    private fun neutralSurfaces(scheme: ColorScheme) = listOf(
        scheme.background, scheme.surface, scheme.surfaceVariant, scheme.surfaceBright, scheme.surfaceDim,
        scheme.surfaceContainerLowest, scheme.surfaceContainerLow, scheme.surfaceContainer,
        scheme.surfaceContainerHigh, scheme.surfaceContainerHighest,
    )

    private fun readable(foreground: Color, background: Color) {
        val a = foreground.luminance()
        val b = background.luminance()
        val ratio = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
        assertTrue(ratio >= 4.5f, "Text contrast $ratio must reach 4.5:1 ($foreground on $background)")
    }
}
