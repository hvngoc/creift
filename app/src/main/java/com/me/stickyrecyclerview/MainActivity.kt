package com.me.stickyrecyclerview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.me.stickyrecyclerview.bun.PiAdapter
import com.me.stickyrecyclerview.bun.StickyHeadersLinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
    }

    private fun setupViews() {
        val adapter = MainAdapter()
        val layoutManager = StickyLayoutManager()
        layoutManager.expandItemListener = adapter
        rcItems.layoutManager = layoutManager
        rcItems.addItemDecoration(StickyItemDecoration(resources.getDimensionPixelSize(R.dimen.space_16)))
        rcItems.adapter = adapter

        btnRefresh.setOnClickListener {
            adapter.notifyDataSetChanged()

            /*
            val firstVisibleView = layoutManager.findFirstVisibleItemPosition()
            val lastVisibleView = layoutManager.findLastVisibleItemPosition()
            Log.v("MainActivity", "First visible $firstVisibleView lastVisibleView $lastVisibleView")
            adapter.notifyItemChanged(firstVisibleView)
            */
        }

        burn.setOnClickListener {
        rcItems.setHasFixedSize(true)
            rcItems.adapter = PiAdapter()
            rcItems.layoutManager = StickyHeadersLinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        }
    }
}