/**
 * @file ESP32_Sensor_Client.ino
 * @brief Hexive Edge Node — Multi-Sensor Telemetry Client for AWS IoT Core
 *
 * @details
 * This firmware runs on an ESP32 microcontroller and performs the following:
 *   - Reads environmental data from PIR, ultrasonic, MQ-series gas, and
 *     electret microphone sensors.
 *   - Classifies ambient noise into four states (QUIET, SPEECH, MEDIUM, LOUD)
 *     using an adaptive baseline algorithm.
 *   - Tracks room occupancy via PIR entry detection and ultrasonic exit detection.
 *   - Derives temperature and humidity from air-quality analog readings.
 *   - Publishes a JSON telemetry payload to AWS IoT Core every second via MQTT/TLS.
 *   - Subscribes to a control topic for remote LED brightness commands.
 *
 * @note Ensure Secrets.h is populated with valid AWS IoT certificates before flashing.
 *
 * @author  Hexive Team
 * @version 2.0
 */

#include "Secrets.h"
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include "WiFi.h"

/* ======================== MQTT TOPICS ======================== */
#define AWS_IOT_PUBLISH_TOPIC   "greysense/sensors"   /**< Outbound telemetry topic   */
#define AWS_IOT_SUBSCRIBE_TOPIC "greysense/control"    /**< Inbound control topic      */

/* ======================== PIN DEFINITIONS ======================== */
static const int MIC_PIN  = 34;  /**< Electret microphone analog input          */
static const int AIR_PIN  = 35;  /**< MQ-series gas sensor analog input         */
static const int PIR_PIN  = 27;  /**< PIR motion sensor digital input           */
static const int LED_PIN  =  4;  /**< PWM-controlled LED output                 */
static const int TRIG_PIN =  5;  /**< Ultrasonic sensor trigger output           */
static const int ECHO_PIN = 18;  /**< Ultrasonic sensor echo input               */

/* ======================== SOUND CLASSIFICATION THRESHOLDS ======================== */
static const int NORMAL_OFFSET    = 20;    /**< Amplitude offset for QUIET → SPEECH   */
static const int MEDIUM_OFFSET    = 50;    /**< Amplitude offset for SPEECH → MEDIUM  */
static const int LOUD_OFFSET      = 90;    /**< Amplitude offset for MEDIUM → LOUD    */
static const unsigned long LOUD_DURATION_MS = 1200; /**< Sustained ms before LOUD classification */

/* ======================== RUNTIME STATE ======================== */
bool           loudCandidate  = false;
unsigned long  loudStartTime  = 0;
int            noiseBaseline  = 0;
String         soundState     = "QUIET";

int            totalEntered   = 0;
int            totalLeft      = 0;
bool           pirLatch       = false;
unsigned long  lastMotionTime = 0;
float          temperature    = 25.0;
int            humidity       = 50;
bool           sonicLatch     = false;
unsigned long  exitBlockTime  = 0;
int            entryCooldown  = 0;
int            ledBrightness  = 0;

unsigned long  lastPublishTime = 0;
const unsigned long PUBLISH_INTERVAL_MS = 1000;

/* ======================== NETWORK OBJECTS ======================== */
WiFiClientSecure net;
PubSubClient     client(net);

/**
 * @brief Samples the microphone and returns the peak AC amplitude.
 *
 * Takes 200 rapid ADC samples, computes the DC offset, and returns the
 * maximum deviation from that offset. This provides a noise-floor-independent
 * measure of instantaneous sound amplitude.
 *
 * @return int  Peak amplitude above the computed DC offset.
 */
int getSoundAmplitude() {
  const int NUM_SAMPLES = 200;
  long sum = 0;
  int values[NUM_SAMPLES];

  for (int i = 0; i < NUM_SAMPLES; i++) {
    values[i] = analogRead(MIC_PIN);
    sum += values[i];
  }

  int dcOffset = sum / NUM_SAMPLES;
  int maxAmp   = 0;

  for (int i = 0; i < NUM_SAMPLES; i++) {
    int ac = abs(values[i] - dcOffset);
    if (ac > maxAmp) maxAmp = ac;
  }

  return maxAmp;
}

/**
 * @brief MQTT message callback for processing inbound control commands.
 *
 * Parses the incoming JSON payload and applies LED brightness control
 * if the "led" key is present. Expected format: {"led": <0-255>}.
 *
 * @param topic    The MQTT topic the message was received on.
 * @param payload  The raw message payload bytes.
 * @param length   The length of the payload in bytes.
 */
void messageHandler(char* topic, byte* payload, unsigned int length) {
  StaticJsonDocument<200> doc;
  deserializeJson(doc, payload, length);

  if (doc.containsKey("led")) {
    ledBrightness = constrain((int)doc["led"], 0, 255);
    ledcWrite(LED_PIN, ledBrightness);
  }
}

/**
 * @brief Establishes WiFi and MQTT connections to AWS IoT Core.
 *
 * Configures the TLS client with X.509 certificates from Secrets.h,
 * connects to the AWS IoT MQTT broker, and subscribes to the control topic.
 * Blocks until both WiFi and MQTT connections are established.
 */
void connectAWS() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }

  net.setCACert(AWS_CERT_CA);
  net.setCertificate(AWS_CERT_CRT);
  net.setPrivateKey(AWS_CERT_PRIVATE);

  client.setServer(AWS_IOT_ENDPOINT, 8883);
  client.setCallback(messageHandler);

  while (!client.connected()) {
    if (client.connect(THINGNAME)) {
      client.subscribe(AWS_IOT_SUBSCRIBE_TOPIC);
    } else {
      delay(5000);
    }
  }
}

/**
 * @brief Arduino setup — initialises peripherals and network connection.
 */
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

/**
 * @brief Main loop — reads sensors, classifies data, and publishes telemetry.
 *
 * Executes the following pipeline every 1 second:
 *   1. Measure ultrasonic distance (entry/exit detection)
 *   2. Sample and classify microphone audio
 *   3. Read air quality and PIR sensors
 *   4. Update occupancy counters via pulse-based entry/exit logic
 *   5. Derive temperature and humidity estimates
 *   6. Publish JSON telemetry to AWS IoT Core
 */
void loop() {
  if (!client.connected()) {
    connectAWS();
  }
  client.loop();

  if (millis() - lastPublishTime >= PUBLISH_INTERVAL_MS) {
    lastPublishTime = millis();

  // --- 1. Ultrasonic Distance Measurement ---
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  long   duration   = pulseIn(ECHO_PIN, HIGH);
  float  distanceCm = duration * 0.034f / 2.0f;
  if (distanceCm > 400 || distanceCm < 2) distanceCm = -1;

  // --- 2. Microphone Analysis & Noise Classification ---
  int amplitude = getSoundAmplitude();
  if (noiseBaseline == 0) {
    noiseBaseline = amplitude;
  } else {
    noiseBaseline = (noiseBaseline * 9 + amplitude) / 10;
  }

  if (amplitude < noiseBaseline + NORMAL_OFFSET)      soundState = "QUIET";
  else if (amplitude < noiseBaseline + MEDIUM_OFFSET)  soundState = "SPEECH";
  else if (amplitude < noiseBaseline + LOUD_OFFSET)    soundState = "MEDIUM";
  else {
    if (!loudCandidate) { loudCandidate = true; loudStartTime = millis(); }
    if (millis() - loudStartTime >= LOUD_DURATION_MS)  soundState = "LOUD";
  }

  int displayDb = map(amplitude, noiseBaseline, noiseBaseline + LOUD_OFFSET + 100, 35, 95);
  displayDb = constrain(displayDb, 35, 95);

  // --- 3. Auxiliary Sensor Readings ---
  int  airRaw = analogRead(AIR_PIN);
  bool pir    = digitalRead(PIR_PIN);

  // --- 4. Occupancy: Entry (PIR) / Exit (Ultrasonic) Logic ---
  if (pir && !pirLatch && millis() > exitBlockTime && millis() > entryCooldown) {
    totalEntered++;
    pirLatch      = true;
    entryCooldown = millis() + 2000;
  }
  if (!pir) pirLatch = false;

  if (distanceCm > 0 && distanceCm < 50) {
    if (!sonicLatch) {
      if ((totalEntered - totalLeft) > 0) {
        totalLeft++;
        exitBlockTime = millis() + 4000;
      }
      sonicLatch = true;
    }
  } else {
    sonicLatch = false;
  }

  // --- 5. Derived Environmental Estimates ---
  float targetTemp = 24.0f + (airRaw / 500.0f);
  temperature = (0.95f * temperature) + (0.05f * targetTemp);

  int targetHum = 40 + (airRaw / 30);
  humidity = (int)((0.95f * humidity) + (0.05f * targetHum));

  // --- 6. Publish Telemetry to AWS IoT Core ---
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
  }
}
