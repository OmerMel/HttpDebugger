
# SimpleHTTP Debugger

A lightweight Android debugging library for capturing and inspecting HTTP traffic from your app — complete with an in-app web UI, real-time log viewer, and filtering capabilities. Perfect for developers using Retrofit, OkHttp, or any HTTP client.

![screenshot](https://your-image-url-if-applicable) <!-- Optional GIF or Screenshot -->


---

## 🚀 Installation

Add JitPack to your project-level `build.gradle`:

```groovy
dependencyResolutionManagement {
        maven { url = uri("https://jitpack.io") }
}
```

Then add the library dependency:

```groovy
dependencies {
    implementation("com.github.OmerMel:HttpDebugger:-SNAPSHOT")
}
```

---

## 🧰 Usage

### 1. Initialize the Debug Web Server

```kotlin
val server = DebugWebServerHelper.quickStart(context, port = 8080)
```

You can also start manually:

```kotlin
val server = DebugWebServer.create(context)
    .setPort(8080)
    .enableLogging(true)
    .build()

server.startServer()
```

### 2. Add Interceptor to OkHttp

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(HttpDebugInterceptor())
    .build()
```

Now every HTTP request made by this client is logged.

---

## 🧪 Example

```kotlin
val request = Request.Builder()
    .url("https://jsonplaceholder.typicode.com/posts/1")
    .get()
    .build()

client.newCall(request).enqueue(...)
```

You can then access the debugger by opening the device’s IP in a browser:
```
http://<device-ip>:<port:8080>
```

---

## 🎯 Features

- Capture all HTTP methods (GET, POST, PUT, DELETE, PATCH)
- Inspect request & response headers and bodies
- Monitor duration and status code
- View logs in real-time via web interface
- Search, filter, export, and clear logs
- Toggle dark/light themes
- Automatically start server with one line

---

## ⚙️ Configuration Options

| Option              | Description                      |
|---------------------|----------------------------------|
| `setPort(Int)`      | Change server port               |
| `quickStart(context, port)` | One-liner setup           |

---

## 🛠️ How It Works

The library works by combining three main components:

### 1. **OkHttp Interceptor**
- `HttpDebugInterceptor` captures every HTTP request and response.
- It logs:
  - Method, URL, status code, duration
  - Headers and body (both request and response)
- Errors (like timeouts or connection failures) are also logged.

### 2. **In-Memory Log Buffer**
- Captured logs are stored in `HttpLogBuffer`, an in-memory circular buffer.
- This buffer ensures minimal memory usage and fast access.
- It supports filtering and keyword searching in real-time.

### 3. **Embedded Web Server**
- A lightweight `NanoHTTPD`-based server (`DebugWebServer`) is started on the device.
- It serves:
  - Static UI files (`index.html`, `styles.css`, `script.js`)
  - REST API endpoints (`/logs`, `/logs/search`, `/logs/method`, etc.)
- This allows developers to access the debug UI from a browser on the same network.

### Web Interface
- Provides a responsive UI to view captured HTTP traffic.
- Supports dark/light theme, search, filtering, and detailed inspection.
- All interaction happens over the local device network (no internet required).

This design ensures the library is non-intrusive, developer-friendly, and can be safely used in debug builds without affecting app performance or behavior.


---

## 📱 Requirements

- **Minimum SDK:** Android 5.0 (API 21+)
- **Dependencies:**
  - OkHttp
  - NanoHTTPD

---

## 🆕 Changelog

### v1.0

- Initial release
- Full HTTP logging
- Embedded web server with UI
- Live search/filter/export

---

## ⚠️ Known Issues

- Only supports OkHttp-based requests
- May not capture requests from other libraries unless manually intercepted
- UI hosted locally; does not support HTTPS internally
---

## 🙏 Acknowledgements

- [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)
- [OkHttp](https://square.github.io/okhttp/)
- Inspired by tools like Wireshark, but built natively for Android debugging.
