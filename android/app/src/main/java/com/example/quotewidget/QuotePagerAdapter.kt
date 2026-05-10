package com.example.quotewidget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class QuotePagerAdapter(
    private val inflater: LayoutInflater
) : RecyclerView.Adapter<QuotePagerAdapter.PageHolder>() {

    private val pages = mutableListOf<View>()

    inner class PageHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun getItemCount() = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val layoutId = if (viewType == 0) R.layout.page_quotes else R.layout.page_schedules
        val view = inflater.inflate(layoutId, parent, false)
        pages.add(view)
        return PageHolder(view)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {}

    override fun getItemViewType(position: Int) = position

    fun getPageView(position: Int): View = pages.getOrElse(position) {
        throw IndexOutOfBoundsException("Page $position not created yet")
    }
}
