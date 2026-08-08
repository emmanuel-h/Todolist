package fr.mandarine.todolist.ui

import android.content.res.ColorStateList
import android.graphics.Paint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodoListsAdapter(
    private val onListClick: (TodoList) -> Unit,
    private val onDeleteClick: (TodoList) -> Unit,
    private val onRenameClick: (TodoList) -> Unit,
    private val onDragStart: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class ListRow {
        data class Item(val summary: TodoListSummary) : ListRow()
        data class Divider(val doneCount: Int) : ListRow()
    }

    companion object {
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_DIVIDER = 2

        fun formatTargetDate(date: LocalDate, showYear: Boolean, locale: Locale): String {
            val skeleton = if (showYear) "EEEdMMMy" else "EEEdMMM"
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
            return date.format(DateTimeFormatter.ofPattern(pattern, locale))
        }
    }

    private var rows: List<ListRow> = emptyList()

    fun submitList(activeSummaries: List<TodoListSummary>, doneSummaries: List<TodoListSummary>) {
        rows = buildRows(activeSummaries, doneSummaries)
        @Suppress("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    fun activeItemCount(): Int = rows.count { it is ListRow.Item && !(it as ListRow.Item).summary.allDone }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val mutable = rows.toMutableList()
        val item = mutable.removeAt(fromPosition)
        mutable.add(toPosition, item)
        rows = mutable
        notifyItemMoved(fromPosition, toPosition)
    }

    private fun buildRows(
        activeSummaries: List<TodoListSummary>,
        doneSummaries: List<TodoListSummary>
    ): List<ListRow> {
        val result = mutableListOf<ListRow>()
        activeSummaries.forEach { result += ListRow.Item(it) }
        if (activeSummaries.isNotEmpty() && doneSummaries.isNotEmpty()) {
            result += ListRow.Divider(doneSummaries.size)
        }
        doneSummaries.forEach { result += ListRow.Item(it) }
        return result
    }

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int =
        when (rows[position]) {
            is ListRow.Item -> VIEW_TYPE_ITEM
            is ListRow.Divider -> VIEW_TYPE_DIVIDER
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_DIVIDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_todo_divider, parent, false)
                DividerViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_todo_list, parent, false)
                ViewHolder(view)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is ListRow.Item -> (holder as ViewHolder).bind(row.summary, onListClick, onDeleteClick, onRenameClick, onDragStart)
            is ListRow.Divider -> (holder as DividerViewHolder).bind(row.doneCount)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card: MaterialCardView = view as MaterialCardView
        private val nameView: MaterialTextView = view.findViewById(R.id.textListName)
        private val activeCountBadge: MaterialTextView = view.findViewById(R.id.badgeActiveCount)
        private val completedCountBadge: MaterialTextView = view.findViewById(R.id.badgeCompletedCount)
        private val deleteButton: MaterialButton = view.findViewById(R.id.btnDeleteList)
        private val editButton: MaterialButton = view.findViewById(R.id.btnEditList)
        val dragHandle: ImageView = view.findViewById(R.id.dragHandleList)
        private val layoutTargetDate: LinearLayout = view.findViewById(R.id.layoutTargetDate)
        private val iconTargetDate: ImageView = view.findViewById(R.id.iconTargetDate)
        private val textTargetDate: MaterialTextView = view.findViewById(R.id.textTargetDate)
        private val layoutDueDate: LinearLayout = view.findViewById(R.id.layoutDueDate)
        private val iconDueDate: ImageView = view.findViewById(R.id.iconDueDate)
        private val textDueDate: MaterialTextView = view.findViewById(R.id.textDueDate)

        fun bind(
            summary: TodoListSummary,
            onListClick: (TodoList) -> Unit,
            onDeleteClick: (TodoList) -> Unit,
            onRenameClick: (TodoList) -> Unit,
            onDragStart: (RecyclerView.ViewHolder) -> Unit
        ) {
            val list = summary.list
            nameView.text = list.name
            activeCountBadge.text = summary.activeCount.toString()
            completedCountBadge.text = summary.completedCount.toString()
            itemView.setOnClickListener { onListClick(list) }
            editButton.setOnClickListener { onRenameClick(list) }
            deleteButton.setOnClickListener { onDeleteClick(list) }
            dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onDragStart(this)
                }
                false
            }
            applyAllDoneStyle(summary.allDone)
            bindTargetDate(summary)
            bindDueDate(summary)
        }

        private fun bindTargetDate(summary: TodoListSummary) {
            val targetDate = summary.list.targetDate
            if (targetDate == null) {
                layoutTargetDate.visibility = View.GONE
                return
            }
            layoutTargetDate.visibility = View.VISIBLE
            val locale = Locale.getDefault(Locale.Category.FORMAT)
            textTargetDate.text = formatTargetDate(targetDate, summary.showTargetYear, locale)
            val tint = if (summary.isTargetDateElapsed) {
                resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
            } else {
                resolveColor(com.google.android.material.R.attr.colorPrimary)
            }
            iconTargetDate.imageTintList = ColorStateList.valueOf(tint)
            textTargetDate.setTextColor(tint)
        }

        private fun bindDueDate(summary: TodoListSummary) {
            val dueDate = summary.list.dueDate
            if (dueDate == null || summary.dueDateStatus == null) {
                layoutDueDate.visibility = View.GONE
                return
            }
            layoutDueDate.visibility = View.VISIBLE
            val locale = Locale.getDefault(Locale.Category.FORMAT)
            textDueDate.text = formatTargetDate(dueDate, summary.showDueDateYear, locale)
            val tint = when (summary.dueDateStatus) {
                DueDateStatus.FUTURE -> resolveColor(com.google.android.material.R.attr.colorPrimary)
                DueDateStatus.TODAY -> resolveColor(fr.mandarine.todolist.R.attr.colorWarning)
                DueDateStatus.OVERDUE -> resolveColor(com.google.android.material.R.attr.colorError)
            }
            iconDueDate.imageTintList = ColorStateList.valueOf(tint)
            textDueDate.setTextColor(tint)
        }

        private fun resolveColor(attr: Int): Int {
            val typedValue = TypedValue()
            itemView.context.theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }

        private fun applyAllDoneStyle(allDone: Boolean) {
            if (allDone) {
                val typedValue = TypedValue()
                itemView.context.theme.resolveAttribute(
                    com.google.android.material.R.attr.colorSecondaryContainer,
                    typedValue,
                    true
                )
                card.setCardBackgroundColor(typedValue.data)
                nameView.paintFlags = nameView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                nameView.alpha = 0.5f
            } else {
                val typedValue = TypedValue()
                itemView.context.theme.resolveAttribute(
                    com.google.android.material.R.attr.colorSurface,
                    typedValue,
                    true
                )
                card.setCardBackgroundColor(typedValue.data)
                nameView.paintFlags = nameView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                nameView.alpha = 1.0f
            }
        }
    }

    class DividerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val label: MaterialTextView = view.findViewById(R.id.textDividerLabel)

        fun bind(doneCount: Int) {
            label.text = doneCount.toString()
        }
    }
}
