package com.me.stickyrecyclerview

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.HashMap

class MainAdapter: RecyclerView.Adapter<MainAdapter.ViewHolder>(), ExpandRecyclerItem {

    private val rnd = Random()
    private val colorCached = HashMap<Int, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvTitle.text = position.toString()
        val color: Int = if (colorCached[position] != null) {
            colorCached[position]!!
        } else {
            val colorGen = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
            colorCached[position] = colorGen
            colorGen
        }
        holder.itemView.setBackgroundColor(color)
    }

    override fun getItemCount(): Int {
        return 30
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView by lazy { itemView.findViewById(R.id.tvTitle) }
    }

//    override fun isStickyHeader(position: Int): Boolean {
//        return position % 5 == 0
//    }

    override fun isExpandItem(position: Int): Boolean {
        return position % 5 == 0 && position != 0
    }

    override fun bumpAnimation(position: Int) {

    }
}