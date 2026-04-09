# Pomodoro

A multi-client Pomodoro timer system. A central backend manages timer state and broadcasts updates to all connected clients via SSE.

```
pomodoro-server/          Node.js/Express backend
pomodoro-javafx-client/   JavaFX desktop application
pomodoro-ui-extension/    Chrome browser extension
```

---

## pomodoro-server

**Node.js · Express · TypeScript · Port 4000**

The backend that all clients talk to. Owns all timer state, persistence, and real-time broadcasting.

- REST endpoints: `GET/PATCH /timer`, `GET /timer/events` (SSE), `GET/PUT /blocked-sites`
- Persists state to `~/.pomodoro/` as JSON files
- Broadcasts `timerUpdate` and `blockedSitesUpdate` events to all connected clients via Server-Sent Events
- Handles timer expiry and mode progression automatically: work → short break → long break (every 3rd cycle)

**Run:** `npm run dev`

---

## pomodoro-javafx-client

**Java 21 · JavaFX 21 · Maven**

Desktop GUI that connects to `pomodoro-server` and provides a native window for controlling the timer.

- Displays a circular progress ring with countdown
- Mode tabs (Focus / Short Break / Long Break), Start/Pause/Reset controls
- Editable blocked sites list synced with the server
- Stays in sync with other clients via SSE
- Reconnects automatically if the server goes offline

**Run:** `mvn javafx:run`

---

## pomodoro-ui-extension

**React 19 · TypeScript · Vite · Chrome Extension MV3**

Browser extension that connects to `pomodoro-server` and controls the timer from within Chrome.

- Popup UI with the same timer controls and blocked sites editor
- Blocks configured domains using Chrome's Declarative Net Request API when a work session is active
- Communicates with `pomodoro-server` for shared state across clients
- Background service worker manages tab blocking and Chrome alarms/notifications

**Build:** `npm run build` — loads unpacked from `dist/`

---

## How it fits together

```
Chrome Extension  ──┐
                    ├──  REST + SSE  ──  pomodoro-server  ──  ~/.pomodoro/*.json
JavaFX Client    ──┘
```

All clients connect to `http://localhost:4000`. State changes made in one client are immediately reflected in all others through SSE broadcasts.

> **Planned:** The `pomodoro-server` backend will be embedded directly inside the JavaFX application, making it the single process that both runs the UI and serves the API.
