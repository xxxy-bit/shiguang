package com.example.quotewidget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class QuotePagerAdapter(
    private val inflater: LayoutInflater
) : RecyclerView.Adapter<QuotePagerAdapter.PageHolder>() {

    private val pages = mutableListOf<View>()
    private val layoutIds = intArrayOf(
        R.layout.page_quotes,
        R.layout.page_schedules,
        R.layout.page_settings
    )

    inner class PageHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun getItemCount() = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val view = inflater.inflate(layoutIds[viewType], parent, false)
        pages.add(view)
        return PageHolder(view)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {}

    override fun getItemViewType(position: Int) = position

    fun getPageView(position: Int): View? = pages.getOrNull(position)
}
