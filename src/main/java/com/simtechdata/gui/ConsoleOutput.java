package com.simtechdata.gui;

import com.simtechdata.enums.TabType;
import com.simtechdata.utility.Core;
import com.simtechdata.utility.Log;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.TextFlow;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConsoleOutput extends AnchorPane {

    public boolean autoScroll = true;
    private final int MAX_ITEMS = 2000;
    private final Tab tabError = new Tab("Error");
    private final Tab tabFinished = new Tab("Complete");
    private final Tab tabCanceled = new Tab("Canceled");
    private final TabPane tabPane;
    private boolean errorState = false;
    private final ListView<TextFlow> lvError = new ListView<>();
    private final ListView<TextFlow> lvFinished = new ListView<>();
    private final ListView<TextFlow> lvCanceled = new ListView<>();
    private final LinkedList<String> listFINISHED = new LinkedList<>();
    private final LinkedList<String> listCANCELED = new LinkedList<>();
    private final LinkedList<String> listERROR = new LinkedList<>();
    private final ExecutorService execLog = new ThreadPoolExecutor(20, 20, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
    private final double width;
    private final double height;

    public ConsoleOutput() {
        super();
        tabError.setClosable(false);
        tabFinished.setClosable(false);
        tabCanceled.setClosable(false);
        tabPane = new TabPane(tabFinished, tabCanceled, tabError);
        width = Core.WIDTH;
        height = Core.SCREEN_HEIGHT - Core.HEIGHT - 100;

        setWidth(width);
        setHeight(height);

        init(lvFinished);
        init(lvCanceled);
        init(lvError);

        tabFinished.setContent(lvFinished);
        tabCanceled.setContent(lvCanceled);
        tabError.setContent(lvError);
        lvError.setOnMouseEntered(e -> errorState = false);

        Button btnClipboard = getClipboardButton();

        getChildren().addAll(tabPane, btnClipboard);

        setLeftAnchor(tabPane, 0.0);
        setTopAnchor(tabPane, 0.0);
        setRightAnchor(tabPane, 0.0);
        setBottomAnchor(tabPane, 0.0);

        setRightAnchor(btnClipboard, 35.0);
        setBottomAnchor(btnClipboard, 25.0);

        tabPane.setOnMouseClicked(e -> {
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            if (tab == tabError) {
                errorState = false;
            }
        });
    }

    private Button getClipboardButton() {
        Button btnClipboard = new Button("Clipboard");
        btnClipboard.setOpacity(.01);
        btnClipboard.setOnMouseEntered(e -> btnClipboard.setOpacity(1.0));
        btnClipboard.setOnMouseExited(e -> btnClipboard.setOpacity(.01));
        btnClipboard.setPrefWidth(100);
        btnClipboard.setPrefHeight(45);
        btnClipboard.toFront();
        btnClipboard.setOnAction(e -> {
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            if (tab == tabError) toClipboard(listERROR);
            if (tab == tabFinished) toClipboard(listFINISHED);
            if (tab == tabCanceled) toClipboard(listCANCELED);
        });
        return btnClipboard;
    }

    private void init(ListView<TextFlow> listView) {
        listView.setPrefWidth(width);
        listView.setPrefHeight(height);
        listView.setOnMouseExited(e -> autoScroll = false);
        listView.setOnScroll(e -> autoScroll = false);
        listView.setOnSwipeUp(e -> autoScroll = false);
    }

    private void toClipboard(LinkedList<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String line : list) {
            sb.append(line).append("\n");
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);
    }

    private Runnable flashErrorTab() {
        return () -> {
            errorState = true;
            String label = tabError.getText();
            while (errorState) {
                for (int x = 0; x < 3; x++) {
                    Platform.runLater(() -> tabError.setText("-".repeat(label.length())));
                    Core.sleep(700);
                    Platform.runLater(() -> tabError.setText(label));
                    Core.sleep(700);
                }
                Core.sleep(10000);
            }
        };
    }

    public void add(Log log) {
        execLog.submit(() -> {
            TextFlow tf = log.get();
            TabType tabType = log.getTabType();
            if (tabType == null)
                return;
            switch (tabType) {
                case FINISHED -> setLogMessage(log, tf, lvFinished, listFINISHED);
                case CANCELED -> setLogMessage(log, tf, lvCanceled, listCANCELED);
                case ERROR -> {
                    setLogMessage(log, tf, lvError, listERROR);
                    if (!errorState) {
                        new Thread(flashErrorTab()).start();
                    }
                }
            }
        });
    }

    private void setLogMessage(Log log, TextFlow tf, ListView<TextFlow> lvError, LinkedList<String> listERROR) {
        Platform.runLater(() -> lvError.getItems().add(tf));
        listERROR.addLast(log.getMessage());
        if (autoScroll) {
            Platform.runLater(() -> lvError.scrollTo(tf));
        }
        if (lvError.getItems().size() > MAX_ITEMS) {
            int count = lvError.getItems().size() - MAX_ITEMS;
            Platform.runLater(() -> lvError.getItems().subList(0, count).clear());
        }
    }

    public void send(Log log) {
        add(log);
    }
}
