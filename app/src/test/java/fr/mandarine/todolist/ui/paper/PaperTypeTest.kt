package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaperTypeTest {

    @Test
    fun `should give every line the same pitch so glyphs land on their own rule`() {
        val onTheRule = listOf(PaperType.itemLine, PaperType.listLine, PaperType.margin)

        assertTrue(onTheRule.all { it.lineHeight == PaperType.base.lineHeight })
        assertTrue(
            onTheRule.all {
                it.lineHeightStyle?.alignment == LineHeightStyle.Alignment.Bottom
            }
        )
    }

    @Test
    fun `should size the item line under the list line so hierarchy comes from size`() {
        assertTrue(PaperType.itemLine.fontSize < PaperType.listLine.fontSize)
        assertTrue(PaperType.margin.fontSize < PaperType.itemLine.fontSize)
    }

    @Test
    fun `should hold the margin hand at the floor its numerals stay legible at`() {
        assertEquals(14.sp, PaperType.margin.fontSize)
    }

    @Test
    fun `should drop the pitch for text that is not seated on a rule`() {
        assertTrue(PaperType.caption.lineHeight < PaperType.base.lineHeight)
        assertTrue(PaperType.prose.lineHeight < PaperType.base.lineHeight)
        assertEquals(PaperType.itemLine.fontSize, PaperType.prose.fontSize)
    }

    @Test
    fun `should write the whole type scale in the bundled hand without tracking`() {
        val scale = listOf(
            PaperType.typography.bodyLarge,
            PaperType.typography.bodyMedium,
            PaperType.typography.bodySmall,
            PaperType.typography.titleLarge,
            PaperType.typography.titleMedium,
            PaperType.typography.labelMedium,
            PaperType.typography.labelSmall
        )

        assertTrue(scale.all { it.fontFamily == PaperType.hand })
        assertTrue(scale.all { it.letterSpacing == 0.sp })
    }

    @Test
    fun `should type an item and a submitted item at the very same size`() {
        assertEquals(
            PaperType.typography.bodyMedium.fontSize,
            PaperType.typography.bodyLarge.fontSize
        )
    }

    @Test
    fun `should shrink an emoji run so it does not tower over the hand`() {
        val written = handwritten("🍎 Apples")

        assertEquals("🍎 Apples", written.text)
        assertEquals(listOf(SpanStyle(fontSize = 0.8.em)), written.spanStyles.map { it.item })
        assertEquals(0, written.spanStyles.single().start)
        assertEquals(2, written.spanStyles.single().end)
    }

    @Test
    fun `should hold a joined emoji sequence together as one run`() {
        val written = handwritten("👩‍👩‍👧 Family")

        assertEquals(1, written.spanStyles.size)
        assertEquals(0, written.spanStyles.single().start)
        assertEquals(8, written.spanStyles.single().end)
    }

    @Test
    fun `should leave text without pictographs entirely unstyled`() {
        val written = handwritten("Acheter du pain")

        assertEquals("Acheter du pain", written.text)
        assertTrue(written.spanStyles.isEmpty())
    }

    @Test
    fun `should shrink a trailing emoji as well as a leading one`() {
        val written = handwritten("Milk 🥛")

        assertEquals(5, written.spanStyles.single().start)
        assertEquals(7, written.spanStyles.single().end)
    }
}
