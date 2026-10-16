package id.gustyxganteng.agent.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GeminiDarkColorScheme = darkColorScheme(
    primary = GeminiPrimary,
    onPrimary = GeminiBackground,
    primaryContainer = GeminiActiveChipContainer,
    onPrimaryContainer = GeminiActiveChipContent,
    secondary = GeminiPrimary,
    onSecondary = GeminiBackground,
    secondaryContainer = GeminiActiveChipContainer,
    onSecondaryContainer = GeminiActiveChipContent,
    background = GeminiBackground,
    onBackground = GeminiTextPrimary,
    surface = GeminiSurface,
    onSurface = GeminiTextPrimary,
    surfaceVariant = GeminiSurfaceVariant,
    onSurfaceVariant = GeminiTextSecondary,
    outline = GeminiOutline,
    outlineVariant = GeminiSurfaceVariant,
    error = GeminiError,
    onError = GeminiBackground,
    errorContainer = GeminiErrorContainer,
    onErrorContainer = GeminiError
)

@Composable
fun GGAgentTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GeminiDarkColorScheme,
        content = content
    )
}
