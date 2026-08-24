package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

internal fun paperColorScheme(palette: PaperPalette): ColorScheme = lightColorScheme(
    primary = palette.inkBlue,
    onPrimary = palette.paper,
    primaryContainer = palette.inkBluePale,
    onPrimaryContainer = palette.inkBlueDeep,
    inversePrimary = palette.inkBlueFaded,
    secondary = palette.pencil,
    onSecondary = palette.paper,
    secondaryContainer = palette.paperShadeDeep,
    onSecondaryContainer = palette.ink,
    tertiary = palette.inkSoft,
    onTertiary = palette.paper,
    tertiaryContainer = palette.paperShade,
    onTertiaryContainer = palette.ink,
    background = palette.paper,
    onBackground = palette.ink,
    surface = palette.paper,
    onSurface = palette.ink,
    surfaceVariant = palette.paperShade,
    onSurfaceVariant = palette.inkSoft,
    surfaceTint = Color.Transparent,
    inverseSurface = palette.ink,
    inverseOnSurface = palette.paper,
    error = palette.inkRed,
    onError = palette.paper,
    errorContainer = palette.inkRedWash,
    onErrorContainer = palette.inkRedDeep,
    outline = palette.pencil,
    outlineVariant = palette.rule,
    surfaceBright = palette.paperSheet,
    surfaceDim = palette.paperSunken,
    surfaceContainerLowest = palette.paper,
    surfaceContainerLow = palette.paperShade,
    surfaceContainer = palette.paperShade,
    surfaceContainerHigh = palette.paperShadeDeep,
    surfaceContainerHighest = palette.paperSunken
)

@Composable
fun PaperTheme(
    palette: PaperPalette = PaperPalette.light,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(palette) { paperColorScheme(palette) }
    val pitch = maxOf(pagePitch(PaperType.itemLine), pagePitch(PaperType.listLine))
    val hand = rememberRuledHand(pitch)
    CompositionLocalProvider(
        LocalPaperPalette provides palette,
        LocalPagePitch provides pitch,
        LocalRuledHand provides hand
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = hand.typography
        ) {
            CompositionLocalProvider(
                LocalIndication provides PaperIndication,
                content = content
            )
        }
    }
}
