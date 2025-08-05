package com.example.httpdebugger

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.RequestBody
import okio.Buffer
import java.nio.charset.Charset

class HttpDebugInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val startNs = System.nanoTime()

        val requestBody = request.body
        val requestBodyString = requestBody?.let { bodyToString(it) } ?: ""

        request.headers.forEach {
            Log.d("HttpDebug", "🟡 REQUEST HEADER => ${it.first}: ${it.second}")
        }
        Log.d("HttpDebug", "🟡 header count: ${request.headers.size}")

        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            DebugHttpLogger.logError(
                method = request.method,
                url = request.url.toString(),
                headers = request.headers.toString(),
                requestBody = requestBodyString,
                error = e.message ?: "Unknown error"
            )
            throw e
        }

        val tookMs = (System.nanoTime() - startNs) / 1_000_000

        val responseBody = response.peekBody(Long.MAX_VALUE)
        val responseBodyString = responseBody.string()

        Log.d("HttpDebugInterceptor", "➡ Logging request: ${request.method} ${request.url}")
        DebugHttpLogger.logRequestResponse(
            method = request.method,
            url = request.url.toString(),
            statusCode = response.code,
            durationMs = tookMs,
            requestHeaders = request.headers.joinToString("\n") { "${it.first}: ${it.second}" },
            requestBody = requestBodyString,
            responseHeaders = response.headers.toString().replace("\r\n", "\n"),
            responseBody = responseBodyString
        )
        Log.d("HttpDebugInterceptor", "✅ Logged to DebugHttpLogger (${response.code})")

        return response
    }

    private fun bodyToString(requestBody: RequestBody): String {
        return try {
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            buffer.readString(Charset.forName("UTF-8"))
        } catch (e: Exception) {
            "Could not read request body"
        }
    }
}
