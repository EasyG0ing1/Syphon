package com.simtechdata.enums;

public enum MessageType {
    NORMAL,
    MEDIUM,
    ALERT,
    ACTION;

    public static MessageType getType(String typeString) {
        return switch(typeString) {
            case "MEDIUM" -> MEDIUM;
            case "ALERT" -> ALERT;
            case "ACTION" -> ACTION;
            default -> NORMAL;
        };
    }

    public String get() {
        return switch(this) {
            case ALERT -> "ALERT";
            case MEDIUM -> "MEDIUM";
            case NORMAL -> "NORMAL";
            case ACTION -> "ACTION";
        };
    }
}
