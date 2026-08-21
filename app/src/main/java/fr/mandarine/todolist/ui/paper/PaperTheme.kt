package fr.mandarine.todolist.ui.paper

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PaperColorScheme = lightColorScheme(
    primary = PaperInk.inkBlue,
    onPrimary = PaperInk.paper,
    primaryContainer = PaperInk.inkBluePale,
    onPrimaryContainer = PaperInk.inkBlueDeep,
    inversePrimary = PaperInk.inkBlueFaded,
    secondary = PaperInk.pencil,
    onSecondary = PaperInk.paper,
    secondaryContainer = PaperInk.paperShadeDeep,
    onSecondaryContainer = PaperInk.ink,
    tertiary = PaperInk.inkSoft,
    onTertiary = PaperInk.paper,
    tertiaryContainer = PaperInk.paperShade,
    onTertiaryContainer = PaperInk.ink,
    background = PaperInk.paper,
    onBackground = PaperInk.ink,
    surface = PaperInk.paper,
    onSurface = PaperInk.ink,
    surfaceVariant = PaperInk.paperShade,
    onSurfaceVariant = PaperInk.inkSoft,
    surfaceTint = Color.Transparent,
    inverseSurface = PaperInk.ink,
    inverseOnSurface = PaperInk.paper,
    error = PaperInk.inkRed,
    onError = PaperInk.paper,
    errorContainer = PaperInk.inkRedWash,
    onErrorContainer = PaperInk.inkRedDeep,
    outline = PaperInk.pencil,
    outlineVariant = PaperInk.rule,
    surfaceBright = PaperInk.paperSheet,
    surfaceDim = PaperInk.paperSunken,
    surfaceContainerLowest = PaperInk.paper,
    surfaceContainerLow = PaperInk.paperShade,
    surfaceContainer = PaperInk.paperShade,
    surfaceContainerHigh = PaperInk.paperShadeDeep,
    surfaceContainerHighest = PaperInk.paperSunken
)

@Composable
fun PaperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaperColorScheme,
        content = content
    )
}
