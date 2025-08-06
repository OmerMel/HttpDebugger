package com.omermel.httpdebugger

import android.content.Context
import android.util.Log

/**
 * Helper class for easy integration of DebugWebServer
 */
object DebugWebServerHelper {

    /**
     * Quick start method for debugging
     * Shows a toast with the server URL
     */
    fun quickStart(context: Context, port: Int = 8080): DebugWebServer {
        val server = DebugWebServer.Companion.create(context)
            .setPort(port)
            .enableLogging(true)
            .build()

        server.setServerListener(object : DebugWebServer.ServerListener {
            override fun onServerStarted(serverUrl: String) {
                Log.d("DebugWebServer", "Server started at: $serverUrl")
            }

            override fun onServerStopped() {
                Log.d("DebugWebServer", "Server stopped")
            }

            override fun onServerError(error: String) {
                Log.e("DebugWebServer", "Server error: $error")
            }

            override fun onRequest(uri: String, clientIp: String) {
                // Silent by default
            }
        })

        server.startServer()
        return server
    }

    /**
     * Get currently running server instance
     */
    fun getCurrentServer(): DebugWebServer? {
        return DebugWebServer.Companion.getInstance()
    }

    /**
     * Check if server is running
     */
    fun isServerRunning(): Boolean {
        return DebugWebServer.Companion.getInstance()?.isRunning() == true
    }

    /**
     * Get server URL if running
     */
    fun getServerUrl(): String {
        return DebugWebServer.Companion.getInstance()?.getServerUrl() ?: ""
    }
}