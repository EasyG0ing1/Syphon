package com.simtechdata.utility;

import com.simtechdata.enums.TabType;
import com.simtechdata.enums.MessageType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import static com.simtechdata.enums.MessageType.ALERT;
import static com.simtechdata.enums.MessageType.NORMAL;

public class Log {
    private final MessageType messageType;
    private final TabType tabType;
    private final String typeMsg;
    private final String msg;

    public Log(String line, TabType tabType) {
        this.tabType = tabType;
        if(line.contains(";")) {
            String[] parts = line.split(";");
            messageType = MessageType.getType(parts[0]);
            typeMsg = parts[1];
            msg = parts[2];
        }
        else {
            messageType = tabType.equals(TabType.ERROR) ? ALERT : NORMAL;
            typeMsg = "";
            msg = line;
        }
    }

    private String red = "-fx-fill: rgb(255,0,0)";
    private String orange = "-fx-fill: rgb(240,125,0)";
    private String green = "-fx-fill: rgb(0,175,0)";
    private String blue = "-fx-fill: rgb(0,0,255)";
    private String black = "-fx-fill: rgb(0,0,0)";

    private int offset = 15;

    public TextFlow get() {
        int len = typeMsg.length();
        offset = Math.max(len, offset);
        int repeat = typeMsg.isEmpty() ? offset : offset - typeMsg.length();
        String typeString = " ".repeat(repeat) + typeMsg + ": ";
        Text textType = new Text(typeString);
        Text textMsg = new Text(msg);
        Font courier = Font.font("Courier");
        switch(messageType) {
            case NORMAL -> textType.setStyle(blue);
            case MEDIUM -> textType.setStyle(orange);
            case ALERT -> textType.setStyle(red);
            case ACTION -> textType.setStyle(green);
        }
        textType.setFont(courier);
        textType.setWrappingWidth(Double.MAX_VALUE);
        textMsg.setFont(courier);
        textMsg.setStyle(black);
        textMsg.setWrappingWidth(Double.MAX_VALUE);
        TextFlow tf = new TextFlow(textType,textMsg);
        tf.setTextAlignment(TextAlignment.LEFT);
        return tf;
    }

    public TabType getTabType() {
        return tabType;
    }

    public static void l(MessageType messageType, String typeMsg, String msg, TabType tabType) {
        String line = messageType.get() + ";" + typeMsg + ";" + msg + ";" + tabType.get();
        if(messageType.equals(ALERT)) {
            System.err.println(line);
        }
        else {
            System.out.println(line);
        }
    }

    public String getMessage() {
        return msg;
    }
}
