const apiBase = "/logs";

function createLogHTML(log) {
    return `
        <div class="log-entry">
            <div><strong>Method:</strong> ${log.method}</div>
            <div><strong>URL:</strong> ${log.url}</div>
            <div><strong>Status:</strong> ${log.statusCode}</div>
            <div><strong>Time:</strong> ${new Date(log.timestamp).toLocaleString()}</div>
            <div><strong>Duration:</strong> ${log.durationMs} ms</div>
            <div><strong>Req Body:</strong> <pre>${log.requestBody}</pre></div>
            <div><strong>Res Body:</strong> <pre>${log.responseBody}</pre></div>
        </div>
    `;
}

async function fetchLogs() {
    const method = document.getElementById("methodFilter").value.trim();
    const search = document.getElementById("searchInput").value.trim();
    let endpoint = apiBase;

    if (search !== "") {
        endpoint += `/search?q=${encodeURIComponent(search)}`;
    } else if (method !== "") {
        endpoint += `/method?m=${encodeURIComponent(method)}`;
    }

    console.log("📡 Fetching from:", endpoint);

    try {
        const res = await fetch(endpoint);
        if (!res.ok) throw new Error(`HTTP error ${res.status}`);
        const data = await res.json();
        console.log("📥 Logs received:", data.length);
        return data;
    } catch (err) {
        console.error("❌ Failed to fetch logs:", err);
        return [];
    }
}

async function refreshLogs() {
    console.log("🔄 Refreshing logs...");
    const logs = await fetchLogs();
    const container = document.getElementById("logContainer");
    container.innerHTML = logs.map(createLogHTML).join("");
}

async function clearLogs() {
    console.log("🧹 Clearing logs...");
    try {
        await fetch(`${apiBase}/clear`);
        refreshLogs();
    } catch (err) {
        console.error("❌ Failed to clear logs:", err);
    }
}

window.onload = () => {
    console.log("✅ script.js loaded");

    document.getElementById("searchInput").addEventListener("input", refreshLogs);
    document.getElementById("methodFilter").addEventListener("change", refreshLogs);

    document.querySelector("button[onclick='refreshLogs()']").addEventListener("click", refreshLogs);
    document.querySelector("button[onclick='clearLogs()']").addEventListener("click", clearLogs);

    refreshLogs();
};
