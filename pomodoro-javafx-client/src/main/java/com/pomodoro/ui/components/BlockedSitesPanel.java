package com.pomodoro.ui.components;

import com.pomodoro.ui.Theme;
import com.pomodoro.viewmodel.TimerViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class BlockedSitesPanel extends TitledPane {

    public BlockedSitesPanel(TimerViewModel vm) {
        setText("Blocked Sites");
        setExpanded(false);

        setStyle(
            "-fx-base: #16213e;" +
            "-fx-background-color: #16213e;" +
            "-fx-text-fill: #eeeeee;" +
            "-fx-font-size: 13px;"
        );

        TextArea textArea = new TextArea();
        textArea.setPromptText("one domain per line\nexample.com\ntwitter.com");
        textArea.setWrapText(false);
        textArea.setPrefRowCount(6);
        textArea.setStyle(
            "-fx-control-inner-background: #0f0f23;" +
            "-fx-text-fill: #eeeeee;" +
            "-fx-background-color: #0f0f23;" +
            "-fx-border-color: #333355;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );

        // Populate textarea from blockedSites list (only when not focused)
        vm.getBlockedSites().addListener((javafx.collections.ListChangeListener<String>) change -> {
            if (!textArea.isFocused()) {
                textArea.setText(String.join("\n", vm.getBlockedSites()));
            }
        });

        Text hint = new Text("One domain per line. Changes take effect after saving.");
        hint.setFont(Theme.labelFont());
        hint.setFill(Theme.TEXT_MUTED);

        Button saveBtn = new Button("Save");
        saveBtn.setFont(Theme.buttonFont());
        saveBtn.setStyle(
            "-fx-background-color: #e94560;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-background-radius: 16;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 6 20 6 20;"
        );
        saveBtn.setOnAction(e -> vm.saveBlockedSites(textArea.getText()));

        // Update save button accent when mode changes
        vm.modeProperty().addListener((obs, o, mode) ->
            saveBtn.setStyle(
                "-fx-background-color: " + Theme.accentHexFor(mode) + ";" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 16;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 6 20 6 20;"
            )
        );

        HBox buttons = new HBox(saveBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(8, textArea, hint, buttons);
        content.setPadding(new Insets(8, 0, 8, 0));
        VBox.setVgrow(textArea, Priority.ALWAYS);

        setContent(content);
    }
}
