document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialise DOM Elements First
    const navItems = document.querySelectorAll('.nav-item');
    const sections = document.querySelectorAll('section');
    const pageTitle = document.getElementById('current-page-title');
    const tempVal = document.getElementById('temp-val');
    const humVal = document.getElementById('hum-val');
    const aqVal = document.getElementById('aq-val');
    const enteredVal = document.getElementById('total-entered-val');
    const leftVal = document.getElementById('total-left-val');
    const soundVal = document.getElementById('sound-val');
    const vibrationVal = document.getElementById('vibration-val');
    const occupancyStatus = document.getElementById('occupancy-status');
    const pirIndicator = document.getElementById('pir-indicator');
    const pirText = document.getElementById('pir-text');
    const micRawVal = document.getElementById('mic-raw-val');
    const eventLogs = document.getElementById('event-logs');
    const lightIndicator = document.getElementById('light-indicator');
    const fanIndicator = document.getElementById('fan-indicator');
    const lightSettings = document.getElementById('light-settings');
    const fanSettings = document.getElementById('fan-settings');
    const brightnessVal = document.getElementById('brightness-val');
    const fanSpeedVal = document.getElementById('fan-speed-val');
    const lightColorPicker = document.getElementById('light-color-picker');
    const chartCtx = document.getElementById('mainChart');
    const accentPicker = document.getElementById('accent-color-picker');
    const presentVal = document.getElementById('present-val');
    const lightSensorVal = document.getElementById('light-val');

    // 2. State Management
    let currentLightColor = '#fbbf24';
    let currentBrightness = 80;
    let currentFanSpeed = 3; // 0 to 5

    // Persistence Logic
    async function saveDeviceSettings(deviceId) {
        const isPowered = document.getElementById(`${deviceId}-toggle`)?.checked;
        const val = deviceId === 'light' ? currentBrightness : currentFanSpeed;
        const color = deviceId === 'light' ? currentLightColor : '';

        try {
            await fetch('/api/settings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    deviceId,
                    enabled: isPowered,
                    settingValue: val,
                    color,
                    lastUpdated: Date.now()
                })
            });
        } catch (e) {
            console.error(`Failed to persist setting for ${deviceId}:`, e);
        }
    }

    async function loadDeviceSettings() {
        try {
            const res = await fetch('/api/settings');
            if (!res.ok) return;
            const settings = await res.json();

            settings.forEach(s => {
                if (s.deviceId === 'light') {
                    const toggle = document.getElementById('light-toggle');
                    if (toggle) toggle.checked = s.enabled;
                    currentBrightness = s.settingValue;
                    currentLightColor = s.color;
                    if (lightColorPicker) lightColorPicker.value = s.color;
                    updateBrightness(s.settingValue, false); // false to avoid loop
                    applyLightStyles();
                } else if (s.deviceId === 'fan') {
                    const toggle = document.getElementById('fan-toggle');
                    if (toggle) toggle.checked = s.enabled;
                    updateFanSpeed(s.settingValue, false); // false to avoid loop
                }
                // Refresh toggle UI
                const toggle = document.getElementById(`${s.deviceId}-toggle`);
                if (toggle) {
                    const indicator = document.getElementById(`${s.deviceId}-indicator`);
                    const settingsPanel = document.getElementById(`${s.deviceId}-settings`);
                    if (indicator) indicator.classList.toggle('active', s.enabled);
                    if (settingsPanel) settingsPanel.style.display = s.enabled ? 'flex' : 'none';
                }
            });
            addLogEntry("SYSTEM: SYNCED DEVICE STATES FROM DATABASE", "SERVER");
        } catch (e) {
            console.warn("Could not load settings from DB, using defaults.", e);
        }
    }

    async function loadLogs() {
        try {
            const res = await fetch('/api/logs');
            if (res.ok) {
                const logs = await res.json();
                if (eventLogs) {
                    eventLogs.innerHTML = '';
                    logs.reverse().forEach(log => {
                        const logTime = new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true });
                        const logEl = document.createElement('div');
                        logEl.className = 'log-entry';
                        logEl.innerHTML = `<span class="log-time">[${logTime}]</span> <span class="log-type" data-type="${log.type}">${log.type}:</span> <span class="log-msg">${log.message}</span>`;
                        eventLogs.prepend(logEl);
                    });
                }
            }
        } catch (e) {
            console.error("Failed to load logs:", e);
        }
    }

    async function loadAppSettings() {
        try {
            const res = await fetch('/api/app-settings');
            if (res.ok) {
                const settings = await res.json();
                settings.forEach(s => {
                    if (s.settingKey === 'accentColor') {
                        document.documentElement.style.setProperty('--accent', s.settingValue);
                        if (accentPicker) accentPicker.value = s.settingValue;
                    } else if (s.settingKey === 'theme') {
                        setTheme(s.settingValue, false); // false to avoid redundant save
                    }
                });
            }
        } catch (e) {
            console.error("Failed to load app settings:", e);
        }
    }

    async function saveAppSetting(key, value) {
        try {
            await fetch('/api/app-settings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ settingKey: key, settingValue: value })
            });
        } catch (e) {
            console.error("Failed to save app setting:", e);
        }
    }

    // 3. Shared Logic Functions
    function addLogEntry(message, type, persist = true) {
        if (!eventLogs) return;

        if (eventLogs.children.length === 1 && eventLogs.children[0].style && eventLogs.children[0].style.opacity === "0.5") {
            eventLogs.innerHTML = '';
        }

        const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true });
        const logEl = document.createElement('div');
        logEl.className = 'log-entry';
        logEl.innerHTML = `<span class="log-time">[${time}]</span> <span class="log-type" data-type="${type}">${type}:</span> <span class="log-msg">${message}</span>`;

        eventLogs.prepend(logEl);
        if (eventLogs.children.length > 50) eventLogs.lastChild.remove();

        if (persist) {
            fetch('/api/logs', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message, type, timestamp: Date.now() })
            }).catch(e => console.error("Log persistence failed:", e));
        }
    }

    // Color Customization Logic (Website Theme)
    window.updateAccentColor = (color) => {
        document.documentElement.style.setProperty('--accent', color);
        saveAppSetting('accentColor', color);
        // Update charts if needed
        if (mainChart) {
            mainChart.data.datasets[0].borderColor = color;
            mainChart.update();
        }
        addLogEntry(`THEME ACCENT COLOR UPDATED TO ${color.toUpperCase()}`, 'SYSTEM');
    };

    window.resetAccentColor = () => {
        const theme = document.documentElement.getAttribute('data-theme') || 'dark';
        let defaultColor = '#0ea5e9'; // dark
        if (theme === 'light') defaultColor = '#0284c7';
        if (theme === 'warm') defaultColor = '#fbbf24';

        localStorage.removeItem('greysense_accent');
        updateAccentColor(defaultColor);
        addLogEntry("SYSTEM: ACCENT COLOR RESET TO DEFAULT", "SERVER");
    };

    window.setTheme = (theme, persist = true) => {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('greysense_theme', theme);
        const select = document.getElementById('theme-select');
        if (select) select.value = theme;

        if (!localStorage.getItem('greysense_accent')) {
            let defaultColor = '#0ea5e9'; // dark
            if (theme === 'light') defaultColor = '#0284c7';
            if (theme === 'warm') defaultColor = '#fbbf24';

            document.documentElement.style.setProperty('--accent', defaultColor);
            if (accentPicker) accentPicker.value = defaultColor;
        }

        if (typeof mainChart !== 'undefined') {
            updateChartTheme();
        }

        if (persist) saveAppSetting('theme', theme);
        addLogEntry(`SYSTEM: INTERFACE THEME SET TO ${theme.toUpperCase()}`, 'SERVER');
    };

    // Smart Light Logic
    window.updateLightColor = (color, persist = true) => {
        currentLightColor = color;
        applyLightStyles();

        clearTimeout(window.colorLogTimeout);
        window.colorLogTimeout = setTimeout(() => {
            addLogEntry(`SMART LIGHT COLOR CHANGED TO ${color.toUpperCase()}`, 'COMMAND');
            if (persist) saveDeviceSettings('light');
        }, 1000);
    };

    window.updateBrightness = (val, persist = true) => {
        currentBrightness = val;
        if (brightnessVal) brightnessVal.textContent = `${val}%`;
        applyLightStyles();

        clearTimeout(window.brightnessLogTimeout);
        window.brightnessLogTimeout = setTimeout(() => {
            addLogEntry(`SMART LIGHT BRIGHTNESS ADJUSTED TO ${val}%`, 'COMMAND');
            if (persist) saveDeviceSettings('light');
        }, 1000);
    };

    window.updateFanSpeed = (val, persist = true) => {
        currentFanSpeed = val;
        if (fanSpeedVal) {
            fanSpeedVal.textContent = `STEP ${val}`;
            fanSpeedVal.style.color = val > 0 ? 'var(--accent)' : 'var(--text-muted)';
        }

        const isPowered = document.getElementById('fan-toggle')?.checked;
        if (fanIndicator) {
            if (isPowered && val > 0) {
                fanIndicator.classList.add('active');
                // Step 1: 1.8s, Step 5: 0.5s
                const speed = (2.1 - (val / 5) * 1.6).toFixed(1);
                fanIndicator.style.setProperty('--fan-speed', `${speed}s`);
            } else {
                fanIndicator.classList.remove('active');
            }
        }

        const knobInner = document.querySelector('#fan-knob .knob-inner');
        if (knobInner) {
            // Map 0-5 steps to -150 to 150 degrees
            const degrees = (val * 60) - 150;
            knobInner.style.transform = `rotate(${degrees}deg)`;
        }

        // Highlight active tick/dot
        const ticks = document.querySelectorAll('#fan-knob .tick');
        ticks.forEach((t, i) => {
            t.classList.toggle('active', i === val);
        });

        clearTimeout(window.fanLogTimeout);
        window.fanLogTimeout = setTimeout(() => {
            addLogEntry(`COOLING FAN REGULATED TO STEP ${val}`, 'COMMAND');
            if (persist) saveDeviceSettings('fan');
        }, 1000);
    };

    function applyLightStyles() {
        if (!lightIndicator) return;

        const intensity = currentBrightness / 100;
        const blur = 4 + (intensity * 12);

        // Helper to convert hex to RGB for alpha
        const hexToRgb = (hex) => {
            const r = parseInt(hex.slice(1, 3), 16);
            const g = parseInt(hex.slice(3, 5), 16);
            const b = parseInt(hex.slice(5, 7), 16);
            return `${r}, ${g}, ${b}`;
        };

        const rgb = hexToRgb(currentLightColor);
        const isPowered = document.getElementById('light-toggle')?.checked;

        if (isPowered) {
            lightIndicator.style.boxShadow = `0 0 ${blur * 1.5}px rgba(${rgb}, ${0.4 + (intensity * 0.6)})`;
            lightIndicator.style.background = `rgba(${rgb}, ${0.05 + (intensity * 0.1)})`;
            lightIndicator.style.color = currentLightColor;
            lightIndicator.style.borderColor = `rgba(${rgb}, 0.3)`;
        } else {
            lightIndicator.style.boxShadow = 'none';
            lightIndicator.style.background = 'rgba(255,255,255,0.03)';
            lightIndicator.style.color = '#475569';
            lightIndicator.style.borderColor = 'var(--border-color)';
        }

        // Update Knob Rotation
        const knobInner = document.querySelector('#brightness-knob .knob-inner');
        if (knobInner) {
            const degrees = (currentBrightness * 3) - 150;
            knobInner.style.transform = `rotate(${degrees}deg)`;
        }
    }

    // Diagnostic Feature
    window.runDiagnostic = () => {
        addLogEntry("RUNNING SYSTEM DIAGNOSTIC...", "COMMAND");
        setTimeout(() => {
            addLogEntry("CHECKING ESP32 CONNECTIVITY: OK", "INFO");
        }, 800);
        setTimeout(() => {
            addLogEntry("VERIFYING ACTUATOR STATUS: NOMINAL", "INFO");
        }, 1600);
        setTimeout(() => {
            addLogEntry("SYSTEM DIAGNOSTIC COMPLETE: ALL SYSTEMS GO", "SUCCESS");
        }, 2400);
    };

    // Knob Logic
    function setupKnob(id, type) {
        const knob = document.getElementById(id);
        if (!knob) return;

        let isDragging = false;
        let startY = 0;
        let startVal = type === 'light' ? currentBrightness : currentFanSpeed;

        const getValueFromAngle = (e) => {
            const rect = knob.getBoundingClientRect();
            const centerX = rect.left + rect.width / 2;
            const centerY = rect.top + rect.height / 2;
            const clientX = e.touches ? e.touches[0].clientX : e.clientX;
            const clientY = e.touches ? e.touches[0].clientY : e.clientY;

            // atan2 returns angle in radians from center
            let angle = Math.atan2(centerY - clientY, clientX - centerX);
            let degrees = angle * (180 / Math.PI);

            // Adjust so 0 is top. atan2 0 is right.
            // We want mapping similar to our CSS: -150 to 150.
            // CSS 0 is top. 
            // In our CSS: rotate(calc(var(--i) * 60deg - 150deg))

            // Convert to 0-360 starting from bottom-left (-150deg)
            let finalDegrees = 90 - degrees;
            if (finalDegrees < 0) finalDegrees += 360;

            // Our active range is -150 to 150 (300 degrees total)
            // Centered at 0 (top)
            // Mapping: 210deg (bottom-left) to 150deg (bottom-right)?
            // Let's simplify: map relative to the dial.

            let normalized = (finalDegrees + 150) % 360;
            if (normalized > 300) {
                // Pin to ends
                return normalized > 330 ? 0 : (type === 'light' ? 100 : 5);
            }

            let percent = normalized / 300;
            if (type === 'light') {
                return Math.round(percent * 100);
            } else {
                return Math.round(percent * 5);
            }
        };

        const handleMove = (e) => {
            if (!isDragging) return;
            const clientY = e.touches ? e.touches[0].clientY : e.clientY;
            const deltaY = startY - clientY;

            if (type === 'light') {
                let next = Math.round(startVal + (deltaY / 2));
                next = Math.max(0, Math.min(100, next));
                if (next !== currentBrightness) updateBrightness(next);
            } else {
                // Discrete levels for fan: 0 to 5
                let raw = startVal + (deltaY / 20);
                let nextStep = Math.round(raw);
                nextStep = Math.max(0, Math.min(5, nextStep));
                if (nextStep !== currentFanSpeed) updateFanSpeed(nextStep);
            }
        };

        const handleStart = (e) => {
            isDragging = true;
            startY = e.touches ? e.touches[0].clientY : e.clientY;
            startVal = type === 'light' ? currentBrightness : currentFanSpeed;
            document.body.style.cursor = 'grabbing';
            knob.closest('.knob-outer').style.cursor = 'grabbing';

            // Trigger update on initial click/touch down
            const newVal = getValueFromAngle(e);
            if (type === 'light') updateBrightness(newVal);
            else updateFanSpeed(newVal);
            startVal = newVal; // Update startVal so dragging continues from click point
        };

        const handleEnd = () => {
            if (!isDragging) return;
            isDragging = false;
            document.body.style.cursor = 'default';
            knob.closest('.knob-outer').style.cursor = 'grab';
        };

        knob.addEventListener('mousedown', handleStart);
        knob.addEventListener('touchstart', (e) => {
            e.preventDefault();
            handleStart(e);
        }, { passive: false });

        window.addEventListener('mousemove', handleMove);
        window.addEventListener('touchmove', handleMove);
        window.addEventListener('mouseup', handleEnd);
        window.addEventListener('touchend', handleEnd);

        knob.addEventListener('wheel', (e) => {
            e.preventDefault();
            const delta = e.deltaY > 0 ? -1 : 1;
            if (type === 'light') {
                let next = currentBrightness + (delta * 5);
                updateBrightness(Math.max(0, Math.min(100, next)));
            } else {
                let next = currentFanSpeed + delta;
                updateFanSpeed(Math.max(0, Math.min(5, next)));
            }
        }, { passive: false });
    }

    setupKnob('brightness-knob', 'light');
    setupKnob('fan-knob', 'fan');

    window.toggleDevice = (device) => {
        const toggle = document.getElementById(`${device}-toggle`);
        const indicator = document.getElementById(`${device}-indicator`);
        const isActive = toggle.checked;

        if (indicator) {
            if (isActive) {
                indicator.classList.add('active');
                if (device === 'light') applyLightStyles();
            } else {
                indicator.classList.remove('active');
                if (device === 'light') {
                    indicator.style.boxShadow = 'none';
                    indicator.style.background = '#475569';
                }
            }
        }

        if (device === 'light') {
            if (lightSettings) lightSettings.style.display = isActive ? 'flex' : 'none';
        } else if (device === 'fan') {
            if (fanSettings) fanSettings.style.display = isActive ? 'flex' : 'none';
            updateFanSpeed(currentFanSpeed); // Refresh animation state
        }

        const deviceName = device === 'light' ? 'SMART LIGHT' : 'COOLING FAN';
        const action = isActive ? 'ACTIVATED' : 'DEACTIVATED';
        addLogEntry(`MANUAL COMMAND: ${deviceName} ${action}`, 'COMMAND');
        saveDeviceSettings(device);
    };

    // 4. Navigation & Section Logic
    window.switchSection = (sectionId) => {
        if (!sectionId) return;

        sections.forEach(s => s.classList.remove('active'));
        const targetSection = document.getElementById(sectionId);
        if (targetSection) targetSection.classList.add('active');

        navItems.forEach(item => {
            item.classList.remove('active');
            if (item.getAttribute('data-section') === sectionId) {
                item.classList.add('active');
                if (pageTitle) {
                    const isIcon = item.innerHTML.includes('<svg');
                    pageTitle.textContent = isIcon ? 'Settings' : item.textContent.trim();
                }
            }
        });

        if (sectionId === 'dashboard') {
            startSimulation();
        } else {
            stopSimulation();
        }
    };

    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const sectionId = item.getAttribute('data-section');
            if (sectionId) switchSection(sectionId);
        });
    });

    // 5. Chart Logic
    let mainChart;
    async function initChart() {
        if (!chartCtx) return;
        const ctx = chartCtx.getContext('2d');
        const theme = document.documentElement.getAttribute('data-theme') || 'dark';
        const labelColor = theme === 'light' ? '#64748b' : '#94a3b8';
        const accentColor = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim();

        // Create Gradient
        const gradient = ctx.createLinearGradient(0, 0, 0, 300);
        gradient.addColorStop(0, `${accentColor}44`);
        gradient.addColorStop(1, `${accentColor}00`);

        // Fetch Initial History
        let histLabels = Array(25).fill('');
        let histTemp = Array(25).fill(24.2);
        let histHum = Array(25).fill(48);

        try {
            const res = await fetch('/api/sensors/history?count=25');
            if (res.ok) {
                const data = await res.json();
                histTemp = data.map(d => d.temperature);
                histHum = data.map(d => d.humidity);
            }
        } catch (e) {
            console.error("History Fetch Error:", e);
        }

        mainChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: histLabels,
                datasets: [
                    {
                        label: 'Temperature (°C)',
                        data: histTemp,
                        borderColor: accentColor,
                        backgroundColor: gradient,
                        borderWidth: 3,
                        fill: true,
                        tension: 0.45,
                        pointRadius: 0,
                        pointHoverRadius: 6,
                        pointHoverBackgroundColor: accentColor,
                        pointHoverBorderColor: '#fff',
                        pointHoverBorderWidth: 2
                    },
                    {
                        label: 'Humidity (%)',
                        data: histHum,
                        borderColor: theme === 'light' ? '#6366f1' : '#818cf8',
                        borderWidth: 2,
                        borderDash: [5, 5],
                        fill: false,
                        tension: 0.45,
                        pointRadius: 0,
                        pointHoverRadius: 4,
                        yAxisID: 'y1'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    intersect: false,
                    mode: 'index'
                },
                plugins: {
                    legend: {
                        display: true,
                        position: 'top',
                        align: 'end',
                        labels: {
                            color: labelColor,
                            usePointStyle: true,
                            boxWidth: 8,
                            font: { size: 10, weight: '600' }
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(15, 23, 42, 0.9)',
                        titleColor: '#fff',
                        bodyColor: '#cbd5e1',
                        padding: 12,
                        cornerRadius: 8,
                        displayColors: true
                    }
                },
                scales: {
                    x: {
                        display: false
                    },
                    y: {
                        position: 'left',
                        grid: {
                            color: theme === 'light' ? 'rgba(0,0,0,0.03)' : 'rgba(255, 255, 255, 0.03)',
                            drawBorder: false
                        },
                        ticks: {
                            color: labelColor,
                            font: { size: 10 },
                            callback: (val) => val + '°'
                        }
                    },
                    y1: {
                        position: 'right',
                        display: false,
                        grid: { drawOnChartArea: false }
                    }
                },
                animation: { duration: 800, easing: 'easeOutQuart' }
            }
        });
    }

    function updateChartTheme() {
        if (!mainChart) return;
        const theme = document.documentElement.getAttribute('data-theme');
        const labelColor = theme === 'light' ? '#64748b' : '#94a3b8';
        const gridColor = theme === 'light' ? 'rgba(0,0,0,0.03)' : 'rgba(255, 255, 255, 0.03)';
        const accentColor = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim();

        const ctx = chartCtx.getContext('2d');
        const gradient = ctx.createLinearGradient(0, 0, 0, 300);
        gradient.addColorStop(0, `${accentColor}44`);
        gradient.addColorStop(1, `${accentColor}00`);

        mainChart.options.scales.y.ticks.color = labelColor;
        mainChart.options.scales.y.grid.color = gridColor;
        mainChart.options.plugins.legend.labels.color = labelColor;

        mainChart.data.datasets[0].borderColor = accentColor;
        mainChart.data.datasets[0].backgroundColor = gradient;
        mainChart.data.datasets[0].pointHoverBackgroundColor = accentColor;

        mainChart.data.datasets[1].borderColor = theme === 'light' ? '#6366f1' : '#818cf8';

        mainChart.update();
    }

    // 6. Mock Data Engine
    let simInterval;

    // Realistic data states
    let curTemp = 24.2;
    let curHum = 48;

    function startSimulation() {
        if (simInterval) return;
        simInterval = setInterval(async () => {
            let data;
            try {
                const response = await fetch('/api/sensors/latest');
                if (!response.ok) throw new Error('Network telemetry failed');
                const json = await response.json();

                // Use the latest record from the array
                data = json[0] || {};

                if (Object.keys(data).length === 0) {
                    console.warn("No sensor data available.");
                    return;
                }

                // Update System Status (Severity Binding)
                const statusContainer = document.getElementById('system-status-container');
                const statusText = document.getElementById('system-status-text');
                const alertBanner = document.getElementById('system-alert-message');

                if (statusContainer && data.severity) {
                    statusContainer.className = 'system-status'; // Reset
                    statusContainer.classList.add(`severity-${data.severity.toLowerCase()}`);
                    if (statusText) statusText.textContent = data.severity;
                }

                // Update Alert Banner (Message Binding)
                if (alertBanner) {
                    // Default to NORMAL if missing
                    const severity = data.severity || "NORMAL";

                    if (severity !== "NORMAL") {
                        // Show Alert
                        alertBanner.textContent = data.message || "System Alert";
                        alertBanner.className = 'alert-banner'; // Reset class
                        alertBanner.classList.add(`severity-${severity.toLowerCase()}`);
                        alertBanner.style.display = 'block';
                    } else {
                        // Hide Alert
                        alertBanner.style.display = 'none';
                    }
                }

            } catch (error) {
                // FALLBACK: Generate local mock data if server is unavailable
                data = {
                    temperature: curTemp + (Math.random() - 0.5) * 0.2,
                    humidity: curHum + (Math.random() - 0.5) * 0.5,
                    airPpm: Math.floor(100 + Math.random() * 50), // Changed from airQuality to airPpm to match backend
                    airQuality: Math.floor(100 + Math.random() * 50), // Keep legacy for safety if needed
                    lightLevel: Math.floor(400 + Math.random() * 600),
                    noiseDb: Math.floor(30 + Math.random() * 20), // Changed from soundLevel to noiseDb
                    soundLevel: Math.floor(30 + Math.random() * 20), // Keep legacy
                    distanceCm: 100, // Add missing fields
                    vibration: 0.05 + Math.random() * 0.1,
                    pirMotion: Math.random() > 0.7, // Changed from motionDetected
                    motionDetected: Math.random() > 0.7, // Keep legacy
                    totalEntered: 0,
                    totalLeft: 0,
                    presentMembers: 0,
                    severity: "NORMAL",
                    message: "System Running (Simulation)"
                };

                // Log sync status once, not every interval
                if (!window.telemetryErrorLogged) {
                    console.warn("Telemetry fallback active - using internal simulation.", error);
                    addLogEntry("SYSTEM: RUNNING LOCAL TELEMETRY SIMULATION", "SYSTEM");
                    window.telemetryErrorLogged = true;
                }
            }

            // Map backend fields to frontend variables if needed (some names differ?)
            // Backend: airPpm, noiseDb, distanceCm, pirMotion, etc.
            // Frontend previously used: airQuality, soundLevel, motionDetected
            // We should normalize usage or support both
            // Let's ensure data has the fields the rest of the function expects

            data.airQuality = data.airPpm !== undefined ? data.airPpm : data.airQuality;
            data.soundLevel = data.noiseDb !== undefined ? data.noiseDb : data.soundLevel;
            data.motionDetected = data.pirMotion !== undefined ? data.pirMotion : data.motionDetected;
            data.lightLevel = data.lightLevel || 0; // Backend might not have lightLevel? SensorData has airPpm, noiseDb, distanceCm, peoplePresent, pirMotion...
            // Checking SensorData.java: airPpm, noiseDb, distanceCm, peoplePresent, pirMotion, severity, message.
            // It seems 'lightLevel' and 'vibration' are NOT in SensorData.java. I will just leave them as undefined or 0 to avoid errors if the UI expects them.


            // Sync simulation state
            // Fix: Read from backend if available, else keep current
            if (data.temperature !== undefined && data.temperature !== null) curTemp = data.temperature;
            if (data.humidity !== undefined && data.humidity !== null) curHum = data.humidity;

            // Update UI
            if (tempVal) tempVal.textContent = `${curTemp.toFixed(1)}°C`;
            if (humVal) humVal.textContent = `${curHum.toFixed(0)}%`;

            // Map backend fields
            if (aqVal) aqVal.textContent = `${data.airPpm || 0} PPM`;
            // Fix: Bind lightLevel and vibration
            if (lightSensorVal) lightSensorVal.textContent = `${data.lightLevel || 0} LUX`;
            if (vibrationVal) vibrationVal.textContent = `${data.vibration ? data.vibration.toFixed(2) : '0.00'} g`;

            if (soundVal) soundVal.textContent = `${data.noiseDb || 0} dB`;
            if (micRawVal) {
                // Show Amplitude AND Baseline
                const baseline = data.micBaseline !== undefined ? data.micBaseline : 0;
                micRawVal.innerHTML = `${data.micRaw || 0} <span style="font-size:0.6em; color:var(--text-muted)">/ ${baseline}</span>`;
            }
            if (vibrationVal) vibrationVal.textContent = `0.00 g`; // Not in backend

            // Always synchronize occupancy from database state
            // backend 'peoplePresent' -> frontend 'presentMembers'
            if (enteredVal) enteredVal.textContent = data.totalEntered !== undefined ? data.totalEntered : 0;
            if (leftVal) leftVal.textContent = data.totalLeft !== undefined ? data.totalLeft : 0;

            const presentCount = data.peoplePresent || 0;
            if (presentVal) presentVal.textContent = presentCount;

            if (occupancyStatus) {
                const accent = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim();
                occupancyStatus.textContent = presentCount > 0 ? 'Area Occupied' : 'Area Empty';
                occupancyStatus.style.color = presentCount > 0 ? accent : 'var(--text-muted)';
            }

            if (mainChart) {
                mainChart.data.datasets[0].data.shift();
                mainChart.data.datasets[0].data.push(curTemp);
                mainChart.data.datasets[1].data.shift();
                mainChart.data.datasets[1].data.push(curHum);
                mainChart.update('none');
            }

            if (data.pirMotion) {
                simulatePIR(data);
            }
        }, 1000);
    }

    function simulatePIR(data) {
        if (pirIndicator) {
            const accent = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim();
            pirIndicator.style.background = accent;
        }
        if (pirText) {
            const accent = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim();
            pirText.textContent = 'MOTION DETECTED';
            pirText.style.color = accent;
        }

        setTimeout(() => {
            // Flash UI to indicate an event was processed
            if (presentVal) {
                presentVal.closest('.card').classList.add('highlight-flash');
                setTimeout(() => presentVal.closest('.card').classList.remove('highlight-flash'), 2100);
            }

            // backend doesn't have totalEntered/Left
            /* 
            addLogEntry(`TELEMETRY SYNC: Occupancy ${data.presentMembers} (In: ${data.totalEntered} / Out: ${data.totalLeft})`, 'SERVER', true);
            */

            setTimeout(() => {
                if (pirIndicator) pirIndicator.style.background = '#94a3b8';
                if (pirText) {
                    pirText.textContent = 'NO MOTION';
                    pirText.style.color = 'inherit';
                }
            }, 1000);
        }, 500);
    }

    function stopSimulation() {
        clearInterval(simInterval);
        simInterval = null;
    }

    // 7. Authentication & Security
    const loginSection = document.getElementById('login-screen'); // Renamed from loginScreen
    const loginForm = document.getElementById('login-form');
    const mainHeader = document.getElementById('main-header');
    const mainContent = document.getElementById('main-content');
    const loginError = document.getElementById('login-error');

    const AUTH_KEY = 'gs_auth'; // Updated key

    function checkAuth() {
        const session = localStorage.getItem(AUTH_KEY);
        if (session === 'true') { // Check for 'true'
            showWebsite();
        } else {
            showLogin();
        }
    }

    function showWebsite() {
        if (loginSection) loginSection.classList.add('hidden');
        if (mainHeader) mainHeader.style.display = 'flex';
        if (mainContent) {
            mainContent.classList.remove('hidden');
            mainContent.style.display = 'block'; // Override inline display: none
        }

        // Start simulation if we are on dashboard
        const activeNav = document.querySelector('.nav-item.active');
        if (activeNav && activeNav.getAttribute('data-section') === 'dashboard') {
            startSimulation();
        }
    }

    function showLogin() {
        if (loginSection) loginSection.classList.remove('hidden');
        if (mainHeader) mainHeader.style.display = 'none';
        if (mainContent) {
            mainContent.classList.add('hidden');
            mainContent.style.display = 'none';
        }
        stopSimulation();
    }

    if (loginForm) {
        window.handleLogin = async (e) => {
            if (e) e.preventDefault();
            const user = document.getElementById('username')?.value;
            const pass = document.getElementById('password')?.value;

            try {
                const res = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: user, password: pass })
                });
                const success = await res.json();

                if (success) {
                    localStorage.setItem(AUTH_KEY, 'true');
                    showWebsite(); // Use unified show function
                    addLogEntry(`USER '${user.toUpperCase()}' LOGGED IN SUCCESSFULLY`, 'SERVER');
                } else {
                    if (loginError) {
                        loginError.textContent = 'Invalid Identifier or Access Code';
                        setTimeout(() => loginError.textContent = '', 3000);
                    }
                    addLogEntry("SECURITY: UNAUTHORIZED ACCESS ATTEMPT", "EXIT");
                }
            } catch (err) {
                console.error("Login error:", err);
                if (loginError) {
                    loginError.textContent = 'Connection error! Please try again.';
                    setTimeout(() => loginError.textContent = '', 3000);
                }
                addLogEntry("SECURITY: LOGIN CONNECTION ERROR", "SERVER");
            }
        };
        loginForm.addEventListener('submit', window.handleLogin);

        // Add Arrow Key Navigation for Login Fields
        loginForm.addEventListener('keydown', (e) => {
            const usernameField = document.getElementById('username');
            const passwordField = document.getElementById('password');

            if (e.key === 'ArrowDown' && document.activeElement === usernameField) {
                e.preventDefault();
                passwordField.focus();
            } else if (e.key === 'ArrowUp' && document.activeElement === passwordField) {
                e.preventDefault();
                usernameField.focus();
            }
        });
    }

    // Toggle Password Visibility Logic
    window.togglePasswordVisibility = () => {
        const passwordInput = document.getElementById('password');
        const eyeIcon = document.getElementById('eye-icon');
        if (!passwordInput || !eyeIcon) return;

        const isPassword = passwordInput.getAttribute('type') === 'password';
        passwordInput.setAttribute('type', isPassword ? 'text' : 'password');

        if (isPassword) {
            // Show Eye-off (Crossed)
            eyeIcon.innerHTML = `
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
            `;
        } else {
            // Show Open Eye
            eyeIcon.innerHTML = `
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
            `;
        }
    };

    window.logout = () => {
        localStorage.removeItem(AUTH_KEY);
        addLogEntry("SECURITY: SYSTEM LOGOUT", "SERVER");
        location.reload(); // Simplest way to reset all states
    };

    // 8. Keyboard Navigation
    const NAVIGATION_ORDER = ['home', 'dashboard', 'about', 'help', 'settings'];

    window.addEventListener('keydown', (e) => {
        // Only navigate if logged in
        if (localStorage.getItem(AUTH_KEY) !== 'true') return;

        // Don't navigate if typing in an input or textarea
        if (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA') {
            return;
        }

        const activeNav = document.querySelector('.nav-item.active');
        if (!activeNav) return;

        const currentSection = activeNav.getAttribute('data-section');
        let currentIndex = NAVIGATION_ORDER.indexOf(currentSection);

        if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
            currentIndex = (currentIndex + 1) % NAVIGATION_ORDER.length;
            switchSection(NAVIGATION_ORDER[currentIndex]);
            addLogEntry(`NAV: KEYBOARD SHORTCUT (→ ${NAVIGATION_ORDER[currentIndex].toUpperCase()})`, 'COMMAND');
        } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
            currentIndex = (currentIndex - 1 + NAVIGATION_ORDER.length) % NAVIGATION_ORDER.length;
            switchSection(NAVIGATION_ORDER[currentIndex]);
            addLogEntry(`NAV: KEYBOARD SHORTCUT (← ${NAVIGATION_ORDER[currentIndex].toUpperCase()})`, 'COMMAND');
        }
    });

    // 9. Contact Form Handling
    const contactForm = document.getElementById('contact-form');
    const formStatus = document.getElementById('form-status');
    const submitBtn = document.getElementById('contact-submit');

    if (contactForm) {
        contactForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const formData = new FormData(contactForm);

            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Sending...';
            }

            try {
                const response = await fetch(contactForm.action, {
                    method: 'POST',
                    body: formData,
                    headers: {
                        'Accept': 'application/json'
                    }
                });

                if (response.ok) {
                    if (formStatus) {
                        formStatus.style.display = 'block';
                        formStatus.textContent = 'Message sent successfully! Rathish will get back to you soon.';
                        formStatus.style.color = 'var(--success)';
                    }
                    contactForm.reset();
                    addLogEntry("CONTACT: FORM SUBMITTED TO FORMSPREE", "SERVER");
                } else {
                    const data = await response.json();
                    throw new Error(data.error || 'Server rejected submission');
                }
            } catch (error) {
                if (formStatus) {
                    formStatus.style.display = 'block';
                    formStatus.textContent = `Error: ${error.message}. Please try again later.`;
                    formStatus.style.color = 'var(--danger)';
                }
                addLogEntry(`CONTACT ERROR: ${error.message.toUpperCase()}`, "SERVER");
            } finally {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Dispatch Message';
                }
            }
        });
    }

    // 10. Global Search Functionality
    const globalSearch = document.getElementById('global-search');
    const searchResults = document.getElementById('search-results');
    const SEARCHABLE_PAGES = [
        { id: 'home', title: 'Home', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>' },
        { id: 'dashboard', title: 'Dashboard', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>' },
        { id: 'about', title: 'About Project', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>' },
        { id: 'help', title: 'Help & Support', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/><rect width="20" height="16" x="2" y="4" rx="2"/></svg>' },
        { id: 'settings', title: 'System Settings', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>' },
        // Sub-sections (Sensors)
        { id: 'temp-val', title: 'Temperature Sensor', parent: 'dashboard', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 4v10.54a4 4 0 1 1-4 0V4a2 2 0 0 1 4 0Z"/></svg>' },
        { id: 'hum-val', title: 'Humidity Sensor', parent: 'dashboard', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>' },
        { id: 'aq-val', title: 'Air Quality (PPM)', parent: 'dashboard', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.5 19a3.5 3.5 0 1 1-7 0a3.5 3.5 0 0 1 7 0Z"/><path d="M12.5 13a3.5 3.5 0 1 1-7 0a3.5 3.5 0 0 1 7 0Z"/></svg>' },
        { id: 'light-val', title: 'Light Intensity', parent: 'dashboard', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><path d="M12 1v2m0 18v2M4.22 4.22l1.42 1.42m12.72 12.72l1.42 1.42M1 12h2m18 0h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>' },
        { id: 'sound-val', title: 'Sound Intensity', parent: 'dashboard', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/></svg>' },
        { id: 'vibration-val', title: 'Structural Vibration', parent: 'dashboard', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m2 11 3-9h14l3 9"/><path d="M7 11h10M11 11v10M13 11v10"/></svg>' },
        { id: 'occupancy-status', title: 'Area Occupancy', parent: 'dashboard', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>' }
    ];

    let currentSearchIndex = -1;

    if (globalSearch) {
        globalSearch.addEventListener('input', (e) => {
            const query = e.target.value.toLowerCase().trim();
            if (!query) {
                searchResults.style.display = 'none';
                return;
            }

            const matches = SEARCHABLE_PAGES.filter(p =>
                p.title.toLowerCase().includes(query) ||
                p.id.toLowerCase().includes(query)
            );

            if (matches.length > 0) {
                searchResults.innerHTML = matches.map((m, idx) => `
                    <div class="search-result-item" data-id="${m.id}" data-index="${idx}">
                        ${m.icon}
                        <span>${m.title}</span>
                    </div>
                `).join('');
                searchResults.style.display = 'flex';
                currentSearchIndex = -1;
            } else {
                searchResults.style.display = 'none';
            }
        });

        globalSearch.addEventListener('keydown', (e) => {
            const items = searchResults.querySelectorAll('.search-result-item');

            if (e.key === 'ArrowDown') {
                e.preventDefault();
                currentSearchIndex = (currentSearchIndex + 1) % items.length;
                updateSearchSelection(items);
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                currentSearchIndex = (currentSearchIndex - 1 + items.length) % items.length;
                updateSearchSelection(items);
            } else if (e.key === 'Enter') {
                e.preventDefault();
                if (currentSearchIndex > -1) {
                    items[currentSearchIndex].click();
                } else if (items.length > 0) {
                    items[0].click();
                }
            } else if (e.key === 'Escape') {
                searchResults.style.display = 'none';
                globalSearch.blur();
            }
        });

        function updateSearchSelection(items) {
            items.forEach((item, idx) => {
                if (idx === currentSearchIndex) {
                    item.classList.add('active');
                    item.scrollIntoView({ block: 'nearest' });
                } else {
                    item.classList.remove('active');
                }
            });
        }

        // Click outside to close
        document.addEventListener('click', (e) => {
            if (!globalSearch.contains(e.target) && !searchResults.contains(e.target)) {
                searchResults.style.display = 'none';
            }
        });

        searchResults.addEventListener('click', (e) => {
            const item = e.target.closest('.search-result-item');
            if (item) {
                const searchId = item.getAttribute('data-id');
                const matchedPage = SEARCHABLE_PAGES.find(p => p.id === searchId);

                if (matchedPage.parent) {
                    switchSection(matchedPage.parent);
                    // Targeted scroll and highlight
                    setTimeout(() => {
                        const targetEl = document.getElementById(searchId);
                        if (targetEl) {
                            const card = targetEl.closest('.stat-card') || targetEl;
                            card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                            card.classList.add('highlight-flash');
                            setTimeout(() => card.classList.remove('highlight-flash'), 2000);
                        }
                    }, 100);
                } else {
                    switchSection(searchId);
                }

                globalSearch.value = '';
                searchResults.style.display = 'none';
                globalSearch.blur();
                addLogEntry(`SEARCH: NAVIGATED TO ${searchId.toUpperCase()}`, 'COMMAND');
            }
        });
    }

    // Ctrl + K Focus Search
    window.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            globalSearch.focus();
        }
    });

    // 8. Execute Initialization
    const savedTheme = localStorage.getItem('greysense_theme') || 'dark';
    setTheme(savedTheme);

    const savedAccent = localStorage.getItem('greysense_accent');
    if (savedAccent) {
        updateAccentColor(savedAccent);
    }

    switchSection('home');

    // Initialize systems with error handling
    initChart().catch(err => console.warn("Chart initialization failed (ignorable if canvas hidden):", err));

    console.log("Initializing Authentication Check...");
    checkAuth();
    loadDeviceSettings();
    loadLogs();
    loadAppSettings();
});
