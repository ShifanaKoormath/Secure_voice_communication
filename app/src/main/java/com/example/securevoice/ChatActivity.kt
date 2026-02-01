package com.example.securevoice

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ChatActivity : AppCompatActivity() {

    // ---------- UI ----------
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatAdapter

    // ---------- DATA ----------
    private val messages = mutableListOf<ChatMessage>()

    // ---------- AUDIO ----------
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private lateinit var audioFile: File

    // ---------- CONFIG ----------
    private val backendBase = "http://10.0.2.2:8000/api"

    // =====================================================
    // PERMISSION
    // =====================================================

    private fun ensureMicPermission(): Boolean {
        return if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1001
            )
            false
        } else true
    }

    // =====================================================
    // RECORDING
    // =====================================================

    private fun startRecording() {
        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(this, "Mic not available", Toast.LENGTH_LONG).show()
            return
        }

        audioFile = File(cacheDir, "voice_${System.currentTimeMillis()}.wav")
        val out = FileOutputStream(audioFile)
        writeWavHeader(out)

        audioRecord?.startRecording()
        isRecording = true

        Thread {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) out.write(buffer, 0, read)
            }
            out.close()
        }.start()
    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        updateWavHeader(audioFile)

        val sender = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString("current_user", "") ?: return
        val receiver = intent.getStringExtra("contact") ?: return

        sendVoiceToBackend(sender, receiver, audioFile)
    }

    // =====================================================
    // WAV HELPERS
    // =====================================================

    private fun writeWavHeader(out: FileOutputStream) {
        out.write(ByteArray(44))
    }

    private fun updateWavHeader(wav: File) {
        val dataSize = wav.length() - 44
        val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt((dataSize + 36).toInt())
        buffer.put("WAVEfmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(1)
        buffer.putInt(16000)
        buffer.putInt(16000 * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize.toInt())

        RandomAccessFile(wav, "rw").use {
            it.seek(0)
            it.write(buffer.array())
        }
    }

    // =====================================================
    // SEND VOICE
    // =====================================================

    private fun sendVoiceToBackend(sender: String, receiver: String, audioFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val boundary = "Boundary-${System.currentTimeMillis()}"
                val conn = URL("$backendBase/send-voice").openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=$boundary"
                )

                val output = DataOutputStream(conn.outputStream)

                fun field(name: String, value: String) {
                    output.writeBytes("--$boundary\r\n")
                    output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    output.writeBytes("$value\r\n")
                }

                field("sender", sender)
                field("receiver", receiver)

                output.writeBytes("--$boundary\r\n")
                output.writeBytes(
                    "Content-Disposition: form-data; name=\"audio\"; filename=\"voice.wav\"\r\n"
                )
                output.writeBytes("Content-Type: audio/wav\r\n\r\n")
                output.write(audioFile.readBytes())
                output.writeBytes("\r\n--$boundary--\r\n")
                output.close()

                conn.inputStream.close()
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    loadMessages(sender)
                }

            } catch (e: Exception) {
                Log.e("VOICE_UPLOAD", "Upload failed", e)
            }
        }
    }

    // =====================================================
    // PLAY VOICE (AudioTrack)
    // =====================================================

    private fun fetchAndPlayEnhancedAudio(messageId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val file = File(cacheDir, "enhanced_$messageId.wav")
            val conn = URL("$backendBase/enhance/$messageId")
                .openConnection() as HttpURLConnection

            conn.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            conn.disconnect()

            Log.d("VOICE_DEBUG", "Enhanced file size = ${file.length()} bytes")

            playWavWithAudioTrack(file)
        }
    }

    private fun playWavWithAudioTrack(file: File) {
        Thread {
            try {
                val input = FileInputStream(file)
                input.skip(44)

                val buffer = ByteArray(4096)
                val audioTrack = AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    16000,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    4096,
                    AudioTrack.MODE_STREAM
                )

                audioTrack.play()

                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    audioTrack.write(buffer, 0, read)
                }

                audioTrack.stop()
                audioTrack.release()
                input.close()

             runOnUiThread {
                adapter.clearPlaying()
            }
            } catch (e: Exception) {
                Log.e("VOICE_PLAY", "AudioTrack playback failed", e)
            }
        }.start()
    }

    // =====================================================
    // SEND TEXT
    // =====================================================

    private fun sendText(sender: String, receiver: String, text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val body =
                "sender=${URLEncoder.encode(sender, "UTF-8")}" +
                        "&receiver=${URLEncoder.encode(receiver, "UTF-8")}" +
                        "&text=${URLEncoder.encode(text, "UTF-8")}"

            val conn = URL("$backendBase/send-text").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray()) }

            conn.inputStream.close()
            conn.disconnect()

            withContext(Dispatchers.Main) {
                loadMessages(sender)
            }
        }
    }

    // =====================================================
    // LOAD MESSAGES (ORDERED)
    // =====================================================

private fun loadMessages(currentUser: String) {
    val contact = intent.getStringExtra("contact") ?: return

    CoroutineScope(Dispatchers.IO).launch {
        val response = URL(
            "$backendBase/messages/${URLEncoder.encode(currentUser, "UTF-8")}"
        ).readText()

        val json = JSONArray(response)

        withContext(Dispatchers.Main) {
            messages.clear()

            val temp = mutableListOf<ChatMessage>()

            // 1️⃣ Collect messages
            for (i in 0 until json.length()) {
                val msg = json.getJSONObject(i)
                val sender = msg.getString("sender")
                val receiver = msg.optString("receiver", "")

                if (
                    (sender == currentUser && receiver == contact) ||
                    (sender == contact && receiver == currentUser)
                ) {
                    temp.add(
                        ChatMessage(
                            messageId = msg.getString("message_id"),
                            sender = sender,
                            content = msg.optString("content", ""),
                            type = msg.optString("type", "VOICE"),
                            timestamp = msg.getString("timestamp")
                        )
                    )
                }
            }

            // 2️⃣ SORT ONCE (IMPORTANT)
            temp.sortBy { it.timestamp }

            // 3️⃣ INSERT DATE SEPARATORS
            var lastDate: String? = null

            for (msg in temp) {
                val date = msg.timestamp.substring(0, 10) // yyyy-mm-dd

                if (date != lastDate) {
                    messages.add(
                        ChatMessage(
                            messageId = "date_$date",
                            sender = "",
                            content = formatDateLabel(date),
                            type = "DATE",
                            timestamp = msg.timestamp
                        )
                    )
                    lastDate = date
                }

                messages.add(msg)
            }

            adapter.notifyDataSetChanged()
            recycler.scrollToPosition(messages.size - 1)
        }
    }
}


    // =====================================================
    // UI
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val currentUser =
            getSharedPreferences("prefs", MODE_PRIVATE).getString("current_user", "") ?: return
        val contact = intent.getStringExtra("contact") ?: return

        findViewById<TextView>(R.id.chatTitle).text = contact

        recycler = findViewById(R.id.messageList)
        recycler.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        adapter = ChatAdapter(currentUser, messages) { msg ->
            if (msg.type == "VOICE") {
                fetchAndPlayEnhancedAudio(msg.messageId)
            }
        }

        recycler.adapter = adapter

        loadMessages(currentUser)

        findViewById<ImageButton>(R.id.sendBtn).setOnClickListener {
            val input = findViewById<EditText>(R.id.messageInput)
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                input.setText("")
                sendText(currentUser, contact, text)
            }
        }

        findViewById<ImageButton>(R.id.micBtn).setOnClickListener {
            if (!isRecording) {
                if (ensureMicPermission()) startRecording()
            } else stopRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (
            requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        }
    }

    private fun formatDateLabel(date: String): String {
    val today = java.time.LocalDate.now()
    val msgDate = java.time.LocalDate.parse(date)

    return when {
        msgDate == today -> "Today"
        msgDate == today.minusDays(1) -> "Yesterday"
        else -> msgDate.toString()
    }
}

}
