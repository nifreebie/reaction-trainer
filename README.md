# Reaction Trainer

Тренажер реакции на базе аппаратной платы NodeMCU ESP8266 и Spring Boot backend. Плата подключается к backend по WebSocket, игрок работает с REST API: регистрируется, получает JWT, создает код сопряжения, вводит код на устройстве и проходит игру из нескольких раундов.

## Что есть в проекте

- `src/main/java/nifreebie/ardodo` - Spring Boot backend.
- `firmware/reaction_trainer/reaction_trainer.ino` - прошивка для NodeMCU v3 Lolin / ESP8266.
- `openapi.yaml` - OpenAPI описание REST API и WebSocket endpoint.
- `compose.yaml` - PostgreSQL для локального запуска.

## Стек

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring WebSocket
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Maven
- ESP8266 + Arduino IDE
- ArduinoJson
- WebSockets library by Markus Sattler

## Аппаратная часть

Прошивка рассчитана на NodeMCU v3 Lolin / ESP8266.

Используемые модули:

- клавиатура 4x3 для ввода 6-значного кода сопряжения;
- PCF8574 I2C expander для клавиатуры;
- TM1638-подобная панель с 8 кнопками, LED и 7-сегментным дисплеем;
- buzzer для звуковых сигналов.

Пины из прошивки:

| Назначение | NodeMCU pin | GPIO |
| --- | --- | --- |
| I2C SDA | `D2` | GPIO4 |
| I2C SCL | `D3` | GPIO0 |
| Buzzer | `D1` | GPIO5 |
| TM STB | `D5` | GPIO14 |
| TM CLK | `D6` | GPIO12 |
| TM DIO | `D7` | GPIO13 |

Адрес PCF8574 по умолчанию: `0x20`.

Клавиатура:

```text
1 2 3
4 5 6
7 8 9
* 0 #
```

`*` очищает введенный код, `#` отправляет код на backend.

## Локальный запуск backend

### 1. Требования

- JDK 17
- Maven
- Docker и Docker Compose

### 2. Настроить переменные окружения

Backend читает `.env` из корня проекта. Создайте файл:

```dotenv
POSTGRES_USER=nifreebie
POSTGRES_PASSWORD=nis150905
POSTGRES_URL=jdbc:postgresql://localhost:5432/reaction_trainer_db
```

Значения должны совпадать с `compose.yaml`, если запускаете локальный PostgreSQL из проекта.

### 3. Запустить PostgreSQL

```bash
docker compose up -d
```

### 4. Запустить приложение

```bash
./mvnw spring-boot:run
```

Если `mvnw` отсутствует:

```bash
mvn spring-boot:run
```

Backend будет доступен на `http://localhost:8080`.

## Настройка прошивки

Откройте `firmware/reaction_trainer/reaction_trainer.ino` в Arduino IDE.

Установите:

- ESP8266 board package;
- библиотеку `WebSockets` by Markus Sattler;
- библиотеку `ArduinoJson` by Benoit Blanchon.

В начале прошивки заполните:

```cpp
const char* WIFI_SSID = "YOUR_WIFI";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

const char* BACKEND_HOST = "192.168.1.10";
const uint16_t BACKEND_PORT = 8080;

const char* DEVICE_ID = "nodemcu-01";
const char* DEVICE_TOKEN = "PASTE_DEVICE_TOKEN_FROM_BACKEND";
```

`BACKEND_HOST` - IP компьютера, на котором запущен backend. Для реальной платы обычно нужен IP в локальной сети, а не `localhost`.

`DEVICE_TOKEN` появляется после регистрации устройства через REST API.

## Полный flow взаимодействия

### 1. Игрок регистрируется или входит

Регистрация:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"player1","password":"secret123"}'
```

Ответ содержит JWT:

```json
{
  "accessToken": "jwt-token",
  "username": "player1"
}
```

Вход:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","password":"secret123"}'
```

Дальше защищенные REST endpoint вызываются с заголовком:

```http
Authorization: Bearer jwt-token
```

### 2. Устройство регистрируется в backend

Регистрация устройства не требует JWT:

```bash
curl -X POST http://localhost:8080/api/devices/register \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"nodemcu-01","name":"Desk trainer","firmwareVersion":"1.0.0"}'
```

Ответ:

```json
{
  "deviceId": "nodemcu-01",
  "name": "Desk trainer",
  "firmwareVersion": "1.0.0",
  "deviceToken": "plain-device-token"
}
```

Этот `deviceToken` нужно вставить в `DEVICE_TOKEN` в прошивке. Backend хранит только hash токена, поэтому plaintext token показывается только при регистрации.

### 3. Плата подключается к Wi-Fi и WebSocket

После прошивки и включения NodeMCU:

1. плата подключается к Wi-Fi;
2. открывает WebSocket:

```text
ws://BACKEND_HOST:8080/ws/devices?deviceId=nodemcu-01&token=plain-device-token
```

3. backend проверяет `deviceId` и `token`;
4. backend помечает устройство online;
5. backend отправляет на плату:

```json
{
  "type": "connected",
  "deviceId": "nodemcu-01"
}
```

Плата каждые 5 секунд отправляет heartbeat:

```json
{
  "type": "heartbeat"
}
```

Если heartbeat долго не приходит, backend считает устройство offline. Таймаут задается в `app.device.heartbeat-timeout-seconds`.

### 4. Игрок создает код сопряжения

Игрок вызывает:

```bash
curl -X POST http://localhost:8080/api/pair-codes \
  -H "Authorization: Bearer jwt-token"
```

Ответ:

```json
{
  "code": "123456",
  "expiresIn": 120
}
```

Код действует ограниченное время. По умолчанию это 120 секунд.

Проверить активный код:

```bash
curl http://localhost:8080/api/pair-codes/active \
  -H "Authorization: Bearer jwt-token"
```

### 5. Игрок вводит код на плате

На клавиатуре платы игрок вводит 6 цифр и нажимает `#`.

Плата отправляет в WebSocket:

```json
{
  "type": "pair_request",
  "code": "123456"
}
```

Backend:

1. ищет активный неиспользованный pair-code;
2. проверяет, что устройство online;
3. привязывает устройство к игроку;
4. создает `GameSession` в статусе `WAITING`;
5. помечает pair-code использованным;
6. отвечает на плату:

```json
{
  "type": "pair_success",
  "sessionId": "uuid",
  "playerName": "player1"
}
```

Сразу после успешного сопряжения backend запускает игру.

### 6. Backend стартует игру и отправляет раунды

Игра создается с настройками из `application.yaml`:

```yaml
app:
  game:
    rounds-count: 10
    timeout-ms: 1500
    target-buttons-count: 8
    stimulus-delay-min-ms: 500
    stimulus-delay-max-ms: 2000
```

Для каждого раунда backend выбирает случайную целевую кнопку от 1 до 8 и случайную задержку стимула.

Сообщение на плату:

```json
{
  "type": "round_start",
  "sessionId": "uuid",
  "roundNumber": 1,
  "targetButton": 4,
  "stimulusDelayMs": 1200,
  "timeoutMs": 1500
}
```

На плате:

1. после `pair_success` начинается обратный отсчет 10 секунд;
2. плата показывает номер целевой кнопки;
3. включает LED целевой кнопки;
4. ждет нажатия кнопки на игровой панели;
5. измеряет время реакции от появления стимула.

### 7. Плата отправляет результат раунда

Формат сообщения:

```json
{
  "type": "round_result",
  "sessionId": "uuid",
  "roundNumber": 1,
  "pressedButton": 4,
  "reactionTimeMs": 312,
  "result": "HIT"
}
```

Возможные значения `result`:

| Result | Когда возникает |
| --- | --- |
| `HIT` | нажата правильная кнопка до timeout |
| `WRONG_BUTTON` | нажата неправильная кнопка |
| `MISS` | игрок не нажал кнопку до timeout |
| `FALSE_START` | кнопка нажата до появления стимула |

Backend дополнительно нормализует результат:

- если `pressedButton` пустой;
- если `reactionTimeMs` пустой;
- если `reactionTimeMs` больше `timeoutMs`;
- если нажата не та кнопка.

После сохранения результата backend либо отправляет следующий `round_start`, либо завершает игру.

### 8. Завершение игры

После последнего раунда backend переводит сессию в `FINISHED`, сохраняет лучший результат игрока и отправляет:

```json
{
  "type": "game_finished",
  "sessionId": "uuid",
  "avgReactionMs": 360,
  "bestReactionMs": 280,
  "missesCount": 1,
  "wrongButtonsCount": 0,
  "falseStartsCount": 0
}
```

Плата показывает среднее время реакции, проигрывает финальную мелодию и ждет нажатия любой кнопки, чтобы вернуться к вводу нового pair-code.

## REST API

Полная спецификация находится в `openapi.yaml`.

Основные endpoint:

| Метод | Endpoint | Назначение | Auth |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | регистрация игрока | нет |
| `POST` | `/api/auth/login` | вход игрока | нет |
| `GET` | `/api/me` | текущий игрок | JWT |
| `PATCH` | `/api/me` | изменить имя игрока | JWT |
| `GET` | `/api/me/stats` | статистика игрока | JWT |
| `POST` | `/api/devices/register` | регистрация устройства | нет |
| `GET` | `/api/devices/me` | устройства текущего игрока | JWT |
| `GET` | `/api/devices/online` | online устройства | JWT |
| `GET` | `/api/devices/{id}` | устройство игрока | JWT |
| `PATCH` | `/api/devices/{id}` | переименовать устройство | JWT |
| `DELETE` | `/api/devices/{id}` | отвязать устройство | JWT |
| `POST` | `/api/pair-codes` | создать pair-code | JWT |
| `GET` | `/api/pair-codes/active` | активный pair-code | JWT |
| `GET` | `/api/sessions/current` | текущая сессия игрока | JWT |
| `GET` | `/api/sessions/me` | история сессий | JWT |
| `GET` | `/api/sessions/{id}` | детали сессии | JWT |
| `GET` | `/api/sessions/{id}/rounds` | раунды сессии | JWT |
| `GET` | `/api/results/leaders` | leaderboard | JWT |
| `GET` | `/api/results/me` | результаты игрока | JWT |
| `GET` | `/api/results/me/best` | лучший результат игрока | JWT |

## WebSocket протокол устройства

Endpoint:

```text
ws://localhost:8080/ws/devices?deviceId={deviceId}&token={deviceToken}
```

Сообщения от устройства:

```json
{"type":"heartbeat"}
```

```json
{"type":"pair_request","code":"123456"}
```

```json
{"type":"round_result","sessionId":"uuid","roundNumber":1,"pressedButton":2,"reactionTimeMs":350,"result":"HIT"}
```

Сообщения от backend:

```json
{"type":"connected","deviceId":"nodemcu-01"}
```

```json
{"type":"pair_success","sessionId":"uuid","playerName":"player1"}
```

```json
{"type":"round_start","sessionId":"uuid","roundNumber":1,"targetButton":2,"stimulusDelayMs":800,"timeoutMs":1500}
```

```json
{"type":"game_finished","sessionId":"uuid","avgReactionMs":360,"bestReactionMs":280,"missesCount":1,"wrongButtonsCount":0,"falseStartsCount":0}
```

```json
{"type":"error","message":"Invalid or expired code"}
```

## Проверка результата через API

Текущая сессия:

```bash
curl http://localhost:8080/api/sessions/current \
  -H "Authorization: Bearer jwt-token"
```

Раунды конкретной сессии:

```bash
curl http://localhost:8080/api/sessions/{sessionId}/rounds \
  -H "Authorization: Bearer jwt-token"
```

Статистика игрока:

```bash
curl http://localhost:8080/api/me/stats \
  -H "Authorization: Bearer jwt-token"
```

Leaderboard:

```bash
curl "http://localhost:8080/api/results/leaders?limit=10" \
  -H "Authorization: Bearer jwt-token"
```

## Тесты

```bash
mvn test
```

Тестовый профиль использует H2 in-memory database из `src/test/resources/application.yaml`.

## Важные настройки

`src/main/resources/application.yaml`:

| Настройка | Значение по умолчанию | Описание |
| --- | --- | --- |
| `app.jwt.expiration-ms` | `86400000` | время жизни JWT |
| `app.pair-code.length` | `6` | длина кода сопряжения |
| `app.pair-code.ttl-seconds` | `120` | срок жизни кода |
| `app.device.heartbeat-timeout-seconds` | `20` | timeout online-статуса устройства |
| `app.game.rounds-count` | `10` | количество раундов |
| `app.game.timeout-ms` | `1500` | время на нажатие после стимула |
| `app.game.target-buttons-count` | `8` | количество игровых кнопок |
| `app.game.stimulus-delay-min-ms` | `500` | минимальная задержка стимула |
| `app.game.stimulus-delay-max-ms` | `2000` | максимальная задержка стимула |

## Типичный сценарий разработки

1. Запустить PostgreSQL: `docker compose up -d`.
2. Запустить backend: `mvn spring-boot:run`.
3. Зарегистрировать устройство через `/api/devices/register`.
4. Вставить `deviceToken`, `DEVICE_ID`, Wi-Fi и IP backend в прошивку.
5. Залить прошивку на NodeMCU.
6. Зарегистрировать или залогинить игрока через `/api/auth/*`.
7. Создать pair-code через `/api/pair-codes`.
8. Ввести код на клавиатуре платы и нажать `#`.
9. Пройти 10 раундов.
10. Проверить сессию, раунды, статистику и leaderboard через REST API.
