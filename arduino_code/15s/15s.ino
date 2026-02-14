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
#define TRIG_PIN   5
#define ECHO_PIN   18

/* ================= SOUND SETTINGS (USER PROVIDED) ================= */
// ---- THRESHOLD OFFSETS (RELATIVE TO BASELINE) ----
#define NORMAL_OFFSET   20
#define MEDIUM_OFFSET   50
#define LOUD_OFFSET     90

// ---- DURATION CONTROL ----
#define LOUD_DURATION_MS 1200   // must be loud for 1.2 sec

bool loudCandidate = false;
unsigned long loudStartTime = 0;
int noiseBaseline = 0;
String soundState = "QUIET";


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
  
  // Mic Setup (User Config)
  pinMode(MIC_PIN, INPUT);
  // analogReadResolution(12); // Default
  analogSetAttenuation(ADC_6db); // User requested line
  
  // Ultrasonic
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  
  // LED
  ledcAttach(LED_PIN, 5000, 8); 

  // Wifi
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi Connected!");
  Serial.println("GreySense v2 - Adaptive Audio + Ultrasonic");
}

/* ================= USER SOUND FUNCTION ================= */
int getSoundAmplitude() {
  const int samples = 200;
  long sum = 0;
  int values[samples];

  for (int i = 0; i < samples; i++) {
    values[i] = analogRead(MIC_PIN);
    sum += values[i];
  }

  int dcOffset = sum / samples;
  int maxAmp = 0;

  for (int i = 0; i < samples; i++) {
    int ac = abs(values[i] - dcOffset);
    if (ac > maxAmp) maxAmp = ac;
  }
  return maxAmp;
}


/* ================= LOOP ================= */
void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    WiFi.reconnect();
    return;
  }

  // 1. ULTRASONIC
  digitalWrite(TRIG_PIN, LOW); delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH); delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  long duration = pulseIn(ECHO_PIN, HIGH);
  float distanceCm = duration * 0.034 / 2;
  if (distanceCm > 400 || distanceCm < 2) distanceCm = -1;

  // 2. ADAPTIVE SOUND (USER LOGIC)
  int amplitude = getSoundAmplitude();
  
  // Adaptive baseline
  if (noiseBaseline == 0) {
    noiseBaseline = amplitude;
  } else {
    // Slower adaptation to background noise
    noiseBaseline = (noiseBaseline * 9 + amplitude) / 10; 
  }

  // Classification
  if (amplitude < noiseBaseline + NORMAL_OFFSET) {
    soundState = "QUIET";
    loudCandidate = false;
  }
  else if (amplitude < noiseBaseline + MEDIUM_OFFSET) {
    soundState = "SPEECH";
    loudCandidate = false;
  }
  else if (amplitude < noiseBaseline + LOUD_OFFSET) {
    soundState = "MEDIUM";
    loudCandidate = false;
  }
  else {
    // Potential loud sound
    if (!loudCandidate) {
      loudCandidate = true;
      loudStartTime = millis();
    }
    if (millis() - loudStartTime >= LOUD_DURATION_MS) {
      soundState = "LOUD";
    }
  }

  // Map to dB for Backend Display (Visual Approximation)
  int displayDb = map(amplitude, noiseBaseline, noiseBaseline + LOUD_OFFSET + 100, 35, 95);
  displayDb = constrain(displayDb, 35, 95);


  // 3. OTHER SENSORS
  int airRaw = analogRead(AIR_PIN);     
  bool pir   = digitalRead(PIR_PIN);

  // 4. PIR LOGIC (Global Counters)
  if (pir && !pirLatch) {
    totalEntered++;
    pirLatch = true;
    lastMotionTime = millis();
  }
  if (!pir && pirLatch && (millis() - lastMotionTime > 5000)) {
     totalLeft++;
     pirLatch = false;
  }

  // 5. TEMP/HUMIDITY SIMULATION
  float targetTemp = 24.0 + (airRaw / 500.0); 
  temperature = (0.95 * temperature) + (0.05 * targetTemp);
  int targetHum = 40 + (airRaw / 30); 
  humidity = (0.95 * humidity) + (0.05 * targetHum);

  // 6. LED CONTROL
  int ledBrightness = 0;
  HTTPClient httpGet;
  httpGet.begin(lightGetUrl.c_str());
  if (httpGet.GET() == 200) {
    StaticJsonDocument<128> doc;
    deserializeJson(doc, httpGet.getString());
    ledBrightness = doc["brightness"];
  } 
  httpGet.end();
  ledcWrite(LED_PIN, map(ledBrightness, 0, 100, 0, 255)); 

  // 7. SEND PAYLOAD
  StaticJsonDocument<512> out;
  out["airPpm"]        = airRaw;
  out["noiseDb"]       = displayDb;     
  out["micRaw"]        = amplitude;     // User can see 'Amplitude' in logs
  out["micBaseline"]   = noiseBaseline; // SENDING BASELINE (Adjusting Value)
  
  out["temperature"]   = temperature;  
  out["humidity"]      = humidity;     
  out["distanceCm"]    = distanceCm;    
  out["pirMotion"]     = pir;
  out["peoplePresent"] = (totalEntered - totalLeft); 
  out["totalEntered"]  = totalEntered;
  out["totalLeft"]     = totalLeft;    
  out["ledLevel"]      = ledBrightness; 
  out["message"]       = soundState;    

  String payload;
  serializeJson(out, payload);

  HTTPClient httpPost;
  httpPost.begin(sensorPostUrl.c_str());
  httpPost.addHeader("Content-Type", "application/json");
  httpPost.POST(payload);
  httpPost.end();

  Serial.println(payload);
  delay(500); 
}
