package com.pomodoro.javafx.service;

import com.pomodoro.javafx.model.AppConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {

    private final boolean restoreTabs;

    public ConfigService(@Value("${pomodoro.blocking.restore-tabs:false}") boolean restoreTabs) {
        this.restoreTabs = restoreTabs;
    }

    public AppConfig getConfig() {
        return new AppConfig(restoreTabs);
    }
}
