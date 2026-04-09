package com.pomodoro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomodoro.model.TimerState;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SseService {

    private static final String SSE_URL = "http://localhost:4000/timer/events";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Consumer<TimerState> onTimerUpdate;
    private final Consumer<List<String>> onSitesUpdate;
    private final Runnable onOffline;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-reconnect");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean destroyed = false;

    public SseService(Consumer<TimerState> onTimerUpdate,
                      Consumer<List<String>> onSitesUpdate,
                      Runnable onOffline) {
        this.onTimerUpdate = onTimerUpdate;
        this.onSitesUpdate = onSitesUpdate;
        this.onOffline = onOffline;
    }

    public void start() {
        scheduler.submit(this::connect);
    }

    public void stop() {
        destroyed = true;
        scheduler.shutdownNow();
    }

    private void connect() {
        if (destroyed) return;
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(SSE_URL))
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .GET()
            .build();

        httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofLines())
            .thenAccept(resp -> consumeStream(resp.body()))
            .exceptionally(ex -> {
                scheduleReconnect();
                return null;
            });
    }

    private void consumeStream(java.util.stream.Stream<String> lines) {
        String[] eventName = {null};
        try {
            for (String line : (Iterable<String>) lines::iterator) {
                if (destroyed) break;
                if (line.startsWith(":")) continue; // keep-alive comment
                if (line.startsWith("event:")) {
                    eventName[0] = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    dispatch(eventName[0], data);
                    eventName[0] = null;
                }
                // blank lines are event separators — already reset above
            }
            scheduleReconnect(); // stream ended cleanly
        } catch (Exception e) {
            scheduleReconnect();
        }
    }

    private void dispatch(String eventName, String data) {
        try {
            if ("timerUpdate".equals(eventName)) {
                TimerState state = mapper.readValue(data, TimerState.class);
                onTimerUpdate.accept(state);
            } else if ("blockedSitesUpdate".equals(eventName)) {
                List<String> sites = mapper.readValue(data, new TypeReference<List<String>>() {});
                onSitesUpdate.accept(sites);
            }
        } catch (Exception e) {
            // Malformed event — skip silently
        }
    }

    private void scheduleReconnect() {
        if (destroyed) return;
        onOffline.run();
        scheduler.schedule(this::connect, 3, TimeUnit.SECONDS);
    }
}
