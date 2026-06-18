package fr.mandarine.todolist.ui

import android.graphics.Paint
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem

class TodoListAdapter(
    private val onToggle: (String) -> Unit,
    private val onDelete: (String) -> Unit,
    private val onEdit: (String, String) -> Unit,
    private val onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class ListRow {
        data class Item(val todo: TodoItem) : ListRow()
        data class Divider(val completedCount: Int) : ListRow()
        data object InlineAdd : ListRow()
    }

    var onSubmitInlineAdd: ((String) -> Unit)? = null

    private var rows: List<ListRow> = emptyList()

    companion object {
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_INLINE_ADD = 1
        const val VIEW_TYPE_DIVIDER = 2
    }

    fun submitList(activeItems: List<TodoItem>, completedItems: List<TodoItem>) {
        rows = buildRows(activeItems, completedItems)
        @Suppress("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    fun activeItemCount(): Int = rows.count { it is ListRow.Item && !(it as ListRow.Item).todo.isCompleted }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val mutableRows = rows.toMutableList()
        val item = mutableRows.removeAt(fromPosition)
        mutableRows.add(toPosition, item)
        rows = mutableRows
        notifyItemMoved(fromPosition, toPosition)
    }

    private fun buildRows(
        activeItems: List<TodoItem>,
        completedItems: List<TodoItem>
    ): List<ListRow> {
        val result = mutableListOf<ListRow>()
        activeItems.forEach { result += ListRow.Item(it) }
        result += ListRow.InlineAdd
        if (activeItems.isNotEmpty() && completedItems.isNotEmpty()) {
            result += ListRow.Divider(completedItems.size)
        }
        completedItems.forEach { result += ListRow.Item(it) }
        return result
    }

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int =
        when (rows[position]) {
            is ListRow.Item -> VIEW_TYPE_ITEM
            is ListRow.InlineAdd -> VIEW_TYPE_INLINE_ADD
            is ListRow.Divider -> VIEW_TYPE_DIVIDER
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_INLINE_ADD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_todo_inline_add, parent, false)
                InlineAddViewHolder(view)
            }
            VIEW_TYPE_DIVIDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_todo_divider, parent, false)
                DividerViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_todo, parent, false)
                ItemViewHolder(view)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is ListRow.Item -> (holder as ItemViewHolder).bind(row.todo, onToggle, onDelete, onEdit, onStartDrag)
            is ListRow.InlineAdd -> (holder as InlineAddViewHolder).bind(onSubmitInlineAdd)
            is ListRow.Divider -> (holder as DividerViewHolder).bind(row.completedCount)
        }
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        internal val titleView: MaterialTextView = view.findViewById(R.id.textTitle)
        internal val editTitleInline: TextInputEditText = view.findViewById(R.id.editTitleInline)
        internal val dragHandle: ImageView = view.findViewById(R.id.dragHandle)
        private val btnToggleComplete: MaterialButton = view.findViewById(R.id.btnToggleComplete)
        private val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
        private val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)

        fun bind(
            item: TodoItem,
            onToggle: (String) -> Unit,
            onDelete: (String) -> Unit,
            onEdit: (String, String) -> Unit,
            onStartDrag: ((RecyclerView.ViewHolder) -> Unit)?
        ) {
            titleView.text = item.title
            exitEditMode()

            if (item.isCompleted) {
                titleView.alpha = 0.5f
                titleView.paintFlags = titleView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                btnToggleComplete.setIconResource(R.drawable.ic_undo)
                btnToggleComplete.contentDescription =
                    btnToggleComplete.context.getString(R.string.item_mark_incomplete)
                dragHandle.visibility = View.INVISIBLE
            } else {
                titleView.alpha = 1.0f
                titleView.paintFlags = titleView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                btnToggleComplete.setIconResource(R.drawable.ic_check)
                btnToggleComplete.contentDescription =
                    btnToggleComplete.context.getString(R.string.item_mark_completed)
                dragHandle.visibility = View.VISIBLE
                dragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        onStartDrag?.invoke(this)
                    }
                    false
                }
            }

            val gestureDetector = GestureDetector(
                titleView.context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        onToggle(item.id)
                        return true
                    }
                }
            )
            titleView.setOnTouchListener { v, event ->
                val consumed = gestureDetector.onTouchEvent(event)
                if (!consumed) v.performClick()
                consumed
            }

            btnToggleComplete.setOnClickListener { onToggle(item.id) }
            btnDelete.setOnClickListener { onDelete(item.id) }

            btnEdit.setOnClickListener {
                enterEditMode(item.id, item.title, onEdit)
            }
        }

        private fun enterEditMode(
            itemId: String,
            currentTitle: String,
            onEdit: (String, String) -> Unit
        ) {
            titleView.visibility = View.GONE
            editTitleInline.visibility = View.VISIBLE
            editTitleInline.setText(currentTitle)
            editTitleInline.setSelection(currentTitle.length)
            editTitleInline.requestFocus()

            val imm = editTitleInline.context.getSystemService(InputMethodManager::class.java)
            imm.showSoftInput(editTitleInline, InputMethodManager.SHOW_IMPLICIT)

            fun commitEdit() {
                val newTitle = editTitleInline.text?.toString().orEmpty()
                if (newTitle.isNotBlank()) {
                    onEdit(itemId, newTitle)
                }
                exitEditMode()
                val immHide = editTitleInline.context.getSystemService(InputMethodManager::class.java)
                immHide.hideSoftInputFromWindow(editTitleInline.windowToken, 0)
            }

            editTitleInline.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    commitEdit()
                    true
                } else {
                    false
                }
            }

            editTitleInline.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    commitEdit()
                }
            }
        }

        internal fun exitEditMode() {
            editTitleInline.visibility = View.GONE
            titleView.visibility = View.VISIBLE
            editTitleInline.setOnEditorActionListener(null)
            editTitleInline.setOnFocusChangeListener(null)
        }
    }

    class InlineAddViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ghostRow: View = view.findViewById(R.id.ghostRow)
        private val expandedRow: View = view.findViewById(R.id.expandedRow)
        internal val editText: TextInputEditText = view.findViewById(R.id.editInlineAdd)
        private val submitButton: MaterialButton = view.findViewById(R.id.btnInlineSubmit)
        private var keepExpanded = false

        fun bind(onSubmit: ((String) -> Unit)?) {
            val startExpanded = keepExpanded
            keepExpanded = false
            showExpanded(startExpanded)
            if (startExpanded) {
                editText.post { editText.requestFocus() }
            }

            fun trySubmit() {
                val title = editText.text?.toString().orEmpty()
                if (title.isNotBlank()) {
                    keepExpanded = true
                    onSubmit?.invoke(title)
                    editText.text?.clear()
                }
            }

            editText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    trySubmit()
                    true
                } else {
                    false
                }
            }

            submitButton.setOnClickListener { trySubmit() }

            editText.setOnFocusChangeListener { _, hasFocus ->
                showExpanded(hasFocus)
                if (hasFocus) {
                    val imm = editText.context.getSystemService(InputMethodManager::class.java)
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                }
            }

            ghostRow.setOnClickListener {
                showExpanded(true)
                editText.requestFocus()
            }
        }

        private fun showExpanded(expanded: Boolean) {
            ghostRow.visibility = if (expanded) View.GONE else View.VISIBLE
            expandedRow.visibility = if (expanded) View.VISIBLE else View.GONE
        }
    }

    class DividerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val label: MaterialTextView = view.findViewById(R.id.textDividerLabel)

        fun bind(completedCount: Int) {
            label.text = completedCount.toString()
        }
    }
}
