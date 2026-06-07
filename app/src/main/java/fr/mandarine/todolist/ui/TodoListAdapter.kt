package fr.mandarine.todolist.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem

class TodoListAdapter(
    private val onCommit: (String) -> Unit,
    private val onToggle: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<TodoItem> = emptyList()
    private var focusAddRowOnNextBind: Boolean = true

    fun submitList(newItems: List<TodoItem>) {
        items = newItems
        @Suppress("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    fun requestAddRowFocus() {
        focusAddRowOnNextBind = true
        notifyItemChanged(items.size)
    }

    override fun getItemViewType(position: Int): Int =
        if (position == items.size) VIEW_TYPE_ADD else VIEW_TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_ADD) {
            val view = inflater.inflate(R.layout.item_todo_inline_add, parent, false)
            InlineAddViewHolder(view, onCommit)
        } else {
            val view = inflater.inflate(R.layout.item_todo, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> holder.bind(items[position], onToggle)
            is InlineAddViewHolder -> {
                if (focusAddRowOnNextBind) {
                    focusAddRowOnNextBind = false
                    holder.requestFocusAndShowKeyboard()
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size + 1

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

    class InlineAddViewHolder(
        view: View,
        private val onCommit: (String) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val editText: TextInputEditText = view.findViewById(R.id.editInlineAdd)

        init {
            editText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val title = editText.text.toString()
                    if (title.isBlank()) {
                        true
                    } else {
                        editText.text?.clear()
                        onCommit(title)
                        true
                    }
                } else {
                    false
                }
            }
        }

        fun requestFocusAndShowKeyboard() {
            editText.requestFocus()
            val imm = editText.context.getSystemService<InputMethodManager>()
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_ADD = 1
    }
}
