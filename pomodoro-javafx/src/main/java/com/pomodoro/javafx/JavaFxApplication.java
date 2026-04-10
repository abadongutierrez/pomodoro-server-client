package com.pomodoro.javafx;

import com.pomodoro.javafx.ui.MainStage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    // JavaFX instantiates this class internally via reflection, bypassing Spring.
    // The context is handed over by JavaFxLauncher before Application.launch() is called.
    static ConfigurableApplicationContext applicationContext;

    @Override
    public void start(Stage primaryStage) {
        MainStage mainStage = applicationContext.getBean(MainStage.class);
        mainStage.show(primaryStage);
    }

    @Override
    public void stop() {
        // Close Spring context (stops embedded Tomcat) when window is closed.
        applicationContext.close();
        Platform.exit();
    }
}
