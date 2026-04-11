package com.pomodoro.javafx.event;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class BlockedSitesChangedEvent extends ApplicationEvent {

    private final List<String> sites;

    public BlockedSitesChangedEvent(Object source, List<String> sites) {
        super(source);
        this.sites = sites;
    }

    public List<String> getSites() {
        return sites;
    }
}
