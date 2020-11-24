package com.me.stickyrecyclerview

import android.view.View

interface ExpandRecyclerItem {
    fun isExpandItem(position: Int): Boolean

    fun bumpAnimation(view : View, position: Int)
}