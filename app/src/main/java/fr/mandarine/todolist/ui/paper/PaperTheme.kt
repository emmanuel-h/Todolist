package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

internal fun paperColorScheme(palette: PaperPalette): ColorScheme = paperBaseline(palette).copy(
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

private fun paperBaseline(palette: PaperPalette): ColorScheme =
    if (palette.byLamplight) darkColorScheme() else lightColorScheme()

/**
 * The pad is one pad; the light in the room is what changes. Which sheet the app
 * writes on is read here once, so every mark, rule and shadow downstream is a
 * value taken from the palette rather than a branch taken on the theme.
 */
@Composable
fun paperUnderTheLight(): PaperPalette =
    if (isSystemInDarkTheme()) PaperPalette.night else PaperPalette.light

/**
 * Wraps the whole app and establishes the paper it is drawn on. Called once, in
 * `TodoListsActivity.setContent`.
 *
 * ### CompositionLocals — how the theme reaches the leaves
 * A composable deep in the tree needs the palette, but threading a `PaperPalette`
 * parameter through every function between here and there would be intolerable. A
 * `CompositionLocal` is an implicit parameter for a subtree:
 * `CompositionLocalProvider(LocalPaperPalette provides palette) { … }` makes it
 * available to everything inside, read as `LocalPaperPalette.current`.
 *
 * Six are provided here, and between them they are the design system:
 * - `LocalPaperPalette` — every colour (`PaperPalette`)
 * - `LocalPagePitch` — the distance between ruled lines, derived from the largest
 *   text style so the rules fit whatever font scale the reader has chosen
 * - `LocalPageFit` / `LocalPaperGutter` — how the sheet sits in the window, and how
 *   wide its margin is
 * - `LocalRuledHand` — the type scale, seated on that pitch
 * - `LocalPaperVeil` — the shared dimming used behind dialogs
 *
 * `MaterialTheme` is still wrapped around it. Material 3 components are used for
 * their behaviour and accessibility, so they must be handed a `ColorScheme`; that
 * scheme is derived from the palette by `paperColorScheme`, which is what stops a
 * stock component drawing in Material purple. `LocalIndication` is replaced so
 * every ripple in the app becomes a pen mark instead.
 *
 * The whole theme depends on nothing but the palette. To try the app under a
 * different light, pass a different [palette] — that is what the previews do.
 */
@Composable
fun PaperTheme(
    palette: PaperPalette = paperUnderTheLight(),
    content: @Composable () -> Unit
) {
    val colorScheme = remember(palette) { paperColorScheme(palette) }
    val pitch = maxOf(pagePitch(PaperType.itemLine), pagePitch(PaperType.listLine))
    val hand = rememberRuledHand(pitch)
    val veil = remember { PaperVeil() }
    val fit = rememberPageFit()
    CompositionLocalProvider(
        LocalPaperPalette provides palette,
        LocalPagePitch provides pitch,
        LocalPageFit provides fit,
        LocalPaperGutter provides gutterFor(fit),
        LocalRuledHand provides hand,
        LocalPaperVeil provides veil
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
