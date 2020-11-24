package com.me.stickyrecyclerview

interface ExpandRecyclerItem {
    fun isExpandItem(position: Int): Boolean

    fun bumpAnimation(position: Int)
}