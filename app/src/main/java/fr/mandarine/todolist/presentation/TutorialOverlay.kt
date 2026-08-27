package fr.mandarine.todolist.presentation

enum class TutorialCaption {
    TARGET_DATE,
    DUE_DATE
}

/**
 * What the tour says while it does a thing, one line per scene.
 *
 * The demonstration used to be silent apart from the two date captions, and a
 * reader watching a hand pull a row sideways could see *that* it happened without
 * being told it was theirs to do. These are the sentences; the two captions stay
 * what they are — they point at a particular glyph rather than at a scene.
 */
enum class TutorialLine {
    WRITE_A_LIST,
    A_DAY_AND_A_NOTE,
    OPEN_IT,
    WRITE_ITEMS,
    TICK_AND_MOVE,
    EDIT_AND_TEAR
}

interface TutorialOverlay {
    suspend fun narrate(line: TutorialLine)

    suspend fun glideTo(bounds: TutorialBounds, durationMillis: Long)

    /**
     * Moves the hand without waiting for it to arrive, for the one case where it is
     * not travelling to something but holding something that is travelling with it.
     */
    suspend fun dragTo(bounds: TutorialBounds)

    suspend fun tap()

    suspend fun grip()

    suspend fun release()

    suspend fun showCaption(caption: TutorialCaption, below: TutorialBounds)

    suspend fun updateCaption(caption: TutorialCaption)

    suspend fun hideCaption()

    suspend fun showBanner(content: TutorialBannerContent)
}
