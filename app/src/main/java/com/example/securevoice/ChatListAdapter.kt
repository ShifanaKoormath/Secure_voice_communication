package com.example.securevoice

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import android.graphics.Color
import android.graphics.Typeface

class ChatListAdapter(
    context: Context,
    private val users: List<String>,
    private val contactMeta: Map<String, Triple<Int, String, Long>>
) : ArrayAdapter<String>(context, 0, users)
 {

   override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val view = convertView ?: LayoutInflater.from(context)
        .inflate(R.layout.item_chat, parent, false)

    val name = view.findViewById<TextView>(R.id.chatName)
    val last = view.findViewById<TextView>(R.id.lastMessage)
    val avatar = view.findViewById<TextView>(R.id.avatar)

    val user = users[position]

    // contactMeta[user] = Triple(unreadCount, highestPriority, latestTimestamp)
val meta = contactMeta[user]
val unreadCount = meta?.first ?: 0
val priority = meta?.second ?: "SAFE"


    // ---------- NAME (Priority Indicator) ----------
    when (priority) {
        "URGENT" -> {
            name.text = "⚠ $user"
            name.setTextColor(android.graphics.Color.parseColor("#D32F2F")) // strong red
            name.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        "HARMFUL" -> {
            name.text = "☣ $user"
            name.setTextColor(android.graphics.Color.parseColor("#F57C00")) // amber
            name.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        else -> {
            name.text = user
            name.setTextColor(android.graphics.Color.BLACK)
            name.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    // ---------- LAST LINE (Unread Indicator) ----------
    if (unreadCount > 0) {
        last.text = "$unreadCount unread message(s)"
        last.setTextColor(android.graphics.Color.parseColor("#1976D2")) // blue
        last.setTypeface(null, android.graphics.Typeface.BOLD)
    } else {
        last.text = "Tap to open secure chat"
        last.setTextColor(android.graphics.Color.GRAY)
        last.setTypeface(null, android.graphics.Typeface.NORMAL)
    }

    // ---------- Avatar ----------
    avatar.text = user.first().toString()

    return view
}
}