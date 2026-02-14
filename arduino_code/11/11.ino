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

  /* ------------------- 1. READ SENSORS (PEAK-TO-PEAK) ------------------- */
  // Sampling Window to catch the audio waveform amplitude
  unsigned long startMillis = millis(); 
  unsigned int peakToPeak = 0;
  unsigned int signalMax = 0;
  unsigned int signalMin = 4095;

  // Sample for 50ms (detects lowest frequency ~20Hz)
  while (millis() - startMillis < 50) {
      int sample = analogRead(MIC_PIN);
      if (sample < 4095) { // toss out spurious readings
         if (sample > signalMax) signalMax = sample;
         if (sample < signalMin) signalMin = sample;
      }
  }
  peakToPeak = signalMax - signalMin;  // max - min = amplitude
  
  // Debug to Serial Monitor
  Serial.print("Mic Pk-Pk: "); 
  Serial.print(peakToPeak); 
  Serial.print("\t");

  // Map Amplitude (0-4095) to Decibels (35-95)
  // Logic: 
  // If sensor is disconnected/silent (constant 0 or 4095), Max=Min, Pk-Pk=0 -> 35dB.
  // If sensor is loud, Pk-Pk > 2000 -> 90+ dB.
  // We use a non-linear map or simple constraints.
  
  int displayDb = map(constrain(peakToPeak, 40, 3000), 40, 3000, 35, 95);
  
  // If below noise floor, force silence floor
  if (peakToPeak < 40) displayDb = 35; 

  Serial.print("Mapped dB: "); 
  Serial.println(displayDb);

  int airRaw = analogRead(AIR_PIN);     
  bool pir   = digitalRead(PIR_PIN);


  // FIX: Better Entry/Exit Logic
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

  // FIX: Make Temp/Humidity more dynamic based on Air Quality
  // Make it fluctuate a bit more visibly
  float targetTemp = 24.0 + (airRaw / 500.0); // 24 + (0 to 8)
  temperature = (0.95 * temperature) + (0.05 * targetTemp);
  
  int targetHum = 40 + (airRaw / 30); // 40 + (0 to 100)
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
  out["temperature"]   = temperature;  
  out["humidity"]      = humidity;     
  out["distanceCm"]    = -1;           
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

  // Serial.println(payload); // Optional: Print JSON
  // delay(1000); // Reduce delay slightly if sampling takes 50ms
  delay(500); 
}
