package au.howesthat.whirl.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// Neutral surfaces stay grey at every elevation; orange is reserved for emphasis and actions.
internal val LightColors = lightColorScheme(
    primary = Color(0xFFAD3F00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2CC),
    onPrimaryContainer = Color(0xFF642500),
    inversePrimary = Color(0xFFFF9B59),
    secondary = Color(0xFF595959),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E5E5),
    onSecondaryContainer = Color(0xFF242424),
    tertiary = Color(0xFF666666),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEEEEEE),
    onTertiaryContainer = Color(0xFF303030),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF595959),
    surfaceTint = Color(0xFF737373),
    inverseSurface = Color(0xFF2C2C2C),
    inverseOnSurface = Color(0xFFF5F5F5),
    outline = Color(0xFF737373),
    outlineVariant = Color(0xFFD6D6D6),
    scrim = Color.Black,
    surfaceBright = Color(0xFFFAFAFA),
    surfaceDim = Color(0xFFDEDEDE),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFEFEFEF),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE2E2E2),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9B59),
    onPrimary = Color(0xFF321200),
    primaryContainer = Color(0xFF48250F),
    onPrimaryContainer = Color(0xFFFFDCC4),
    inversePrimary = Color(0xFFAD3F00),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF242424),
    secondaryContainer = Color(0xFF303030),
    onSecondaryContainer = Color(0xFFE5E5E5),
    tertiary = Color(0xFFCBCBCB),
    onTertiary = Color(0xFF303030),
    tertiaryContainer = Color(0xFF383838),
    onTertiaryContainer = Color(0xFFEEEEEE),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF181818),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceTint = Color(0xFFBDBDBD),
    inverseSurface = Color(0xFFE5E5E5),
    inverseOnSurface = Color(0xFF242424),
    outline = Color(0xFF909090),
    outlineVariant = Color(0xFF444444),
    scrim = Color.Black,
    surfaceBright = Color(0xFF383838),
    surfaceDim = Color(0xFF121212),
    surfaceContainerLowest = Color(0xFF101010),
    surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF242424),
    surfaceContainerHigh = Color(0xFF2B2B2B),
    surfaceContainerHighest = Color(0xFF333333),
)

@Composable
fun WhirlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    /** Multiplies Android's font size for all of the app's text (Settings → Text size). */
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors) {
        if (textScale == 1f) {
            // Keep the system Density: on Android 14+ it carries the non-linear font scaling for large text.
            content()
        } else {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * textScale), content = content)
        }
    }
}
