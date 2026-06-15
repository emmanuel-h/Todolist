package fr.mandarine.todolist.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoList

class TodoListsAdapter(
    private val onListClick: (TodoList) -> Unit,
    private val onDeleteClick: (TodoList) -> Unit,
    private val onRenameClick: (TodoList) -> Unit
) : RecyclerView.Adapter<TodoListsAdapter.ViewHolder>() {

    private var lists: List<TodoList> = emptyList()

    fun submitList(newLists: List<TodoList>) {
        lists = newLists
        @Suppress("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lists[position], onListClick, onDeleteClick, onRenameClick)
    }

    override fun getItemCount(): Int = lists.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameView: MaterialTextView = view.findViewById(R.id.textListName)
        private val deleteButton: MaterialButton = view.findViewById(R.id.btnDeleteList)
        private val editButton: MaterialButton = view.findViewById(R.id.btnEditList)

        fun bind(
            list: TodoList,
            onListClick: (TodoList) -> Unit,
            onDeleteClick: (TodoList) -> Unit,
            onRenameClick: (TodoList) -> Unit
        ) {
            nameView.text = list.name
            itemView.setOnClickListener { onListClick(list) }
            editButton.setOnClickListener { onRenameClick(list) }
            deleteButton.setOnClickListener { onDeleteClick(list) }
        }
    }
}
