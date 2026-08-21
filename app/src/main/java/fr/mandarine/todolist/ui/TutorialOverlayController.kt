package fr.mandarine.todolist.ui

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.domain.TutorialStep
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialCaption
import fr.mandarine.todolist.presentation.TutorialDirector
import fr.mandarine.todolist.presentation.TutorialOverlay
import fr.mandarine.todolist.presentation.TutorialStage
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.presentation.TutorialViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class TutorialOverlayController(
    private val tutorialViewModel: TutorialViewModel,
    private val scope: CoroutineScope
) : TutorialOverlay {

    private var overlayView: View? = null
    private var handView: View? = null
    private var bannerCard: MaterialCardView? = null
    private var bannerText: MaterialTextView? = null
    private var captionPill: MaterialCardView? = null
    private var captionText: MaterialTextView? = null
    private var sceneJob: Job? = null
    private var dots: List<View> = emptyList()
    private var primaryColor: Int = 0
    private var hollowColor: Int = 0

    fun attachToActivity(activity: AppCompatActivity) {
        val decorView = activity.window.decorView as ViewGroup
        val inflated = LayoutInflater.from(activity)
            .inflate(R.layout.overlay_tutorial, decorView, false)
        overlayView = inflated
        handView = inflated.findViewById(R.id.tutorialHand)
        bannerCard = inflated.findViewById(R.id.tutorialBannerCard)
        bannerText = inflated.findViewById(R.id.tutorialBannerText)
        captionPill = inflated.findViewById(R.id.tutorialCaptionPill)
        captionText = inflated.findViewById(R.id.tutorialCaptionText)
        val skipButton = inflated.findViewById<View>(R.id.btnTutorialSkip)

        primaryColor = resolveAttrColor(activity, com.google.android.material.R.attr.colorPrimary)
        hollowColor = resolveAttrColor(activity, com.google.android.material.R.attr.colorOutlineVariant)

        dots = listOf(
            inflated.findViewById(R.id.tutorialDot1),
            inflated.findViewById(R.id.tutorialDot2),
            inflated.findViewById(R.id.tutorialDot3),
            inflated.findViewById(R.id.tutorialDot4),
            inflated.findViewById(R.id.tutorialDot5),
        )
        dots.forEach { it.backgroundTintList = ColorStateList.valueOf(hollowColor) }

        handView!!.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ColorUtils.setAlphaComponent(primaryColor, 130))
        }
        handView!!.translationY = 4000f

        skipButton.setOnClickListener { onSkipRequested() }
        decorView.addView(inflated)
    }

    fun detachFromActivity() {
        sceneJob?.cancel()
        sceneJob = null
        val ov = overlayView ?: return
        (ov.parent as? ViewGroup)?.removeView(ov)
        overlayView = null
        handView = null
        bannerCard = null
        bannerText = null
        captionPill = null
        captionText = null
        dots = emptyList()
    }

    fun onSkipRequested() {
        sceneJob?.cancel()
        tutorialViewModel.skip()
        scope.launch(Dispatchers.Main) { fadeOutAndDetach() }
    }

    fun handleState(state: TutorialUiState, activity: AppCompatActivity) {
        val stage = activity as? TutorialStage ?: return
        if (state is TutorialUiState.ReadyToStart && overlayView == null) {
            attachToActivity(activity)
        }
        updateProgressDots(state)

        val director = TutorialDirector(stage, this, tutorialViewModel) { LocalDate.now() }
        when (state) {
            TutorialUiState.Hidden -> {}
            TutorialUiState.ReadyToStart -> {
                if (stage.screen == TutorialScreen.LISTS) {
                    showOverlay()
                    launchScene { director.playOpening() }
                }
            }
            is TutorialUiState.Active -> {
                if (entersScreen(state.step, stage.screen)) showOverlay()
                launchScene { director.play(state.step) }
            }
            TutorialUiState.Dismissed -> launchScene { fadeOutAndDetach() }
        }
    }

    private fun entersScreen(step: TutorialStep, screen: TutorialScreen): Boolean =
        (step == TutorialStep.OPEN_LIST && screen == TutorialScreen.ITEMS) ||
            (step == TutorialStep.DELETE_LIST && screen == TutorialScreen.LISTS)

    private fun updateProgressDots(state: TutorialUiState) {
        if (dots.isEmpty()) return
        val filledCount = when (state) {
            TutorialUiState.ReadyToStart -> 1
            is TutorialUiState.Active -> state.step.ordinal + 1
            else -> 0
        }
        dots.forEachIndexed { index, dot ->
            dot.backgroundTintList = ColorStateList.valueOf(
                if (index < filledCount) primaryColor else hollowColor
            )
        }
    }

    private fun launchScene(block: suspend () -> Unit) {
        sceneJob?.cancel()
        sceneJob = scope.launch(Dispatchers.Main) { block() }
    }

    private fun showOverlay() {
        overlayView?.visibility = View.VISIBLE
    }

    // ── TutorialOverlay ──

    override suspend fun glideTo(bounds: TutorialBounds, durationMillis: Long) {
        val hand = handView ?: return
        val overlay = overlayView ?: return

        val ovLoc = IntArray(2)
        overlay.getLocationOnScreen(ovLoc)

        val handW = hand.width.takeIf { it > 0 }
            ?: (52 * overlay.resources.displayMetrics.density).toInt()
        val handH = hand.height.takeIf { it > 0 } ?: handW

        val tx = (bounds.left - ovLoc[0] + bounds.width / 2 - handW / 2).toFloat()
        val ty = (bounds.top - ovLoc[1] + bounds.height / 2 - handH / 2).toFloat()

        awaitAnimation(hand.animate().translationX(tx).translationY(ty).setDuration(durationMillis))
    }

    override suspend fun tap() {
        val hand = handView ?: return
        awaitAnimation(hand.animate().scaleX(0.72f).scaleY(0.72f).setDuration(100))
        delay(130)
        awaitAnimation(hand.animate().scaleX(1f).scaleY(1f).setDuration(100))
    }

    override suspend fun grip() {
        val hand = handView ?: return
        awaitAnimation(hand.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150))
    }

    override suspend fun release() {
        val hand = handView ?: return
        awaitAnimation(hand.animate().scaleX(1f).scaleY(1f).setDuration(150))
    }

    override suspend fun showCaption(caption: TutorialCaption, below: TutorialBounds) {
        val pill = captionPill ?: return
        val tv = captionText ?: return
        val overlay = overlayView ?: return
        tv.text = captionTextFor(caption)
        tv.alpha = 1f
        val ovLoc = IntArray(2)
        overlay.getLocationOnScreen(ovLoc)
        val gap = 12 * overlay.resources.displayMetrics.density
        pill.translationY = (below.top - ovLoc[1] + below.height) + gap
        pill.alpha = 0f
        pill.visibility = View.VISIBLE
        animateAlpha(pill, 1f, 300)
    }

    override suspend fun updateCaption(caption: TutorialCaption) {
        val tv = captionText ?: return
        animateAlpha(tv, 0f, 150)
        tv.text = captionTextFor(caption)
        animateAlpha(tv, 1f, 150)
    }

    override suspend fun hideCaption() {
        val pill = captionPill ?: return
        animateAlpha(pill, 0f, 300)
        pill.visibility = View.INVISIBLE
    }

    override suspend fun showBanner(content: TutorialBannerContent) {
        val card = bannerCard ?: return
        val tv = bannerText ?: return
        val overlay = overlayView ?: return

        tv.text = "🔔 ${content.listName}${formatBannerDate(content.dueDate)}"
        card.visibility = View.INVISIBLE

        card.measure(
            View.MeasureSpec.makeMeasureSpec(overlay.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val statusBarInset = androidx.core.view.ViewCompat.getRootWindowInsets(overlay)
            ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())?.top ?: 0
        card.translationY = -(card.measuredHeight.toFloat() + 32f)
        card.visibility = View.VISIBLE
        delay(50)

        awaitAnimation(card.animate().translationY(statusBarInset.toFloat()).setDuration(350))
        delay(2200)
        awaitAnimation(card.animate().translationY(-(card.height.toFloat() + 32f)).setDuration(350))
        card.visibility = View.GONE
    }

    private fun captionTextFor(caption: TutorialCaption): String {
        val context = overlayView?.context ?: return ""
        return when (caption) {
            TutorialCaption.TARGET_DATE ->
                "📅 " + context.getString(R.string.date_kind_target_caption)
            TutorialCaption.DUE_DATE ->
                "⏰ " + context.getString(R.string.date_kind_due_caption)
        }
    }

    private fun formatBannerDate(dueDate: LocalDate?): String {
        if (dueDate == null) return ""
        val locale = Locale.getDefault(Locale.Category.FORMAT)
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "dM")
        return " ⏰ ${dueDate.format(DateTimeFormatter.ofPattern(pattern, locale))}"
    }

    private suspend fun fadeOutAndDetach() {
        val ov = overlayView ?: return
        awaitAnimation(ov.animate().alpha(0f).setDuration(500))
        detachFromActivity()
    }

    private suspend fun animateAlpha(view: View, target: Float, duration: Long) {
        awaitAnimation(view.animate().alpha(target).setDuration(duration))
    }

    private suspend fun awaitAnimation(animator: android.view.ViewPropertyAnimator) {
        suspendCancellableCoroutine { cont ->
            animator.withEndAction { if (cont.isActive) cont.resume(Unit) }
            cont.invokeOnCancellation { animator.cancel() }
            animator.start()
        }
    }

    private fun resolveAttrColor(activity: AppCompatActivity, attrRes: Int): Int {
        val tv = TypedValue()
        activity.theme.resolveAttribute(attrRes, tv, true)
        return tv.data
    }
}
