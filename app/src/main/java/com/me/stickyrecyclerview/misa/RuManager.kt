package com.me.stickyrecyclerview.misa

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.me.stickyrecyclerview.ExpandRecyclerItem

/**
 * Created by hvngoc on 11/14/20
 */
class RuManager(
    private val checker: ExpandRecyclerItem,
    context: Context,
    orientation: Int,
    reverseLayout: Boolean
) :
    LinearLayoutManager(context, orientation, reverseLayout) {

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        val orientation = orientation
        if (orientation == HORIZONTAL) {
            val scrolled = super.scrollHorizontallyBy(dx, recycler, state)

            val last = findLastVisibleItemPosition()
            val child = findViewByPosition(last)
            val childWidth = child?.measuredWidth ?: 0

            Log.w("rururu", "full $width  childWidth $childWidth  last $last  xxx ${child?.x}")

            if (checker.isExpandItem(last)) {
                val next = recycler.getViewForPosition(last + 1)
                addView(next, 0)
                measureChildWithMargins(next, 0, 0)
                Log.i("rururu", "next ${next.measuredWidth}  xxx ${next.x} ")
                layoutDecorated(next, 0,0,0,0)
            }
            return scrolled
        } else {
            return 0
        }
    }
}