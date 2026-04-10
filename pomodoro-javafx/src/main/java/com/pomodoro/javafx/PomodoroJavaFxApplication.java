package com.pomodoro.javafx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Must NOT extend javafx.application.Application — JavaFX launcher is
// triggered after Spring context is ready via JavaFxLauncher.
@SpringBootApplication
public class PomodoroJavaFxApplication {

    public static void main(String[] args) {
        SpringApplication.run(PomodoroJavaFxApplication.class, args);
    }
}
