package com.simtechdata;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatItem {

    private final String dateString;
    private final String message;
    private final LocalDateTime dateTime;

    public ChatItem(String dateString, String message) {
        this.dateString = dateString;
        this.message = message;
        this.dateTime = convertDateTime();
    }

    private LocalDateTime convertDateTime() {
        String date = dateString.replace("[", "").replace("]", "");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[MM/dd/yy, h:mm:ss a]");
        return LocalDateTime.parse(date, formatter);
    }

    public String getDateString() {
        return dateString;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    @Override
    public String toString() {
        return dateString + ": " + message;
    }
}
