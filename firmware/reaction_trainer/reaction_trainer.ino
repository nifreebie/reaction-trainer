/*
  Ardodo reaction trainer firmware for NodeMCU v3 Lolin / ESP8266.

  Arduino IDE libraries to install:
  - ESP8266 board package
  - WebSockets by Markus Sattler
  - ArduinoJson by Benoit Blanchon

  Pairing flow:
  1. Register device in backend: POST /api/devices/register
  2. Put returned deviceToken into DEVICE_TOKEN below.
  3. Start backend, power NodeMCU, enter 6-digit pair code on keypad.
  4. Press # to submit, * to clear.
*/

#include <Arduino.h>
#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <WebSocketsClient.h>
#include <Wire.h>

// ====== EDIT THESE VALUES ======
const char* WIFI_SSID = "YOUR_WIFI";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

const char* BACKEND_HOST = "192.168.1.10";
const uint16_t BACKEND_PORT = 8080;

const char* DEVICE_ID = "nodemcu-01";
const char* DEVICE_TOKEN = "PASTE_DEVICE_TOKEN_FROM_BACKEND";
// =================================

// NodeMCU pin mapping.
const uint8_t I2C_SDA_PIN = D2;      // GPIO4
const uint8_t I2C_SCL_PIN = D3;      // GPIO0
const uint8_t BUZZER_PIN = D1;       // GPIO5

const uint8_t TM_STB_PIN = D5;       // GPIO14
const uint8_t TM_CLK_PIN = D6;       // GPIO12
const uint8_t TM_DIO_PIN = D7;       // GPIO13

const uint8_t PCF8574_ADDR = 0x20;   // Common addresses: 0x20 or 0x27

const uint8_t ROW_COUNT = 4;
const uint8_t COL_COUNT = 3;
const char KEYMAP[ROW_COUNT][COL_COUNT] = {
  {'1', '2', '3'},
  {'4', '5', '6'},
  {'7', '8', '9'},
  {'*', '0', '#'}
};

const uint8_t ROW_PINS[ROW_COUNT] = {0, 1, 2, 3}; // PCF P0..P3
const uint8_t COL_PINS[COL_COUNT] = {4, 5, 6};    // PCF P4..P6

enum GameState {
  ENTER_PAIR_CODE,
  WAIT_PAIRING,
  COUNTDOWN,
  WAIT_ROUND,
  ROUND_PAUSE,
  WAIT_STIMULUS,
  WAIT_PRESS,
  GAME_FINISHED
};

WebSocketsClient webSocket;
GameState state = ENTER_PAIR_CODE;

String pairCode;
String sessionId;
int currentRound = 0;
int targetButton = 0;
int timeoutMs = 1500;
int stimulusDelayMs = 0;
unsigned long roundStartAtMs = 0;
unsigned long stimulusAtMs = 0;
bool resultSent = false;
bool roundPending = false;
unsigned long countdownStartedAtMs = 0;
int lastCountdownValue = -1;
unsigned long roundPauseStartedAtMs = 0;
unsigned long lastHeartbeatAtMs = 0;

const unsigned long HEARTBEAT_INTERVAL_MS = 5000;

char lastKey = 0;
unsigned long lastKeyAtMs = 0;

const uint8_t SEG_DIGITS[10] = {
  0x3F, // 0
  0x06, // 1
  0x5B, // 2
  0x4F, // 3
  0x66, // 4
  0x6D, // 5
  0x7D, // 6
  0x07, // 7
  0x7F, // 8
  0x6F  // 9
};

const uint8_t SEG_BLANK = 0x00;
const uint8_t SEG_DASH = 0x40;

void pcfWrite(uint8_t value) {
  Wire.beginTransmission(PCF8574_ADDR);
  Wire.write(value);
  Wire.endTransmission();
}

uint8_t pcfRead() {
  Wire.requestFrom(PCF8574_ADDR, (uint8_t)1);
  if (Wire.available()) {
    return Wire.read();
  }
  return 0xFF;
}

char readKeypad() {
  for (uint8_t col = 0; col < COL_COUNT; col++) {
    uint8_t mask = 0xFF;
    bitClear(mask, COL_PINS[col]);
    pcfWrite(mask);
    delayMicroseconds(250);

    uint8_t data = pcfRead();
    for (uint8_t row = 0; row < ROW_COUNT; row++) {
      if (bitRead(data, ROW_PINS[row]) == 0) {
        pcfWrite(0xFF);
        return KEYMAP[row][col];
      }
    }
  }

  pcfWrite(0xFF);
  return 0;
}

char readDebouncedKeypad() {
  char key = readKeypad();
  unsigned long now = millis();

  if (key == 0) {
    lastKey = 0;
    return 0;
  }

  if (key != lastKey || now - lastKeyAtMs > 300) {
    lastKey = key;
    lastKeyAtMs = now;
    return key;
  }

  return 0;
}

void tmStart() {
  digitalWrite(TM_STB_PIN, LOW);
}

void tmStop() {
  digitalWrite(TM_STB_PIN, HIGH);
}

void tmWriteByte(uint8_t data) {
  pinMode(TM_DIO_PIN, OUTPUT);
  for (uint8_t i = 0; i < 8; i++) {
    digitalWrite(TM_CLK_PIN, LOW);
    digitalWrite(TM_DIO_PIN, data & 0x01);
    data >>= 1;
    digitalWrite(TM_CLK_PIN, HIGH);
  }
}

uint8_t tmReadByte() {
  uint8_t data = 0;
  pinMode(TM_DIO_PIN, INPUT);

  for (uint8_t i = 0; i < 8; i++) {
    digitalWrite(TM_CLK_PIN, LOW);
    delayMicroseconds(2);
    if (digitalRead(TM_DIO_PIN)) {
      data |= (1 << i);
    }
    digitalWrite(TM_CLK_PIN, HIGH);
  }

  pinMode(TM_DIO_PIN, OUTPUT);
  return data;
}

void tmCommand(uint8_t command) {
  tmStart();
  tmWriteByte(command);
  tmStop();
}

void tmWriteAddress(uint8_t address, uint8_t data) {
  tmCommand(0x44);
  tmStart();
  tmWriteByte(0xC0 | address);
  tmWriteByte(data);
  tmStop();
}

void tmClear() {
  tmCommand(0x40);
  tmStart();
  tmWriteByte(0xC0);
  for (uint8_t i = 0; i < 16; i++) {
    tmWriteByte(0x00);
  }
  tmStop();
}

void tmSetBrightness(uint8_t brightness) {
  brightness = min<uint8_t>(brightness, 7);
  tmCommand(0x88 | brightness);
}

void tmSetLed(uint8_t ledNumber, bool on) {
  if (ledNumber < 1 || ledNumber > 8) {
    return;
  }
  tmWriteAddress((ledNumber - 1) * 2 + 1, on ? 0x01 : 0x00);
}

void tmAllLedsOff() {
  for (uint8_t i = 1; i <= 8; i++) {
    tmSetLed(i, false);
  }
}

void tmSetDigit(uint8_t position, uint8_t segments) {
  if (position > 7) {
    return;
  }
  tmWriteAddress(position * 2, segments);
}

void tmDisplayNumber(int value) {
  tmClear();

  if (value == 0) {
    tmSetDigit(7, SEG_DIGITS[0]);
    return;
  }

  uint8_t pos = 7;
  int number = abs(value);
  while (number > 0 && pos < 8) {
    tmSetDigit(pos, SEG_DIGITS[number % 10]);
    number /= 10;
    if (pos == 0) {
      break;
    }
    pos--;
  }

  if (value < 0 && pos < 8) {
    tmSetDigit(pos, SEG_DASH);
  }
}

void tmDisplayPairCode() {
  tmClear();
  uint8_t start = pairCode.length() > 8 ? 0 : 8 - pairCode.length();
  for (uint8_t i = 0; i < pairCode.length() && i < 8; i++) {
    char c = pairCode[i];
    if (c >= '0' && c <= '9') {
      tmSetDigit(start + i, SEG_DIGITS[c - '0']);
    }
  }
}

int readTmButton() {
  tmStart();
  tmWriteByte(0x42);

  uint8_t bytes[4];
  for (uint8_t i = 0; i < 4; i++) {
    bytes[i] = tmReadByte();
  }
  tmStop();

  for (uint8_t i = 0; i < 4; i++) {
    if (bytes[i] & 0x01) {
      return i + 1;
    }
    if (bytes[i] & 0x10) {
      return i + 5;
    }
  }

  return 0;
}

void beep(uint16_t durationMs) {
  digitalWrite(BUZZER_PIN, HIGH);
  delay(durationMs);
  digitalWrite(BUZZER_PIN, LOW);
}

void playHitSound() {
  tone(BUZZER_PIN, 1200, 70);
  delay(80);
  noTone(BUZZER_PIN);
}

void playMarioFinishMelody() {
  const uint16_t notes[] = {
    660, 660, 0, 660, 0, 520, 660, 0, 784, 0, 392
  };
  const uint16_t durations[] = {
    90, 90, 90, 90, 90, 90, 90, 90, 160, 140, 160
  };

  for (uint8_t i = 0; i < sizeof(notes) / sizeof(notes[0]); i++) {
    if (notes[i] > 0) {
      tone(BUZZER_PIN, notes[i], durations[i]);
    }
    delay(durations[i] + 25);
    noTone(BUZZER_PIN);
  }
}

void sendPairRequest() {
  StaticJsonDocument<128> doc;
  doc["type"] = "pair_request";
  doc["code"] = pairCode;

  String payload;
  serializeJson(doc, payload);
  Serial.print("WS -> ");
  Serial.println(payload);
  webSocket.sendTXT(payload);

  state = WAIT_PAIRING;
  tmDisplayNumber(0);
}

void sendRoundResult(const char* result, int pressedButton, int reactionTimeMs) {
  if (resultSent || sessionId.length() == 0) {
    return;
  }

  StaticJsonDocument<256> doc;
  doc["type"] = "round_result";
  doc["sessionId"] = sessionId;
  doc["roundNumber"] = currentRound;
  doc["result"] = result;

  if (pressedButton > 0) {
    doc["pressedButton"] = pressedButton;
  } else {
    doc["pressedButton"] = nullptr;
  }

  if (reactionTimeMs >= 0) {
    doc["reactionTimeMs"] = reactionTimeMs;
  } else {
    doc["reactionTimeMs"] = nullptr;
  }

  String payload;
  serializeJson(doc, payload);
  Serial.print("WS -> ");
  Serial.println(payload);
  webSocket.sendTXT(payload);

  resultSent = true;
  tmAllLedsOff();
}

void sendHeartbeat() {
  unsigned long now = millis();
  if (now - lastHeartbeatAtMs < HEARTBEAT_INTERVAL_MS) {
    return;
  }

  StaticJsonDocument<64> doc;
  doc["type"] = "heartbeat";

  String payload;
  serializeJson(doc, payload);
  webSocket.sendTXT(payload);

  lastHeartbeatAtMs = now;
}

int keypadButtonNumber(char key) {
  if (key >= '1' && key <= '8') {
    return key - '0';
  }
  return 0;
}

int readGameButton() {
  return readTmButton();
}

void startCountdown() {
  countdownStartedAtMs = millis();
  lastCountdownValue = -1;
  roundPending = false;
  state = COUNTDOWN;
  tmAllLedsOff();
  tmDisplayNumber(10);
}

void beginCurrentRound() {
  roundPending = false;
  resultSent = false;
  stimulusAtMs = millis();

  tmClear();
  tmAllLedsOff();
  tmDisplayNumber(targetButton);
  tmSetLed(targetButton, true);

  state = WAIT_PRESS;
}

void startRoundPause() {
  roundPauseStartedAtMs = millis();
  tmClear();
  tmAllLedsOff();
  state = ROUND_PAUSE;
}

void handleRoundPause() {
  if (readTmButton() > 0) {
    roundPauseStartedAtMs = millis();
    return;
  }

  if (millis() - roundPauseStartedAtMs >= 1000) {
    beginCurrentRound();
  }
}

void handleCountdown() {
  unsigned long elapsedSeconds = (millis() - countdownStartedAtMs) / 1000;
  int value = 10 - (int)elapsedSeconds;

  if (value < 0) {
    if (roundPending) {
      beginCurrentRound();
    } else {
      tmClear();
      state = WAIT_ROUND;
    }
    return;
  }

  if (value != lastCountdownValue) {
    lastCountdownValue = value;
    tmAllLedsOff();
    tmDisplayNumber(value);
  }
}

void resetForNextPairing() {
  pairCode = "";
  sessionId = "";
  currentRound = 0;
  targetButton = 0;
  stimulusDelayMs = 0;
  stimulusAtMs = 0;
  roundStartAtMs = 0;
  resultSent = false;
  roundPending = false;
  lastCountdownValue = -1;

  tmClear();
  tmAllLedsOff();
  state = ENTER_PAIR_CODE;
}

void handleGameFinishedInput() {
  int panelButton = readTmButton();
  char keypadKey = readDebouncedKeypad();

  if (panelButton > 0 || keypadKey == '*' || keypadKey == '#') {
    Serial.println("Resetting for next pair code");
    resetForNextPairing();
  }
}

void handlePairCodeInput() {
  char key = readDebouncedKeypad();
  if (key == 0) {
    return;
  }

  if (key >= '0' && key <= '9' && pairCode.length() < 6) {
    pairCode += key;
    tmDisplayPairCode();
    return;
  }

  if (key == '*') {
    pairCode = "";
    tmClear();
    return;
  }

  if (key == '#' && pairCode.length() == 6) {
    sendPairRequest();
  }
}

void handleGameLoop() {
  int pressedButton = readGameButton();
  unsigned long now = millis();

  if (state == WAIT_STIMULUS) {
    if (pressedButton > 0) {
      sendRoundResult("FALSE_START", pressedButton, -1);
      state = WAIT_ROUND;
      beep(250);
      return;
    }

    if (now - roundStartAtMs >= (unsigned long)stimulusDelayMs) {
      stimulusAtMs = now;
      tmAllLedsOff();
      tmSetLed(targetButton, true);
      tmDisplayNumber(targetButton);
      state = WAIT_PRESS;
      beep(40);
    }
    return;
  }

  if (state == WAIT_PRESS) {
    if (pressedButton > 0) {
      int reactionTimeMs = (int)(now - stimulusAtMs);
      const char* result = pressedButton == targetButton ? "HIT" : "WRONG_BUTTON";
      sendRoundResult(result, pressedButton, reactionTimeMs);
      state = WAIT_ROUND;
      if (pressedButton == targetButton) {
        playHitSound();
      }
      return;
    }

    if (now - stimulusAtMs >= (unsigned long)timeoutMs) {
      sendRoundResult("MISS", -1, -1);
      state = WAIT_ROUND;
    }
  }
}

void onRoundStart(JsonDocument& doc) {
  sessionId = doc["sessionId"].as<String>();
  currentRound = doc["roundNumber"] | 0;
  targetButton = doc["targetButton"] | 0;
  stimulusDelayMs = doc["stimulusDelayMs"] | 1000;
  timeoutMs = doc["timeoutMs"] | 1500;

  Serial.print("Round ");
  Serial.print(currentRound);
  Serial.print(", target=");
  Serial.print(targetButton);
  Serial.print(", delay=");
  Serial.print(stimulusDelayMs);
  Serial.print(", timeout=");
  Serial.println(timeoutMs);

  roundStartAtMs = millis();
  stimulusAtMs = 0;
  roundPending = true;

  if (state != COUNTDOWN) {
    startRoundPause();
  }
}

void handleWsText(uint8_t* payload, size_t length) {
  Serial.print("WS <- ");
  Serial.write(payload, length);
  Serial.println();

  StaticJsonDocument<512> doc;
  DeserializationError error = deserializeJson(doc, payload, length);
  if (error) {
    Serial.print("JSON parse error: ");
    Serial.println(error.c_str());
    return;
  }

  String type = doc["type"] | "";

  if (type == "connected") {
    Serial.println("Device connected to backend");
    state = ENTER_PAIR_CODE;
    pairCode = "";
    tmClear();
    return;
  }

  if (type == "pair_success") {
    sessionId = doc["sessionId"].as<String>();
    Serial.print("Pair success, sessionId=");
    Serial.println(sessionId);
    startCountdown();
    return;
  }

  if (type == "round_start") {
    onRoundStart(doc);
    return;
  }

  if (type == "game_finished") {
    Serial.println("Game finished");
    state = GAME_FINISHED;
    tmAllLedsOff();
    tmDisplayNumber(doc["avgReactionMs"] | 0);
    playMarioFinishMelody();
    return;
  }

  if (type == "error") {
    Serial.print("Backend error: ");
    Serial.println(doc["message"].as<String>());
    state = ENTER_PAIR_CODE;
    pairCode = "";
    tmClear();
  }
}

void webSocketEvent(WStype_t type, uint8_t* payload, size_t length) {
  switch (type) {
    case WStype_CONNECTED:
      Serial.println("WebSocket connected");
      lastHeartbeatAtMs = 0;
      sendHeartbeat();
      break;
    case WStype_TEXT:
      handleWsText(payload, length);
      break;
    case WStype_DISCONNECTED:
      Serial.println("WebSocket disconnected");
      state = ENTER_PAIR_CODE;
      tmClear();
      break;
    default:
      break;
  }
}

void connectWifi() {
  Serial.print("Connecting WiFi to ");
  Serial.println(WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(300);
  }

  Serial.println();
  Serial.print("WiFi connected, IP=");
  Serial.println(WiFi.localIP());
}

void connectWebSocket() {
  String path = String("/ws/devices?deviceId=") + DEVICE_ID + "&token=" + DEVICE_TOKEN;
  Serial.print("Connecting WebSocket to ");
  Serial.print(BACKEND_HOST);
  Serial.print(":");
  Serial.print(BACKEND_PORT);
  Serial.println(path);
  webSocket.begin(BACKEND_HOST, BACKEND_PORT, path);
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(3000);
}

void setup() {
  Serial.begin(115200);
  delay(100);
  Serial.println();
  Serial.println("Ardodo firmware boot");

  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);

  pinMode(TM_STB_PIN, OUTPUT);
  pinMode(TM_CLK_PIN, OUTPUT);
  pinMode(TM_DIO_PIN, OUTPUT);
  digitalWrite(TM_STB_PIN, HIGH);
  digitalWrite(TM_CLK_PIN, HIGH);

  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);
  pcfWrite(0xFF);

  tmSetBrightness(4);
  tmClear();
  tmAllLedsOff();

  connectWifi();
  connectWebSocket();
}

void loop() {
  webSocket.loop();
  sendHeartbeat();

  if (state == ENTER_PAIR_CODE) {
    handlePairCodeInput();
  } else if (state == COUNTDOWN) {
    handleCountdown();
  } else if (state == ROUND_PAUSE) {
    handleRoundPause();
  } else if (state == WAIT_STIMULUS || state == WAIT_PRESS) {
    handleGameLoop();
  } else if (state == GAME_FINISHED) {
    handleGameFinishedInput();
  }
}
