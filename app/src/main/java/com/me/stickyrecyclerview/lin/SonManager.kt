package com.me.stickyrecyclerview.lin

import android.content.Context
import android.graphics.PointF
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import kotlin.math.abs


/**
 * Created by hvngoc on 11/13/20
 */

internal class SonManager(private val context: Context, orientation: Int, reverseLayout: Boolean) :
    LinearLayoutManager(
        context,
        orientation,
        reverseLayout
    ) {

    companion object {
        private const val MILLISECONDS_PER_INCH = 50f
    }

    private val mShrinkAmount = 0.15f
    private val mShrinkDistance = 0.9f

    override fun scrollVerticallyBy(dy: Int, recycler: Recycler?, state: RecyclerView.State?): Int {
        val orientation = orientation
        return if (orientation == VERTICAL) {
            val scrolled = super.scrollVerticallyBy(dy, recycler, state)
            val midpoint = height / 2f
            val d0 = 0f
            val d1 = mShrinkDistance * midpoint
            val s0 = 1f
            val s1 = 1f - mShrinkAmount
            for (i in 0 until childCount) {
                val child = getChildAt(i) ?: continue
                val childMidpoint = (getDecoratedBottom(child) + getDecoratedTop(child)) / 2f
                val d = d1.coerceAtMost(abs(midpoint - childMidpoint))
                val scale = s0 + (s1 - s0) * (d - d0) / (d1 - d0)
                child.scaleX = scale
                child.scaleY = scale
                if (scale > .9f) {
                    child.alpha = 1f
                } else {
                    child.alpha = 0.5f
                }
            }
            scrolled
        } else {
            0
        }
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: Recycler?,
        state: RecyclerView.State?
    ): Int {
        val orientation = orientation
        return if (orientation == HORIZONTAL) {
            val scrolled = super.scrollHorizontallyBy(dx, recycler, state)
            val midpoint = width / 2f
            val d0 = 0f
            val d1 = mShrinkDistance * midpoint
            val s0 = 1f
            val s1 = 1f - mShrinkAmount
            for (i in 0 until childCount) {
                val child = getChildAt(i) ?: continue
                val childMidpoint = (getDecoratedRight(child) + getDecoratedLeft(child)) / 2f
                val d = d1.coerceAtMost(abs(midpoint - childMidpoint))
                val scale = s0 + (s1 - s0) * (d - d0) / (d1 - d0)
                child.scaleX = scale
                child.scaleY = scale
                if (scale > .9f) {
                    child.alpha = 1f
                } else {
                    child.alpha = 0.5f
                }
            }
            scrolled
        } else {
            0
        }
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView?,
        state: RecyclerView.State?,
        position: Int
    ) {
        val smoothScroller: LinearSmoothScroller = object : LinearSmoothScroller(context) {
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                return this@SonManager.computeScrollVectorForPosition(targetPosition)
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
            }

            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }

        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }
}