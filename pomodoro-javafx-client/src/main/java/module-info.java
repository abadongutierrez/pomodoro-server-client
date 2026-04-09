module com.pomodoro {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    opens com.pomodoro to javafx.graphics;
    opens com.pomodoro.model to com.fasterxml.jackson.databind;
}
