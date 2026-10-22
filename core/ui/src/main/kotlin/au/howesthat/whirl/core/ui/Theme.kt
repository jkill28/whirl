package au.howesthat.whirl.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

// Silver & ember: cool grey surface layers, with warm orange for actions and unread activity.
internal val LightColors = lightColorScheme(
    primary = Color(0xFFA63D0B),
    onPrimary = Color(0xFFF8F0EA),
    primaryContainer = Color(0xFFF4DFD1),
    onPrimaryContainer = Color(0xFF642500),
    inversePrimary = Color(0xFFFFAA72),
    secondary = Color(0xFF58616D),
    onSecondary = Color(0xFFF4F5F7),
    secondaryContainer = Color(0xFFE3E7EC),
    onSecondaryContainer = Color(0xFF252A32),
    tertiary = Color(0xFF626C79),
    onTertiary = Color(0xFFF4F5F7),
    tertiaryContainer = Color(0xFFE8EBEF),
    onTertiaryContainer = Color(0xFF303844),
    background = Color(0xFFECEFF2),
    onBackground = Color(0xFF252A32),
    surface = Color(0xFFF4F5F7),
    onSurface = Color(0xFF252A32),
    surfaceVariant = Color(0xFFE3E7EC),
    onSurfaceVariant = Color(0xFF58616D),
    surfaceTint = Color(0xFF68717E),
    inverseSurface = Color(0xFF29313B),
    inverseOnSurface = Color(0xFFEEF1F4),
    outline = Color(0xFF727C89),
    outlineVariant = Color(0xFFD4DAE1),
    scrim = Color.Black,
    surfaceBright = Color(0xFFF4F5F7),
    surfaceDim = Color(0xFFDBE0E6),
    surfaceContainerLowest = Color(0xFFF4F5F7),
    surfaceContainerLow = Color(0xFFECEFF2),
    surfaceContainer = Color(0xFFE8EBEF),
    surfaceContainerHigh = Color(0xFFE3E7EC),
    surfaceContainerHighest = Color(0xFFDBE0E6),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFFAA72),
    onPrimary = Color(0xFF342011),
    primaryContainer = Color(0xFF493020),
    onPrimaryContainer = Color(0xFFFFDCC4),
    inversePrimary = Color(0xFFA63D0B),
    secondary = Color(0xFFB5BEC9),
    onSecondary = Color(0xFF252A32),
    secondaryContainer = Color(0xFF2B3440),
    onSecondaryContainer = Color(0xFFEEF1F4),
    tertiary = Color(0xFFC2CBD6),
    onTertiary = Color(0xFF303844),
    tertiaryContainer = Color(0xFF343E49),
    onTertiaryContainer = Color(0xFFEEF1F4),
    background = Color(0xFF15191F),
    onBackground = Color(0xFFEEF1F4),
    surface = Color(0xFF232A33),
    onSurface = Color(0xFFEEF1F4),
    surfaceVariant = Color(0xFF2B3440),
    onSurfaceVariant = Color(0xFFB5BEC9),
    surfaceTint = Color(0xFFB5BEC9),
    inverseSurface = Color(0xFFE3E7EC),
    inverseOnSurface = Color(0xFF252A32),
    outline = Color(0xFF8D99A7),
    outlineVariant = Color(0xFF3B444F),
    scrim = Color.Black,
    surfaceBright = Color(0xFF343E49),
    surfaceDim = Color(0xFF15191F),
    surfaceContainerLowest = Color(0xFF15191F),
    surfaceContainerLow = Color(0xFF1B2129),
    surfaceContainer = Color(0xFF232A33),
    surfaceContainerHigh = Color(0xFF2B3440),
    surfaceContainerHighest = Color(0xFF343E49),
)

internal val LightUnreadContainer = Color(0xFFF1ECE8)
internal val DarkUnreadContainer = Color(0xFF2E2B29)
private val LocalUnreadContainer = staticCompositionLocalOf { LightUnreadContainer }

/** Extra surface roles used by the forum lists, alongside the Material color scheme. */
object WhirlThemeTokens {
    val unreadContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalUnreadContainer.current
}

private val WhirlTypography = Typography().let { defaults ->
    Typography(
        titleLarge = defaults.titleLarge.copy(fontSize = 23.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        titleSmall = defaults.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun WhirlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    /** Multiplies Android's font size for all of the app's text (Settings → Text size). */
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val wallpaperColors = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        wallpaperColors -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val unreadContainer = when {
        wallpaperColors -> colors.primary.copy(alpha = 0.08f).compositeOver(colors.surface)
        darkTheme -> DarkUnreadContainer
        else -> LightUnreadContainer
    }
    MaterialTheme(colorScheme = colors, typography = WhirlTypography) {
        CompositionLocalProvider(LocalUnreadContainer provides unreadContainer) {
            if (textScale == 1f) {
                // Keep the system Density: on Android 14+ it carries the non-linear font scaling for large text.
                content()
            } else {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * textScale), content = content)
            }
        }
    }
}
