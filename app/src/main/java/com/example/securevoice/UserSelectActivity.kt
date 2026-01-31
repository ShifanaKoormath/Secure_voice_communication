package com.example.securevoice

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class UserSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_select)

        findViewById<Button>(R.id.btnUserA).setOnClickListener {
saveUser("UserA")
        }

        findViewById<Button>(R.id.btnUserB).setOnClickListener {
            saveUser("UserB")
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
