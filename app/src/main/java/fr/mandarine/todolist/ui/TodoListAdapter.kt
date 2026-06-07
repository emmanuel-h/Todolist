package fr.mandarine.todolist.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem

class TodoListAdapter(
    private val onToggle: (String) -> Unit
) : RecyclerView.Adapter<TodoListAdapter.ItemViewHolder>() {

    private var items: List<TodoItem> = emptyList()

    fun submitList(newItems: List<TodoItem>) {
        items = newItems
        @Suppress("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], onToggle)
    }

    override fun getItemCount(): Int = items.size

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
}
