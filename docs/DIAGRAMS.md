# Hexive — Visual Diagram Specifications

> **Design Language: Cyber-Industrial**
> Dark background (#0a0a0f), neon amber accents (#f59e0b), electric cyan highlights (#06b6d4),
> monospaced typography (JetBrains Mono / Fira Code), sharp geometric borders, subtle glow effects.

---

## 1. System Architecture Diagram

**Purpose:** Show the end-to-end data pipeline from physical sensors to the dashboard UI.

**Layout:** Left-to-right horizontal flow across three distinct zones.

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│  BACKGROUND: #0a0a0f (near-black)  •  ALL BORDERS: 1px solid #f59e0b (amber glow)      │
│  ZONE LABELS: Fira Code, 10pt, #6b7280 (muted grey), uppercase tracking                │
│                                                                                          │
│  ── EDGE LAYER ──            ── TRANSPORT LAYER ──          ── APPLICATION LAYER ──      │
│                                                                                          │
│  ┌─────────────────┐         ┌─────────────────┐          ┌──────────────────────┐      │
│  │  ⬡ ESP32 NODE   │  ═══▶   │  ☁ AWS IoT Core │   ═══▶   │  ☕ Spring Boot      │      │
│  │                 │  MQTT   │                 │   MQTT   │                      │      │
│  │  ┌───────────┐  │  TLS    │  • Message      │   Sub    │  ┌────────────────┐  │      │
│  │  │ PIR       │──│─ 1.2 ──▶│    Broker       │──────────│──│ AwsIotConfig   │  │      │
│  │  │ Ultrasonic│  │         │  • X.509        │          │  │      ↓         │  │      │
│  │  │ MQ-135    │  │         │    mTLS Auth    │          │  │ SensorData     │  │      │
│  │  │ Mic       │  │         │  • Topic        │ ◀──JSON──│──│ Service        │  │      │
│  │  └───────────┘  │         │    Routing      │  Control │  │ (Decision Eng) │  │      │
│  │        ↓        │         └─────────────────┘          │  │      ↓         │  │      │
│  │  ADC → JSON     │                                      │  │ JPA Repository │  │      │
│  │  Telemetry      │                                      │  │      ↓         │  │      │
│  │                 │                                      │  │ H2 / Postgres  │  │      │
│  │  ┌───────────┐  │                                      │  │      ↓         │  │      │
│  │  │ LED (PWM) │◀─│──────────────── MQTT Control ────────│──│ REST API       │  │      │
│  │  └───────────┘  │                                      │  │      ↓         │  │      │
│  └─────────────────┘                                      │  │ Dashboard UI   │  │      │
│                                                           │  └────────────────┘  │      │
│                                                           └──────────────────────┘      │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

### Visual Specifications

| Element | Style |
|---|---|
| **Background** | Solid #0a0a0f with subtle radial gradient (#111827 center) |
| **Node boxes** | Rounded rect (8px radius), 1px #f59e0b border, 0 0 12px rgba(245,158,11,0.3) box-shadow |
| **Data arrows** | 2px solid #06b6d4 (cyan) with animated dash-offset for "flowing data" effect |
| **Control arrows** | 2px dashed #f59e0b (amber), reverse direction |
| **Zone labels** | Fira Code 10pt, #6b7280, uppercase, letter-spacing: 3px |
| **Component labels** | JetBrains Mono 12pt, #e5e7eb (light grey) |
| **Icons** | Minimalist line icons — hexagon for ESP32, cloud for AWS, coffee cup for Spring |
| **Sensor sub-nodes** | Smaller boxes inside ESP32 node, #1f2937 fill, 1px #374151 border |

---

## 2. Decision Logic Flowchart

**Purpose:** Visualize the `SensorDataService` priority cascade as a decision tree.

```
                           ┌──────────────────┐
                           │  INCOMING DATA   │
                           │  SensorData obj  │
                           └────────┬─────────┘
                                    ▼
                        ┌───────────────────────┐
                        │  temp > 45°C ?        │
                        └────┬─────────────┬────┘
                          YES│             │NO
                             ▼             ▼
                   ┌─────────────────┐     │
                   │ airPpm > 1000?  │     │
                   └──┬──────────┬───┘     │
                   YES│          │NO       │
                      ▼          ▼         │
              ┌──────────┐ ┌──────────┐    │
              │ CRITICAL │ │ CRITICAL │    │
              │ FIRE     │ │ EXTREME  │    │
              │ HAZARD   │ │ HEAT     │    │
              └──────────┘ └──────────┘    │
                                           ▼
                              ┌───────────────────────┐
                              │  airPpm ≥ 2000 ?      │
                              └────┬─────────────┬────┘
                                YES│             │NO
                                   ▼             ▼
                           ┌──────────┐  ┌───────────────────┐
                           │ CRITICAL │  │ airPpm ≥ 1200 ?   │
                           │ HAZARD   │  └──┬────────────┬───┘
                           │ AIR      │  YES│            │NO
                           └──────────┘     ▼            ▼
                                     ┌──────────┐       │
                                     │ WARNING  │       │
                                     │ POOR     │       │
                                     │ VENT     │       │
                                     └──────────┘       │
                                                        ▼
                                        ┌─────────────────────────┐
                                        │ PIR + distance < 50cm ? │
                                        └────┬───────────────┬────┘
                                          YES│               │NO
                                             ▼               ▼
                                     ┌──────────┐  ┌─────────────────┐
                                     │ WARNING  │  │ noiseDb > 85 ?  │
                                     │ PROX     │  └──┬──────────┬───┘
                                     │ BREACH   │  YES│          │NO
                                     └──────────┘     ▼          ▼
                                               ┌──────────┐ ┌──────────┐
                                               │ WARNING  │ │ NORMAL   │
                                               │ HIGH     │ │ SYSTEM   │
                                               │ NOISE    │ │ NOMINAL  │
                                               └──────────┘ └──────────┘
```

### Visual Specifications

| Element | Style |
|---|---|
| **Decision diamonds** | Rotated square, #1f2937 fill, 2px #06b6d4 border, cyan glow |
| **CRITICAL outcomes** | Rounded rect, #7f1d1d fill (dark red), 2px #ef4444 border (red), red glow |
| **WARNING outcomes** | Rounded rect, #78350f fill (dark amber), 2px #f59e0b border (amber), amber glow |
| **NORMAL outcome** | Rounded rect, #064e3b fill (dark emerald), 2px #10b981 border (green), green glow |
| **YES/NO labels** | Fira Code 10pt, YES=#10b981 (green), NO=#ef4444 (red) |
| **Arrows** | 1.5px solid #4b5563, subtle animation on hover |
| **Entry node** | Pill shape, #f59e0b fill, #0a0a0f text |

---

## 3. Hardware Schematic (ESP32 Pinout)

**Purpose:** Show the physical wiring from the ESP32 to each sensor and actuator.

```
                            ┌────────────────────────────────┐
                            │        ESP32 DevKit v1         │
                            │         ┌──────────┐           │
  [HC-SR501 PIR] ──────────▶│ GPIO 27 │          │           │
                            │         │          │           │
  [HC-SR04 TRIG] ◀──────────│ GPIO 5  │   ESP32  │           │
  [HC-SR04 ECHO] ──────────▶│ GPIO 18 │  Xtensa  │           │
                            │         │  LX6     │           │
  [MQ-135 AOUT] ───────────▶│ GPIO 35 │  240MHz  │ GPIO 4  │──────▶ [PWM LED]
                            │  (ADC1) │          │  (PWM)   │
  [Electret Mic] ──────────▶│ GPIO 34 │          │           │
                            │  (ADC1) │          │           │
                            │         │          │           │
                            │         │  WiFi    │           │
                            │         │  802.11  │           │
                            │         │  b/g/n   │           │
                            │         └──────────┘           │
                            │                                │
                            │  VIN ◀── 5V USB                │
                            │  GND ──▶ Common Ground Rail    │
                            └────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────────────┐
  │  POWER RAIL: 3.3V from ESP32 regulator → sensor VCC lines          │
  │  GROUND RAIL: Common GND bus connecting all sensor GND pins         │
  │  ADC NOTE: GPIO 34/35 are input-only pins (no internal pull-up)     │
  │  PWM NOTE: GPIO 4 configured at 5kHz, 8-bit resolution (0–255)     │
  └──────────────────────────────────────────────────────────────────────┘
```

### Pin Assignment Table

| GPIO | Direction | Signal Type | Component | Wire Colour (Suggested) |
|------|-----------|-------------|-----------|-------------------------|
| 27 | INPUT (Digital) | HIGH/LOW 3.3V | HC-SR501 PIR Motion Sensor | 🟡 Yellow |
| 5 | OUTPUT (Digital) | 10μs Trigger Pulse | HC-SR04 Ultrasonic (TRIG) | 🟠 Orange |
| 18 | INPUT (Digital) | Echo Pulse Timing | HC-SR04 Ultrasonic (ECHO) | 🟠 Orange |
| 35 | INPUT (ADC1_CH7) | 0–3.3V Analog | MQ-135 Gas Sensor (AOUT) | 🟢 Green |
| 34 | INPUT (ADC1_CH6) | 0–3.3V Analog | Electret Microphone Amp | 🔵 Blue |
| 4 | OUTPUT (PWM) | 5kHz 8-bit Duty Cycle | LED Indicator / Actuator | 🔴 Red |
| VIN | POWER | 5V USB Input | Power Supply | ⚫ Black |
| GND | GROUND | Common Reference | All Components | ⚫ Black |

### Visual Specifications

| Element | Style |
|---|---|
| **ESP32 board** | Central rectangle, #111827 fill, 2px #f59e0b border, golden glow |
| **GPIO pins** | Small circles on board edge, #06b6d4 fill for inputs, #f59e0b fill for outputs |
| **Sensor modules** | Rounded rectangles with component icons, #1f2937 fill, labelled with component name |
| **Wires** | Coloured lines matching wire colour column, 2px width, right-angle routing |
| **Power/GND** | Red (#ef4444) for VCC, Black (#1f2937) for GND, thicker 3px lines |
| **Labels** | Fira Code 9pt, #9ca3af (grey), positioned beside each wire |

---

## Recommended Tools for Rendering

| Tool | Best For | Export |
|---|---|---|
| **Figma** | System Architecture Diagram (most control over aesthetics) | SVG / PNG |
| **draw.io (diagrams.net)** | Decision Flowchart (built-in flowchart shapes) | SVG / PNG |
| **Fritzing** | Hardware Schematic (native ESP32 component libraries) | PNG / PDF |
| **Excalidraw** | Quick hand-drawn aesthetic if preferred | SVG / PNG |
| **Mermaid.js** | Embed directly in GitHub markdown (limited styling) | Rendered in-repo |

> **Export at 2x resolution** (minimum 2400px wide) for crisp rendering on GitHub.
> Save all final diagrams to `docs/diagrams/` as both SVG and PNG.
