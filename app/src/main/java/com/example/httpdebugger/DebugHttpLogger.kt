package com.example.httpdebugger

object DebugHttpLogger {

    private val logBuffer = HttpLogBuffer(maxSize = 200)

    fun logRequestResponse(
        method: String,
        url: String,
        statusCode: Int,
        durationMs: Long,
        requestHeaders: String,
        requestBody: String,
        responseHeaders: String,
        responseBody: String
    ) {
        val entry = HttpLogEntry(
            method = method,
            url = url,
            statusCode = statusCode,
            durationMs = durationMs,
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            responseHeaders = responseHeaders,
            responseBody = responseBody
        )
        logBuffer.add(entry)
    }

    fun logError(
        method: String,
        url: String,
        headers: String,
        requestBody: String,
        error: String
    ) {
        logRequestResponse(
            method = method,
            url = url,
            statusCode = 0,
            durationMs = 0L,
            requestHeaders = headers,
            requestBody = requestBody,
            responseHeaders = "",
            responseBody = "ERROR: $error"
        )
    }

    fun getLogs(): List<HttpLogEntry> = logBuffer.getAll()

    fun clearLogs() = logBuffer.clear()

    fun filterByMethod(method: String): List<HttpLogEntry> =
        logBuffer.filterByMethod(method)

    fun search(keyword: String): List<HttpLogEntry> =
        logBuffer.search(keyword)
}