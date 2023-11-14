package com.simtechdata.enums;

public enum MessageType {
    NORMAL,
    MEDIUM,
    ALERT,
    ACTION;

    public String get() {
        return switch(this) {
            case ALERT -> "ALERT";
            case MEDIUM -> "MEDIUM";
            case NORMAL -> "NORMAL";
            case ACTION -> "ACTION";
        };
    }
}
