package com.example.quotewidget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuoteAdapter(
    private val onEdit: (id: Int, text: String) -> Unit,
    private val onDelete: (id: Int) -> Unit
) : RecyclerView.Adapter<QuoteAdapter.ViewHolder>() {

    private val quotes = mutableListOf<QuoteApi.Quote>()
    private var isLoading = false

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.quote_text)
        val meta: TextView = view.findViewById(R.id.quote_meta)
        val btnEdit: ImageView = view.findViewById(R.id.btn_edit)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quote, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val q = quotes[position]
        holder.text.text = q.text
        holder.meta.text = "#${q.id}  ${q.created_at}"
        holder.btnEdit.setOnClickListener { onEdit(q.id, q.text) }
        holder.btnDelete.setOnClickListener { onDelete(q.id) }
    }

    override fun getItemCount() = quotes.size

    fun setQuotes(newQuotes: List<QuoteApi.Quote>) {
        quotes.clear()
        quotes.addAll(newQuotes)
        notifyDataSetChanged()
    }

    fun addQuotes(newQuotes: List<QuoteApi.Quote>) {
        val start = quotes.size
        quotes.addAll(newQuotes)
        notifyItemRangeInserted(start, newQuotes.size)
    }

    fun getQuoteAt(position: Int) = if (position < quotes.size) quotes[position] else null

    fun removeQuote(id: Int) {
        val pos = quotes.indexOfFirst { it.id == id }
        if (pos >= 0) {
            quotes.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }

    fun updateQuote(id: Int, newText: String) {
        val pos = quotes.indexOfFirst { it.id == id }
        if (pos >= 0) {
            quotes[pos] = quotes[pos].copy(text = newText)
            notifyItemChanged(pos)
        }
    }

    fun isLoading() = isLoading
    fun setLoading(loading: Boolean) { isLoading = loading }
}
