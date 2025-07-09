package com.example.httpdebugger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat

class DebugWebServerService : Service() {

    companion object {
        private const val CHANNEL_ID = "debug_web_server_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, port: Int = 8080) {
            val intent = Intent(context, DebugWebServerService::class.java).apply {
                putExtra("port", port)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DebugWebServerService::class.java)
            context.stopService(intent)
        }
    }

    private val binder = LocalBinder()
    private var debugServer: DebugWebServer? = null
    private var isServerRunning = false

    inner class LocalBinder : Binder() {
        fun getService(): DebugWebServerService = this@DebugWebServerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra("port", 8080) ?: 8080

        if (!isServerRunning) {
            startDebugServer(port)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDebugServer()
    }

    private fun startDebugServer(port: Int) {
        debugServer = DebugWebServer.create(this)
            .setPort(port)
            .enableLogging(true)
            .build()

        debugServer?.setServerListener(object : DebugWebServer.ServerListener {
            override fun onServerStarted(serverUrl: String) {
                isServerRunning = true
                showNotification(serverUrl)
            }

            override fun onServerStopped() {
                isServerRunning = false
                stopSelf()
            }

            override fun onServerError(error: String) {
                isServerRunning = false
                // Handle error (could show error notification)
            }

            override fun onRequest(uri: String, clientIp: String) {
                // Handle request logging if needed
            }
        })

        debugServer?.startServer()
    }

    private fun stopDebugServer() {
        debugServer?.stopServer()
        debugServer = null
        isServerRunning = false
    }

    private fun createNotificationChannel() {
        val name = "Debug Web Server"
        val descriptionText = "Debug web server for development"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification(serverUrl: String) {
        val stopIntent = Intent(this, DebugWebServerService::class.java).apply {
            action = "STOP_SERVER"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Debug Web Server Running")
            .setContentText(serverUrl)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    fun getServerUrl(): String {
        return debugServer?.getServerUrl() ?: ""
    }

    fun isServerRunning(): Boolean = isServerRunning
}