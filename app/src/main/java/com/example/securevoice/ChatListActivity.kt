package com.example.securevoice

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder

class ChatListActivity : AppCompatActivity() {

    private val backendBase = "http://10.0.2.2:8000/api"
    private lateinit var listView: ListView
    private lateinit var contacts: MutableList<String>
    private var currentUser: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        currentUser = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString("current_user", null) ?: return

        val allUsers = listOf("UserA", "UserB", "UserC", "UserD")
        contacts = allUsers.filter { it != currentUser }.toMutableList()

        listView = findViewById(R.id.chatList)

        // Default empty adapter (prevents blank screen)
        listView.adapter = ChatListAdapter(
            context = this,
            users = contacts,
            contactMeta = emptyMap()
        )

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("contact", contacts[position])
            startActivity(intent)
        }
    }

    // 🔥 REFRESH EVERY TIME USER RETURNS
    override fun onResume() {
        super.onResume()
        loadChatList()
    }

    private fun loadChatList() {
        val user = currentUser ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = URL(
                    "$backendBase/messages/${URLEncoder.encode(user, "UTF-8")}"
                ).readText()

                val json = JSONArray(response)

                // contact -> (unreadCount, highestPriority, latestTimestamp)
                val contactMeta = mutableMapOf<String, Triple<Int, String, Long>>()

                for (contact in contacts) {
                    contactMeta[contact] = Triple(0, "SAFE", 0L)
                }

                for (i in 0 until json.length()) {
                    val msg = json.getJSONObject(i)

                    val sender = msg.getString("sender")
                    val receiver = msg.optString("receiver", "")
                    val priority = msg.optString("priority", "SAFE")
                    val isRead = msg.optBoolean("is_read", false)

                    val timestamp = try {
                        java.time.Instant.parse(msg.getString("timestamp") + "Z")
                            .toEpochMilli()
                    } catch (e: Exception) {
                        0L
                    }

                    val contact =
                        if (sender == user) receiver
                        else if (receiver == user) sender
                        else null

                    if (contact != null && contactMeta.containsKey(contact)) {
                        val (unread, topPriority, latest) = contactMeta[contact]!!

                        val newUnread =
                            if (!isRead && receiver == user) unread + 1 else unread

                        val newLatest = maxOf(latest, timestamp)

                        val newPriority = when {
                            priority == "URGENT" -> "URGENT"
                            priority == "HARMFUL" && topPriority != "URGENT" -> "HARMFUL"
                            else -> topPriority
                        }

                        contactMeta[contact] =
                            Triple(newUnread, newPriority, newLatest)
                    }
                }

                // 🔥 SORT LOGIC (Correct Order)
                contacts.sortWith(
                    compareByDescending<String> { contactMeta[it]!!.first > 0 }   // unread first
                        .thenByDescending {
                            when (contactMeta[it]!!.second) {
                                "URGENT" -> 2
                                "HARMFUL" -> 1
                                else -> 0
                            }
                        }
                        .thenByDescending { contactMeta[it]!!.third }            // latest message
                )

                withContext(Dispatchers.Main) {
                    listView.adapter = ChatListAdapter(
                        context = this@ChatListActivity,
                        users = contacts,
                        contactMeta = contactMeta
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}