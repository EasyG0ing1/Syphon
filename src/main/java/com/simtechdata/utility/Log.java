package com.simtechdata.utility;

import com.simtechdata.enums.MessageType;
import com.simtechdata.enums.TabType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class Log {
    private final MessageType messageType;
    private final TabType tabType;
    private final String typeMsg;
    private final String msg;

    public Log(MessageType messageType, String typeMsg, String msg, TabType tabType) {
        this.messageType = messageType;
        this.typeMsg = typeMsg;
        this.msg = msg;
        this.tabType = tabType;
    }

    private final String red = "-fx-fill: rgb(255,0,0)";
    private final String orange = "-fx-fill: rgb(240,125,0)";
    private final String green = "-fx-fill: rgb(0,175,0)";
    private final String blue = "-fx-fill: rgb(0,0,255)";
    private final String black = "-fx-fill: rgb(0,0,0)";

    private int offset = 15;

    public TextFlow get() {
        int len = typeMsg.length();
        offset = Math.max(len, offset);
        int repeat = typeMsg.isEmpty() ? offset : offset - typeMsg.length();
        String typeString = " ".repeat(repeat) + typeMsg + ": ";
        Text textType = new Text(typeString);
        Text textMsg = new Text(msg);
        Font courier = Font.font("Courier");
        switch (messageType) {
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
        TextFlow tf = new TextFlow(textType, textMsg);
        tf.setTextAlignment(TextAlignment.LEFT);
        return tf;
    }

    public TabType getTabType() {
        return tabType;
    }

    public String getMessage() {
        return msg;
    }
}
