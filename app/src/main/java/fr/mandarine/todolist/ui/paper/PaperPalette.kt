package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private const val LAMPLIT = 0.18f

@Immutable
data class PaperPalette(
    val paper: Color,
    val paperSheet: Color,
    val paperShade: Color,
    val paperShadeDeep: Color,
    val paperSunken: Color,
    val desk: Color,
    val deskLit: Color,
    val ink: Color,
    val inkDone: Color,
    val inkSoft: Color,
    val pencil: Color,
    val inkBlue: Color,
    val inkBlueDeep: Color,
    val inkBluePale: Color,
    val inkBlueFaded: Color,
    val inkRed: Color,
    val inkRedSoft: Color,
    val inkRedDeep: Color,
    val inkRedWash: Color,
    val inkRedWashLit: Color,
    val inkAmber: Color,
    val stickyNote: Color,
    val stickyNoteMid: Color,
    val stickyNoteBack: Color,
    val stickyNoteEdge: Color,
    val stickyNoteInk: Color,
    val rule: Color,
    val vignette: Color,
    val keyboardSeam: Color,
    val shadow: Color,
    val lift: Color
) {
    companion object {
        val light = PaperPalette(
            paper = Color(0xFFFAF5EA),
            paperSheet = Color(0xFFFFFCF4),
            paperShade = Color(0xFFF5EFE1),
            paperShadeDeep = Color(0xFFEFE7D5),
            paperSunken = Color(0xFFE7DEC8),
            desk = Color(0xFFEFE7D5),
            deskLit = Color(0xFFF3ECDC),
            ink = Color(0xFF2B2420),
            inkDone = Color(0xFF6B6E75),
            inkSoft = Color(0xFF5C6068),
            pencil = Color(0xFF6F6A5E),
            inkBlue = Color(0xFF2E5AA8),
            inkBlueDeep = Color(0xFF16305C),
            inkBluePale = Color(0xFFDEE5F2),
            inkBlueFaded = Color(0xFFA8C0E8),
            inkRed = Color(0xFFA8392F),
            inkRedSoft = Color(0xFFB4655B),
            inkRedDeep = Color(0xFF5A1E18),
            inkRedWash = Color(0xFFEFD7CD),
            inkRedWashLit = Color(0xFFFAE7DF),
            inkAmber = Color(0xFF8F5D12),
            stickyNote = Color(0xFFE0D3B6),
            stickyNoteMid = Color(0xFFD3C5A4),
            stickyNoteBack = Color(0xFFC3B48F),
            stickyNoteEdge = Color(0xFFA99872),
            stickyNoteInk = Color(0xFF2B2420),
            rule = Color(0xFFD5CCB6),
            vignette = Color(0x063A2A10),
            keyboardSeam = Color(0x0F3A2A10),
            shadow = Color(0xFF3A2A10),
            lift = Color(0xFF3A2A10)
        )

        /**
         * The same pad under a lamp rather than a cream page turned inside out: a
         * warm charcoal sheet on a darker desk, chalk-white ink, rules that lie a
         * shade above the paper instead of a shade below it, and pens that keep
         * their colour by giving up their saturation. The pad is a second stock of
         * paper rather than a lamp: it lies a shade above the page by lamplight
         * exactly as it does by daylight, and is written on in the same chalk as
         * everything else.
         */
        val night = PaperPalette(
            paper = Color(0xFF201D19),
            paperSheet = Color(0xFF2A2622),
            paperShade = Color(0xFF1B1815),
            paperShadeDeep = Color(0xFF171512),
            paperSunken = Color(0xFF151311),
            desk = Color(0xFF141210),
            deskLit = Color(0xFF181613),
            ink = Color(0xFFE8E1D4),
            inkDone = Color(0xFF8F8A80),
            inkSoft = Color(0xFFBDB5A6),
            pencil = Color(0xFF9A927F),
            inkBlue = Color(0xFFA9C1EE),
            inkBlueDeep = Color(0xFFD9E4F8),
            inkBluePale = Color(0xFF2C3A55),
            inkBlueFaded = Color(0xFF6E86B8),
            inkRed = Color(0xFFE0897C),
            inkRedSoft = Color(0xFFC4736A),
            inkRedDeep = Color(0xFFF6D5CE),
            inkRedWash = Color(0xFF3A211C),
            inkRedWashLit = Color(0xFF4A2A24),
            inkAmber = Color(0xFFE4A56A),
            stickyNote = Color(0xFF4C443A),
            stickyNoteMid = Color(0xFF443C33),
            stickyNoteBack = Color(0xFF3B342C),
            stickyNoteEdge = Color(0xFF2E2823),
            stickyNoteInk = Color(0xFFE8E1D4),
            rule = Color(0xFF3A342C),
            vignette = Color(0x14060404),
            keyboardSeam = Color(0x14E8E1D4),
            shadow = Color(0xFF060404),
            lift = Color(0xFFF5EFE2)
        )
    }
}

internal val Color.unlit: Boolean get() = luminance() < LAMPLIT

internal val PaperPalette.byLamplight: Boolean get() = paper.unlit

val LocalPaperPalette = staticCompositionLocalOf { PaperPalette.light }
