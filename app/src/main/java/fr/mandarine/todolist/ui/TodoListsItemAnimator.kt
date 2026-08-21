package fr.mandarine.todolist.ui

import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class TodoListsItemAnimator(
    private val shouldAnimate: () -> Boolean
) : DefaultItemAnimator() {

    var pendingListAdded: Boolean = false

    private val pendingCustomAdds = mutableListOf<RecyclerView.ViewHolder>()
    private val pendingCustomAddIsSlide = mutableListOf<Boolean>()
    private val runningCustomAdds = mutableSetOf<RecyclerView.ViewHolder>()

    private val interp = DecelerateInterpolator()
    private val animDuration = 200L
    private val slideYDp = 16f

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        if (!shouldAnimate()) {
            dispatchAddStarting(holder)
            dispatchAddFinished(holder)
            return false
        }
        val isSlide = holder.itemViewType == TodoListsAdapter.VIEW_TYPE_ITEM && pendingListAdded
        endAnimation(holder)
        val view = holder.itemView
        val dp = view.resources.displayMetrics.density
        view.alpha = 0f
        if (isSlide) view.translationY = -(slideYDp * dp)
        pendingCustomAdds.add(holder)
        pendingCustomAddIsSlide.add(isSlide)
        return true
    }

    override fun runPendingAnimations() {
        pendingListAdded = false

        val adds = pendingCustomAdds.toList()
        val slides = pendingCustomAddIsSlide.toList()
        pendingCustomAdds.clear()
        pendingCustomAddIsSlide.clear()

        super.runPendingAnimations()

        adds.forEachIndexed { index, holder ->
            val isSlide = slides.getOrElse(index) { false }
            runningCustomAdds.add(holder)
            dispatchAddStarting(holder)
            val view = holder.itemView
            val anim = view.animate()
                .alpha(1f)
                .setDuration(animDuration)
                .setInterpolator(interp)
            if (isSlide) anim.translationY(0f)
            anim.withEndAction {
                view.translationY = 0f
                runningCustomAdds.remove(holder)
                dispatchAddFinished(holder)
                if (!isRunning()) dispatchAnimationsFinished()
            }.start()
        }
    }

    override fun isRunning(): Boolean =
        pendingCustomAdds.isNotEmpty() ||
            runningCustomAdds.isNotEmpty() ||
            super.isRunning()

    override fun endAnimation(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.translationY = 0f
        val idx = pendingCustomAdds.indexOf(holder)
        if (idx >= 0) {
            pendingCustomAdds.removeAt(idx)
            pendingCustomAddIsSlide.removeAt(idx)
        }
        runningCustomAdds.remove(holder)
        super.endAnimation(holder)
    }

    override fun endAnimations() {
        (pendingCustomAdds + runningCustomAdds).toList().forEach { endAnimation(it) }
        super.endAnimations()
    }
}
