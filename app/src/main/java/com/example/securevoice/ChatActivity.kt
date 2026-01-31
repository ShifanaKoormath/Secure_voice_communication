package com.example.securevoice

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ChatActivity : AppCompatActivity() {

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ArrayAdapter<String>

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private lateinit var audioFile: File

    private val backendBase = "http://10.0.2.2:8000/api"

    // ---------------- PERMISSION ----------------

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

    // ---------------- RECORDING ----------------

    private fun startRecording() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, channelConfig, audioFormat
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
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

    // ---------------- WAV ----------------

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

    // ---------------- SEND VOICE ----------------

    private fun sendVoiceToBackend(
        sender: String,
        receiver: String,
        audioFile: File
    ) {
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

    // ---------------- PLAY VOICE ----------------

    private fun fetchAndPlayEnhancedAudio(messageId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val file = File(cacheDir, "enhanced_$messageId.wav")
            val conn = URL("$backendBase/enhance/$messageId").openConnection() as HttpURLConnection

            conn.inputStream.use { input ->
                FileOutputStream(file).use { input.copyTo(it) }
            }

            conn.disconnect()

            withContext(Dispatchers.Main) {
                val player = MediaPlayer()
                player.setDataSource(file.absolutePath)
                player.prepare()
                player.start()
                player.setOnCompletionListener { player.release() }
            }
        }
    }

    // ---------------- TEXT ----------------

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

    // ---------------- LOAD ----------------

    private fun loadMessages(currentUser: String) {
        val contact = intent.getStringExtra("contact") ?: return

        CoroutineScope(Dispatchers.IO).launch {
    val response = URL(
        "$backendBase/messages/${URLEncoder.encode(currentUser, "UTF-8")}"
    ).readText()

    val json = JSONArray(response)

    withContext(Dispatchers.Main) {
        messages.clear()

        for (i in 0 until json.length()) {
            val msg = json.getJSONObject(i)
            val sender = msg.getString("sender")
            val receiver = msg.optString("receiver", "")

            if (
                (sender == currentUser && receiver == contact) ||
                (sender == contact && receiver == currentUser)
            ) {
                messages.add(
                    ChatMessage(
                        msg.getString("message_id"),
                        sender,
                        msg.optString("content", ""),
                        msg.optString("type", "VOICE")
                    )
                )
            }
        }

        adapter.clear()
        adapter.addAll(
            messages.map {
                if (it.type == "VOICE")
                    "🎤 Voice message – tap to play"
                else
                    "${it.sender}: ${it.content}"
            }
        )
        adapter.notifyDataSetChanged()
    }
}

    }

    // ---------------- UI ----------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val currentUser =
            getSharedPreferences("prefs", MODE_PRIVATE).getString("current_user", "") ?: return
        val contact = intent.getStringExtra("contact") ?: return

        findViewById<TextView>(R.id.chatTitle).text = contact

        val listView = findViewById<ListView>(R.id.messageList)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, pos, _ ->
            val msg = messages[pos]
            if (msg.type == "VOICE") {
                fetchAndPlayEnhancedAudio(msg.messageId!!)
            }
        }

        loadMessages(currentUser)

        findViewById<ImageButton>(R.id.sendBtn).setOnClickListener {
            val text = findViewById<EditText>(R.id.messageInput).text.toString()
            if (text.isNotBlank()) {
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
}
