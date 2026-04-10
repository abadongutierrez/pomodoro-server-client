package com.pomodoro.javafx;

import javafx.application.Application;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class JavaFxLauncher implements ApplicationListener<ApplicationReadyEvent> {

    private final ConfigurableApplicationContext applicationContext;

    public JavaFxLauncher(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        JavaFxApplication.applicationContext = applicationContext;

        // Application.launch() is blocking — run on a dedicated thread so the
        // Spring main thread is not permanently blocked.
        Thread fxThread = new Thread(
            () -> Application.launch(JavaFxApplication.class),
            "javafx-launcher"
        );
        fxThread.setDaemon(false);
        fxThread.start();
    }
}
