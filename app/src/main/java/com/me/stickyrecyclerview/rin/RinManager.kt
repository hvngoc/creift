package com.me.stickyrecyclerview.rin

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.me.stickyrecyclerview.ExpandRecyclerItem
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

/**
 * Created by hvngoc on 11/16/20
 */
class RinManager : RecyclerView.LayoutManager() {

    // Save first view position in each scrolling step....
    private var mFirstPosition = 0
    private var mFirstPositionOffset = 0
    var expandItemListener: ExpandRecyclerItem? = null

    private val recycleViews: MutableList<RecyclerView.ViewHolder> = CopyOnWriteArrayList()
    private val expandItemCurrentHeightPercent = ConcurrentHashMap<Int, Double>()

    private val boundaryExpandItem get() = (width * 0.1f).toInt()

    // call when initial layout manager and every notify data changed
    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        val parentEnd = width - paddingEnd
        var oldLeft = paddingLeft
        if (childCount > 0) {
            // happen after first init, may trigger by notify data set changed
            val oldLeftView = getChildAt(0)
            if (oldLeftView != null) {
                oldLeft = oldLeftView.left
            }

            // try to store the old current height info of expand item
            for (i in 0 until childCount) {
                val itemView = getChildAt(i) ?: continue
                val adapterPosition = getPosition(itemView)
                val isExpandItem = expandItemListener?.isExpandItem(adapterPosition) ?: false
                if (isExpandItem) {
                    val measureViewWidth = getDecoratedMeasuredWidth(itemView)
                    val currentWidth = getDecoratedRight(itemView) - getDecoratedLeft(itemView)
                    val percent = currentWidth.toDouble() / measureViewWidth
                    expandItemCurrentHeightPercent[adapterPosition] = percent
                }
            }
        } else {
            // happen on first init or after screen rotation, config changed...
            if (mFirstPositionOffset < state.itemCount) {
                oldLeft += mFirstPositionOffset
            }
        }
        Log.v("bowbow", "oldLeft 0ld View $oldLeft")

        // put view back to scrap heap => remove all visible view on screen
        detachAndScrapAttachedViews(recycler)
        var leftPos = oldLeft
        var rightPos = 0
        val topPos = paddingTop
        val bottomPos = height - paddingBottom
        val count = state.itemCount
        // fill up the whole screen with new view again
        run {
            var i = 0
            while (mFirstPosition + i < count && leftPos < parentEnd) {
                val position = mFirstPosition + i

                // this logic used to layout the view on initial or notify data changed
                val view = recycler.getViewForPosition(position)
                addView(view, i)
                measureChildWithMargins(view, 0, 0)

                // check for expand item
                val isExpandItem = expandItemListener?.isExpandItem(position) ?: false
                if (isExpandItem) {
                    val percent = expandItemCurrentHeightPercent[position] ?: 1.0
                    rightPos = leftPos + ((getDecoratedMeasuredWidth(view) * percent).toInt())
                    layoutDecorated(view, leftPos, topPos, rightPos, bottomPos)
                } else {
                    rightPos = leftPos + getDecoratedMeasuredWidth(view)
                    layoutDecorated(view, leftPos, topPos, rightPos, bottomPos)
                }

                i++
                leftPos = rightPos
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
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun canScrollHorizontally(): Boolean {
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


    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (childCount == 0) {
            return 0
        }
        var scrolled = 0
        val topPadding = paddingTop
        val bottomPadding = height - paddingBottom

        if (dx < 0) {
            Log.v("bowbow", "Fill Left scrolled $scrolled dx $dx")
            // fill top
            while (scrolled > dx) {
                val leftView = getChildAt(0)
                val hangingLeft = (-getDecoratedLeft(leftView!!)).coerceAtLeast(0)
                val scrollBy = (scrolled - dx).coerceAtMost(hangingLeft)
                Log.i("bowbow", "Fill Left scrolled $scrolled  scrollBy $scrollBy dx $dx")
                scrolled -= scrollBy

                // scroll view
                for (i in 0 until childCount) {
                    val forceStopEarly =
                        layoutItemLeft(i, state.itemCount, scrollBy, topPadding, bottomPadding)
                    if (forceStopEarly) {
                        break
                    }
                }

                if (mFirstPosition > 0 && scrolled > dx) {
                    mFirstPosition--
                    val view = recycler.getViewForPosition(mFirstPosition)
                    addView(view, 0)
                    measureChildWithMargins(view, 0, 0)
                    val right = getDecoratedRight(leftView)
                    val left = right - getDecoratedMeasuredWidth(view)
                    // scroll up => height of expand item already as full size
                    layoutDecorated(view, left, topPadding, right, bottomPadding)
                } else {
                    break
                }
            }
        } else if (dx > 0) {
            Log.v("bowbow", "Fill Right")
            // fill bottom
            val parentWidth = width
            while (scrolled < dx) {
                val rightView = getChildAt(childCount - 1)
                val hangingRight =
                    (getDecoratedRight(rightView!!) - parentWidth).coerceAtLeast(0)
                val scrollBy = -(dx - scrolled).coerceAtMost(hangingRight)
                scrolled -= scrollBy

                // scroll view (shift content already in screen)
                for (i in 0 until childCount) {
                    val forceStopEarly =
                        layoutItemRight(i, state.itemCount, scrollBy, topPadding, bottomPadding)
                    if (forceStopEarly) {
                        break
                    }
                }

                // create new view when still have remain space on screen
                if (scrolled < dx && state.itemCount > mFirstPosition + childCount) {
                    val view = recycler.getViewForPosition(mFirstPosition + childCount)
                    val left = getDecoratedRight(getChildAt(childCount - 1)!!)
                    addView(view)
                    measureChildWithMargins(view, 0, 0)
                    val itemPosition = getPosition(view)
                    val isExpandItem = expandItemListener?.isExpandItem(itemPosition) ?: false
                    if (isExpandItem) {
                        Log.e(
                            "bowbow",
                            "Append new item $itemPosition is expand item $isExpandItem"
                        )
                        // default height is 0
                        layoutDecorated(view, left, topPadding, left, bottomPadding)
                    } else {
                        val right = left + getDecoratedMeasuredWidth(view)
                        layoutDecorated(view, left, topPadding, right, bottomPadding)
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

    private fun layoutItemLeft(
        index: Int,
        totalItemCount: Int,
        scrollBy: Int,
        top: Int,
        bottom: Int
    ): Boolean {
        val childAtPosition = getChildAt(index) ?: return false
        val adapterPosition = getPosition(childAtPosition)
        val isExpandItem = expandItemListener?.isExpandItem(adapterPosition) ?: false
        if (isExpandItem) {
            val measureViewWidth = getDecoratedMeasuredWidth(childAtPosition)
            val measureWidthWithoutDecorated =
                measureViewWidth - getLeftDecorationWidth(childAtPosition) - getRightDecorationWidth(
                    childAtPosition
                )
            val currentWidth =
                getDecoratedRight(childAtPosition) - getDecoratedLeft(childAtPosition)


            val nextView = if (index < childCount - 1) getChildAt(index + 1) else null
            var shouldCollapseItem = false
            if (nextView != null) {
                // top off next view appear more than 10% of screen at bottom
                if (width - nextView.left <= boundaryExpandItem) {
                    shouldCollapseItem = true
                }
            } else {
                // don't have next view => may be the last view
                val isLastItem = adapterPosition == totalItemCount - 1
                if (isLastItem) {
                    shouldCollapseItem = true
                }
            }
            childAtPosition.visibility =
                if (currentWidth <= (measureViewWidth - measureWidthWithoutDecorated)) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }
            if (currentWidth > 0 && shouldCollapseItem) {
                nextView?.run { correctDockItemPosition(this) }

                val newCurrentWidth = currentWidth - abs(scrollBy)
                // all the scroll by value has been used for expand item height => All the item below should be stay the same at it position
                if (newCurrentWidth > 0) {
                    getChildAt(index)?.offsetLeftAndRight(scrollBy)
                    layoutDecorated(
                        childAtPosition,
                        getDecoratedLeft(childAtPosition),
                        top,
                        getDecoratedRight(childAtPosition) - abs(scrollBy),
                        bottom
                    )
                } else {
                    val itemScrollBy = -currentWidth
                    Log.e(
                        "bowbow left",
                        "Over scrolling...... currentHeight $currentWidth newCurrentHeight $newCurrentWidth measureViewHeight $measureViewWidth itemScrollBy $itemScrollBy"
                    )
                    getChildAt(index)?.offsetLeftAndRight(scrollBy)
                    layoutDecorated(
                        childAtPosition,
                        getDecoratedLeft(childAtPosition),
                        top,
                        getDecoratedRight(childAtPosition) + itemScrollBy,
                        bottom
                    )

                    val delta = scrollBy + itemScrollBy
                    for (j in index + 1 until childCount) {
                        val forceStopEarly = layoutItemLeft(j, totalItemCount, delta, top, bottom)
                        if (forceStopEarly) {
                            break
                        }
                    }
                }

                return true
            } else {
                getChildAt(index)?.offsetLeftAndRight(scrollBy)
            }
        } else {
            childAtPosition.visibility = View.VISIBLE
            getChildAt(index)?.offsetLeftAndRight(scrollBy)
        }

        return false
    }

    private fun layoutItemRight(
        index: Int,
        totalItemCount: Int,
        scrollBy: Int,
        top: Int,
        bottom: Int
    ): Boolean {
        val childAtPosition = getChildAt(index) ?: return false
        val adapterPosition = getPosition(childAtPosition)
        val isExpandItem = expandItemListener?.isExpandItem(adapterPosition) ?: false
        if (isExpandItem) {
            val measureViewWidth = getDecoratedMeasuredWidth(childAtPosition)
            val measureWidthWithoutDecorated =
                measureViewWidth - getLeftDecorationWidth(childAtPosition) - getRightDecorationWidth(
                    childAtPosition
                )
            val currentWidth =
                getDecoratedRight(childAtPosition) - getDecoratedLeft(childAtPosition)
            Log.v(
                "bowbow right",
                "=======> Is Expand Item Found $adapterPosition Expected width $measureViewWidth " +
                        "Current width $currentWidth  left ${getDecoratedLeft(childAtPosition)} " +
                        "right ${getDecoratedRight(childAtPosition)}"
            )
            val nextView = if (index < childCount - 1) getChildAt(index + 1) else null
            var shouldExpandItem = false
            if (nextView != null) {
                // top off next view appear more than 10% of screen at bottom
                if (width - nextView.left >= boundaryExpandItem) {
                    shouldExpandItem = true
                }
            } else {
                // don't have next view => may be the last view
                val isLastItem = adapterPosition == totalItemCount - 1
                if (isLastItem) {
                    shouldExpandItem = true
                }
            }
            childAtPosition.visibility =
                if (currentWidth <= (measureViewWidth - measureWidthWithoutDecorated)) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }
            if (currentWidth < measureViewWidth && shouldExpandItem) {
                nextView?.run { correctDockItemPosition(this) }

                val newCurrentWidth = currentWidth + abs(scrollBy)
                Log.d("bowbow", "newCurrentWidth $newCurrentWidth")
                // all the scroll by value has been used for expand item height => All the item below should be stay the same at it position
                if (newCurrentWidth <= measureViewWidth) {
                    layoutDecorated(
                        childAtPosition,
                        getDecoratedLeft(childAtPosition),
                        top,
                        getDecoratedRight(childAtPosition) + abs(scrollBy),
                        bottom
                    )
                    getChildAt(index)?.offsetLeftAndRight(scrollBy)
                } else {
                    val itemScrollBy = measureViewWidth - currentWidth
                    Log.e(
                        "bowbow",
                        "currentWidth $currentWidth newCurrentWidth $newCurrentWidth" +
                                " measureViewWidth $measureViewWidth itemScrollBy $itemScrollBy"
                    )
                    layoutDecorated(
                        childAtPosition,
                        getDecoratedLeft(childAtPosition),
                        top,
                        getDecoratedRight(childAtPosition) + itemScrollBy,
                        bottom
                    )
                    getChildAt(index)?.offsetLeftAndRight(scrollBy)
                    expandItemListener?.bumpAnimation(childAtPosition, index)

                    val delta = scrollBy + itemScrollBy
                    for (j in index + 1 until childCount) {
                        val forceStopEarly = layoutItemRight(j, totalItemCount, delta, top, bottom)
                        if (forceStopEarly) {
                            break
                        }
                    }
                }
                return true
            } else {
                getChildAt(index)?.offsetLeftAndRight(scrollBy)
            }
        } else {
            childAtPosition.visibility = View.VISIBLE
            getChildAt(index)?.offsetLeftAndRight(scrollBy)
        }

        return false
    }

    private fun correctDockItemPosition(view: View) {
        val currentLeft = view.left
        val expectedLeft = width - boundaryExpandItem
        val offset = expectedLeft - currentLeft
        if (offset > 0) {
            offsetChildrenHorizontal(offset)
        }
    }

    private fun recycleViewsOutOfBounds(recycler: RecyclerView.Recycler) {
        val childCount = childCount
        val parentWidth = width
        val parentHeight = height
        var foundFirst = false
        var first = 0
        var last = 0
        for (i in 0 until childCount) {
            val view = getChildAt(i) ?: continue
            if (view.hasFocus() || getDecoratedRight(view) >= 0 && getDecoratedLeft(view) <= parentWidth && getDecoratedBottom(
                    view
                ) >= 0 && getDecoratedTop(view) <= parentHeight
            ) {
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
            getDecoratedLeft(view) - (view.layoutParams as RecyclerView.LayoutParams).leftMargin - paddingLeft
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
                val measureViewWidth = getDecoratedMeasuredWidth(itemView)
                val currentWidth = getDecoratedRight(itemView) - getDecoratedLeft(itemView)
                val percent = currentWidth.toDouble() / measureViewWidth
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
            expandItemCurrentHeightPercent =
                `in`.readSerializable() as ConcurrentHashMap<Int, Double>
        }

        constructor(
            position: Int,
            offset: Int,
            currentHeightPercent: ConcurrentHashMap<Int, Double>
        ) {
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