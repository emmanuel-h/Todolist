package fr.mandarine.todolist.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem

class TodoListAdapter : RecyclerView.Adapter<TodoListAdapter.ViewHolder>() {

    private var items: List<TodoItem> = emptyList()
    private val completedIds: MutableSet<String> = mutableSetOf()

    fun submitList(newItems: List<TodoItem>) {
        items = newItems
        @Suppress("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], completedIds) { id, checked ->
            if (checked) completedIds.add(id) else completedIds.remove(id)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: TextView = view.findViewById(R.id.textTitle)
        private val checkBox: MaterialCheckBox = view.findViewById(R.id.checkCompleted)

        fun bind(item: TodoItem, completedIds: Set<String>, onToggle: (String, Boolean) -> Unit) {
            titleView.text = item.title
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = item.id in completedIds
            checkBox.setOnCheckedChangeListener { _, checked -> onToggle(item.id, checked) }
        }
    }
}
