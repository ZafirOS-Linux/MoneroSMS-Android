package com.monerosms.unofficial

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class LoginActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        editText = findViewById(R.id.editText)
        saveButton = findViewById(R.id.saveButton)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        runOnUiThread {
            supportActionBar?.title = getString(R.string.app_name_unofficial)
        }

        if (ActivityCompat.checkSelfPermission(
                this@LoginActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@LoginActivity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS
            )
        }

        val userId = sharedPreferences.getString("user_id", null)
        if (userId != null) {
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
            finish()
        }

        saveButton.setOnClickListener {
            val userId2 = editText.text.toString()
            with(sharedPreferences.edit()) {
                putString("user_id", userId2)
                apply()
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.putExtra("user_id", userId2)
                startActivity(intent)
                finish()
            }
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 1001
    }
}