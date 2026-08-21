package fr.mandarine.todolist.ui

import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import fr.mandarine.todolist.domain.AnimationEvent

class TodoItemAnimator(
    private val shouldAnimate: () -> Boolean
) : DefaultItemAnimator() {

    var pendingEvent: AnimationEvent? = null

    private val pendingCustomAdds = mutableListOf<RecyclerView.ViewHolder>()
    private val addEventMap = mutableMapOf<RecyclerView.ViewHolder, AnimationEvent?>()
    private val pendingCustomRemoves = mutableListOf<RecyclerView.ViewHolder>()
    private val runningCustomAdds = mutableSetOf<RecyclerView.ViewHolder>()
    private val runningCustomRemoves = mutableSetOf<RecyclerView.ViewHolder>()

    private val interp = DecelerateInterpolator()
    private val animDuration = 200L
    private val slideYDp = 16f

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        if (!shouldAnimate()) {
            dispatchAddStarting(holder)
            dispatchAddFinished(holder)
            return false
        }
        if (holder.itemViewType == TodoListAdapter.VIEW_TYPE_INLINE_ADD) {
            return super.animateAdd(holder)
        }
        val event = pendingEvent
        endAnimation(holder)
        val view = holder.itemView
        val dp = view.resources.displayMetrics.density
        when {
            holder.itemViewType == TodoListAdapter.VIEW_TYPE_DIVIDER -> {
                view.alpha = 0f
            }
            event is AnimationEvent.ItemAdded -> {
                view.alpha = 0f
                view.translationY = slideYDp * dp
            }
            event is AnimationEvent.ItemCompleted || event is AnimationEvent.ItemRestored -> {
                view.alpha = 0f
                view.scaleY = 0f
                view.pivotY = 0f
            }
            else -> view.alpha = 0f
        }
        addEventMap[holder] = event
        pendingCustomAdds.add(holder)
        return true
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        if (!shouldAnimate()) {
            dispatchRemoveStarting(holder)
            dispatchRemoveFinished(holder)
            return false
        }
        return when (pendingEvent) {
            is AnimationEvent.ItemCompleted,
            is AnimationEvent.ItemRestored,
            is AnimationEvent.ItemDeleted -> {
                endAnimation(holder)
                pendingCustomRemoves.add(holder)
                true
            }
            else -> super.animateRemove(holder)
        }
    }

    override fun runPendingAnimations() {
        pendingEvent = null

        val removes = pendingCustomRemoves.toList()
        pendingCustomRemoves.clear()
        val adds = pendingCustomAdds.toList()
        pendingCustomAdds.clear()

        removes.forEach { holder ->
            runningCustomRemoves.add(holder)
            dispatchRemoveStarting(holder)
            holder.itemView.animate()
                .alpha(0f)
                .setDuration(animDuration)
                .setInterpolator(interp)
                .withEndAction {
                    holder.itemView.alpha = 1f
                    runningCustomRemoves.remove(holder)
                    dispatchRemoveFinished(holder)
                    if (!isRunning()) dispatchAnimationsFinished()
                }
                .start()
        }

        super.runPendingAnimations()

        val delay = if (removes.isNotEmpty()) animDuration else 0L
        val runAdds = Runnable {
            adds.forEach { holder ->
                val event = addEventMap.remove(holder)
                runningCustomAdds.add(holder)
                dispatchAddStarting(holder)
                val view = holder.itemView
                val anim = view.animate()
                    .setDuration(animDuration)
                    .setInterpolator(interp)
                when {
                    holder.itemViewType == TodoListAdapter.VIEW_TYPE_DIVIDER -> anim.alpha(1f)
                    event is AnimationEvent.ItemAdded -> anim.alpha(1f).translationY(0f)
                    event is AnimationEvent.ItemCompleted || event is AnimationEvent.ItemRestored ->
                        anim.alpha(1f).scaleY(1f)
                    else -> anim.alpha(1f)
                }
                anim.withEndAction {
                    view.translationY = 0f
                    view.scaleY = 1f
                    runningCustomAdds.remove(holder)
                    dispatchAddFinished(holder)
                    if (!isRunning()) dispatchAnimationsFinished()
                }.start()
            }
        }
        if (delay > 0L && adds.isNotEmpty()) {
            adds.first().itemView.postDelayed(runAdds, delay)
        } else {
            runAdds.run()
        }
    }

    override fun isRunning(): Boolean =
        pendingCustomAdds.isNotEmpty() ||
            pendingCustomRemoves.isNotEmpty() ||
            runningCustomAdds.isNotEmpty() ||
            runningCustomRemoves.isNotEmpty() ||
            super.isRunning()

    override fun endAnimation(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.translationY = 0f
        holder.itemView.scaleY = 1f
        pendingCustomAdds.remove(holder)
        pendingCustomRemoves.remove(holder)
        runningCustomAdds.remove(holder)
        runningCustomRemoves.remove(holder)
        addEventMap.remove(holder)
        super.endAnimation(holder)
    }

    override fun endAnimations() {
        (pendingCustomAdds + pendingCustomRemoves + runningCustomAdds + runningCustomRemoves)
            .toList()
            .forEach { endAnimation(it) }
        super.endAnimations()
    }
}
