#include "Secrets.h"
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include "WiFi.h"

/* ================= WIFI SETTINGS ================= */
const char* WIFI_SSID = "Toji";
const char* WIFI_PASS = "22051977";
/* ================= TOPICS ================= */
#define AWS_IOT_PUBLISH_TOPIC   "greysense/sensors"
#define AWS_IOT_SUBSCRIBE_TOPIC "greysense/control"

/* ================= PIN DEFINITIONS ================= */
#define MIC_PIN   34
#define AIR_PIN   35
#define PIR_PIN   27
#define LED_PIN    4  
#define TRIG_PIN   33
#define ECHO_PIN   26

/* ================= SOUND SETTINGS ================= */
#define NORMAL_OFFSET   20
#define MEDIUM_OFFSET   50
#define LOUD_OFFSET     90
#define LOUD_DURATION_MS 1200 

bool loudCandidate = false;
unsigned long loudStartTime = 0;
int noiseBaseline = 0;
String soundState = "QUIET";

/* ================= GLOBAL VARIABLES ================= */
int totalEntered = 0;
int totalLeft = 0;   
bool pirLatch = false;
unsigned long lastMotionTime = 0;
float temperature = 25.0;
int humidity = 50;
bool sonicLatch = false;
unsigned long exitBlockTime = 0;
unsigned long entryCooldown = 0;

int ledBrightness = 0; // Controlled via MQTT

/* ================= OBJECTS ================= */
WiFiClientSecure net = WiFiClientSecure();
PubSubClient client(net);

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

/* ================= MQTT CALLBACK ================= */
void messageHandler(char* topic, byte* payload, unsigned int length) {
  Serial.print("incoming: ");
  Serial.println(topic);

  StaticJsonDocument<200> doc;
  deserializeJson(doc, payload);
  
  // Check for LED control message: {"led": 50}
  if (doc.containsKey("led")) {
      ledBrightness = doc["led"];
      ledBrightness = constrain(ledBrightness, 0, 255);
      ledcWrite(LED_PIN, ledBrightness);
      Serial.print("LED Updated to: ");
      Serial.println(ledBrightness);
  }
}

/* ================= CONNECT TO AWS ================= */
void connectAWS() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);

  Serial.println("Connecting to Wi-Fi");

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  // Configure Secure Client with Secrets from Secrets.h
  net.setCACert(AWS_CERT_CA);
  net.setCertificate(AWS_CERT_CRT);
  net.setPrivateKey(AWS_CERT_PRIVATE);

  client.setServer(AWS_IOT_ENDPOINT, 8883);
  client.setCallback(messageHandler);

  Serial.println("\nConnecting to AWS IOT");

  while (!client.connected()) {
    Serial.print(".");
    if (client.connect(THINGNAME)) {
      Serial.println("\nAWS IoT Connected!");
      client.subscribe(AWS_IOT_SUBSCRIBE_TOPIC);
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      delay(5000);
    }
  }
}

/* ================= SETUP ================= */
void setup() {
  Serial.begin(115200);
  
  pinMode(PIR_PIN, INPUT);
  pinMode(MIC_PIN, INPUT);
  analogSetAttenuation(ADC_6db); 
  
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  
  ledcAttach(LED_PIN, 5000, 8); 

  connectAWS();
}

/* ================= LOOP ================= */
void loop() {
  if (!client.connected()) {
    connectAWS();
  }
  client.loop();

  // 1. ULTRASONIC
  digitalWrite(TRIG_PIN, LOW); delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH); delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  long duration = pulseIn(ECHO_PIN, HIGH);
  float distanceCm = duration * 0.034 / 2;
  if (distanceCm > 400 || distanceCm < 2) distanceCm = -1;

  // 2. MIC ANALYSIS
  int amplitude = getSoundAmplitude();
  if (noiseBaseline == 0) noiseBaseline = amplitude;
  else noiseBaseline = (noiseBaseline * 9 + amplitude) / 10; 

  if (amplitude < noiseBaseline + NORMAL_OFFSET) soundState = "QUIET";
  else if (amplitude < noiseBaseline + MEDIUM_OFFSET) soundState = "SPEECH";
  else if (amplitude < noiseBaseline + LOUD_OFFSET) soundState = "MEDIUM";
  else {
    if (!loudCandidate) { loudCandidate = true; loudStartTime = millis(); }
    if (millis() - loudStartTime >= LOUD_DURATION_MS) soundState = "LOUD";
  }

  int displayDb = map(amplitude, noiseBaseline, noiseBaseline + LOUD_OFFSET + 100, 35, 95);
  displayDb = constrain(displayDb, 35, 95);

  // 3. OTHER SENSORS
  int airRaw = analogRead(AIR_PIN);     
  bool pir   = digitalRead(PIR_PIN);

  // 4. ENTRY / EXIT LOGIC (Pulse Based)
  if (pir && !pirLatch && millis() > exitBlockTime && millis() > entryCooldown) {
    totalEntered++;
    pirLatch = true; 
    entryCooldown = millis() + 2000; 
    Serial.println("EVENT: ENTRY DETECTED (PIR)");
  }
  if (!pir) pirLatch = false;

  if (distanceCm > 0 && distanceCm < 50) {
    if (!sonicLatch) {
        if ((totalEntered - totalLeft) > 0) {
            totalLeft++;
            exitBlockTime = millis() + 4000; 
            Serial.println("EVENT: EXIT DETECTED (ULTRASONIC)");
        }
        sonicLatch = true; 
    }
  } else {
    sonicLatch = false; 
  }

  // 5. TEMP SIMULATION
  float targetTemp = 24.0 + (airRaw / 500.0); 
  temperature = (0.95 * temperature) + (0.05 * targetTemp);
  int targetHum = 40 + (airRaw / 30); 
  humidity = (0.95 * humidity) + (0.05 * targetHum);

  // 6. PUBLISH TO AWS
  StaticJsonDocument<512> out;
  out["airPpm"]        = airRaw;
  out["noiseDb"]       = displayDb;     
  out["micRaw"]        = amplitude;     
  out["micBaseline"]   = noiseBaseline; 
  
  out["temperature"]   = temperature;  
  out["humidity"]      = humidity;     
  out["distanceCm"]    = distanceCm;    
  out["pirMotion"]     = pir;
  out["peoplePresent"] = (totalEntered - totalLeft); 
  out["totalEntered"]  = totalEntered;
  out["totalLeft"]     = totalLeft;    
  out["ledLevel"]      = ledBrightness; 
  out["message"]       = soundState;    

  char jsonBuffer[512];
  serializeJson(out, jsonBuffer);

  client.publish(AWS_IOT_PUBLISH_TOPIC, jsonBuffer);
  Serial.print("Published: ");
  Serial.println(jsonBuffer);

  delay(1000); 
}
