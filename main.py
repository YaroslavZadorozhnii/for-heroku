import logging
import math
from typing import List, Dict, Any
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Request
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.templating import Jinja2Templates
import httpx

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("geo_server_local")

app = FastAPI(title="Local Anti-Spoofing Nav Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

templates = Jinja2Templates(directory="templates")

GOOGLE_API_KEY = "AIzaSyBG-GeQRANH-7IgAf6IrmfBY8BxFtILFA8"

# Пороги для економії токенів Google API
MIN_DISTANCE_METERS = 20.0  # Мінімальний зсув у метрах між попередніми й новими координатами


class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
        logger.info("Браузер підключився до WebSocket")

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)
            logger.info("Браузер відключився від WebSocket")

    async def broadcast(self, message: dict):
        for connection in list(self.active_connections):
            try:
                await connection.send_json(message)
            except Exception as e:
                logger.error(f"Помилка надсилання у WebSocket: {e}")


manager = ConnectionManager()

current_location = {
    "lat": 50.4501,
    "lng": 30.5234,
    "accuracy": 500.0,
    "status": "waiting"
}

# Збереження попередніх сирих даних від ESP32
last_telemetry_payload: Dict[str, Any] = {}

import asyncio
import time

# Час останнього отримання телеметрії від ESP32.
last_esp_seen = 0.0

# Якщо ESP32 мовчить довше цього часу — вважаємо його вимкненим.
ESP_TIMEOUT_SECONDS = 30

def calculate_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Обчислення відстані між координатами в метрах (Гаверсинус)."""
    if None in (lat1, lon1, lat2, lon2):
        return 0.0
    r = 6371000.0
    d_lat = math.radians(lat2 - lat1)
    d_lon = math.radians(lon2 - lon1)
    a = (math.sin(d_lat / 2) ** 2 +
         math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(d_lon / 2) ** 2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return r * c

async def esp_watchdog():
    """
    Контролює зв'язок із ESP32.

    Якщо ESP32 не надсилав телеметрію довше ESP_TIMEOUT_SECONDS,
    переводимо систему в offline.

    ВАЖЛИВО:
    Watchdog НЕ звертається до Google API.
    """
    global current_location

    while True:
        await asyncio.sleep(5)

        # ESP32 ще жодного разу не підключався.
        if last_esp_seen == 0:
            continue

        elapsed = time.monotonic() - last_esp_seen

        if elapsed > ESP_TIMEOUT_SECONDS:
            if current_location["status"] != "offline":
                logger.warning(
                    f"[ESP OFFLINE] Немає телеметрії {elapsed:.1f} сек."
                )

                current_location = {
                    "lat": None,
                    "lng": None,
                    "accuracy": None,
                    "status": "offline"
                }

                # Повідомляємо браузер, що ESP32 вимкнений.
                await manager.broadcast(current_location)


@app.on_event("startup")
async def startup_event():
    """
    Запускаємо watchdog при старті FastAPI.
    """
    asyncio.create_task(esp_watchdog())
@app.get("/", response_class=HTMLResponse)
async def get_index(request: Request):
    try:
        return templates.TemplateResponse(
            request=request,
            name="index.html"
        )
    except Exception as e:
        logger.error(f"Помилка рендерингу шаблону: {e}")
        return HTMLResponse(content=f"<h1>Помилка шаблону: {e}</h1>", status_code=500)


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    await websocket.send_json(current_location)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(websocket)


@app.post("/api/telemetry")
async def receive_telemetry(payload: Dict[str, Any]):
    global current_location
    global last_telemetry_payload
    global last_esp_seen

    # ESP32 живий — оновлюємо heartbeat.
    last_esp_seen = time.monotonic()

    cell_towers = payload.get("cell_towers", [])
    wifi_aps = payload.get("wifi_aps", [])

    logger.info(f"[TELEMETRY] Отримано: Cell Towers={len(cell_towers)}, Wi-Fi APs={len(wifi_aps)}")

    # 1. Якщо телеметрія порожня або збігається з попередньою — ігноруємо запит до Google API
    if not cell_towers and not wifi_aps:
        logger.info("[SKIP API] Телеметрія порожня. Запит скасовано.")
        return {"status": "skipped", "reason": "empty_data", "location": current_location}

    if payload == last_telemetry_payload:
        logger.info("[SKIP API] Список веж/Wi-Fi не змінився від минулого разу. 0 токенів витрачено.")
        return {"status": "skipped", "reason": "unchanged_telemetry", "location": current_location}

    if GOOGLE_API_KEY == "YOUR_GOOGLE_MAPS_API_KEY" or not GOOGLE_API_KEY:
        logger.warning("GOOGLE_API_KEY не вказано! Повертаємо тестові координати.")
        current_location = {
            "lat": 50.4501,
            "lng": 30.5234,
            "accuracy": 150.0,
            "status": "demo"
        }
        await manager.broadcast(current_location)
        return {"status": "demo", "location": current_location}

    google_payload = {
        "homeMobileCountryCode": 255,
        "considerIp": False,
        "cellTowers": cell_towers,
        "wifiAccessPoints": wifi_aps
    }

    url = f"https://www.googleapis.com/geolocation/v1/geolocate?key={GOOGLE_API_KEY}"

    async with httpx.AsyncClient(timeout=10.0) as client:
        try:
            resp = await client.post(url, json=google_payload)
            if resp.status_code == 200:
                data = resp.json()
                location = data.get("location", {})
                accuracy = data.get("accuracy", 0.0)

                new_lat = location.get("lat")
                new_lng = location.get("lng")

                # 2. Перевірка зсуву координат
                if current_location["lat"] is not None and current_location["lng"] is not None:
                    dist = calculate_distance(current_location["lat"], current_location["lng"], new_lat, new_lng)
                    if dist < MIN_DISTANCE_METERS and current_location["status"] == "ok":
                        logger.info(f"[SKIP BROADCAST] Зсув всього {dist:.1f}m (менше {MIN_DISTANCE_METERS}m).")
                        last_telemetry_payload = payload
                        return {"status": "ok", "reason": "minimal_movement", "location": current_location}

                current_location = {
                    "lat": new_lat,
                    "lng": new_lng,
                    "accuracy": accuracy,
                    "status": "ok"
                }
                last_telemetry_payload = payload

                logger.info(
                    f"[GOOGLE API SUCCESS] Lat: {current_location['lat']}, Lng: {current_location['lng']} (Accuracy: {accuracy}m)")
                await manager.broadcast(current_location)
                return {"status": "success", "location": current_location}
            else:
                logger.error(f"[GOOGLE API ERROR] {resp.status_code}: {resp.text}")
                return JSONResponse(status_code=400, content={"status": "error", "message": resp.text})
        except Exception as e:
            logger.error(f"[SERVER ERROR] {e}")
            return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)