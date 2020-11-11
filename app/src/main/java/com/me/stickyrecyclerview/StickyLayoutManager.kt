package com.me.stickyrecyclerview

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

class StickyLayoutManager : RecyclerView.LayoutManager() {

    // Save first view position in each scrolling step....
    private var mFirstPosition = 0
    private var mFirstPositionOffset = 0
    var expandItemListener: ExpandRecyclerItem? = null

    private val recycleViews: MutableList<RecyclerView.ViewHolder> = CopyOnWriteArrayList()
    private val expandItemCurrentHeightPercent = ConcurrentHashMap<Int, Double>()

    private val boundaryExpandItem get() = (height * 0.1f).toInt()

    /**
     * The callback used for retrieving information about a RecyclerView and its children in the
     * vertical direction.
     */
    private val mVerticalBoundCheckCallback: ViewBoundsCheck.Callback =
        object : ViewBoundsCheck.Callback {
            override fun getChildAt(index: Int): View {
                return this@StickyLayoutManager.getChildAt(index)!!
            }

            override fun getParentStart(): Int {
                return this@StickyLayoutManager.paddingTop
            }

            override fun getParentEnd(): Int {
                return this@StickyLayoutManager.height - this@StickyLayoutManager.paddingBottom
            }

            override fun getChildStart(view: View): Int {
                val params = view.layoutParams as RecyclerView.LayoutParams
                return getDecoratedTop(view) - params.topMargin
            }

            override fun getChildEnd(view: View): Int {
                val params = view.layoutParams as RecyclerView.LayoutParams
                return getDecoratedBottom(view) + params.bottomMargin
            }
        }
    private var mVerticalBoundCheck = ViewBoundsCheck(mVerticalBoundCheckCallback)

    /**
     * Support method
     */
    fun findFirstVisibleItemPosition(): Int {
        val child = findOneVisibleChild(0, childCount,
            completelyVisible = false,
            acceptPartiallyVisible = true
        )
        return getPosition(child)
    }

    fun findLastVisibleItemPosition(): Int {
        val child = findOneVisibleChild(childCount - 1, -1,
            completelyVisible = false,
            acceptPartiallyVisible = true
        )
        return getPosition(child)
    }

    // Returns the first child that is visible in the provided index range, i.e. either partially or
    // fully visible depending on the arguments provided. Completely invisible children are not
    // acceptable by this method, but could be returned
    // using #findOnePartiallyOrCompletelyInvisibleChild
    private fun findOneVisibleChild(fromIndex: Int, toIndex: Int, completelyVisible: Boolean, acceptPartiallyVisible: Boolean): View {
        @ViewBoundsCheck.ViewBounds var preferredBoundsFlag = 0
        @ViewBoundsCheck.ViewBounds var acceptableBoundsFlag = 0
        preferredBoundsFlag = if (completelyVisible) {
            (ViewBoundsCheck.FLAG_CVS_GT_PVS or ViewBoundsCheck.FLAG_CVS_EQ_PVS
                    or ViewBoundsCheck.FLAG_CVE_LT_PVE or ViewBoundsCheck.FLAG_CVE_EQ_PVE)
        } else {
            (ViewBoundsCheck.FLAG_CVS_LT_PVE
                    or ViewBoundsCheck.FLAG_CVE_GT_PVS)
        }
        if (acceptPartiallyVisible) {
            acceptableBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVE
                    or ViewBoundsCheck.FLAG_CVE_GT_PVS)
        }
        return mVerticalBoundCheck.findOneViewWithinBoundFlags(
            fromIndex,
            toIndex,
            preferredBoundsFlag,
            acceptableBoundsFlag
        )
    }

    // call when initial layout manager and every notify data changed
    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        val parentBottom = height - paddingBottom
        var oldTop = paddingTop
        if (childCount > 0) {
            // happen after first init, may trigger by notify data set changed
            val oldTopView = getChildAt(0)
            if (oldTopView != null) {
                oldTop = oldTopView.top
            }

            // try to store the old current height info of expand item
            for (i in 0 until childCount) {
                val itemView = getChildAt(i) ?: continue
                val adapterPosition = getPosition(itemView)
                val isExpandItem = expandItemListener?.isExpandItem(adapterPosition) ?: false
                if (isExpandItem) {
                    val measureViewHeight = getDecoratedMeasuredHeight(itemView)
                    val currentHeight = getDecoratedBottom(itemView) - getDecoratedTop(itemView)
                    val percent = currentHeight.toDouble() / measureViewHeight
                    expandItemCurrentHeightPercent[adapterPosition] = percent
                }
            }
        } else {
            // happen on first init or after screen rotation, config changed...
            if (mFirstPositionOffset < state.itemCount) {
                oldTop += mFirstPositionOffset
            }
        }
        Log.v("StickyLayoutManager", "Top 0ld View $oldTop")

        // put view back to scrap heap => remove all visible view on screen
        detachAndScrapAttachedViews(recycler)
        var top = oldTop
        var bottom = 0
        val left = paddingLeft
        val right = width - paddingRight
        val count = state.itemCount
        // fill up the whole screen with new view again
        run {
            var i = 0
            while (mFirstPosition + i < count && top < parentBottom) {
                val position = mFirstPosition + i

                // this logic used to layout the view on initial or notify data changed
                val view = recycler.getViewForPosition(position)
                addView(view, i)
                measureChildWithMargins(view, 0, 0)

                // check for expand item
                val isExpandItem = expandItemListener?.isExpandItem(position) ?: false
                if (isExpandItem) {
                    val percent = expandItemCurrentHeightPercent[position]
                    if (percent == null) {
                        bottom = top + getDecoratedMeasuredHeight(view)
                        layoutDecorated(view, left, top, right, bottom)
                    } else {
                        bottom = top + ((getDecoratedMeasuredHeight(view) * percent).toInt())
                        layoutDecorated(view, left, top, right, bottom)
                    }
                } else {
                    bottom = top + getDecoratedMeasuredHeight(view)
                    layoutDecorated(view, left, top, right, bottom)
                }

                i++
                top = bottom
            }
        }

        // recycler view adapter used...
        recycleViews.clear()
        recycleViews.addAll(recycler.scrapList)
        for (i in recycleViews.indices) {
            val viewHolder = recycleViews[i]
            recycler.recycleView(viewHolder.itemView)
        }

        expandItemCurrentHeightPercent.clear()
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun scrollToPosition(position: Int) {
        mFirstPosition = position
        mFirstPositionOffset = 0
        requestLayout()
    }

    fun scrollToPositionWithOffset(position: Int, offset: Int) {
        mFirstPosition = position
        mFirstPositionOffset = offset
        requestLayout()
    }

    override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: RecyclerView.State): Int {
        if (childCount == 0) {
            return 0
        }
        var scrolled = 0
        val left = paddingLeft
        val right = width - paddingRight
        // scrolling up => move content down
        if (dy < 0) {
            Log.v("StickyLayoutManager", "Fill Top")
            // fill top
            while (scrolled > dy) {
                val topView = getChildAt(0)
                val hangingTop = (-getDecoratedTop(topView!!)).coerceAtLeast(0)
                val scrollBy = (scrolled - dy).coerceAtMost(hangingTop)
                scrolled -= scrollBy

                // scroll view
                for (i in 0 until childCount) {
                    val forceStopEarly = layoutItemTop(i, state.itemCount, scrollBy, left, right)
                    if (forceStopEarly) {
                        break
                    }
                }

                if (mFirstPosition > 0 && scrolled > dy) {
                    mFirstPosition--
                    val view = recycler.getViewForPosition(mFirstPosition)
                    addView(view, 0)
                    measureChildWithMargins(view, 0, 0)
                    val bottom = getDecoratedTop(topView)
                    val top = bottom - getDecoratedMeasuredHeight(view)
                    // scroll up => height of expand item already as full size
                    layoutDecorated(view, left, top, right, bottom)
                } else {
                    break
                }
            }
        }
        // scrolling down
        else if (dy > 0) {
            Log.v("StickyLayoutManager", "Fill Bottom")
            // fill bottom
            val parentHeight = height
            while (scrolled < dy) {
                val bottomView = getChildAt(childCount - 1)
                val hangingBottom = (getDecoratedBottom(bottomView!!) - parentHeight).coerceAtLeast(0)
                val scrollBy = -(dy - scrolled).coerceAtMost(hangingBottom)
                scrolled -= scrollBy

                // scroll view (shift content already in screen)
                for (i in 0 until childCount) {
                    val forceStopEarly = layoutItemBottom(i, state.itemCount, scrollBy, left, right)
                    if (forceStopEarly) {
                        break
                    }
                }

                // create new view when still have remain space on screen
                if (scrolled < dy && state.itemCount > mFirstPosition + childCount) {
                    val view = recycler.getViewForPosition(mFirstPosition + childCount)
                    val top = getDecoratedBottom(getChildAt(childCount - 1)!!)
                    addView(view)
                    measureChildWithMargins(view, 0, 0)
                    val itemPosition = getPosition(view)
                    val isExpandItem = expandItemListener?.isExpandItem(itemPosition) ?: false
                    if (isExpandItem) {
                        Log.e("StickyLayoutManager", "Append new item $itemPosition is expand item $isExpandItem")
                        // default height is 0
                        layoutDecorated(view, left, top, right, top)
                    } else {
                        val bottom = top + getDecoratedMeasuredHeight(view)
                        layoutDecorated(view, left, top, right, bottom)
                    }
                } else {
                    break
                }
            }
        }
        recycleViewsOutOfBounds(recycler)
        updateAnchorOffset()
        return scrolled
    }

    private fun layoutItemTop(index: Int, totalItemCount: Int, scrollBy: Int, left: Int, right: Int): Boolean {
        val childAtPosition = getChildAt(index) ?: return false
        val adapterPosition = getPosition(childAtPosition)
        val isExpandItem = expandItemListener?.isExpandItem(adapterPosition) ?: false
        if (isExpandItem) {
            val measureViewHeight = getDecoratedMeasuredHeight(childAtPosition)
            val measureHeightWithoutDecorated = measureViewHeight - getTopDecorationHeight(childAtPosition) - getBottomDecorationHeight(childAtPosition)
            val currentHeight = getDecoratedBottom(childAtPosition) - getDecoratedTop(childAtPosition)
            Log.v("StickyLayoutManager", "=======> Is Expand Item Found $adapterPosition Expected Height $measureViewHeight Current Height $currentHeight scroll by value $scrollBy top ${getDecoratedTop(childAtPosition)} bottom ${getDecoratedBottom(childAtPosition)}")
            val nextView = if (index < childCount - 1) getChildAt(index + 1) else null
            var shouldCollapseItem = false
            if (nextView != null) {
                // top off next view appear more than 10% of screen at bottom
                if (height - nextView.top <= boundaryExpandItem) {
                    shouldCollapseItem = true
                }
            } else {
                // don't have next view => may be the last view
                val isLastItem = adapterPosition == totalItemCount - 1
                if (isLastItem) {
                    shouldCollapseItem = true
                }
            }
            childAtPosition.visibility = if (currentHeight <= (measureViewHeight - measureHeightWithoutDecorated)) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            if (currentHeight > 0 && shouldCollapseItem) {
                nextView?.run { correctDockItemPosition(this) }

                val newCurrentHeight = currentHeight - abs(scrollBy)
                // all the scroll by value has been used for expand item height => All the item below should be stay the same at it position
                if (newCurrentHeight > 0) {
                    getChildAt(index)?.offsetTopAndBottom(scrollBy)
                    layoutDecorated(childAtPosition, left, getDecoratedTop(childAtPosition), right, getDecoratedBottom(childAtPosition) - abs(scrollBy))
                } else {
                    val itemScrollBy = -currentHeight
                    Log.e("StickyLayoutManager", "Over scrolling...... currentHeight $currentHeight newCurrentHeight $newCurrentHeight measureViewHeight $measureViewHeight itemScrollBy $itemScrollBy")
                    getChildAt(index)?.offsetTopAndBottom(scrollBy)
                    layoutDecorated(childAtPosition, left, getDecoratedTop(childAtPosition), right, getDecoratedBottom(childAtPosition) + itemScrollBy)

                    val delta = scrollBy + itemScrollBy
                    for (j in index + 1 until childCount) {
                        val forceStopEarly = layoutItemTop(j, totalItemCount, delta, left, right)
                        if (forceStopEarly) {
                            break
                        }
                    }
                }

                return true
            } else {
                getChildAt(index)?.offsetTopAndBottom(scrollBy)
            }
        } else {
            childAtPosition.visibility = View.VISIBLE
            getChildAt(index)?.offsetTopAndBottom(scrollBy)
        }

        return false
    }

    private fun layoutItemBottom(index: Int, totalItemCount: Int, scrollBy: Int, left: Int, right: Int): Boolean {
        val childAtPosition = getChildAt(index) ?: return false
        val adapterPosition = getPosition(childAtPosition)
        val isExpandItem = expandItemListener?.isExpandItem(adapterPosition) ?: false
        if (isExpandItem) {
            val measureViewHeight = getDecoratedMeasuredHeight(childAtPosition)
            val measureHeightWithoutDecorated = measureViewHeight - getTopDecorationHeight(childAtPosition) - getBottomDecorationHeight(childAtPosition)
            val currentHeight = getDecoratedBottom(childAtPosition) - getDecoratedTop(childAtPosition)
            Log.v("StickyLayoutManager", "=======> Is Expand Item Found $adapterPosition Expected Height $measureViewHeight Current Height $currentHeight scroll by value $scrollBy top ${getDecoratedTop(childAtPosition)} bottom ${getDecoratedBottom(childAtPosition)}")
            val nextView = if (index < childCount - 1) getChildAt(index + 1) else null
            var shouldExpandItem = false
            if (nextView != null) {
                // top off next view appear more than 10% of screen at bottom
                if (height - nextView.top >= boundaryExpandItem) {
                    shouldExpandItem = true
                }
            } else {
                // don't have next view => may be the last view
                val isLastItem = adapterPosition == totalItemCount - 1
                if (isLastItem) {
                    shouldExpandItem = true
                }
            }
            childAtPosition.visibility = if (currentHeight <= (measureViewHeight - measureHeightWithoutDecorated)) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            if (currentHeight < measureViewHeight && shouldExpandItem) {
                nextView?.run { correctDockItemPosition(this) }

                val newCurrentHeight = currentHeight + abs(scrollBy)
                // all the scroll by value has been used for expand item height => All the item below should be stay the same at it position
                if (newCurrentHeight <= measureViewHeight) {
                    layoutDecorated(childAtPosition, left, getDecoratedTop(childAtPosition), right, getDecoratedBottom(childAtPosition) + abs(scrollBy))
                    getChildAt(index)?.offsetTopAndBottom(scrollBy)
                } else {
                    val itemScrollBy = measureViewHeight - currentHeight
                    Log.e("StickyLayoutManager", "Over scrolling...... currentHeight $currentHeight newCurrentHeight $newCurrentHeight measureViewHeight $measureViewHeight itemScrollBy $itemScrollBy")
                    layoutDecorated(childAtPosition, left, getDecoratedTop(childAtPosition), right, getDecoratedBottom(childAtPosition) + itemScrollBy)
                    getChildAt(index)?.offsetTopAndBottom(scrollBy)

                    val delta = scrollBy + itemScrollBy
                    for (j in index + 1 until childCount) {
                        val forceStopEarly = layoutItemBottom(j, totalItemCount, delta, left, right)
                        if (forceStopEarly) {
                            break
                        }
                    }
                }
                return true
            } else {
                getChildAt(index)?.offsetTopAndBottom(scrollBy)
            }
        } else {
            childAtPosition.visibility = View.VISIBLE
            getChildAt(index)?.offsetTopAndBottom(scrollBy)
        }

        return false
    }

    private fun correctDockItemPosition(view: View) {
        val currentTop = view.top
        val expectedTop = height - boundaryExpandItem
        val offset = expectedTop - currentTop
        if (offset > 0) {
            offsetChildrenVertical(offset)
        }
    }

    private fun recycleViewsOutOfBounds(recycler: Recycler) {
        val childCount = childCount
        val parentWidth = width
        val parentHeight = height
        var foundFirst = false
        var first = 0
        var last = 0
        for (i in 0 until childCount) {
            val view = getChildAt(i) ?: continue
            if (view.hasFocus() || getDecoratedRight(view) >= 0 && getDecoratedLeft(view) <= parentWidth && getDecoratedBottom(view) >= 0 && getDecoratedTop(view) <= parentHeight) {
                if (!foundFirst) {
                    first = i
                    foundFirst = true
                }
                last = i
            }
        }
        for (i in childCount - 1 downTo last + 1) {
            removeAndRecycleViewAt(i, recycler)
        }
        for (i in first - 1 downTo 0) {
            removeAndRecycleViewAt(i, recycler)
        }
        if (getChildCount() == 0) {
            mFirstPosition = 0
        } else {
            mFirstPosition += first
        }
    }

    private fun updateAnchorOffset() {
        mFirstPositionOffset = if (childCount > 0) {
            val view = getChildAt(0) ?: return
            getDecoratedTop(view) - (view.layoutParams as RecyclerView.LayoutParams).topMargin - paddingTop
        } else {
            0
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        // set expand item percent size.....
        expandItemCurrentHeightPercent.clear()
        for (i in 0 until childCount) {
            val itemView = getChildAt(i) ?: continue
            val adapterPosition = getPosition(itemView)
            val isExpandItem = expandItemListener?.isExpandItem(adapterPosition) ?: false
            if (isExpandItem) {
                val measureViewHeight = getDecoratedMeasuredHeight(itemView)
                val currentHeight = getDecoratedBottom(itemView) - getDecoratedTop(itemView)
                val percent = currentHeight.toDouble() / measureViewHeight
                expandItemCurrentHeightPercent[adapterPosition] = percent
            }
        }
        return SavedState(mFirstPosition, mFirstPositionOffset, expandItemCurrentHeightPercent)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
        if (state is SavedState) {
            mFirstPosition = state.mAnchorPosition
            mFirstPositionOffset = state.mAnchorOffset
            expandItemCurrentHeightPercent.clear()
            expandItemCurrentHeightPercent.putAll(state.expandItemCurrentHeightPercent)
        }
    }

    class SavedState : Parcelable {
        var mAnchorPosition: Int
        var mAnchorOffset: Int
        var expandItemCurrentHeightPercent = ConcurrentHashMap<Int, Double>()

        @Suppress("UNCHECKED_CAST")
        internal constructor(`in`: Parcel) {
            mAnchorPosition = `in`.readInt()
            mAnchorOffset = `in`.readInt()
            expandItemCurrentHeightPercent = `in`.readSerializable() as ConcurrentHashMap<Int, Double>
        }

        constructor(position: Int, offset: Int, currentHeightPercent: ConcurrentHashMap<Int, Double>) {
            mAnchorPosition = position
            mAnchorOffset = offset
            expandItemCurrentHeightPercent = currentHeightPercent
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(mAnchorPosition)
            dest.writeInt(mAnchorOffset)
            dest.writeSerializable(expandItemCurrentHeightPercent)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}