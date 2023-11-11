package com.simtechdata.gui;

import com.simtechdata.utility.Core;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProgressObject extends VBox {

    private Label lblFile;
    private ProgressBar pBar;
    private final double height = 40;

    public ProgressObject() {
        pBar = new ProgressBar();
        pBar.setProgress(0.0);
        pBar.setPrefWidth(Core.WIDTH * .968);
        pBar.setPrefHeight(height / 3);
        lblFile = new Label("WAITING");
        lblFile.setPrefWidth(Core.WIDTH * .968);
        lblFile.setPrefHeight(height / 2);
        getChildren().addAll(lblFile, pBar);
        setPadding(new Insets(0));
        setSpacing(5);
        setAlignment(Pos.CENTER);
        setWidth(Core.WIDTH * .85);
        setHeight(height);
        VBox.setVgrow(lblFile, Priority.ALWAYS);
        VBox.setVgrow(pBar, Priority.ALWAYS);
    }

    public void setProgress(String label, double progress) {
        Platform.runLater(() -> {
            lblFile.setText(label);
            pBar.setProgress(progress);
        });
    }

    public void clear() {
        lblFile.setText("WAITING");
        pBar.setProgress(0.0);
    }
}
