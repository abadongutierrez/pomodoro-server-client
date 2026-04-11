package com.pomodoro.javafx.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomodoro.javafx.event.BlockedSitesChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class BlockedSitesService {

    private static final Logger log = LoggerFactory.getLogger(BlockedSitesService.class);

    private static final Path SITES_DIR  = Path.of(System.getProperty("user.home"), ".pomodoro");
    private static final Path SITES_FILE = SITES_DIR.resolve("blocked-sites.json");

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private List<String> sites;

    public BlockedSitesService(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.sites = loadSites();
    }

    public synchronized List<String> getSites() {
        return new ArrayList<>(sites);
    }

    public synchronized List<String> setSites(List<String> incoming) {
        sites = new ArrayList<>(incoming);
        persistSites();
        eventPublisher.publishEvent(new BlockedSitesChangedEvent(this, new ArrayList<>(sites)));
        return new ArrayList<>(sites);
    }

    private List<String> loadSites() {
        try {
            if (Files.exists(SITES_FILE)) {
                String json = Files.readString(SITES_FILE);
                List<String> loaded = objectMapper.readValue(json, new TypeReference<List<String>>() {});
                log.info("[blocked-sites] loaded {} site(s) from {}", loaded.size(), SITES_FILE);
                return loaded;
            }
        } catch (IOException e) {
            log.warn("[blocked-sites] could not read {}: {}", SITES_FILE, e.getMessage());
        }
        return new ArrayList<>();
    }

    private void persistSites() {
        try {
            Files.createDirectories(SITES_DIR);
            Files.writeString(SITES_FILE, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sites));
            log.info("[blocked-sites] persisted {} site(s) to {}", sites.size(), SITES_FILE);
        } catch (IOException e) {
            log.error("[blocked-sites] could not write {}: {}", SITES_FILE, e.getMessage());
        }
    }
}
