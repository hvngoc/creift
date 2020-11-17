package com.me.stickyrecyclerview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.me.stickyrecyclerview.lin.SonAdapter
import com.me.stickyrecyclerview.lin.SonManager
import com.me.stickyrecyclerview.lin.SonScrolling
import com.me.stickyrecyclerview.misa.RuManager
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

//        burn.setOnClickListener {
//            rcItems.setHasFixedSize(true)
//            rcItems.adapter = PiAdapter()
//            rcItems.layoutManager =
//                StickyHeadersLinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
//        }

        burn.setOnClickListener {
//            val manager = SonManager(this, RecyclerView.HORIZONTAL, false)
//            val sonter = SonAdapter()
//
//            rcItems.setHasFixedSize(true)
//            rcItems.addOnScrollListener(SonScrolling(manager, sonter, rcItems))
//            rcItems.adapter = sonter
//            rcItems.layoutManager = manager

            val sonter = SonAdapter()
            val manager = RuManager( sonter, this, RecyclerView.HORIZONTAL, false)

            rcItems.setHasFixedSize(true)
            rcItems.adapter = sonter
            rcItems.layoutManager = manager
        }
    }
}