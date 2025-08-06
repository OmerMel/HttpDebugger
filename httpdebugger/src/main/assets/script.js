
let currentTheme = 'dark';
let requests = [];
let filteredRequests = [];

// DOM elements
const clearLogsBtn = document.getElementById('clearLogs');
const toggleThemeBtn = document.getElementById('toggleTheme');
const downloadLogsBtn = document.getElementById('downloadLogs');
const searchInput = document.getElementById('searchInput');
const methodFilter = document.getElementById('methodFilter');
const statusFilter = document.getElementById('statusFilter');
const timeFilter = document.getElementById('timeFilter');
const failedOnlyCheckbox = document.getElementById('failedOnly');
const emptyState = document.getElementById('emptyState');
const requestsList = document.getElementById('requestsList');

// Init
document.addEventListener('DOMContentLoaded', function () {
    fetchAndDisplayLogs();
    setupEventListeners();
});

function setupEventListeners() {
    toggleThemeBtn.addEventListener('click', toggleTheme);
    downloadLogsBtn.addEventListener('click', downloadLogs);
    clearLogsBtn.addEventListener('click', clearLogs);
    searchInput.addEventListener('input', applyFilters);
    methodFilter.addEventListener('change', applyFilters);
    statusFilter.addEventListener('change', applyFilters);
    timeFilter.addEventListener('change', applyFilters);
    failedOnlyCheckbox.addEventListener('change', applyFilters);
}

function toggleTheme() {
    currentTheme = currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', currentTheme);
}

function clearLogs() {
    fetch("/logs/clear")
        .then(() => fetchAndDisplayLogs())
        .catch(err => console.error("Failed to clear logs", err));
}

function downloadLogs() {
    const data = JSON.stringify(filteredRequests, null, 2);
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `http-logs-${new Date().toISOString().split('T')[0]}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

function applyFilters() {
    const searchTerm = searchInput.value.toLowerCase();
    const methodFilterValue = methodFilter.value;
    const statusFilterValue = statusFilter.value;
    const failedOnly = failedOnlyCheckbox.checked;

    filteredRequests = requests.filter(request => {
        if (searchTerm && !request.url.toLowerCase().includes(searchTerm) &&
            !request.method.toLowerCase().includes(searchTerm) &&
            !request.statusCode.toString().includes(searchTerm)) {
            return false;
        }

        if (methodFilterValue && request.method !== methodFilterValue) {
            return false;
        }

        if (statusFilterValue) {
            const statusClass = Math.floor(request.statusCode / 100);
            if (statusClass.toString() !== statusFilterValue) {
                return false;
            }
        }

        if (failedOnly && request.statusCode < 400) {
            return false;
        }

        return true;
    });

    updateRequestsList();
}

function fetchAndDisplayLogs() {
    fetch("/logs")
        .then(res => res.json())
        .then(data => {
            requests = data.map((entry, index) => {
                const parsedRequestHeaders = parseHeaders(entry.requestHeaders);
                const parsedResponseHeaders = parseHeaders(entry.responseHeaders);

                // 🟡 Fallback: if request headers are empty, show placeholder
                if (Object.keys(parsedRequestHeaders).length === 0) {
                    parsedRequestHeaders["(No headers)"] = "";
                }

                return {
                    id: index,
                    timestamp: new Date(entry.timestamp).toLocaleTimeString(),
                    method: entry.method,
                    statusCode: entry.statusCode,
                    url: entry.url,
                    duration: entry.durationMs,
                    requestHeaders: parsedRequestHeaders,
                    responseHeaders: parsedResponseHeaders,
                    requestBody: entry.requestBody || "",
                    responseBody: formatJson(entry.responseBody) || ""
                };
            }).reverse();

            filteredRequests = [...requests];
            updateRequestsList();
        })
        .catch(err => {
            console.error("Failed to fetch logs", err);
        });
}

function parseHeaders(headerString) {
    const headers = {};
    headerString.split("\n").forEach(line => {
        const [key, ...rest] = line.split(": ");
        if (key && rest.length > 0) {
            headers[key] = rest.join(": ");
        }
    });
    return headers;
}

function formatJson(text) {
    try {
        return JSON.stringify(JSON.parse(text), null, 2);
    } catch (e) {
        return text; // return original if not valid JSON
    }
}

function updateRequestsList() {
    if (filteredRequests.length === 0) {
        emptyState.style.display = 'block';
        requestsList.innerHTML = '';
        return;
    }

    emptyState.style.display = 'none';
    requestsList.innerHTML = filteredRequests.map(request => createRequestRow(request)).join('');
}

function createRequestRow(request) {
    const statusClass = Math.floor(request.statusCode / 100);
    return `
        <div class="request-row" data-id="${request.id}">
            <div class="request-summary" onclick="toggleRequestDetails(${request.id})">
                <div class="timestamp">${request.timestamp}</div>
                <div class="method-tag method-${request.method}" data-tooltip="${request.method} Request">${request.method}</div>
                <div class="status-code" data-tooltip="HTTP ${request.statusCode}">
                    <div class="status-dot status-${statusClass}xx"></div>
                    ${request.statusCode}
                </div>
                <div class="url" title="${request.url}">${request.url}</div>
                <div class="duration">${request.duration}ms</div>
                <div class="expand-icon">▶</div>
            </div>
            <div class="request-details">
                <div class="details-tabs">
                    <button class="tab active" onclick="switchTab(${request.id}, 'overview')">Overview</button>
                    <button class="tab" onclick="switchTab(${request.id}, 'headers')">Headers</button>
                    <button class="tab" onclick="switchTab(${request.id}, 'body')">Body</button>
                </div>

                <div class="tab-content active" id="overview-${request.id}">
                    <div class="detail-section">
                        <div class="section-title">Request Details</div>
                        <div class="code-block">
                            <strong>URL:</strong> ${request.url}<br>
                            <strong>Method:</strong> ${request.method}<br>
                            <strong>Status:</strong> ${request.statusCode}<br>
                            <strong>Duration:</strong> ${request.duration}ms<br>
                            <strong>Timestamp:</strong> ${request.timestamp}
                        </div>
                    </div>
                </div>

                <div class="tab-content" id="headers-${request.id}">
                    <div class="detail-section">
                        <div class="section-title">Request Headers</div>
                        <div class="headers-list">
                            ${Object.entries(request.requestHeaders).map(([key, value]) => `
                                <div class="header-row">
                                    <div class="header-name">${key}:</div>
                                    <div class="header-value">${value}</div>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                    <div class="detail-section">
                        <div class="section-title">Response Headers</div>
                        <div class="headers-list">
                            ${Object.entries(request.responseHeaders).map(([key, value]) => `
                                <div class="header-row">
                                    <div class="header-name">${key}:</div>
                                    <div class="header-value">${value}</div>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                </div>

                <div class="tab-content" id="body-${request.id}">
                    ${request.requestBody ? `
                        <div class="detail-section">
                            <div class="section-title">Request Body</div>
                            <div class="code-block"><pre>${formatJson(request.responseBody)}</pre></div>
                        </div>` : ''
                    }
                    <div class="detail-section">
                        <div class="section-title">Response Body</div>
                        <div class="code-block"><pre>${formatJson(request.responseBody)}</pre></div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function toggleRequestDetails(id) {
    const row = document.querySelector(`[data-id="${id}"]`);
    row.classList.toggle('expanded');
}

function switchTab(requestId, tabName) {
    const row = document.querySelector(`[data-id="${requestId}"]`);
    const tabs = row.querySelectorAll('.tab');
    const contents = row.querySelectorAll('.tab-content');

    tabs.forEach(tab => tab.classList.remove('active'));
    contents.forEach(content => content.classList.remove('active'));

    row.querySelector(`.tab[onclick*="${tabName}"]`).classList.add('active');
    row.querySelector(`#${tabName}-${requestId}`).classList.add('active');
}
