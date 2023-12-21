package com.monerosms.unofficial

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import okhttp3.*
import java.io.IOException

class BackgroundJobIntentService : JobIntentService() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onHandleWork(intent: Intent) {
        val userId = intent.getStringExtra("userId")
        val rd = intent.getStringExtra("response_data")
        if (userId != null && rd != null) {
            checkForUpdates(userId, rd)
        }
    }

    private fun checkForUpdates(userId: String, rd: String) {
        val url = "https://api.monerosms.com/$userId/list"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()

                    if (responseBody == "No threads."){
                        return
                    }

                    if (responseBody != null && rd != responseBody) {
                        sendNotification("You have unread SMS\nTap here to view the message(s)", userId)
                        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        with(sharedPreferences.edit()) {
                            putString("response_data", responseBody)
                            apply()
                        }
                    }
                } catch (_: IOException) {

                }
            }

            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    private fun sendNotification(message: String, userId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Crear la intención para abrir la actividad principal
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra("user_id", userId)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Crear la notificación
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New Message")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Mostrar la notificación
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val JOB_ID = 1
        const val CHANNEL_ID = "default_channel"
        const val NOTIFICATION_ID = 2

        fun enqueueWork(context: Context, userId: String, rd: String) {
            val intent = Intent(context.applicationContext, BackgroundJobIntentService::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("response_data", rd)
            enqueueWork(context, BackgroundJobIntentService::class.java, JOB_ID, intent)
        }
    }
}