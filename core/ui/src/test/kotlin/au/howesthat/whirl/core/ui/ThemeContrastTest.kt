package au.howesthat.whirl.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Test
import kotlin.test.assertTrue

class ThemeContrastTest {
    @Test
    fun `light and dark themes keep text and actions readable`() {
        listOf(LightColors to LightUnreadContainer, DarkColors to DarkUnreadContainer).forEach { (scheme, unread) ->
            (greySurfaces(scheme) + unread).forEach { surface ->
                readable(scheme.onSurface, surface)
                readable(scheme.onSurfaceVariant, surface)
                readable(scheme.primary, surface)
            }
            readable(scheme.onPrimary, scheme.primary)
            readable(scheme.onPrimaryContainer, scheme.primaryContainer)
            readable(scheme.primary, scheme.primaryContainer)
            readable(scheme.onSecondary, scheme.secondary)
            readable(scheme.onSecondaryContainer, scheme.secondaryContainer)
            readable(scheme.onTertiary, scheme.tertiary)
            readable(scheme.onTertiaryContainer, scheme.tertiaryContainer)
        }
    }

    @Test
    fun `surface layers keep a cool grey tone and visible separation`() {
        listOf(LightColors, DarkColors).forEach { scheme ->
            greySurfaces(scheme).forEach { color ->
                val range = maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue)
                assertTrue(range <= 0.12f, "Surface must stay within the cool grey palette: $color")
                assertTrue(color.blue >= color.red, "Surface must retain its cool grey tint: $color")
            }
            assertTrue(scheme.surface.luminance() > scheme.background.luminance(), "Reading panels must lift above the page canvas")
            assertTrue(scheme.surface != scheme.surfaceContainerHigh, "Recessed controls and reading panels need separate surface roles")
        }
        greySurfaces(LightColors).forEach { color ->
            assertTrue(color.luminance() in 0.7f..0.94f, "Light surfaces must remain visibly light grey rather than white: $color")
        }
    }

    private fun greySurfaces(scheme: ColorScheme) = listOf(
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
