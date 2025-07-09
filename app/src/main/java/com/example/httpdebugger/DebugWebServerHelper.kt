package com.example.httpdebugger

import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Helper class for easy integration of DebugWebServer
 */
object DebugWebServerHelper {

    /**
     * Quick start method for debugging
     * Shows a toast with the server URL
     */
    fun quickStart(context: Context, port: Int = 8080): DebugWebServer {
        val server = DebugWebServer.create(context)
            .setPort(port)
            .enableLogging(true)
            .build()

        server.setServerListener(object : DebugWebServer.ServerListener {
            override fun onServerStarted(serverUrl: String) {
                Toast.makeText(context, "Debug server: $serverUrl", Toast.LENGTH_LONG).show()
            }

            override fun onServerStopped() {
                Toast.makeText(context, "Debug server stopped", Toast.LENGTH_SHORT).show()
            }

            override fun onServerError(error: String) {
                Toast.makeText(context, "Server error: $error", Toast.LENGTH_LONG).show()
            }

            override fun onRequest(uri: String, clientIp: String) {
                // Silent by default
            }
        })

        server.startServer()
        return server
    }

    /**
     * Start server as a background service
     */
    fun startAsService(context: Context, port: Int = 8080) {
        DebugWebServerService.start(context, port)
    }

    /**
     * Stop server service
     */
    fun stopService(context: Context) {
        DebugWebServerService.stop(context)
    }

    /**
     * Get currently running server instance
     */
    fun getCurrentServer(): DebugWebServer? {
        return DebugWebServer.getInstance()
    }

    /**
     * Check if server is running
     */
    fun isServerRunning(): Boolean {
        return DebugWebServer.getInstance()?.isRunning() == true
    }

    /**
     * Get server URL if running
     */
    fun getServerUrl(): String {
        return DebugWebServer.getInstance()?.getServerUrl() ?: ""
    }
}