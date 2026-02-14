package com.example.securevoice

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ChatListAdapter(
    context: Context,
    private val users: List<String>
) : ArrayAdapter<String>(context, 0, users) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_chat, parent, false)

        val name = view.findViewById<TextView>(R.id.chatName)
        val last = view.findViewById<TextView>(R.id.lastMessage)
        val avatar = view.findViewById<TextView>(R.id.avatar)

        val user = users[position]

        name.text = user
        last.text = "Tap to open secure chat"
        avatar.text = user.first().toString()

        return view
    }
}
