package com.simtechdata;

import com.simtechdata.gui.GUI;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {


    private final String coreURL = "https://lms.onnocenter.or.id/pustaka/";
    private GUI gui;

    public static void main(String[] args) throws IOException {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        GUI.show();
    }
}
