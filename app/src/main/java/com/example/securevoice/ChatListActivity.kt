package com.example.securevoice

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class ChatListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        val currentUser = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString("current_user", null)
            ?: return  // hard fail if not logged in (correct)

        // 🔒 Deterministic demo users
        val contacts = when (currentUser) {
            "UserA" -> listOf("UserB")
            "UserB" -> listOf("UserA")
            else -> emptyList()
        }

        val listView = findViewById<ListView>(R.id.chatList)
        listView.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, contacts)

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("contact", contacts[position])
            startActivity(intent)
        }
    }
}
