package com.monerosms.unofficial

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messages: MutableList<Message>) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.received_message_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.messageTextView.text = message.content
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addMessages(newMessages: List<Message>) {
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}