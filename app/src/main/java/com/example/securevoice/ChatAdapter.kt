package com.example.securevoice
import android.graphics.Color

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
var urgentUsers: Set<String> = emptySet()
class ChatAdapter(
    private val currentUser: String,
    private val messages: List<ChatMessage>,
    private val onVoiceClick: (ChatMessage) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    // ✅ MUST be INSIDE the class
    private var playingMessageId: String? = null

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bubble: LinearLayout = view.findViewById(R.id.bubble)
        val text: TextView = view.findViewById(R.id.messageText)
        val time: TextView = view.findViewById(R.id.timeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]
        val isMe = msg.sender == currentUser

        // 🟡 DATE SEPARATOR
        if (msg.type == "DATE") {
            holder.text.text = msg.content
            holder.time.text = ""
            holder.bubble.background = null
            holder.text.setTextColor(Color.parseColor("#6A5ACD")) // slate purple
holder.text.textSize = 12f

            val params = holder.bubble.layoutParams as LinearLayout.LayoutParams
            params.gravity = Gravity.CENTER
            holder.bubble.layoutParams = params

            holder.itemView.setOnClickListener(null)
            return
        }

        // 🟢 TEXT / VOICE
      val baseText = when {
    msg.type == "VOICE" && msg.messageId == playingMessageId ->
        "🎤 Playing..."
    msg.type == "VOICE" ->
        "🎤 Voice message"
    else ->
        msg.content
}

holder.text.text = baseText


        holder.text.setTextColor(
    if (isMe)
        Color.parseColor("#4B2E83")  // deep purple
    else
        Color.parseColor("#333333")
)

        holder.time.text = msg.timestamp.takeLast(5)

        val params = holder.bubble.layoutParams as LinearLayout.LayoutParams
        params.gravity = if (isMe) Gravity.END else Gravity.START
        holder.bubble.layoutParams = params

      holder.bubble.background = ContextCompat.getDrawable(
    holder.itemView.context,
    if (isMe) R.drawable.bg_message_right
    else R.drawable.bg_message_left
)
when (msg.priority) {
    "URGENT" -> holder.bubble.setBackgroundColor(0xFFFFCDD2.toInt())
    "HARMFUL" -> holder.bubble.setBackgroundColor(0xFFFFE0B2.toInt())
}
// ================= PRIORITY COLOR =================
when (msg.priority) {
    "URGENT" -> holder.bubble.setBackgroundColor(0x30FF4444)   // light red
    "HARMFUL" -> holder.bubble.setBackgroundColor(0x30FFC107)  // amber
}

        // ================= PRIORITY COLOR =================
when (msg.priority) {
    "URGENT" -> holder.bubble.setBackgroundColor(0x30FF4444)   // light red
    "HARMFUL" -> holder.bubble.setBackgroundColor(0x30FFC107)  // amber
}


        holder.itemView.setOnClickListener {
            if (msg.type == "VOICE") {
                playingMessageId = msg.messageId
                notifyDataSetChanged()
                onVoiceClick(msg)
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    // ✅ MUST ALSO be INSIDE the class
    fun clearPlaying() {
        playingMessageId = null
        notifyDataSetChanged()
    }
}
