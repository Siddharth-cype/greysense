# Hexive — Architecture Decision Record

## Why Spring Boot?

Spring Boot was selected as the backend framework for the following enterprise-grade reasons:

1. **Mature Ecosystem:** Spring Data JPA, Spring Web, and Spring Security provide battle-tested abstractions for REST APIs, ORM, and authentication — critical for any production IoT platform.
2. **Dependency Injection:** The IoC container simplifies integration of the MQTT client, repositories, and the decision engine service without tight coupling.
3. **Extensibility:** Adding new sensor types or control commands requires only a new model field and controller endpoint — zero framework changes.
4. **Containerization:** Spring Boot applications package as self-contained JARs, trivially deployable to Docker/Kubernetes for horizontal scaling.

## Why ESP32?

The ESP32 microcontroller was chosen as the edge-node platform because:

1. **Dual-Core + WiFi:** The Xtensa LX6 dual-core processor runs sensor polling and network communication concurrently without an external WiFi module.
2. **Hardware TLS:** Native TLS 1.2 support via `WiFiClientSecure` enables direct mutual-TLS authentication with AWS IoT Core — no gateway required.
3. **Rich Peripherals:** 18 ADC channels, 16 PWM channels, and multiple GPIO banks support the full sensor suite (PIR, ultrasonic, gas, microphone) plus LED actuation on a single board.
4. **Cost Efficiency:** At ~$4 per unit, the ESP32 enables cost-effective fleet deployment compared to SBCs like Raspberry Pi.

## Why AWS IoT Core?

AWS IoT Core serves as the message broker for the following reasons:

1. **Managed MQTT:** Eliminates the operational burden of running a self-hosted Mosquitto broker.
2. **X.509 Authentication:** Per-device certificate authentication is the IoT industry standard — no password management at scale.
3. **Rule Engine (Future):** AWS IoT Rules can route messages to Lambda, DynamoDB, or SNS for serverless event processing without backend changes.
4. **Scalability:** Natively supports billions of messages per day with automatic scaling.

## Data Flow

```
ESP32 Edge Node                    AWS IoT Core                   Spring Boot Backend
┌──────────────┐                  ┌──────────────┐               ┌──────────────────┐
│ Sensor Read  │───MQTT/TLS 1.2──▶│ Topic Router │───MQTT Sub───▶│ AwsIotConfig     │
│ JSON Build   │                  │ greysense/*  │               │   ↓               │
│ Publish      │                  └──────────────┘               │ SensorDataService │
│              │◀──MQTT Sub───────────────────────────────────────│ (Decision Engine) │
│ LED Control  │  greysense/control                              │   ↓               │
└──────────────┘                                                 │ SensorDataRepo    │
                                                                 │ (H2 / PostgreSQL) │
                                                                 │   ↓               │
                                                                 │ REST API Layer    │
                                                                 │   ↓               │
                                                                 │ Dashboard UI      │
                                                                 └──────────────────┘
```

## Decision Engine Priority Cascade

The `SensorDataService` classifies incoming telemetry using a strictly ordered priority system:

| Priority | Condition | Severity | Message |
|---|---|---|---|
| 1 | Temperature > 45°C + Air > 1000 PPM | CRITICAL | POTENTIAL FIRE HAZARD |
| 1 | Temperature > 45°C | CRITICAL | EXTREME HEAT DETECTED |
| 2 | Air ≥ 2000 PPM | CRITICAL | Hazardous Air Quality |
| 2 | Air ≥ 1200 PPM | WARNING | Poor Ventilation |
| 3 | PIR Motion + Distance < 50 cm | WARNING | Proximity Breach Detected |
| 4 | Noise > 85 dB | WARNING | High Noise Level |
| — | None triggered | NORMAL | System Nominal |

Higher priority conditions override lower ones. CRITICAL always takes precedence over WARNING.
