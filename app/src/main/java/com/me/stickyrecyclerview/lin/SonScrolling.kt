package com.me.stickyrecyclerview.lin

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.me.stickyrecyclerview.ExpandRecyclerItem

/**
 * Created by hvngoc on 11/13/20
 */
class SonScrolling(
    private val manager: LinearLayoutManager,
    private val checker: ExpandRecyclerItem,
    private val recyclerView: RecyclerView
) : RecyclerView.OnScrollListener() {

    private var measuredChildWidth: Int = 0

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val last = manager.findLastVisibleItemPosition()
        Log.i("toang", "tutu$last")

        if (!checker.isExpandItem(last)) {
            return
        }

        updateCurrentOffset()

    }

    private fun findFirstVisibleView(): View? {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val childCount = layoutManager.childCount
        if (childCount == 0) {
            return null
        }

        var closestChild: View? = null
        var firstVisibleChildX = Integer.MAX_VALUE

        for (i in 0 until childCount) {
            val child = layoutManager.getChildAt(i)

            val childStart = child!!.x.toInt()

            if (childStart + child.measuredWidth < firstVisibleChildX && childStart + child.measuredWidth > getCurrentFrameLeft()) {
                firstVisibleChildX = childStart
                closestChild = child
            }
        }

        return closestChild
    }

    private fun updateCurrentOffset() {
        val leftView = findFirstVisibleView() ?: return

        var position = recyclerView.getChildAdapterPosition(leftView)
        if (position == RecyclerView.NO_POSITION) {
            return
        }
        val itemCount = recyclerView.adapter!!.itemCount

        // In case there is an infinite pager
        if (position >= itemCount && itemCount != 0) {
            position %= itemCount
        }

        val offset = (getCurrentFrameLeft() - leftView.x) / leftView.measuredWidth

        if (offset in 0.0..1.0 && position < itemCount) {
            Log.d("toang", "position $position     offset$offset")
        }
    }

    private fun getCurrentFrameLeft(): Float {
        return (recyclerView.measuredWidth - getChildWidth().toFloat()) / 2
    }

    private fun getChildWidth(): Int {
        if (measuredChildWidth == 0) {
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                if (child.measuredWidth != 0) {
                    measuredChildWidth = child.measuredWidth
                    return measuredChildWidth
                }
            }
        }
        return measuredChildWidth
    }
}