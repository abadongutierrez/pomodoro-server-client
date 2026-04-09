package com.pomodoro;

import com.pomodoro.service.PomodoroApiService;
import com.pomodoro.service.SseService;
import com.pomodoro.ui.MainWindow;
import com.pomodoro.viewmodel.TimerViewModel;
import javafx.application.Application;
import javafx.stage.Stage;

public class PomodoroApp extends Application {

    private SseService sseService;

    @Override
    public void start(Stage stage) {
        PomodoroApiService apiService = new PomodoroApiService();
        TimerViewModel viewModel = new TimerViewModel(apiService);

        sseService = new SseService(
            viewModel::applyServerState,
            viewModel::applyBlockedSites,
            () -> viewModel.setServerOnline(false)
        );

        MainWindow window = new MainWindow(viewModel, stage);
        window.show();

        sseService.start();
    }

    @Override
    public void stop() {
        if (sseService != null) {
            sseService.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
