package fr.mandarine.todolist.presentation

enum class TutorialCaption {
    TARGET_DATE,
    DUE_DATE
}

interface TutorialOverlay {
    suspend fun glideTo(bounds: TutorialBounds, durationMillis: Long)

    suspend fun tap()

    suspend fun grip()

    suspend fun release()

    suspend fun showCaption(caption: TutorialCaption, below: TutorialBounds)

    suspend fun updateCaption(caption: TutorialCaption)

    suspend fun hideCaption()

    suspend fun showBanner(content: TutorialBannerContent)
}
