package com.simtechdata.gui;

import com.simtechdata.enums.MessageType;
import com.simtechdata.enums.TabType;
import com.simtechdata.settings.AppSettings;
import com.simtechdata.utility.Core;
import com.simtechdata.utility.Download;
import com.simtechdata.utility.Log;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class GUI {

    private static final GUI INSTANCE = new GUI();
    private VBox vbox;
    private HBox hboxTop;
    private SplitPane splitPane;
    private TextField tfURL;
    private TextField tfFolder;
    private Button btnGo;
    private Button btnStop;
    private ListView<VBox> listViewLeft;
    private ListView<VBox> listViewRight;
    private Label lblQue;
    private final ConsoleOutput consoleOutput = new ConsoleOutput();
    private int jobQueSize = 0;
    private int max = AppSettings.GET.threads();
    private final Stage stage;
    private boolean ready = false;
    private Spinner<Integer> spinThreads;
    private final CopyOnWriteArrayList<Download> jobQue = new CopyOnWriteArrayList<>();
    private final BooleanProperty stop = new SimpleBooleanProperty(true);
    private final BooleanProperty started = new SimpleBooleanProperty(false);
    private ExecutorService exec;
    private final ExecutorService execStop = new ThreadPoolExecutor(20, 20, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
    private final Map<Integer, ProgressObject> progressMap = new HashMap<>();
    private final Set<Integer> indexSet = new HashSet<>();
    private ProgressObject poTotals;

    private final Timer timer;

    public GUI() {
        this.stage = new Stage();
        makeControls();
        this.timer = new Timer();
    }

    private void makeControls() {
        vbox = new VBox();
        vbox.setPadding(new Insets(5));
        vbox.setSpacing(20);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPrefWidth(Core.WIDTH);
        vbox.setPrefHeight(Core.HEIGHT);


        hboxTop = new HBox();
        hboxTop.setPadding(new Insets(15));
        hboxTop.setPrefWidth(Core.WIDTH);
        hboxTop.setAlignment(Pos.CENTER);
        hboxTop.setSpacing(5);


        listViewLeft = new ListView<>();
        listViewLeft.setPrefWidth(Core.WIDTH * .5);
        listViewLeft.setPrefHeight(Core.HEIGHT * .8);
        listViewLeft.setStyle("-fx-vbar-policy: NEVER; -fx-hbar-policy: NEVER;");

        listViewRight = new ListView<>();
        listViewRight.setPrefWidth(Core.WIDTH * .5);
        listViewRight.setPrefHeight(Core.HEIGHT * .8);
        listViewRight.setStyle("-fx-vbar-policy: NEVER; -fx-hbar-policy: NEVER;");

        poTotals = new ProgressObject();
        progressMap.put(0, poTotals);
        poTotals.setPrefWidth(Core.WIDTH);
        poTotals.setPrefHeight(45);
        makeProgressControls();
        hboxTop.getChildren().addAll(listViewLeft, listViewRight);
        hboxTop.setPrefHeight(Core.HEIGHT * .65);
        vbox.getChildren().addAll(getURLField(), getFolderField(), poTotals, hboxTop);
        vbox.setSpacing(5);
        splitPane = new SplitPane();
        splitPane.getItems().addAll(vbox, consoleOutput);
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.setDividerPosition(0, .71);
    }

    private void makeProgressControls() {
        int top = AppSettings.GET.threads();
        int midPoint = top / 2;
        listViewLeft.getItems().clear();
        listViewRight.getItems().clear();
        for (int x = 1; x <= top; x++) {
            ProgressObject po = new ProgressObject();
            po.setPrefWidth(Core.WIDTH * .4);
            po.setPrefHeight(45);
            progressMap.put(x, po);
            if(x <= midPoint) {
                listViewLeft.getItems().add(po);
            }
            else {
                listViewRight.getItems().add(po);
            }
        }
        ready = true;
    }

    public static void setValues(int index, String label, double progress) {
        if (INSTANCE.stop.getValue().equals(false)) {
            Platform.runLater(() -> INSTANCE.progressMap.get(index).setProgress(label, progress));
        }
    }

    public static void release(int index) {
        Platform.runLater(() -> {
            INSTANCE.progressMap.get(index).clear();
            INSTANCE.indexSet.remove(index);
        });
    }

    public static Integer getIndex() {
        for (int x = 1; x <= INSTANCE.max; x++) {
            if (!INSTANCE.indexSet.contains(x)) {
                INSTANCE.indexSet.add(x);
                return x;
            }
        }
        return null;
    }
    private HBox getURLField() {
        Label label = new Label("URL");
        label.setPrefWidth(50);
        Label lblSpin = new Label("Download Threads");
        lblSpin.setPrefWidth(110);
        spinThreads = new Spinner<>(1,20,1);
        spinThreads.setEditable(false);
        spinThreads.getValueFactory().setValue(AppSettings.GET.threads());
        spinThreads.getValueFactory().valueProperty().addListener(((observableValue, oldValue, newValue) -> {
            AppSettings.SET.threads(newValue);
            max = newValue;
            makeProgressControls();
        }));
        spinThreads.setPrefWidth(70);
        HBox spinBox = new HBox(5, lblSpin, spinThreads);
        spinBox.setPadding(new Insets(0));
        tfURL = newTextField(AppSettings.GET.lastURL(), "Full URL to top folder");
        tfURL.setOnAction(e -> start());
        tfURL.disableProperty().bind(stop.not());
        btnGo = new Button("Go");
        btnGo.setOnAction(e -> start());
        btnGo.setPrefWidth(55);
        lblQue = new Label();
        lblQue.setPrefWidth(200);
        AnchorPane ap = new AnchorPane(lblQue, spinBox);
        ap.setPrefWidth(200);
        ap.setPrefHeight(30);
        AnchorPane.setLeftAnchor(lblQue,0.0);
        AnchorPane.setTopAnchor(lblQue,0.0);
        AnchorPane.setLeftAnchor(spinBox,0.0);
        AnchorPane.setTopAnchor(spinBox,0.0);
        BooleanBinding spinBind = stop.and(started.not());
        spinBox.visibleProperty().bind(spinBind);
        listViewLeft.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> listViewLeft.getSelectionModel().clearSelection());
        listViewRight.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> listViewRight.getSelectionModel().clearSelection());
        EventHandler<Event> consumeHandler = Event::consume;
        listViewLeft.addEventFilter(Event.ANY, consumeHandler);
        listViewRight.addEventFilter(Event.ANY, consumeHandler);
        return newHBox(label, tfURL, btnGo, ap);
    }

    private HBox getFolderField() {
        Label label = new Label("Folder");
        label.setPrefWidth(50);
        tfFolder = newTextField(AppSettings.GET.lastFolder(), "Path to mirror path structure");
        tfFolder.disableProperty().bind(stop.not());
        Button btnSet = new Button("Set");
        btnSet.setPrefWidth(55);
        btnSet.setOnAction(e -> getFolder());
        btnSet.disableProperty().bind(stop.not());
        btnStop = new Button("Stop");
        btnStop.setPrefWidth(55);
        btnStop.setOnAction(e -> stop());
        btnStop.visibleProperty().bind(stop.not());
        return newHBox(label, tfFolder, btnSet, btnStop);
    }

    private void stop() {
        listViewLeft.setDisable(true);
        listViewRight.setDisable(true);
        stop.setValue(true);
        new Thread(() -> {
            Core.sleep(500);
            log(MessageType.ALERT, "STOPPING", "THREADS", TabType.ERROR);
            log(MessageType.ALERT, "STOPPING", "Clearing job queue", TabType.ERROR);
            while(!jobQue.isEmpty()) {
                List<Download> remove = new ArrayList<>();
                for (Download download : jobQue) {
                    execStop.submit(download.stop());
                    remove.add(download);
                }
                for (Download download : remove) {
                    jobQue.remove(download);
                    Core.sleep(7);
                }
            }
            Core.sleep(500);
            log(MessageType.ALERT, "STOPPING", "Shutting down thread pool", TabType.ERROR);
            while(!exec.isShutdown()) {
                exec.shutdownNow();
            }
            exec.close();
            log(MessageType.ALERT, "STOPPING", "Clearing index set", TabType.ERROR);
            for (int x = 1; x <= max; x++) {
                INSTANCE.progressMap.get(x).clear();
                indexSet.remove(x);
            }
            log(MessageType.ALERT, "STOPPING", "Stop tasks complete", TabType.ERROR);
            Platform.runLater(() -> btnStop.requestFocus());
        }).start();
    }

    private void log(MessageType messageType, String typeMsg, String msg, TabType tabType) {
        Log.l(messageType, typeMsg, msg, tabType);
    }

    private TextField newTextField(String text, String promptText) {
        TextField textField = new TextField(text);
        textField.setPromptText(promptText);
        textField.setPrefWidth(Core.WIDTH * .6);
        return textField;
    }

    private HBox newHBox(Node... nodes) {
        HBox hbox = new HBox(5, nodes);
        hbox.setPrefWidth(Core.WIDTH * .8);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(5));
        return hbox;
    }

    private void getFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File(AppSettings.GET.lastFolder()));
        File folder = dc.showDialog(null);
        if (folder != null) {
            AppSettings.SET.lastFolder(folder.getAbsolutePath());
            tfFolder.setText(folder.getAbsolutePath());
        }
    }

    private long startTime, endTime;
    private TimerTask reportQueSize() {
        return new TimerTask() {
            @Override
            public void run() {
                endTime = System.currentTimeMillis();
                long seconds = (endTime - startTime) / 1000;
                jobQueSize = jobQue.size();
                double total = Core.getTotal();
                double downloaded = Core.getDownloaded();
                double progress = downloaded / total;
                double bps = downloaded / seconds;
                String bpsString = getAmount(bps) + "B/s";
                String totalString = getAmount(total);
                String downloadedString = getAmount(downloaded);
                String percent = String.format("%.2f", progress * 100) + "%";
                String message = "Total Progress - " + downloadedString + " / " + totalString + " = " + percent + " @ " + bpsString;
                Platform.runLater(() -> {
                    progressMap.get(0).setProgress(message, progress);
                    lblQue.setText("Job Que: " + jobQueSize + " (" + Core.getFilesDownloaded() + " downloaded)");
                });
                if(stop.getValue().equals(true) && jobQueSize == 0) {
                    timer.cancel();
                }
            }
        };
    }

    private static String getAmount(double totalBytes) {
        double amountRemaining = totalBytes;
        String unit = "B";
        if (amountRemaining > 1000) {
            amountRemaining /= 1000;
            unit = "K";
        }
        if (amountRemaining > 1000) {
            amountRemaining /= 1000;
            unit = "M";
        }
        if (amountRemaining > 1000) {
            amountRemaining /= 1000;
            unit = "G";
        }
        return String.format("%.2f", amountRemaining) + unit;
    }

    public void addDownload(Element link) {
        if (stop.getValue().equals(true))
            return;
        //downloadSizeList.addLast(link);
        Download download = new Download(link);
        jobQue.add(download);
        exec.submit(download.start());
    }

    public static void show() {
        while (!INSTANCE.ready) {
            Core.sleep(10);
        }
        Scene scene = new Scene(INSTANCE.splitPane);
        INSTANCE.stage.setScene(scene);
        INSTANCE.stage.setWidth(Core.WIDTH);
        INSTANCE.stage.setHeight(Core.SCREEN_HEIGHT - 50);
        INSTANCE.stage.setOnCloseRequest(e -> System.exit(0));
        INSTANCE.stage.show();
    }

    private void start() {
        exec = new ThreadPoolExecutor(max, max, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        stop.setValue(false);
        started.setValue(true);
        timer.scheduleAtFixedRate(reportQueSize(), 1000, 750);
        startTime = System.currentTimeMillis();
        Platform.runLater(() -> btnGo.setDisable(true));
        String urlFolder = tfFolder.getText();
        if (!urlFolder.endsWith("/"))
            urlFolder += "/";
        Core.baseFolder = urlFolder;
        try {
            stop.setValue(false);
            String urlString = tfURL.getText();
            AppSettings.SET.lastURL(urlString);
            URL url = new URL(urlString);
            if (url.openConnection() != null) {
                Document doc = Jsoup.connect(urlString).get();
                Elements links = doc.select("a[href]");
                LinkedList<Element> list = new LinkedList<>(links);
                list.sort(Comparator.comparing(Element::wholeText).reversed());
                new Thread(() -> {
                    for (Element link : list) {
                        if (stop.getValue().equals(true))
                            return;
                        if (Core.isFolder(link)) {
                            getFolders(link);
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void getFolders(Element element) {
        if (Core.isFolder(element)) {
            try {
                Document doc = Jsoup.connect(Core.getFullURL(element)).get();
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    if (stop.getValue().equals(true))
                        return;
                    if (Core.isValidLink(link)) {
                        if (Core.isFolder(link)) {
                            getFolders(link);
                        }
                        else {
                            addDownload(link);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
