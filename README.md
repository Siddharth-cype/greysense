<div align="center">

<img src="docs/hexive_banner.png" alt="Hexive Banner" width="100%"/>

# ⬡ Hexive: Neural Infrastructure for Industrial IoT

**A high-concurrency monitoring ecosystem bridging edge-node sensing with enterprise-grade data orchestration.**

Hexive leverages a distributed architecture, utilizing ESP32-based hardware nodes for real-time environmental telemetry and a robust Java Spring Boot backbone for scalable data processing. Designed with a cyber-industrial aesthetic, it prioritizes low-latency communication and modular hardware integration.

[![Java](https://img.shields.io/badge/Backend-Java%2017%20%7C%20Spring%20Boot-6DB33F?style=flat-square&logo=spring)](src/backend)
[![ESP32](https://img.shields.io/badge/Hardware-ESP32%20%7C%20C++-00979D?style=flat-square&logo=arduino)](src/hardware)
[![AWS IoT](https://img.shields.io/badge/Cloud-AWS%20IoT%20Core-FF9900?style=flat-square&logo=amazonaws)](docs/ARCHITECTURE.md)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](#license)

</div>

---

## 📐 Technical Architecture

Hexive implements a **three-tier edge-to-cloud architecture** connecting hardware sensing, cloud messaging, and enterprise data processing:

```
┌─────────────────────┐          ┌──────────────────────┐          ┌──────────────────────┐
│   HARDWARE LAYER    │          │    TRANSPORT LAYER    │          │   APPLICATION LAYER  │
│                     │   MQTT   │                      │   JPA    │                      │
│  ESP32 Edge Node    │ ──────── │  AWS IoT Core        │ ──────── │  Spring Boot Backend │
│  • PIR Sensor       │  TLS 1.2 │  • Message Broker    │  H2/SQL  │  • Decision Engine   │
│  • Ultrasonic       │          │  • X.509 Auth        │          │  • REST API Layer    │
│  • MQ Gas Sensor    │          │  • Topic Routing     │          │  • Event Logging     │
│  • Electret Mic     │          │                      │          │  • Dashboard UI      │
│  • LED Actuator     │          └──────────────────────┘          │                      │
└─────────────────────┘                                           └──────────────────────┘
```

### Hardware-to-Software Integration (ECE Focus)

| Component | Interface | Signal Type | Processing |
|---|---|---|---|
| HC-SR501 PIR Sensor | GPIO 27 (Digital) | HIGH/LOW pulse | Entry detection with 2s cooldown latch |
| HC-SR04 Ultrasonic | GPIO 5/18 (Trigger/Echo) | Pulse timing → cm | Exit detection with 4s blocking window |
| MQ-135 Gas Sensor | GPIO 35 (ADC) | 0–4095 analog | PPM mapping + derived temp/humidity |
| Electret Microphone | GPIO 34 (ADC) | 0–4095 analog | 200-sample DC-offset → adaptive dB classification |
| PWM LED Actuator | GPIO 4 (PWM) | 0–255 duty cycle | Remote brightness control via MQTT subscription |

> For detailed architectural decisions, see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## 🗂️ Repository Structure

```
hexive/
├── src/
│   ├── backend/                 # Java Spring Boot application
│   │   ├── src/main/java/       # Source code (controllers, models, services)
│   │   ├── src/main/resources/  # Configuration & static dashboard
│   │   └── pom.xml              # Maven build configuration
│   └── hardware/                # ESP32 Arduino firmware
│       └── ESP32_Sensor_Client/ # Main sensor telemetry client
├── docs/                        # Architecture documentation & diagrams
├── tests/                       # Test suites
├── .gitignore                   # Comprehensive exclusion rules
└── README.md                    # This file
```

---

## 🚀 Getting Started

### Prerequisites

- **Backend:** Java 17+, Maven 3.8+
- **Hardware:** ESP32 DevKit v1, Arduino IDE 2.x with ESP32 board package
- **Cloud:** AWS Account with IoT Core configured

### Backend

```bash
cd src/backend
./mvnw spring-boot:run
```

The dashboard will be available at `http://localhost:8080`.

### Hardware (ESP32)

1. Copy `Secrets.h.template` → `Secrets.h` and insert your WiFi and AWS IoT credentials.
2. Open `ESP32_Sensor_Client.ino` in Arduino IDE.
3. Select **ESP32 Dev Module** as the board and flash.

---

## 📈 Scalability

Hexive's architecture supports horizontal scaling through:

| Layer | Strategy |
|---|---|
| **Edge Nodes** | Deploy N ESP32 nodes; each publishes to a unique sub-topic (`greysense/sensors/<node_id>`) |
| **Transport** | AWS IoT Core handles millions of concurrent MQTT connections natively |
| **Backend** | Spring Boot is containerisable (Docker/K8s) with stateless REST endpoints behind a load balancer |
| **Database** | Swap H2 for PostgreSQL/TimescaleDB for production-grade time-series storage |
| **Frontend** | Static dashboard served via CDN; API calls are already CORS-enabled |

---

## 🖼️ Visual Roadmap — Diagrams to Include

The following visual assets are recommended and should be placed in `docs/`:

### 1. System Model Diagram
A high-level block diagram showing the complete data flow:
> **Sensors** → **ESP32 (ADC/GPIO)** → **WiFi/MQTT (TLS 1.2)** → **AWS IoT Core** → **Spring Boot Backend** → **H2 Database** → **REST API** → **Dashboard UI**

### 2. Logic Flowchart
A decision-tree flowchart of the sensor data pipeline:
> `loop()` → Read Ultrasonic → Read Microphone → Classify Noise State → Read PIR/Gas → Update Occupancy → Derive Temp/Humidity → Build JSON → Publish to AWS → `SensorDataService.processSensorData()` → Severity Classification → Persist to DB

### 3. Circuit Schematic
An ESP32 pinout diagram showing:
> GPIO 34 → Electret Mic, GPIO 35 → MQ-135, GPIO 27 → HC-SR501 PIR, GPIO 5/18 → HC-SR04 Ultrasonic, GPIO 4 → PWM LED

### 4. UI/UX Dashboard Screenshots
The Hexive dashboard should embody a **Cyber-Industrial** aesthetic:
> Dark background (#0a0a0f) with amber (#f59e0b) accent highlights, monospaced telemetry readouts, glowing card borders, real-time updating sensor gauges, and a scrollable event log with severity-colour-coded entries.

---

## 📄 License

This project is licensed under the MIT License. See `LICENSE` for details.
