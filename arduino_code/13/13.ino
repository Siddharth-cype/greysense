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
#define MIC_PIN   34
#define AIR_PIN   35
#define PIR_PIN   27
#define LED_PIN    4  

// ULTRASONIC PINS (Added as per request)
#define TRIG_PIN   5
#define ECHO_PIN   18

/* ================= GLOBAL VARIABLES ================= */
int totalEntered = 0;
int totalLeft = 0;   
bool pirLatch = false;
unsigned long lastMotionTime = 0;

// Variables for smoothing/deriving
float temperature = 25.0;
int humidity = 50;
float micSmooth = 0;

/* ================= SETUP ================= */
void setup() {
  Serial.begin(115200);
  pinMode(PIR_PIN, INPUT);
  
  // Ultrasonic Setup
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

  /* ------------------- 1. READ SENSORS (User Logic + Ultrasonic) ------------------- */
  
  // Ultrasonic Reading
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  long duration = pulseIn(ECHO_PIN, HIGH);
  float distanceCm = duration * 0.034 / 2;
  if (distanceCm > 400 || distanceCm < 2) distanceCm = -1;

  // Existing Logic (Preserved as requested)
  int micRaw = analogRead(MIC_PIN);     
  int airRaw = analogRead(AIR_PIN);     
  bool pir   = digitalRead(PIR_PIN);

  // User's Smoothing Logic
  int micActivity = abs(micRaw - 2000); 
  micSmooth = (0.9 * micSmooth) + (0.1 * micActivity);
  
  // Map Mic to 30-95 dB range 
  // FIX: If mic is raw 0 or 4095 (disconnected), micActivity is 2000 -> 95dB.
  // We add a trap here ONLY for the displayDb, but keep the raw data honest.
  int cleanMicSmooth = (int)micSmooth;
  
  // Anti-Artifact logic for dB display only:
  // If raw value is pinned to rails (0 or 4095), treat as silence for dB calculation
  if (micRaw < 50 || micRaw > 4045) {
      cleanMicSmooth = 0; // Silence
  }

  int displayDb = map(constrain(cleanMicSmooth, 0, 2000), 0, 2000, 35, 95);


  // FIX: Better Entry/Exit Logic (Preserved)
  if (pir && !pirLatch) {
    totalEntered++;
    pirLatch = true;
    lastMotionTime = millis();
  }
  
  // If no motion for 5 seconds, assume person left (Simulation logic)
  if (!pir && pirLatch && (millis() - lastMotionTime > 5000)) {
     totalLeft++;
     pirLatch = false;
  }

  // FIX: Make Temp/Humidity more dynamic based on Air Quality (Preserved)
  float targetTemp = 24.0 + (airRaw / 500.0); 
  temperature = (0.95 * temperature) + (0.05 * targetTemp);
  
  int targetHum = 40 + (airRaw / 30); 
  humidity = (0.95 * humidity) + (0.05 * targetHum);


  /* ------------------- 2. GET LED CONTROL ------------------- */
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

  // Apply Brightness
  ledBrightness = constrain(ledBrightness, 0, 255);
  ledcWrite(LED_PIN, ledBrightness);


  /* ------------------- 3. SEND SENSOR DATA ------------------- */
  StaticJsonDocument<512> out;
  
  out["airPpm"]        = airRaw;
  out["noiseDb"]       = displayDb;     
  out["micRaw"]        = micRaw;        // SENDING ACTUAL RAW DATA
  
  out["temperature"]   = temperature;  
  out["humidity"]      = humidity;     
  out["distanceCm"]    = distanceCm;    // SENDING ULTRASONIC DATA
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

  Serial.println(payload);
  delay(1000); 
}
