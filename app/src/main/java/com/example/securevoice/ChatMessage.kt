package com.example.securevoice

data class ChatMessage(
    val messageId: String,
    val sender: String,
    val content: String,
    val type: String,
    val timestamp: String
)
