package com.example.httpdebugger

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusText: TextView
    private lateinit var testButton: Button
    private var debugServer: DebugWebServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusText = findViewById(R.id.statusText)
        testButton = findViewById(R.id.testButton)

        startButton.setOnClickListener {
            debugServer = DebugWebServerHelper.quickStart(this)
            val url = DebugWebServerHelper.getServerUrl()
            statusText.text = "Server running at:\n$url"
        }

        stopButton.setOnClickListener {
            debugServer?.stopServer()
            statusText.text = "Server stopped"
        }

        testButton.setOnClickListener {
            sendTestRequest()
        }
    }

    private fun sendTestRequest() {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpDebugInterceptor())
            .build()

        val request = Request.Builder()
            .url("https://httpbin.org/get")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Request failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Request succeeded", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        debugServer?.stopServer()
    }
}