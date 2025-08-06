package com.omermel.httpdebugger

class HttpLogBuffer(private val maxSize: Int = 200) {

    private val buffer = ArrayDeque<HttpLogEntry>()
    private val lock = Any()

    fun add(entry: HttpLogEntry) {
        synchronized(lock) {
            if (buffer.size >= maxSize) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
        }
    }

    fun getAll(): List<HttpLogEntry> {
        return synchronized(lock) {
            buffer.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
        }
    }

    fun filterByMethod(method: String): List<HttpLogEntry> {
        return synchronized(lock) {
            buffer.filter { it.method.equals(method, ignoreCase = true) }
        }
    }

    fun search(keyword: String): List<HttpLogEntry> {
        return synchronized(lock) {
            buffer.filter {
                it.url.contains(keyword, ignoreCase = true) ||
                        it.requestBody.contains(keyword, ignoreCase = true) ||
                        it.responseBody.contains(keyword, ignoreCase = true)
            }
        }
    }
}
