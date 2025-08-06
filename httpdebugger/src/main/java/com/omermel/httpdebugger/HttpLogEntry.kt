package com.omermel.httpdebugger

data class HttpLogEntry(
    val method: String,
    val url: String,
    val statusCode: Int,
    val durationMs: Long,
    val requestHeaders: String,
    val requestBody: String,
    val responseHeaders: String,
    val responseBody: String,
    val timestamp: Long = System.currentTimeMillis()
)
