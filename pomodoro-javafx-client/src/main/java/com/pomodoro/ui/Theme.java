package com.pomodoro.ui;

import com.pomodoro.model.TimerMode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public final class Theme {

    public static final Color BG           = Color.web("#1a1a2e");
    public static final Color SURFACE      = Color.web("#16213e");
    public static final Color SURFACE_DARK = Color.web("#0f0f23");
    public static final Color WORK_ACCENT  = Color.web("#e94560");
    public static final Color SHORT_ACCENT = Color.web("#4ade80");
    public static final Color LONG_ACCENT  = Color.web("#60a5fa");
    public static final Color TEXT_PRIMARY = Color.web("#eeeeee");
    public static final Color TEXT_MUTED   = Color.web("#888888");

    private Theme() {}

    public static Color accentFor(TimerMode mode) {
        return switch (mode) {
            case WORK  -> WORK_ACCENT;
            case SHORT -> SHORT_ACCENT;
            case LONG  -> LONG_ACCENT;
        };
    }

    public static String accentHexFor(TimerMode mode) {
        return switch (mode) {
            case WORK  -> "#e94560";
            case SHORT -> "#4ade80";
            case LONG  -> "#60a5fa";
        };
    }

    public static Font timerFont() {
        return Font.font("System", FontWeight.BOLD, 48);
    }

    public static Font labelFont() {
        return Font.font("System", FontWeight.NORMAL, 13);
    }

    public static Font buttonFont() {
        return Font.font("System", FontWeight.SEMI_BOLD, 14);
    }

    /** Convert a Color to a CSS hex string like #rrggbb */
    public static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int) (c.getRed() * 255),
            (int) (c.getGreen() * 255),
            (int) (c.getBlue() * 255));
    }
}
