# Pomodoro

A multi-client Pomodoro timer system. A central backend manages timer state and broadcasts updates to all connected clients via SSE.

```
pomodoro-javafx/          Spring Boot + JavaFX — self-contained server and desktop UI
pomodoro-javafx-client/   Standalone JavaFX desktop client (connects to pomodoro-javafx)
pomodoro-ui-extension/    Chrome browser extension (connects to pomodoro-javafx)
pomodoro-server/          Legacy Node.js/Express backend (replaced by pomodoro-javafx)
```

---

## pomodoro-javafx

**Java 21 · Spring Boot 3.4 · JavaFX 21 · Maven · Port 4000**

The primary application. Runs the embedded HTTP server and the desktop UI in the same process.

- REST API: `GET/PATCH /timer`, `GET /timer/events` (SSE), `GET/PUT /blocked-sites`, `GET /history`
- Persists state to `~/.pomodoro/` as JSON files (compatible with `pomodoro-server` state files)
- Broadcasts `timerUpdate`, `blockedSitesUpdate`, and `configUpdate` SSE events to all connected clients
- Handles timer expiry and Pomodoro mode progression automatically: work → short break → long break (every 3rd cycle)
- Desktop UI with circular progress ring, mode tabs, Start/Pause/Reset controls, and blocked sites panel

**Run:** `mvn javafx:run`

### Configuration (`application.properties`)

| Property | Default | Description |
|---|---|---|
| `server.port` | `4000` | HTTP port |
| `pomodoro.sse.ticks-enabled` | `true` | Broadcast a `timerUpdate` SSE event every second while running. Set to `false` to only broadcast on state transitions. |
| `pomodoro.blocking.restore-tabs` | `false` | When `true`, the browser extension navigates blocked tabs back to their previous page when focus ends. |

---

## pomodoro-javafx-client

**Java 21 · JavaFX 21 · Maven**

Standalone desktop client that connects to `pomodoro-javafx` (or the legacy `pomodoro-server`).

- Circular progress ring, mode tabs, Start/Pause/Reset controls
- Editable blocked sites list synced with the server
- Stays in sync with other clients via SSE; reconnects automatically if the server goes offline

**Run:** `mvn javafx:run` (requires `pomodoro-javafx` running on port 4000)

---

## pomodoro-ui-extension

**React 19 · TypeScript · Vite · Chrome Extension MV3**

Browser extension that connects to `pomodoro-javafx` and controls the timer from within Chrome.

- Popup UI with timer controls and blocked sites editor
- Blocks configured domains using Chrome's Declarative Net Request API during work sessions
- Optionally restores blocked tabs to their previous page when focus ends (controlled by `pomodoro.blocking.restore-tabs`)
- Receives server configuration on connect via the `configUpdate` SSE event
- Background service worker manages tab blocking and Chrome alarms/notifications
- Works offline with local fallback via `chrome.storage.local`

**Build:** `npm run build` — load unpacked from `dist/`

---

## pomodoro-server *(legacy)*

**Node.js · Express · TypeScript · Port 4000**

The original backend, now superseded by `pomodoro-javafx`. Kept for reference.

**Run:** `npm run dev`

---

## How it fits together

```
Chrome Extension  ──┐
                    ├──  REST + SSE  ──  pomodoro-javafx  ──  ~/.pomodoro/*.json
JavaFX Client    ──┘                    (Spring Boot + UI)
```

All clients connect to `http://localhost:4000`. State changes made in one client are immediately reflected in all others through SSE broadcasts.
