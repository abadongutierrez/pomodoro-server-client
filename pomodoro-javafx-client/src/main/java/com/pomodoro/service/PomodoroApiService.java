package com.pomodoro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomodoro.model.TimerState;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PomodoroApiService {

    private static final String BASE_URL = "http://localhost:4000";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public CompletableFuture<TimerState> getState() {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/timer"))
            .GET()
            .build();
        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> parseTimerState(resp.body()));
    }

    public CompletableFuture<TimerState> patchTimer(String status, String mode) {
        String body;
        if (mode != null) {
            body = "{\"status\":\"" + status + "\",\"mode\":\"" + mode + "\"}";
        } else {
            body = "{\"status\":\"" + status + "\"}";
        }
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/timer"))
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build();
        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> parseTimerState(resp.body()));
    }

    public CompletableFuture<List<String>> getBlockedSites() {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/blocked-sites"))
            .GET()
            .build();
        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> parseSitesList(resp.body()));
    }

    public CompletableFuture<List<String>> putBlockedSites(List<String> sites) {
        String body;
        try {
            body = mapper.writeValueAsString(sites);
        } catch (Exception e) {
            body = "[]";
        }
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/blocked-sites"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> parseSitesList(resp.body()));
    }

    private TimerState parseTimerState(String json) {
        try {
            return mapper.readValue(json, TimerState.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse TimerState: " + json, e);
        }
    }

    private List<String> parseSitesList(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse sites list: " + json, e);
        }
    }
}
