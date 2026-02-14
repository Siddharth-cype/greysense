#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

/* ================= WIFI SETTINGS ================= */
const char* WIFI_SSID = "Kadiresan";
const char* WIFI_PASS = "kadir@1971";

/* ================= BACKEND CONFIG ================= */
// REPLACE WITH YOUR PC'S CURRENT IP ADDRESS
const char* SERVER_IP = "192.168.31.69"; 

String sensorPostUrl = String("http://") + SERVER_IP + "/api/sensors";
String lightGetUrl   = String("http://") + SERVER_IP + "/api/device/light";

/* ================= PIN DEFINITIONS ================= */
#define MIC_PIN   12
#define AIR_PIN   35
#define PIR_PIN   27
#define LED_PIN    4  
// ULTRASONIC PINS (CHECK YOUR WIRING)
#define TRIG_PIN   33
#define ECHO_PIN   26

/* ================= GLOBAL VARIABLES ================= */
int totalEntered = 0;
int totalLeft = 0;   
bool pirLatch = false;
unsigned long lastMotionTime = 0;

// Variables for smoothing/deriving
float temperature = 25.0;
int humidity = 50;

/* ================= SETUP ================= */
void setup() {
  Serial.begin(115200);
  pinMode(PIR_PIN, INPUT);
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  
  // ESP32 PWM Setup
  ledcAttach(LED_PIN, 5000, 8); 

  // Connect to WiFi
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi Connected!");
}

/* ================= LOOP ================= */
void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    WiFi.reconnect();
    return;
  }

  /* ------------------- 1. READ ULTRASONIC (New) ------------------- */
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  
  long duration = pulseIn(ECHO_PIN, HIGH);
  float distanceCm = duration * 0.034 / 2;
  
  // Sanity check
  if (distanceCm > 400 || distanceCm < 2) distanceCm = -1;

  /* ------------------- 2. READ MIC (PEAK-TO-PEAK) ------------------- */
  unsigned long startMillis = millis(); 
  unsigned int peakToPeak = 0;
  unsigned int signalMax = 0;
  unsigned int signalMin = 4095;

  // Sample for 50ms
  while (millis() - startMillis < 50) {
      int sample = analogRead(MIC_PIN);
      if (sample < 4095) { 
         if (sample > signalMax) signalMax = sample;
         if (sample < signalMin) signalMin = sample;
      }
  }
  peakToPeak = signalMax - signalMin;  
  
  // Map Amplitude to dB (35-95)
  // If sensor disconnected (Pk-Pk near 0), force 35dB
  int displayDb = map(constrain(peakToPeak, 40, 3000), 40, 3000, 35, 95);
  if (peakToPeak < 40) displayDb = 35; 

  /* ------------------- 3. READ OTHER SENSORS ------------------- */
  int airRaw = analogRead(AIR_PIN);     
  bool pir   = digitalRead(PIR_PIN);

  // PIR Logic
  if (pir && !pirLatch) {
    totalEntered++;
    pirLatch = true;
    lastMotionTime = millis();
  }
  if (!pir && pirLatch && (millis() - lastMotionTime > 5000)) {
     totalLeft++;
     pirLatch = false;
  }

  // Simulation Logic for Temp/Hum
  float targetTemp = 24.0 + (airRaw / 500.0); 
  temperature = (0.95 * temperature) + (0.05 * targetTemp);
  int targetHum = 40 + (airRaw / 30); 
  humidity = (0.95 * humidity) + (0.05 * targetHum);

  /* ------------------- 4. GET LED CONTROL ------------------- */
  int ledBrightness = 0;
  HTTPClient httpGet;
  httpGet.begin(lightGetUrl.c_str());
  int httpCode = httpGet.GET();

  if (httpCode == 200) {
    String response = httpGet.getString();
    StaticJsonDocument<128> doc;
    deserializeJson(doc, response);
    ledBrightness = doc["brightness"];
  } 
  httpGet.end();

  ledBrightness = constrain(ledBrightness, 0, 255);
  ledcWrite(LED_PIN, ledBrightness);

  /* ------------------- 5. SEND DATA ------------------- */
  StaticJsonDocument<512> out;
  
  out["airPpm"]        = airRaw;
  out["noiseDb"]       = displayDb;     
  out["temperature"]   = temperature;  
  out["humidity"]      = humidity;     
  out["distanceCm"]    = distanceCm;      // Now sending real distance
  out["pirMotion"]     = pir;

  out["peoplePresent"] = (totalEntered - totalLeft); 
  out["totalEntered"]  = totalEntered;
  out["totalLeft"]     = totalLeft;    

  out["ledLevel"]      = ledBrightness; 

  String payload;
  serializeJson(out, payload);

  HTTPClient httpPost;
  httpPost.begin(sensorPostUrl.c_str());
  httpPost.addHeader("Content-Type", "application/json");
  int postCode = httpPost.POST(payload);
  httpPost.end();

  delay(500); 
}
