package id.gustyxganteng.agent.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val M3ExpressiveDarkColorScheme = darkColorScheme(
    primary = M3Primary,
    onPrimary = M3OnPrimary,
    primaryContainer = M3PrimaryContainer,
    onPrimaryContainer = M3OnPrimaryContainer,
    secondary = M3Secondary,
    onSecondary = M3OnSecondary,
    secondaryContainer = M3SecondaryContainer,
    onSecondaryContainer = M3OnSecondaryContainer,
    tertiary = M3Tertiary,
    onTertiary = M3OnTertiary,
    tertiaryContainer = M3TertiaryContainer,
    onTertiaryContainer = M3OnTertiaryContainer,
    background = M3Surface,
    onBackground = M3TextPrimary,
    surface = M3SurfaceContainer,
    onSurface = M3TextPrimary,
    surfaceVariant = M3SurfaceContainerHigh,
    onSurfaceVariant = M3TextSecondary,
    surfaceContainerLowest = M3SurfaceContainerLowest,
    surfaceContainerLow = M3SurfaceContainerLow,
    surfaceContainer = M3SurfaceContainer,
    surfaceContainerHigh = M3SurfaceContainerHigh,
    surfaceContainerHighest = M3SurfaceContainerHighest,
    surfaceDim = M3SurfaceDim,
    surfaceBright = M3SurfaceBright,
    outline = M3Outline,
    outlineVariant = M3OutlineVariant,
    error = M3Error,
    onError = M3OnError,
    errorContainer = M3ErrorContainer,
    onErrorContainer = M3OnErrorContainer
)

private val M3ExpressiveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun GGAgentTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = M3ExpressiveDarkColorScheme,
        typography = M3ExpressiveTypography,
        shapes = M3ExpressiveShapes,
        content = content
    )
}
