package fr.mandarine.todolist.ui

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TutorialStep
import fr.mandarine.todolist.presentation.TodoListsState
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class TutorialOverlayController(
    private val tutorialViewModel: TutorialViewModel,
    private val scope: CoroutineScope
) {
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
        if (state is TutorialUiState.ReadyToStart && overlayView == null) {
            attachToActivity(activity)
        }
        updateProgressDots(state)
        when (state) {
            TutorialUiState.Hidden -> {}
            TutorialUiState.ReadyToStart -> {
                if (activity is TodoListsActivity) {
                    showOverlay()
                    launchScene { runScenesOneAndTwo(activity) }
                }
            }
            is TutorialUiState.Active -> handleActiveStep(state.step, activity)
            TutorialUiState.Dismissed -> launchScene { fadeOutAndDetach() }
        }
    }

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

    private fun handleActiveStep(step: TutorialStep, activity: AppCompatActivity) {
        when (step) {
            TutorialStep.CREATE_LIST -> {
                if (activity is TodoListsActivity) launchScene { showBannerThenAdvance(activity) }
            }
            TutorialStep.SET_DUE_DATE -> {
                if (activity is TodoListsActivity) launchScene { tapListRowAndNavigate(activity) }
            }
            TutorialStep.OPEN_LIST -> {
                if (activity is TodoListActivity) {
                    showOverlay()
                    launchScene { runSceneThree(activity) }
                }
            }
            TutorialStep.COMPLETE_AND_REORDER -> {
                if (activity is TodoListActivity) launchScene { runSceneFour(activity) }
            }
            TutorialStep.DELETE_LIST -> {
                if (activity is TodoListActivity) launchScene { navigateBackFromList(activity) }
                else if (activity is TodoListsActivity) {
                    showOverlay()
                    launchScene { runSceneFive(activity) }
                }
            }
        }
    }

    private fun launchScene(block: suspend () -> Unit) {
        sceneJob?.cancel()
        sceneJob = scope.launch(Dispatchers.Main) { block() }
    }

    private fun showOverlay() {
        overlayView?.visibility = View.VISIBLE
    }

    // ── Scene 1 + 2 (TodoListsActivity): type name → set due date → submit ──

    private suspend fun runScenesOneAndTwo(activity: TodoListsActivity) {
        val inlineRow = activity.inlineAddRowInternal
        val editText = inlineRow.findViewById<TextInputEditText>(R.id.editListInlineAdd)
        val dateButton = inlineRow.findViewById<MaterialButton>(R.id.btnListInlineDate)
        val dueDateButton = inlineRow.findViewById<MaterialButton>(R.id.btnListInlineDueDate)
        val submitButton = inlineRow.findViewById<MaterialButton>(R.id.btnListInlineSubmit)
        val fab = activity.fab

        delay(1200)

        glideHandTo(fab, 600)
        delay(400)
        tapAnim()
        fab.performClick()
        delay(700)

        glideHandTo(editText, 400)
        delay(300)
        typeText(editText, "🛒 Groceries")
        hideKeyboard(editText)
        delay(600)

        glideHandTo(dateButton, 500)
        showCaptionPill("📅 " + activity.getString(R.string.date_kind_target_caption), inlineRow)
        delay(1800)

        glideHandTo(dueDateButton, 500)
        updateCaptionPill("⏰ " + activity.getString(R.string.date_kind_due_caption))
        delay(1500)
        tapAnim()
        dueDateButton.performClick()
        delay(1400)

        val tomorrow = LocalDate.now().plusDays(1)
        val duePicker: DatePickerDialog? = activity.lastShownDueDatePicker
        if (duePicker != null) {
            duePicker.updateDate(tomorrow.year, tomorrow.monthValue - 1, tomorrow.dayOfMonth)
            delay(600)
            duePicker.getButton(DialogInterface.BUTTON_POSITIVE)?.performClick()
        } else {
            activity.onInlineDueDatePicked(tomorrow)
        }
        hideCaptionPill()
        delay(600)

        glideHandTo(submitButton, 400)
        delay(400)
        tapAnim()
        submitButton.performClick()

        val content = activity.viewModel.state.first { s ->
            s is TodoListsState.Content && s.activeSummaries.isNotEmpty()
        } as TodoListsState.Content

        val listId = content.activeSummaries.firstOrNull()?.list?.id ?: return
        tutorialViewModel.onDemoListCreated(listId)
    }

    // ── CREATE_LIST step: show notification banner then advance ──

    private suspend fun showBannerThenAdvance(activity: TodoListsActivity) {
        delay(500)
        val state = activity.viewModel.state.value as? TodoListsState.Content
        val summary = state?.activeSummaries?.firstOrNull()
        if (summary != null) {
            showNotificationBanner(summary.list.name, summary.list.dueDate)
        }
        tutorialViewModel.advanceStep()
    }

    // ── SET_DUE_DATE step: tap first list row to navigate into the list ──

    private suspend fun tapListRowAndNavigate(activity: TodoListsActivity) {
        delay(800)
        val recycler = activity.recyclerViewInternal
        val firstChild = recycler.getChildAt(0)
        if (firstChild != null) {
            glideHandTo(firstChild, 500)
            delay(400)
            tapAnim()
            firstChild.performClick()
            tutorialViewModel.advanceStep()
        }
    }

    // ── OPEN_LIST step (TodoListActivity): add 🍎 Apples and 🥖 Bread ──

    private suspend fun runSceneThree(activity: TodoListActivity) {
        delay(1400)
        val recycler = activity.recyclerViewInternal
        val adapter = recycler.adapter as TodoListAdapter

        val inlinePos = (0 until adapter.itemCount).firstOrNull { pos ->
            adapter.getItemViewType(pos) == TodoListAdapter.VIEW_TYPE_INLINE_ADD
        } ?: return

        val holder = recycler.findViewHolderForAdapterPosition(inlinePos)
            as? TodoListAdapter.InlineAddViewHolder ?: return

        val ghostRow = holder.itemView.findViewById<View>(R.id.ghostRow)
        if (ghostRow != null && ghostRow.visibility == View.VISIBLE) {
            glideHandTo(ghostRow, 500)
            delay(400)
            tapAnim()
            ghostRow.performClick()
            delay(700)
        }

        val editText = holder.editText
        val submitButton = holder.itemView.findViewById<MaterialButton>(R.id.btnInlineSubmit)

        typeText(editText, "🍎 Apples")
        hideKeyboard(editText)
        delay(400)
        if (submitButton != null) {
            glideHandTo(submitButton, 300)
            delay(300)
            tapAnim()
            submitButton.performClick()
        }
        delay(900)

        typeText(editText, "🥖 Bread")
        hideKeyboard(editText)
        delay(400)
        if (submitButton != null) {
            glideHandTo(submitButton, 300)
            delay(300)
            tapAnim()
            submitButton.performClick()
        }
        delay(800)

        tutorialViewModel.advanceStep()
    }

    // ── COMPLETE_AND_REORDER step (TodoListActivity) ──

    private suspend fun runSceneFour(activity: TodoListActivity) {
        val recycler = activity.recyclerViewInternal
        val adapter = recycler.adapter as TodoListAdapter
        delay(800)

        // Tap ✓ on 🍎 Apples (position 0) → moves to completed
        val holderApples0 = recycler.findViewHolderForAdapterPosition(0)
            as? TodoListAdapter.ItemViewHolder
        holderApples0?.itemView?.findViewById<MaterialButton>(R.id.btnToggleComplete)?.let { btn ->
            glideHandTo(btn, 500)
            delay(400)
            tapAnim()
            btn.performClick()
            delay(900)
        }

        // Apples is now completed; active=[🥖 Bread(0)], InlineAdd(1), Divider(2), Apples(3)
        delay(300)
        val activeCount = adapter.activeItemCount()
        val applesCompletedPos = activeCount + 1 + 1
        val holderApplesComp = recycler.findViewHolderForAdapterPosition(applesCompletedPos)
            as? TodoListAdapter.ItemViewHolder
        holderApplesComp?.itemView?.findViewById<MaterialButton>(R.id.btnToggleComplete)?.let { btn ->
            glideHandTo(btn, 500)
            delay(400)
            tapAnim()
            btn.performClick() // returns to active below 🥖 Bread
            delay(900)
        }

        // Active=[🥖 Bread(0), 🍎 Apples(1)], InlineAdd(2) — drag Apples to top
        delay(300)
        val holderApplesDrag = recycler.findViewHolderForAdapterPosition(1)
            as? TodoListAdapter.ItemViewHolder
        val dragHandle = holderApplesDrag?.dragHandle
        if (dragHandle != null) {
            glideHandTo(dragHandle, 500)
            delay(400)
            handView?.animate()?.scaleX(1.15f)?.scaleY(1.15f)?.setDuration(150)?.start()
            delay(200)

            val topBefore = recycler.getChildAt(0)
            adapter.moveItem(1, 0)
            delay(100)

            if (topBefore != null) glideHandTo(topBefore, 600)
            delay(400)
            handView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(150)?.start()
            delay(200)

            activity.viewModel.reorderTodos(1, 0)
            delay(800)
        }

        // 🍎 Apples(0), 🥖 Bread(1) — tap ✓ on both
        delay(300)
        recycler.findViewHolderForAdapterPosition(0)
            ?.itemView?.findViewById<MaterialButton>(R.id.btnToggleComplete)?.let { btn ->
                glideHandTo(btn, 400)
                delay(300)
                tapAnim()
                btn.performClick()
                delay(700)
            }

        delay(300)
        recycler.findViewHolderForAdapterPosition(0)
            ?.itemView?.findViewById<MaterialButton>(R.id.btnToggleComplete)?.let { btn ->
                glideHandTo(btn, 400)
                delay(300)
                tapAnim()
                btn.performClick()
                delay(800)
            }

        delay(400)
        tutorialViewModel.advanceStep()
    }

    // ── DELETE_LIST step in TodoListActivity: navigate back ──

    private suspend fun navigateBackFromList(activity: TodoListActivity) {
        delay(600)
        activity.tutorialBackCallback.isEnabled = false
        activity.onBackPressedDispatcher.onBackPressed()
    }

    // ── DELETE_LIST step in TodoListsActivity: tap delete then confirm ──

    private suspend fun runSceneFive(activity: TodoListsActivity) {
        delay(1200)
        val recycler = activity.recyclerViewInternal
        val firstChild = recycler.getChildAt(0) ?: return

        firstChild.findViewById<MaterialButton>(R.id.btnDeleteList)?.let { btn ->
            glideHandTo(btn, 500)
            delay(400)
            tapAnim()
            btn.performClick()
            delay(900)
        }

        firstChild.findViewById<ImageButton>(R.id.btnDeleteConfirm)?.let { btn ->
            glideHandTo(btn, 400)
            delay(400)
            tapAnim()
            btn.performClick()
            delay(1200)
        }

        tutorialViewModel.advanceStep()
    }

    // ── Notification banner ──

    private suspend fun showNotificationBanner(listName: String, dueDate: LocalDate?) {
        val card = bannerCard ?: return
        val tv = bannerText ?: return
        val overlay = overlayView ?: return

        val dateStr = if (dueDate != null) {
            val locale = Locale.getDefault(Locale.Category.FORMAT)
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "dM")
            " ⏰ ${dueDate.format(DateTimeFormatter.ofPattern(pattern, locale))}"
        } else {
            ""
        }
        tv.text = "🔔 $listName$dateStr"
        card.visibility = View.INVISIBLE

        card.measure(
            View.MeasureSpec.makeMeasureSpec(overlay.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val statusBarInset = androidx.core.view.ViewCompat.getRootWindowInsets(overlay)
            ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())?.top ?: 0
        val restingY = statusBarInset.toFloat()
        val initialOffset = -(card.measuredHeight.toFloat() + 32f)
        card.translationY = initialOffset
        card.visibility = View.VISIBLE
        delay(50)

        suspendCancellableCoroutine { cont ->
            val anim = card.animate().translationY(restingY).setDuration(350)
                .withEndAction { if (cont.isActive) cont.resume(Unit) }
            cont.invokeOnCancellation { anim.cancel() }
            anim.start()
        }

        delay(2200)

        val slideOut = -(card.height.toFloat() + 32f)
        suspendCancellableCoroutine { cont ->
            val anim = card.animate().translationY(slideOut).setDuration(350)
                .withEndAction {
                    card.visibility = View.GONE
                    if (cont.isActive) cont.resume(Unit)
                }
            cont.invokeOnCancellation { anim.cancel() }
            anim.start()
        }
    }

    // ── Caption pill helpers ──

    private suspend fun showCaptionPill(text: String, anchor: View) {
        val pill = captionPill ?: return
        val tv = captionText ?: return
        val overlay = overlayView ?: return
        tv.text = text
        tv.alpha = 1f
        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val ovLoc = IntArray(2)
        overlay.getLocationOnScreen(ovLoc)
        val gap = 12 * overlay.resources.displayMetrics.density
        pill.translationY = (anchorLoc[1] - ovLoc[1] + anchor.height) + gap
        pill.alpha = 0f
        pill.visibility = View.VISIBLE
        animateAlpha(pill, 1f, 300)
    }

    private suspend fun updateCaptionPill(text: String) {
        val tv = captionText ?: return
        animateAlpha(tv, 0f, 150)
        tv.text = text
        animateAlpha(tv, 1f, 150)
    }

    private suspend fun hideCaptionPill() {
        val pill = captionPill ?: return
        animateAlpha(pill, 0f, 300)
        pill.visibility = View.INVISIBLE
    }

    private suspend fun animateAlpha(view: View, target: Float, duration: Long) {
        suspendCancellableCoroutine { cont ->
            val anim = view.animate().alpha(target).setDuration(duration)
                .withEndAction { if (cont.isActive) cont.resume(Unit) }
            cont.invokeOnCancellation { anim.cancel() }
            anim.start()
        }
    }

    // ── Fade out and detach ──

    private suspend fun fadeOutAndDetach() {
        val ov = overlayView ?: return
        suspendCancellableCoroutine { cont ->
            val anim = ov.animate().alpha(0f).setDuration(500)
                .withEndAction { if (cont.isActive) cont.resume(Unit) }
            cont.invokeOnCancellation { anim.cancel() }
            anim.start()
        }
        detachFromActivity()
    }

    // ── Hand animation helpers ──

    private suspend fun glideHandTo(targetView: View, durationMs: Long = 500) {
        val hand = handView ?: return
        val overlay = overlayView ?: return

        val tgtLoc = IntArray(2)
        targetView.getLocationOnScreen(tgtLoc)
        val ovLoc = IntArray(2)
        overlay.getLocationOnScreen(ovLoc)

        val handW = hand.width.takeIf { it > 0 }
            ?: (52 * overlay.resources.displayMetrics.density).toInt()
        val handH = hand.height.takeIf { it > 0 } ?: handW

        val tx = (tgtLoc[0] - ovLoc[0] + targetView.width / 2 - handW / 2).toFloat()
        val ty = (tgtLoc[1] - ovLoc[1] + targetView.height / 2 - handH / 2).toFloat()

        suspendCancellableCoroutine { cont ->
            val anim = hand.animate()
                .translationX(tx).translationY(ty)
                .setDuration(durationMs)
                .withEndAction { if (cont.isActive) cont.resume(Unit) }
            cont.invokeOnCancellation { anim.cancel() }
            anim.start()
        }
    }

    private suspend fun tapAnim() {
        val hand = handView ?: return
        suspendCancellableCoroutine { cont ->
            val anim = hand.animate().scaleX(0.72f).scaleY(0.72f).setDuration(100)
                .withEndAction { if (cont.isActive) cont.resume(Unit) }
            cont.invokeOnCancellation { anim.cancel() }
            anim.start()
        }
        delay(130)
        suspendCancellableCoroutine { cont ->
            val anim = hand.animate().scaleX(1f).scaleY(1f).setDuration(100)
                .withEndAction { if (cont.isActive) cont.resume(Unit) }
            cont.invokeOnCancellation { anim.cancel() }
            anim.start()
        }
    }

    private suspend fun typeText(editText: TextInputEditText, text: String) {
        for (char in text) {
            delay(80)
            editText.text?.append(char)
        }
    }

    private fun hideKeyboard(view: View) {
        view.context.getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun resolveAttrColor(activity: AppCompatActivity, attrRes: Int): Int {
        val tv = TypedValue()
        activity.theme.resolveAttribute(attrRes, tv, true)
        return tv.data
    }
}
