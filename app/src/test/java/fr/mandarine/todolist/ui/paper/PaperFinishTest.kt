package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TOLERANCE = 0.5f
private val SHEET = Size(1000f, 2000f)

/**
 * The arithmetic behind the flourish, which is the half of it worth pinning: the
 * `ui` package is outside the mutation gate, so what is checked here is the pure
 * geometry rather than the drawing.
 */
class PaperFinishTest {

    private val scrap = Scrap(
        shard = false,
        ink = 0,
        size = 0.5f,
        throwX = 400f,
        throwY = -1200f,
        spin = 6f,
        phase = 0f
    )

    @Test
    fun `should put no ink on the page before the nib has moved`() {
        val nib = tickUpTo(SHEET, 0f)

        assertEquals(2, nib.size)
        assertEquals(nib.first(), nib.last())
    }

    @Test
    fun `should stop the half-drawn tick short of the corner while the pen is still coming down`() {
        val nib = tickUpTo(SHEET, 0.25f)

        assertEquals(2, nib.size)
        assertTrue("$nib", nib.last().y < SHEET.height * 0.64f)
    }

    @Test
    fun `should reach both ends of the tick once the stroke is finished`() {
        val nib = tickUpTo(SHEET, 1f)

        assertEquals(3, nib.size)
        assertEquals(SHEET.width * 0.28f, nib[0].x, TOLERANCE)
        assertEquals(SHEET.height * 0.46f, nib[0].y, TOLERANCE)
        assertEquals(SHEET.width * 0.44f, nib[1].x, TOLERANCE)
        assertEquals(SHEET.height * 0.64f, nib[1].y, TOLERANCE)
        assertEquals(SHEET.width * 0.75f, nib[2].x, TOLERANCE)
        assertEquals(SHEET.height * 0.28f, nib[2].y, TOLERANCE)
    }

    @Test
    fun `should draw no more of the tick when asked for more than the whole of it`() {
        assertEquals(tickUpTo(SHEET, 1f), tickUpTo(SHEET, 4f))
    }

    @Test
    fun `should turn the corner of the tick before the stroke is half spent`() {
        assertEquals(2, tickUpTo(SHEET, 0.3f).size)
        assertEquals(3, tickUpTo(SHEET, 0.5f).size)
    }

    @Test
    fun `should keep the pen at one speed through the corner of the tick`() {
        val down = tickUpTo(SHEET, 0.5f).last()
        val up = tickUpTo(SHEET, 0.6f).last()

        assertTrue("$down then $up", up.x > down.x)
        assertTrue("$down then $up", up.y < down.y)
    }

    /**
     * The down leg is a third of the up leg. A pen given half the stroke for each
     * would crawl through the first and race through the second.
     */
    @Test
    fun `should spend more of the stroke on the long leg of the tick than on the short one`() {
        val corner = tickUpTo(SHEET, 1f)[1]
        val start = tickUpTo(SHEET, 1f)[0]

        var turned = 0f
        var drawn = 0f
        while (drawn <= 1f) {
            if (tickUpTo(SHEET, drawn).size == 3) {
                turned = drawn
                break
            }
            drawn += 0.01f
        }

        assertTrue("turned at $turned", turned in 0.2f..0.35f)
        assertTrue("$start to $corner", corner.y > start.y)
    }

    @Test
    fun `should start every scrap at the hand that threw it`() {
        val from = Offset(300f, 900f)

        assertEquals(from, scrapAt(from, scrap, 0f))
    }

    @Test
    fun `should throw a scrap up before gravity has it back`() {
        val from = Offset(300f, 900f)

        val rising = scrapAt(from, scrap, 0.15f)

        assertTrue("$rising", rising.y < from.y)
        assertTrue("$rising", rising.x > from.x)
    }

    @Test
    fun `should bring a scrap back below the hand by the end of its flight`() {
        val from = Offset(300f, 900f)

        val fallen = scrapAt(from, scrap, 1.25f)

        assertTrue("$fallen", fallen.y > from.y)
    }

    @Test
    fun `should drag a scrap sideways less as it flies than a straight throw would`() {
        val from = Offset.Zero

        val late = scrapAt(from, scrap, 1f).x
        val straight = scrap.throwX

        assertTrue("$late", late < straight)
    }

    @Test
    fun `should keep every scrap at full ink for most of its flight`() {
        assertEquals(1f, scrapFade(0f), TOLERANCE)
        assertEquals(1f, scrapFade(0.72f), TOLERANCE)
    }

    @Test
    fun `should fade a scrap out over the tail of its flight`() {
        val early = scrapFade(0.8f)
        val late = scrapFade(0.95f)

        assertTrue("$early then $late", early > late)
        assertTrue("$early", early < 1f)
        assertEquals(0f, scrapFade(1f), TOLERANCE)
    }
}
