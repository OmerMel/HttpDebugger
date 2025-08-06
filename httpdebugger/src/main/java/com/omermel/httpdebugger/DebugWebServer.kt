package com.omermel.httpdebugger

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.NetworkInterface
import java.net.SocketException
import kotlin.text.get

class DebugWebServer private constructor(
    private val context: Context,
    private val port: Int,
    private val enableLogging: Boolean
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "DebugWebServer"
        private var instance: DebugWebServer? = null

        fun getInstance(): DebugWebServer? = instance

        fun create(context: Context): Builder {
            return Builder(context)
        }
    }

    class Builder(private val context: Context) {
        private var port: Int = 8080
        private var enableLogging: Boolean = true

        fun setPort(port: Int): Builder {
            this.port = port
            return this
        }

        fun enableLogging(enable: Boolean): Builder {
            this.enableLogging = enable
            return this
        }

        fun build(): DebugWebServer {
            return DebugWebServer(context, port, enableLogging)
        }
    }

    private var isRunning = false
    private var serverListener: ServerListener? = null
    private val gson = com.google.gson.Gson()

    interface ServerListener {
        fun onServerStarted(serverUrl: String)
        fun onServerStopped()
        fun onServerError(error: String)
        fun onRequest(uri: String, clientIp: String)
    }

    fun setServerListener(listener: ServerListener?) {
        this.serverListener = listener
    }

    fun startServer(): Boolean {
        return try {
            start()
            instance = this
            isRunning = true

            val ipAddress = getLocalIpAddress()
            val serverUrl = "http://$ipAddress:$port"

            serverListener?.onServerStarted(serverUrl)
            true
        } catch (e: IOException) {
            val errorMsg = "Failed to start server: ${e.message}"
            serverListener?.onServerError(errorMsg)
            false
        }
    }

    fun stopServer() {
        if (isRunning) {
            stop()
            isRunning = false
            instance = null

            serverListener?.onServerStopped()
        }
    }

    fun isRunning(): Boolean = isRunning

    fun getServerUrl(): String {
        return if (isRunning) {
            "http://${getLocalIpAddress()}:$port"
        } else {
            ""
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val clientIp = session.remoteIpAddress

        serverListener?.onRequest(uri, clientIp)

        // REST API handlers
        if (uri.startsWith("/logs")) {
            return handleLogApi(session)
        }

        return try {
            val target = if (uri == "/") "/index.html" else uri
            val inputStream = context.assets.open(target.substring(1))
            val content = inputStream.bufferedReader().use { it.readText() }
            val mimeType = getMimeType(target)

            val response = newFixedLengthResponse(Response.Status.OK, mimeType, content)
            addCorsHeaders(response)

            response

        } catch (e: IOException) {
            if (uri == "/index.html") {
                val debugHtml = createDebugIndexHtml()
                val response = newFixedLengthResponse(Response.Status.OK, "text/html", debugHtml)
                addCorsHeaders(response)
                return response
            }

            val errorHtml = createErrorHtml(uri)
            val response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", errorHtml)
            addCorsHeaders(response)
            response
        }
    }

    private fun handleLogApi(session: IHTTPSession): Response {
        val uri = session.uri
        val params = session.parameters

        val json = when {
            uri == "/logs/clear" -> {
                DebugHttpLogger.clearLogs()
                """{"status": "cleared"}"""
            }

            uri == "/logs/method" -> {
                val methodParam = params["m"]?.firstOrNull()
                if (methodParam != null) {
                    val filtered = DebugHttpLogger.filterByMethod(methodParam)
                    gson.toJson(filtered)
                } else {
                    """{"error": "Missing ?m=METHOD"}"""
                }
            }

            uri == "/logs/search" -> {
                val query = params["q"]?.firstOrNull()
                if (query != null) {
                    val results = DebugHttpLogger.search(query)
                    gson.toJson(results)
                } else {
                    """{"error": "Missing ?q=QUERY"}"""
                }
            }

            uri == "/logs" -> {
                val allLogs = DebugHttpLogger.getLogs()
                Log.d("DebugWebServer", "🔍 Returning ${DebugHttpLogger.getLogs().size} log entries")
                gson.toJson(allLogs)
            }

            else -> {
                """{"error": "Invalid log endpoint"}"""
            }
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", json).apply {
            addCorsHeaders(this)
        }
    }

    private fun addCorsHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type")
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress?.indexOf(':') == -1) {
                        return address.hostAddress ?: "unknown"
                    }
                }
            }
        } catch (e: SocketException) {
            if (enableLogging) {
                Log.e(TAG, "Error getting IP address", e)
            }
        }
        return "localhost"
    }


    private fun createDebugIndexHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Android Debug Server</title>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                        margin: 0; 
                        padding: 20px; 
                        background: #f5f5f5;
                        color: #333;
                    }
                    .container { 
                        max-width: 800px; 
                        margin: 0 auto; 
                        background: white; 
                        padding: 30px; 
                        border-radius: 10px; 
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    .header { 
                        color: #2196F3; 
                        border-bottom: 2px solid #2196F3; 
                        padding-bottom: 10px; 
                        margin-bottom: 30px;
                    }
                    .debug-info { 
                        background: #f8f9fa; 
                        padding: 20px; 
                        border-radius: 8px; 
                        border-left: 4px solid #2196F3;
                        margin: 20px 0;
                    }
                    .code { 
                        background: #2d2d2d; 
                        color: #f8f8f2; 
                        padding: 15px; 
                        border-radius: 5px; 
                        font-family: 'Courier New', monospace; 
                        overflow-x: auto;
                        margin: 10px 0;
                    }
                    .status { 
                        display: inline-block; 
                        background: #4CAF50; 
                        color: white; 
                        padding: 5px 10px; 
                        border-radius: 3px; 
                        font-size: 12px; 
                        font-weight: bold;
                    }
                    .footer { 
                        text-align: center; 
                        color: #666; 
                        margin-top: 30px; 
                        padding-top: 20px; 
                        border-top: 1px solid #eee;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1 class="header">
                        Android Debug Server
                        <span class="status">RUNNING</span>
                    </h1>
                    
                    <div class="debug-info">
                        <h2>📱 Server Information</h2>
                        <p>This debug server is running on your Android device and accessible from any browser on the same network.</p>
                        <p><strong>Server URL:</strong> ${getServerUrl()}</p>
                        <p><strong>Port:</strong> $port</p>
                        <p><strong>Time:</strong> <span id="currentTime"></span></p>
                    </div>
                    
                    <div class="debug-info">
                        <h2>📁 How to Use</h2>
                        <p>Place your HTML files in the <code>assets</code> folder of your Android project:</p>
                        <div class="code">app/src/main/assets/
├── index.html
├── styles.css
├── script.js
└── (other web files)</div>
                    </div>
                    
                    <div class="debug-info">
                        <h2>🔧 Integration Example</h2>
                        <div class="code">// In your Activity or Service
val debugServer = DebugWebServer.create(this)
    .setPort(8080)
    .enableLogging(true)
    .build()

debugServer.startServer()</div>
                    </div>
                    
                    <div class="footer">
                        <p>DebugWebServer Library - Perfect for debugging web applications during development</p>
                    </div>
                </div>
                
                <script>
                    function updateTime() {
                        document.getElementById('currentTime').textContent = new Date().toLocaleString();
                    }
                    updateTime();
                    setInterval(updateTime, 1000);
                    
                    console.log('✅ Debug server is working!');
                    console.log('🕐 Server time:', new Date().toISOString());
                    console.log('📡 Server URL: ${getServerUrl()}');
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun createErrorHtml(uri: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Debug Server - File Not Found</title>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                        margin: 0; 
                        padding: 20px; 
                        background: #f5f5f5;
                        color: #333;
                    }
                    .container { 
                        max-width: 600px; 
                        margin: 50px auto; 
                        background: white; 
                        padding: 30px; 
                        border-radius: 10px; 
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        text-align: center;
                    }
                    .error-code { 
                        font-size: 72px; 
                        font-weight: bold; 
                        color: #ff6b6b; 
                        margin: 0;
                    }
                    .error-message { 
                        font-size: 24px; 
                        margin: 20px 0; 
                        color: #666;
                    }
                    .file-path { 
                        background: #f8f9fa; 
                        padding: 15px; 
                        border-radius: 5px; 
                        font-family: monospace; 
                        color: #d73a49; 
                        margin: 20px 0;
                    }
                    .back-button { 
                        display: inline-block; 
                        background: #2196F3; 
                        color: white; 
                        padding: 10px 20px; 
                        text-decoration: none; 
                        border-radius: 5px; 
                        margin-top: 20px;
                    }
                    .back-button:hover { 
                        background: #1976D2; 
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1 class="error-code">404</h1>
                    <div class="error-message">File Not Found</div>
                    <div class="file-path">$uri</div>
                    <p>Make sure your files are in the <strong>assets</strong> folder of your Android project.</p>
                    <a href="/" class="back-button">← Back to Debug Index</a>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun getMimeType(uri: String): String {
        return when {
            uri.endsWith(".html") -> "text/html"
            uri.endsWith(".css") -> "text/css"
            uri.endsWith(".js") -> "application/javascript"
            uri.endsWith(".json") -> "application/json"
            uri.endsWith(".png") -> "image/png"
            uri.endsWith(".jpg") || uri.endsWith(".jpeg") -> "image/jpeg"
            uri.endsWith(".gif") -> "image/gif"
            uri.endsWith(".svg") -> "image/svg+xml"
            uri.endsWith(".ico") -> "image/x-icon"
            uri.endsWith(".woff") -> "font/woff"
            uri.endsWith(".woff2") -> "font/woff2"
            uri.endsWith(".ttf") -> "font/ttf"
            uri.endsWith(".otf") -> "font/otf"
            uri.endsWith(".xml") -> "application/xml"
            uri.endsWith(".txt") -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}