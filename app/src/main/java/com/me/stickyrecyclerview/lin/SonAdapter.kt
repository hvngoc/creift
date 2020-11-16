package com.me.stickyrecyclerview.lin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.me.stickyrecyclerview.ExpandRecyclerItem
import com.me.stickyrecyclerview.R

/**
 * Created by hvngoc on 11/13/20
 */
class SonAdapter : RecyclerView.Adapter<SonHolder>(), ExpandRecyclerItem {
    companion object {
        const val TYPE_ITEM = 1
        const val TYPE_ADS = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SonHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        return if (viewType == TYPE_ITEM) {
            ItemHolder(v)
        } else {
            AdsHolder(v)
        }
    }

    override fun onBindViewHolder(holder: SonHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = 20

    override fun getItemViewType(position: Int): Int {
        return if (position > 0 && position % 6 == 0) {
            TYPE_ADS
        } else {
            TYPE_ITEM
        }
    }

    override fun isExpandItem(position: Int): Boolean = getItemViewType(position) == TYPE_ADS
}