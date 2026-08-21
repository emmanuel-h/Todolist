package fr.mandarine.todolist.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.presentation.TodoListsViewModel
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.presentation.TutorialViewModel
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.launch

class TodoListsActivity : AppCompatActivity() {

    internal lateinit var viewModel: TodoListsViewModel
    private lateinit var adapter: TodoListsAdapter
    internal lateinit var fab: FloatingActionButton
    private lateinit var stickyNotePeel: View
    private lateinit var stickyNoteUnderSheets: List<View>
    internal lateinit var recyclerViewInternal: RecyclerView
    internal lateinit var inlineAddRowInternal: View
    internal var itemTouchHelperInternal: ItemTouchHelper? = null
    internal var currentDialogView: View? = null

    private var dragFromIndex: Int = -1
    private var notificationPermissionRequested = false
    private lateinit var listsItemAnimator: TodoListsItemAnimator
    internal var selectedInlineDate: LocalDate? = null
    internal var selectedInlineDueDate: LocalDate? = null
    internal var selectedRenameDate: LocalDate? = null
    internal var selectedRenameDueDate: LocalDate? = null

    internal var lastShownDueDatePicker: DatePickerDialog? = null
    private lateinit var replayButton: MaterialButton

    private lateinit var tutorialViewModel: TutorialViewModel
    private lateinit var tutorialController: TutorialOverlayController
    internal val tutorialBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            tutorialController.onSkipRequested()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_lists)

        val container = (application as TodoListApplication).container
        val todoListRepository = container.todoListRepository
        val todoRepository = container.todoRepository
        val getTodoListsWithStatusUseCase =
            GetTodoListsWithStatusUseCase(todoListRepository, todoRepository, container.clock)
        viewModel = ViewModelProvider(
            this,
            viewModelFactory {
                TodoListsViewModel(
                    CreateTodoListUseCase(todoListRepository),
                    DeleteTodoListUseCase(todoListRepository, todoRepository),
                    EditTodoListUseCase(todoListRepository),
                    getTodoListsWithStatusUseCase,
                    ReorderTodoListsUseCase(todoListRepository, getTodoListsWithStatusUseCase),
                    container.databaseDispatcher
                )
            }
        )[TodoListsViewModel::class.java]

        tutorialViewModel = container.tutorialViewModel

        fab = findViewById(R.id.fabAddList)
        stickyNotePeel = findViewById(R.id.stickyNotePeel)
        stickyNoteUnderSheets = listOf(
            findViewById(R.id.stickyNoteMid),
            findViewById(R.id.stickyNoteBack)
        )
        inlineAddRowInternal = findViewById(R.id.inlineAddListRow)

        adapter = TodoListsAdapter(
            onListClick = { list -> openList(list) },
            onDeleteConfirmed = { list -> viewModel.deleteList(list.id) },
            onRenameClick = { list -> showRenameDialog(list) },
            onDragStart = { holder -> itemTouchHelperInternal?.startDrag(holder) }
        )

        recyclerViewInternal = findViewById(R.id.recyclerViewLists)
        recyclerViewInternal.layoutManager = LinearLayoutManager(this)
        recyclerViewInternal.adapter = adapter

        listsItemAnimator = TodoListsItemAnimator(shouldAnimate = {
            !tutorialViewModel.animationsSuppressed && !isReducedMotion()
        })
        recyclerViewInternal.itemAnimator = listsItemAnimator

        val touchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun isLongPressDragEnabled(): Boolean = false

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder.itemViewType != TodoListsAdapter.VIEW_TYPE_ITEM) return 0
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return 0
                return if (position < adapter.activeItemCount()) {
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                } else {
                    0
                }
            }

            override fun onMove(
                rv: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                val activeCount = adapter.activeItemCount()
                if (to < 0 || to >= activeCount) return false
                if (target.itemViewType != TodoListsAdapter.VIEW_TYPE_ITEM) return false
                if (dragFromIndex < 0) dragFromIndex = from
                adapter.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun clearView(rv: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(rv, viewHolder)
                val to = viewHolder.bindingAdapterPosition
                if (dragFromIndex >= 0 && to >= 0 && dragFromIndex != to) {
                    viewModel.reorderLists(dragFromIndex, to)
                }
                dragFromIndex = -1
            }
        }

        itemTouchHelperInternal = ItemTouchHelper(touchCallback)
        itemTouchHelperInternal!!.attachToRecyclerView(recyclerViewInternal)

        container.notificationScheduler.scheduleDailyCheck()

        wireInlineAddRow()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.animationEvents.collect { _ ->
                    if (!tutorialViewModel.animationsSuppressed && !isReducedMotion()) {
                        listsItemAnimator.pendingListAdded = true
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { renderLists(it) }
            }
        }

        fab.setOnClickListener {
            peelStickyNote()
            showInlineAddRow()
        }

        onBackPressedDispatcher.addCallback(this, tutorialBackCallback)

        tutorialController = TutorialOverlayController(tutorialViewModel, lifecycleScope)
        tutorialController.attachToActivity(this)

        replayButton = findViewById(R.id.btnReplayTutorial)
        replayButton.setOnClickListener { tutorialViewModel.replay() }

        val requestNotificationPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tutorialViewModel.uiState.collect { state ->
                    tutorialBackCallback.isEnabled = state is TutorialUiState.ReadyToStart ||
                        state is TutorialUiState.Active
                    tutorialController.handleState(state, this@TodoListsActivity)
                    if (state is TutorialUiState.Dismissed && !notificationPermissionRequested) {
                        notificationPermissionRequested = true
                        maybeRequestNotificationPermission(requestNotificationPermission)
                    }
                }
            }
        }

        tutorialViewModel.initialize()
    }

    private fun maybeRequestNotificationPermission(
        launcher: androidx.activity.result.ActivityResultLauncher<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        tutorialController.detachFromActivity()
    }

    private fun wireInlineAddRow() {
        val editText = inlineAddRowInternal.findViewById<TextInputEditText>(R.id.editListInlineAdd)
        val submitButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineSubmit)
        val cancelButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineCancel)
        val dateButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineDate)
        val dueDateButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineDueDate)

        fun trySubmit() {
            val name = editText.text?.toString().orEmpty()
            if (name.isBlank()) return
            viewModel.createList(name, selectedInlineDate, selectedInlineDueDate)
            resetInlineDateSelection()
            editText.text?.clear()
            hideInlineAddRow()
        }

        editText.setOnEditorActionListener { _, actionId, event ->
            val isDone = actionId == EditorInfo.IME_ACTION_DONE
            val isUnspecified = actionId == EditorInfo.IME_ACTION_UNSPECIFIED
            val isEnterKey = event != null &&
                event.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_DOWN
            if (isDone || isUnspecified || isEnterKey) {
                trySubmit()
                true
            } else {
                false
            }
        }

        submitButton.setOnClickListener { trySubmit() }

        cancelButton.setOnClickListener {
            resetInlineDateSelection()
            editText.text?.clear()
            hideInlineAddRow()
        }

        dateButton.setOnClickListener {
            val current = selectedInlineDate ?: LocalDate.now()
            val picker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    onInlineTargetDatePicked(LocalDate.of(year, month + 1, dayOfMonth))
                },
                current.year,
                current.monthValue - 1,
                current.dayOfMonth
            )
            picker.show()
        }

        dueDateButton.setOnClickListener {
            val current = selectedInlineDueDate ?: LocalDate.now()
            val picker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    onInlineDueDatePicked(LocalDate.of(year, month + 1, dayOfMonth))
                },
                current.year,
                current.monthValue - 1,
                current.dayOfMonth
            )
            lastShownDueDatePicker = picker
            picker.show()
        }
    }

    internal fun onInlineTargetDatePicked(date: LocalDate) {
        selectedInlineDate = date
        selectedInlineDueDate = null
        val dateButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineDate)
        val dueDateButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineDueDate)
        dateButton.setIconResource(R.drawable.ic_event)
        updateInlineDateButtonTint(dateButton, hasDate = true)
        updateInlineDateButtonTint(dueDateButton, hasDate = false)
    }

    internal fun onInlineDueDatePicked(date: LocalDate) {
        selectedInlineDueDate = date
        selectedInlineDate = null
        val dateButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineDate)
        val dueDateButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineDueDate)
        dateButton.setIconResource(R.drawable.ic_event_add)
        updateInlineDateButtonTint(dateButton, hasDate = false)
        updateInlineDateButtonTint(dueDateButton, hasDate = true)
    }

    private fun resetInlineDateSelection() {
        selectedInlineDate = null
        selectedInlineDueDate = null
        val dateButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineDate)
        val dueDateButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineDueDate)
        dateButton.setIconResource(R.drawable.ic_event_add)
        updateInlineDateButtonTint(dateButton, hasDate = false)
        updateInlineDateButtonTint(dueDateButton, hasDate = false)
    }

    private fun updateInlineDateButtonTint(button: MaterialButton, hasDate: Boolean) {
        val attr = if (hasDate) {
            com.google.android.material.R.attr.colorPrimary
        } else {
            com.google.android.material.R.attr.colorOnSurfaceVariant
        }
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        button.iconTint = ColorStateList.valueOf(typedValue.data)
    }

    private fun showInlineAddRow() {
        inlineAddRowInternal.visibility = View.VISIBLE
        val divider = findViewById<View>(R.id.inlineAddListDivider)
        divider.visibility = View.VISIBLE
        fab.visibility = View.GONE
        stickyNoteUnderSheets.forEach { it.visibility = View.GONE }
        replayButton.visibility = View.GONE
        val editText = inlineAddRowInternal.findViewById<TextInputEditText>(R.id.editListInlineAdd)
        editText.requestFocus()
        val showKeyboard = Runnable {
            val imm = getSystemService(InputMethodManager::class.java)
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
        if (isReducedMotion()) {
            showKeyboard.run()
        } else {
            editText.postDelayed(showKeyboard, STICKY_NOTE_PEEL_TOTAL_MS)
        }
    }

    private fun hideInlineAddRow() {
        inlineAddRowInternal.visibility = View.GONE
        val divider = findViewById<View>(R.id.inlineAddListDivider)
        divider.visibility = View.GONE
        fab.visibility = View.VISIBLE
        stickyNoteUnderSheets.forEach { it.visibility = View.VISIBLE }
        settleStickyNote()
        replayButton.visibility = View.VISIBLE
        val editText = inlineAddRowInternal.findViewById<TextInputEditText>(R.id.editListInlineAdd)
        editText.clearFocus()
        val imm = getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
        renderLists(viewModel.state.value)
        viewModel.refresh()
    }

    private fun showRenameDialog(list: TodoList) {
        selectedRenameDate = list.targetDate
        selectedRenameDueDate = list.dueDate
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename_list, null)
        currentDialogView = dialogView
        val input = dialogView.findViewById<TextInputEditText>(R.id.editDialogRenameList)
        input.setText(list.name)

        val toggleGroup = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.toggleDateKind)
        val initialMode = if (list.dueDate != null) R.id.btnToggleDueDate else R.id.btnToggleTargetDate
        toggleGroup.check(initialMode)

        updateRenameDateDisplay()

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnToggleTargetDate -> {
                    if (selectedRenameDueDate != null) {
                        selectedRenameDate = selectedRenameDueDate
                        selectedRenameDueDate = null
                    }
                }
                R.id.btnToggleDueDate -> {
                    if (selectedRenameDate != null) {
                        selectedRenameDueDate = selectedRenameDate
                        selectedRenameDate = null
                    }
                }
            }
            updateRenameDateDisplay()
        }

        dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.layoutDateBox).setOnClickListener {
            val isTargetMode = toggleGroup.checkedButtonId == R.id.btnToggleTargetDate
            val current = if (isTargetMode) selectedRenameDate ?: LocalDate.now()
                          else selectedRenameDueDate ?: LocalDate.now()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val picked = LocalDate.of(year, month + 1, dayOfMonth)
                    if (toggleGroup.checkedButtonId == R.id.btnToggleTargetDate) {
                        onRenameTargetDatePicked(picked)
                    } else {
                        onRenameDueDatePicked(picked)
                    }
                },
                current.year,
                current.monthValue - 1,
                current.dayOfMonth
            ).show()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnDialogClearDate).setOnClickListener {
            selectedRenameDate = null
            selectedRenameDueDate = null
            updateRenameDateDisplay()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        dialogView.findViewById<MaterialButton>(R.id.btnDialogConfirm).setOnClickListener {
            val newName = input.text.toString()
            if (newName.isNotBlank()) {
                viewModel.editList(list.id, newName, selectedRenameDate, selectedRenameDueDate)
                dialog.dismiss()
            }
        }
        dialogView.findViewById<MaterialButton>(R.id.btnDialogCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    internal fun onRenameTargetDatePicked(date: LocalDate) {
        selectedRenameDate = date
        selectedRenameDueDate = null
        updateRenameDateDisplay()
    }

    internal fun onRenameDueDatePicked(date: LocalDate) {
        selectedRenameDueDate = date
        selectedRenameDate = null
        updateRenameDateDisplay()
    }

    internal fun updateRenameDateDisplay() {
        val dialogView = currentDialogView ?: return
        val toggleGroup = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.toggleDateKind) ?: return
        val dateBox = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.layoutDateBox) ?: return
        val addIcon = dialogView.findViewById<android.widget.ImageView>(R.id.iconDateAddAffordance) ?: return
        val dateText = dialogView.findViewById<MaterialTextView>(R.id.textDialogDate) ?: return
        val clearButton = dialogView.findViewById<MaterialButton>(R.id.btnDialogClearDate) ?: return
        val captionView = dialogView.findViewById<MaterialTextView>(R.id.textDateKindCaption) ?: return
        val isTargetMode = toggleGroup.checkedButtonId == R.id.btnToggleTargetDate
        dateBox.contentDescription = getString(
            if (isTargetMode) R.string.set_target_date else R.string.set_due_date
        )
        captionView.text = getString(
            if (isTargetMode) R.string.date_kind_target_caption else R.string.date_kind_due_caption
        )
        val date = selectedRenameDate ?: selectedRenameDueDate
        if (date != null) {
            val locale = Locale.getDefault(Locale.Category.FORMAT)
            dateText.text = TodoListsAdapter.formatTargetDate(date, showYear = true, locale)
            dateText.visibility = View.VISIBLE
            addIcon.visibility = View.GONE
            clearButton.visibility = View.VISIBLE
        } else {
            dateText.visibility = View.GONE
            addIcon.visibility = View.VISIBLE
            clearButton.visibility = View.GONE
        }
    }

    private fun peelStickyNote() {
        if (isReducedMotion()) return
        val sheet = stickyNotePeel
        sheet.translationX = 0f
        sheet.translationY = 0f
        sheet.rotation = STICKY_NOTE_REST_ROTATION
        sheet.scaleX = 1f
        sheet.scaleY = 1f
        sheet.alpha = 1f
        sheet.visibility = View.VISIBLE

        val lift = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(sheet, View.SCALE_X, 1f, STICKY_NOTE_LIFT_SCALE),
                ObjectAnimator.ofFloat(sheet, View.SCALE_Y, 1f, STICKY_NOTE_LIFT_SCALE),
                ObjectAnimator.ofFloat(
                    sheet, View.ROTATION, STICKY_NOTE_REST_ROTATION, STICKY_NOTE_LIFT_ROTATION
                )
            )
            duration = STICKY_NOTE_LIFT_MS
            interpolator = DecelerateInterpolator()
        }
        val peel = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(
                    sheet, View.TRANSLATION_X,
                    0f, -resources.getDimension(R.dimen.sticky_note_peel_travel_x)
                ),
                ObjectAnimator.ofFloat(
                    sheet, View.TRANSLATION_Y,
                    0f, -resources.getDimension(R.dimen.sticky_note_peel_travel_y)
                ),
                ObjectAnimator.ofFloat(
                    sheet, View.ROTATION, STICKY_NOTE_LIFT_ROTATION, STICKY_NOTE_PEEL_ROTATION
                ),
                ObjectAnimator.ofFloat(sheet, View.SCALE_X, STICKY_NOTE_LIFT_SCALE, 1f),
                ObjectAnimator.ofFloat(sheet, View.SCALE_Y, STICKY_NOTE_LIFT_SCALE, 1f),
                ObjectAnimator.ofFloat(sheet, View.ALPHA, 1f, 0f)
            )
            duration = STICKY_NOTE_PEEL_MS
            interpolator = AccelerateInterpolator()
        }
        AnimatorSet().apply {
            playSequentially(lift, peel)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    sheet.visibility = View.GONE
                }
            })
            start()
        }
    }

    private fun settleStickyNote() {
        if (isReducedMotion()) return
        fab.alpha = 0f
        fab.scaleX = STICKY_NOTE_SETTLE_SCALE
        fab.scaleY = STICKY_NOTE_SETTLE_SCALE
        fab.rotation = STICKY_NOTE_LIFT_ROTATION
        fab.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .rotation(STICKY_NOTE_REST_ROTATION)
            .setDuration(STICKY_NOTE_SETTLE_MS)
            .setInterpolator(DecelerateInterpolator())
    }

    private fun isReducedMotion(): Boolean {
        val scale = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return scale == 0f
    }

    private fun openList(list: TodoList) {
        val intent = Intent(this, TodoListActivity::class.java)
        intent.putExtra("LIST_ID", list.id)
        intent.putExtra("LIST_NAME", list.name)
        startActivity(intent)
    }

    private fun renderLists(state: TodoListsState) {
        when (val s = state) {
            is TodoListsState.Empty -> {
                adapter.submitList(emptyList(), emptyList())
            }
            is TodoListsState.Content -> {
                adapter.submitList(s.activeSummaries, s.doneSummaries)
            }
        }
    }

    private companion object {
        const val STICKY_NOTE_REST_ROTATION = -1f
        const val STICKY_NOTE_LIFT_ROTATION = -6f
        const val STICKY_NOTE_PEEL_ROTATION = -14f
        const val STICKY_NOTE_LIFT_SCALE = 1.08f
        const val STICKY_NOTE_SETTLE_SCALE = 0.9f
        const val STICKY_NOTE_LIFT_MS = 90L
        const val STICKY_NOTE_PEEL_MS = 230L
        const val STICKY_NOTE_PEEL_TOTAL_MS = STICKY_NOTE_LIFT_MS + STICKY_NOTE_PEEL_MS
        const val STICKY_NOTE_SETTLE_MS = 220L
    }
}
