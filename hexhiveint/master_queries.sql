-- ==========================================
-- MASTER QUERY: VIEW ALL SYSTEM DETAILS
-- ==========================================

-- 1. SENSOR DATA (Temp, Humidity, Entry, Exit)
-- This shows all historical readings from sensors.
SELECT * FROM SENSOR_DATA ORDER BY TIMESTAMP DESC;

-- 2. EVENT LOGS (System Actions & Telemetry)
-- This shows everything the server has done or synced.
SELECT * FROM EVENT_LOG ORDER BY TIMESTAMP DESC;

-- 3. DEVICE SETTINGS (Light & Fan Status)
-- This shows the current ON/OFF and speed/brightness states.
SELECT * FROM DEVICE_SETTING;

-- 4. APP SETTINGS (Website Theme & Colors)
-- This shows your chosen UI preferences.
SELECT * FROM APP_SETTING;
