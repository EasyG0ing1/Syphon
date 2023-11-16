package com.simtechdata;

import com.simtechdata.gui.GUI;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        GUI.show();
    }

    private boolean match(String string) {
        String regex = "\\[[0-9 /,:APM]+]";
        Matcher m = Pattern.compile(regex).matcher(string);
        return m.find();
    }

}
