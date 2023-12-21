package com.monerosms.unofficial

import android.Manifest
import android.annotation.SuppressLint
import android.app.job.JobScheduler
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var handler: Handler

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        messageAdapter = MessageAdapter(ArrayList())
        recyclerView.adapter = messageAdapter

        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS
            )
        }

        fetchUserNumber()
        fetchData()

        BackgroundService.startService(this)

        handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                scheduleJob()
                handler.postDelayed(this, 30 * 1000)
            }
        }, 0)
    }

    private fun fetchUserNumber() {
        val userId = intent.getStringExtra("user_id")

        val url = "https://api.monerosms.com/$userId/number"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val userNumber = response.body?.string()
                updateActionBarTitle("+1" + userNumber.toString())
            }

            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    private fun updateActionBarTitle(title: String?) {
        runOnUiThread {
            supportActionBar?.title = title ?: getString(R.string.app_name_unofficial)
        }
    }

    private fun fetchData() {
        val userId = intent.getStringExtra("user_id")

        val url = "https://api.monerosms.com/$userId/list"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("response_data", responseData)
                    apply()
                }
                parseData(responseData)
            }

            override fun onFailure(call: Call, e: IOException) {
            }
        })
    }

    private fun fetchMessages(threadId: String) {
        val userId = intent.getStringExtra("user_id")

        val url = "https://api.monerosms.com/$userId/thread/$threadId/0"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                parseMessages(responseData, threadId)
            }

            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    private fun parseData(response: String?) {
        try {
            val idArray = response?.split("\n") ?: emptyList()
            for (threadId in idArray) {
                if (threadId.isNotEmpty()) {
                    fetchMessages(threadId.trim())
                }
            }
        } catch (e: Exception) {
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun parseMessages(response: String?, threadId: String) {
        try {
            val messages = mutableListOf<Message>()

            if (!response.isNullOrBlank()) {
                if ("85474" in response) {
                    val content = response.trim()
                    val message = Message("received", content)
                    messages.add(message)
                } else if("Internal Server Error" in response){
                    val message = Message("received", "Nothing to see here.")
                    messages.add(message)
                } else {
                    val parts = response.split("\n\n")

                    for (part in parts) {
                        if (part.trim().isNotEmpty()) {
                            val content = part.trim()
                            val message = Message("received", content)
                            messages.add(message)
                        }
                    }
                }
            }

            runOnUiThread {
                messageAdapter.addMessages(messages)
                recyclerView.adapter?.notifyDataSetChanged()
                recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
            }
        } catch (_: Exception) {
        }
    }

    private fun scheduleJob() {
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userId = intent.getStringExtra("user_id")
        val rd = sharedPreferences.getString("response_data", null)
        if (userId != null && rd != null) {
            BackgroundJobIntentService.enqueueWork(this, userId, rd)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelJob()
    }

    private fun cancelJob() {
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(1)
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 1001
    }
}