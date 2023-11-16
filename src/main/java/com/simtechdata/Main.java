package com.simtechdata;

import com.simtechdata.gui.GUI;
import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        File file = new File("/Users/michael/temp/whatsap/_chat.txt");
        String fileString = FileUtils.readFileToString(file, Charset.defaultCharset());
        String[] items = fileString.split(" ~ ");
        Map<String, String> chatMap = new HashMap<>();
        LinkedList<ChatItem> chatItems = new LinkedList<>();
        int count = 0;
        int max = items.length;
        int cap = max - 2;
        for (int x = 0; x < items.length; x++) {
            if (x < cap) {
                String dateString = items[x];
                String message = items[x+1];
                if(match(dateString)) {
                    if(dateString.length() > 23)
                        System.out.println(dateString);
                    chatItems.addLast(new ChatItem(dateString, message));
                }
            }
        }
        chatItems.sort(Comparator.comparing(ChatItem::getDateTime));
        for(ChatItem item : chatItems) {
            System.out.println(item);
            //System.out.println(date);
        }
        System.exit(0);
        GUI.show();
    }

    private boolean match(String string) {
        String regex = "\\[[0-9 /,:APM]+]";
        Matcher m = Pattern.compile(regex).matcher(string);
        return m.find();
    }

}
