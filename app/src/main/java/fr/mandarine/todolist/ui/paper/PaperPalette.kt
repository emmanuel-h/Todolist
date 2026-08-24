package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class PaperPalette(
    val paper: Color,
    val paperSheet: Color,
    val paperShade: Color,
    val paperShadeDeep: Color,
    val paperSunken: Color,
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
    val rule: Color,
    val vignette: Color,
    val keyboardSeam: Color,
    val shadow: Color
) {
    val inkRest: Color get() = ink

    val inkMargin: Color get() = pencil

    val inkLive: Color get() = inkBlue

    val inkDanger: Color get() = inkRed

    companion object {
        val light = PaperPalette(
            paper = Color(0xFFFAF5EA),
            paperSheet = Color(0xFFFFFCF4),
            paperShade = Color(0xFFF5EFE1),
            paperShadeDeep = Color(0xFFEFE7D5),
            paperSunken = Color(0xFFE7DEC8),
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
            stickyNote = Color(0xFFEBDCA4),
            stickyNoteMid = Color(0xFFDCCB90),
            stickyNoteBack = Color(0xFFCBB97C),
            stickyNoteEdge = Color(0xFFB0995C),
            rule = Color(0xFFD5CCB6),
            vignette = Color(0x063A2A10),
            keyboardSeam = Color(0x0F3A2A10),
            shadow = Color(0xFF3A2A10)
        )
    }
}

val LocalPaperPalette = staticCompositionLocalOf { PaperPalette.light }
