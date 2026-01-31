package com.example.securevoice

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ChatActivity : AppCompatActivity() {

    private val messages = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private lateinit var audioFile: File
    private val backendBase = "http://10.0.2.2:8000/api"

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
    } else {
        true
    }
}

private fun startRecording() {
    val sampleRate = 16000
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )

    audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        channelConfig,
        audioFormat,
        bufferSize
    )

    // 🚨 ABSOLUTELY REQUIRED CHECK
    if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
        Toast.makeText(this, "Mic not available", Toast.LENGTH_LONG).show()
        audioRecord?.release()
        audioRecord = null
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

    val currentUser = getSharedPreferences("prefs", MODE_PRIVATE)
        .getString("current_user", "") ?: return

val contact = intent.getStringExtra("contact") ?: return


sendVoiceToBackend(currentUser, contact, audioFile)
}





private fun writeWavHeader(out: FileOutputStream) {
    out.write(ByteArray(44))
}

private fun updateWavHeader(wav: File) {
    val dataSize = wav.length() - 44
    val sampleRate = 16000
    val channels = 1
    val byteRate = sampleRate * channels * 2

    val buffer = ByteBuffer.allocate(44)
    buffer.order(ByteOrder.LITTLE_ENDIAN)

    buffer.put("RIFF".toByteArray())
    buffer.putInt((dataSize + 36).toInt())
    buffer.put("WAVEfmt ".toByteArray())
    buffer.putInt(16)
    buffer.putShort(1)
    buffer.putShort(channels.toShort())
    buffer.putInt(sampleRate)
    buffer.putInt(byteRate)
    buffer.putShort((channels * 2).toShort())
    buffer.putShort(16)
    buffer.put("data".toByteArray())
    buffer.putInt(dataSize.toInt())

    val raf = RandomAccessFile(wav, "rw")
    raf.seek(0)
    raf.write(buffer.array())
    raf.close()
}
private fun sendVoiceToBackend(
    sender: String,
    receiver: String,
    audioFile: File
)
{
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = URL("$backendBase/send-voice")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )

            val output = DataOutputStream(conn.outputStream)

            // sender field
            output.writeBytes("--$boundary\r\n")
            output.writeBytes("Content-Disposition: form-data; name=\"sender\"\r\n\r\n")
            output.writeBytes(sender + "\r\n")

            // receiver field
            output.writeBytes("--$boundary\r\n")
            output.writeBytes("Content-Disposition: form-data; name=\"receiver\"\r\n\r\n")
            output.writeBytes(receiver)
            output.writeBytes("\r\n")

            // audio file field
            output.writeBytes("--$boundary\r\n")
            output.writeBytes(
                "Content-Disposition: form-data; name=\"audio\"; filename=\"voice.wav\"\r\n"
            )
            output.writeBytes("Content-Type: audio/wav\r\n\r\n")
            output.write(audioFile.readBytes())
            output.writeBytes("\r\n")

            output.writeBytes("--$boundary--\r\n")
            output.flush()
            output.close()

          val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) {
            conn.inputStream
        } else {
            conn.errorStream
        }

        val response = stream?.bufferedReader()?.readText()
        Log.d("VOICE_UPLOAD", "HTTP $responseCode → $response")
        conn.disconnect()

            conn.disconnect()

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ChatActivity,
                    "Voice sent (code $responseCode)",
                    Toast.LENGTH_SHORT
                ).show()

                // TEMP: show voice message in chat list
                messages.add("🎤 Voice message")
                adapter.notifyDataSetChanged()
            }

        } catch (e: Exception) {
    Log.e("VOICE_UPLOAD", "Upload failed", e)
    withContext(Dispatchers.Main) {
        Toast.makeText(
            this@ChatActivity,
            "Voice send failed: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}

    }
}



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val contact = intent.getStringExtra("contact") ?: return
        val currentUser = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString("current_user", "") ?: return

        findViewById<TextView>(R.id.chatTitle).text = contact

        val listView = findViewById<ListView>(R.id.messageList)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
        listView.adapter = adapter

        val input = findViewById<EditText>(R.id.messageInput)

        // Load messages from backend
        loadMessages(currentUser)

        // SEND TEXT
        findViewById<ImageButton>(R.id.sendBtn).setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                input.setText("")
                sendText(currentUser, contact, text)
            }
        }

        // VOICE 
 findViewById<ImageButton>(R.id.micBtn).setOnClickListener {
    if (!isRecording) {
        if (!ensureMicPermission()) return@setOnClickListener
        startRecording()
        Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show()
    } else {
        stopRecording()
        Toast.makeText(this, "Voice recorded", Toast.LENGTH_SHORT).show()
    }
}



    }

private fun sendText(sender: String, receiver: String, text: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.d("CHAT", "Sending: $sender -> $receiver : $text")

            val url = URL("$backendBase/send-text")
            val body =
                "sender=${URLEncoder.encode(sender, "UTF-8")}" +
                "&receiver=${URLEncoder.encode(receiver, "UTF-8")}" +
                "&text=${URLEncoder.encode(text, "UTF-8")}"

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded"
            )

            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            Log.d("CHAT", "Send response code = $code")

            conn.inputStream.close()
            conn.disconnect()

            withContext(Dispatchers.Main) {
                loadMessages(sender)
            }
        } catch (e: Exception) {
    Log.e("CHAT_FATAL", "NETWORK FAILURE", e)

    withContext(Dispatchers.Main) {
        Toast.makeText(
            this@ChatActivity,
            "ERROR: ${e.javaClass.simpleName}\n${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}

    }
}

private fun loadMessages(currentUser: String) {
    val contact = intent.getStringExtra("contact") ?: return

    CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.d("CHAT", "Loading messages for $currentUser")

            val encodedUser = URLEncoder.encode(currentUser, "UTF-8")
            val url = URL("$backendBase/messages/$encodedUser")
            val conn = url.openConnection() as HttpURLConnection

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            Log.d("CHAT", "Raw response: $response")

            val json = JSONArray(response)

            withContext(Dispatchers.Main) {
                messages.clear()

                for (i in 0 until json.length()) {
                    val msg = json.getJSONObject(i)
                    val sender = msg.getString("sender")
                    val receiver = msg.getString("receiver")
                    val content = msg.getString("content")

                    Log.d("CHAT", "MSG $sender -> $receiver : $content")

                    if (
                        (sender == currentUser && receiver == contact) ||
                        (sender == contact && receiver == currentUser)
                    ) {
                        messages.add("$sender: $content")
                    }
                }

                Log.d("CHAT", "Messages shown = ${messages.size}")
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
    Log.e("CHAT_FATAL", "NETWORK FAILURE", e)

    withContext(Dispatchers.Main) {
        Toast.makeText(
            this@ChatActivity,
            "ERROR: ${e.javaClass.simpleName}\n${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}

    }
}


override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    if (
        requestCode == 1001 &&
        grantResults.isNotEmpty() &&
        grantResults[0] == PackageManager.PERMISSION_GRANTED
    ) {
        startRecording()
        Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show()
    }
}


}
