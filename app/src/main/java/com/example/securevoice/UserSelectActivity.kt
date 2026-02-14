package com.example.securevoice

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UserSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_select)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                username == "userA" && password == "1234" -> {
                    saveUser("UserA")
                }

                username == "userB" && password == "1234" -> {
                    saveUser("UserB")
                }

                else -> {
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveUser(user: String) {
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit()
            .putString("current_user", user)
            .apply()

        startActivity(Intent(this, ChatListActivity::class.java))
        finish()
    }
}
