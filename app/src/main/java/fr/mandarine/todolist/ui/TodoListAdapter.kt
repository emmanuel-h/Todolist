package fr.mandarine.todolist.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem

class TodoListAdapter(
    private val onToggle: (String) -> Unit,
    private val onSubmit: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<TodoItem> = emptyList()

    companion object {
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_ADD = 1
    }

    fun submitList(newItems: List<TodoItem>) {
        items = newItems
        @Suppress("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(position: Int): Int =
        if (position == items.size) VIEW_TYPE_ADD else VIEW_TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_ADD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_todo_inline_add, parent, false)
                AddInputViewHolder(view, onSubmit)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_todo, parent, false)
                ItemViewHolder(view)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.bind(items[position], onToggle)
        }
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: MaterialTextView = view.findViewById(R.id.textTitle)
        private val checkBox: MaterialCheckBox = view.findViewById(R.id.checkCompleted)

        fun bind(item: TodoItem, onToggle: (String) -> Unit) {
            titleView.text = item.title
            if (item.isCompleted) {
                titleView.alpha = 0.38f
                titleView.paintFlags = titleView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                titleView.alpha = 1.0f
                titleView.paintFlags = titleView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = item.isCompleted
            checkBox.contentDescription = checkBox.context.getString(
                if (item.isCompleted) R.string.item_mark_incomplete else R.string.item_mark_completed
            )
            checkBox.setOnCheckedChangeListener { _, _ ->
                checkBox.contentDescription = checkBox.context.getString(
                    if (checkBox.isChecked) R.string.item_mark_incomplete else R.string.item_mark_completed
                )
                onToggle(item.id)
            }
        }
    }

    class AddInputViewHolder(view: View, onSubmit: (String) -> Unit) : RecyclerView.ViewHolder(view) {
        internal val editText: TextInputEditText = view.findViewById(R.id.editInlineAdd)
        private val ghostRow: View = view.findViewById(R.id.ghostRow)
        private val expandedRow: View = view.findViewById(R.id.expandedRow)

        init {
            val submitButton = view.findViewById<MaterialButton>(R.id.btnInlineSubmit)

            fun trySubmit() {
                val title = editText.text?.toString().orEmpty()
                if (title.isNotBlank()) {
                    onSubmit(title)
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
}
