package com.simtechdata.enums;

public enum TabType {
    ERROR,
    FINISHED,
    CANCELED;

    public static TabType getType(String tabType) {
        return switch (tabType) {
            case "ZERO" -> ERROR;
            case "FINISHED" -> FINISHED;
            case "CANCELED" -> CANCELED;
            default -> null;
        };
    }

    public String get() {
        return switch (this) {
            case ERROR -> "ZERO";
            case FINISHED -> "FINISHED";
            case CANCELED -> "CANCELED";
        };
    }
}
