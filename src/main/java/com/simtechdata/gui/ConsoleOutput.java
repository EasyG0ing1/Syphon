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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.concurrent.*;

public class ConsoleOutput extends AnchorPane {

    public boolean autoScroll = true;
    private int MAX_ITEMS = 2000;
    private Tab tabError = new Tab("Error");
    private Tab tabFinished = new Tab("Complete");
    private Tab tabCanceled = new Tab("Canceled");
    private TabPane tabPane;
    private boolean errorState = false;
    private ListView lvError = new ListView();
    private ListView lvFinished = new ListView();
    private ListView lvCanceled = new ListView();
    private LinkedList<String> listFINISHED = new LinkedList<>();
    private LinkedList<String> listCANCELED = new LinkedList<>();
    private LinkedList<String> listERROR = new LinkedList<>();
    private ExecutorService execLog = new ThreadPoolExecutor(20, 20, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
    private double width;
    private double height;

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
        lvError.setOnMouseEntered(e -> {
            errorState = false;
        });

        Button btnClipboard = getClipboardButton();

        getChildren().addAll(tabPane, btnClipboard);

        setLeftAnchor(tabPane, 0.0);
        setTopAnchor(tabPane, 0.0);
        setRightAnchor(tabPane, 0.0);
        setBottomAnchor(tabPane, 0.0);

        setRightAnchor(btnClipboard, 35.0);
        setBottomAnchor(btnClipboard, 25.0);


        setOutput();

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

    private void init(ListView listView) {
        listView.setPrefWidth(width);
        listView.setPrefHeight(height);
        listView.setOnMouseExited(e -> {
            autoScroll = false;
        });
        listView.setOnScroll(e -> {
            autoScroll = false;
        });
        listView.setOnSwipeUp(e -> {
            autoScroll = false;
        });
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

    private void setOutput() {
        System.setOut(new PrintStream(new CustomOutputStream(this, false)));
        System.setErr(new PrintStream(new CustomOutputStream(this, true)));
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
                case FINISHED -> {
                    setLogMessage(log, tf, lvFinished, listFINISHED);
                }
                case CANCELED -> {
                    setLogMessage(log, tf, lvCanceled, listCANCELED);
                }
                case ERROR -> {
                    setLogMessage(log, tf, lvError, listERROR);
                    if (!errorState) {
                        new Thread(flashErrorTab()).start();
                    }
                }
            }
        });
    }

    private void setLogMessage(Log log, TextFlow tf, ListView lvError, LinkedList<String> listERROR) {
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



    public static class CustomOutputStream extends OutputStream {
        public CustomOutputStream(ConsoleOutput ap, boolean error) {
            this.ap = ap;
            this.error = error;
        }

        private final ConsoleOutput ap;
        private static StringBuilder sb = new StringBuilder();
        private final boolean error;
        private ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        @Override
        public void write(int b) {
            executor.submit(() -> {
                if (b == 10) {
                    String line = sb.toString();
                    if (line.contains(";")) {
                        String tabTypeString = line.split(";")[3];
                        if (tabTypeString != null) {
                            TabType tabType = error ? TabType.ERROR : TabType.getType(tabTypeString);
                            Log log = new Log(sb.toString(), tabType);
                            ap.add(log);
                        }
                    }
                    else {
                        TabType tabType = TabType.ERROR;
                        Log log = new Log(sb.toString(), tabType);
                        ap.add(log);
                    }
                    sb = new StringBuilder();
                }
                else {
                    sb.append((char) b);
                }
            });
        }
    }
}
