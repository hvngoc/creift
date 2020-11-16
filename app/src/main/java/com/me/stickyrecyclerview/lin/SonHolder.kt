package com.me.stickyrecyclerview.lin

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.me.stickyrecyclerview.R
import java.util.*

/**
 * Created by hvngoc on 11/14/20
 */

abstract class SonHolder(v: View) : RecyclerView.ViewHolder(v) {
    open fun bind() {
        itemView.findViewById<TextView>(R.id.tvTitle).text = adapterPosition.toString()

        val random = Random()
        val r: Int = random.nextInt(255)
        val g: Int = random.nextInt(255)
        val b: Int = random.nextInt(255)
        val color = Color.rgb(r, g, b)

        itemView.setBackgroundColor(color)
    }
}

class ItemHolder(v: View) : SonHolder(v) {
}

class AdsHolder(v: View) : SonHolder(v) {

    override fun bind() {
        super.bind()
        itemView.findViewById<TextView>(R.id.tvTitle).text = "ADS $adapterPosition"
    }
}