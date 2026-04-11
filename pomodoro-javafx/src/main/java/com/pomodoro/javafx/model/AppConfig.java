package com.pomodoro.javafx.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AppConfig {

    @JsonProperty("restoreTabs")
    private final boolean restoreTabs;

    public AppConfig(boolean restoreTabs) {
        this.restoreTabs = restoreTabs;
    }

    public boolean isRestoreTabs() {
        return restoreTabs;
    }
}
