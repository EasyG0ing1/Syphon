package com.simtechdata.gui;

import com.simtechdata.enums.MessageType;
import com.simtechdata.enums.TabType;
import com.simtechdata.gui.tree.TreeForm;
import com.simtechdata.settings.AppSettings;
import com.simtechdata.utility.*;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import org.jsoup.nodes.Element;

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
    private Button btnTree;
    private Button btnStop;
    private ListView<VBox> listViewLeft;
    private ListView<VBox> listViewRight;
    private Label lblQue;
    private ChoiceBox<String> cbHistory;
    private final ConsoleOutput consoleOutput = new ConsoleOutput();
    //private ConsoleOutput consoleOutput;
    private int jobQueSize = 0;
    private int max = AppSettings.get.threads();
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
        //splitPane.getItems().addAll(vbox);
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.setDividerPosition(0, .71);
    }

    private void makeProgressControls() {
        int top = AppSettings.get.threads();
        int midPoint = top / 2;
        listViewLeft.getItems().clear();
        listViewRight.getItems().clear();
        for (int x = 1; x <= top; x++) {
            ProgressObject po = new ProgressObject();
            po.setPrefWidth(Core.WIDTH * .4);
            po.setPrefHeight(45);
            progressMap.put(x, po);
            if (x <= midPoint) {
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

    private void setHistory() {
        String rawData = AppSettings.get.urlHistory();
        String[] array = rawData.split(";");
        ObservableList<String> list = FXCollections.observableArrayList(Arrays.asList(array));
        Platform.runLater(() -> cbHistory.setItems(list));
    }
    private HBox getURLField() {
        Label label = new Label("URL");
        label.setPrefWidth(50);
        cbHistory = new ChoiceBox<>();
        setHistory();
        cbHistory.setPrefWidth(300);
        cbHistory.setOnAction(e->{
            String url = cbHistory.getValue();
            AppSettings.set.urlHistory(tfURL.getText());
            Platform.runLater(() -> tfURL.setText(url));
        });
        tfURL = newTextField(AppSettings.get.lastURL(), "Full URL to top folder");
        tfURL.setOnAction(e -> start());
        tfURL.disableProperty().bind(stop.not());
        tfURL.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            String url = tfURL.getText();
            AppSettings.set.lastURL(url);
            setHistory();
        }));
        tfURL.setPrefWidth(800);
        btnTree  = new Button("Tree View");
        btnTree.setOnAction(e->{
            String url = tfURL.getText();
            AppSettings.set.lastURL(url);
            Element link = new Element(url).html(url);
            new TreeForm(new Link(link));
        });
        btnTree.setPrefWidth(75);
        btnGo = new Button("Go");
        btnGo.setOnAction(e -> start());
        btnGo.setPrefWidth(55);
        listViewLeft.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> listViewLeft.getSelectionModel().clearSelection());
        listViewRight.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> listViewRight.getSelectionModel().clearSelection());
        EventHandler<Event> consumeHandler = Event::consume;
        listViewLeft.addEventFilter(Event.ANY, consumeHandler);
        listViewRight.addEventFilter(Event.ANY, consumeHandler);
        return newHBox(label, tfURL, cbHistory, btnTree, btnGo);
    }

    private HBox getFolderField() {
        Label label = new Label("Folder");
        label.setPrefWidth(50);
        tfFolder = newTextField(Core.baseFolder, "Path to mirror path structure");
        tfFolder.disableProperty().bind(stop.not());
        tfFolder.setPrefWidth(800);
        Button btnSet = new Button("Choose");
        btnSet.setPrefWidth(75);
        btnSet.setOnAction(e -> getFolder());
        btnSet.disableProperty().bind(stop.not());
        btnStop = new Button("Stop");
        btnStop.setPrefWidth(55);
        btnStop.setOnAction(e -> stop());
        btnStop.visibleProperty().bind(stop.not());
        Label lblSpin = new Label("Download Threads");
        lblSpin.setPrefWidth(110);
        spinThreads = new Spinner<>(1, 20, 1);
        spinThreads.setEditable(false);
        spinThreads.getValueFactory().setValue(AppSettings.get.threads());
        spinThreads.getValueFactory().valueProperty().addListener(((observableValue, oldValue, newValue) -> {
            AppSettings.set.threads(newValue);
            max = newValue;
            makeProgressControls();
        }));
        spinThreads.setPrefWidth(70);
        HBox spinBox = new HBox(5, lblSpin, spinThreads);
        spinBox.setPadding(new Insets(0));
        BooleanBinding spinBind = stop.and(started.not());
        spinBox.visibleProperty().bind(spinBind);
        lblQue = new Label();
        lblQue.setPrefWidth(200);
        AnchorPane ap = new AnchorPane(lblQue, spinBox);
        ap.setPrefWidth(200);
        ap.setPrefHeight(30);
        AnchorPane.setLeftAnchor(lblQue, 0.0);
        AnchorPane.setTopAnchor(lblQue, 0.0);
        AnchorPane.setLeftAnchor(spinBox, 0.0);
        AnchorPane.setTopAnchor(spinBox, 0.0);
        return newHBox(label, tfFolder, btnSet, btnStop, ap);
    }

    private void stop() {
        listViewLeft.setDisable(true);
        listViewRight.setDisable(true);
        stop.setValue(true);
        new Thread(() -> {
            Core.sleep(500);
            log(MessageType.ALERT, "STOPPING", "THREADS", TabType.ERROR);
            log(MessageType.ALERT, "STOPPING", "Clearing job queue", TabType.ERROR);
            while (!jobQue.isEmpty()) {
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
            while (!exec.isShutdown()) {
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
        Log log = new Log(messageType, typeMsg, msg, tabType);
        sendLog(log);
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
        dc.setInitialDirectory(new File(AppSettings.get.lastFolder()));
        File folder = dc.showDialog(null);
        if (folder != null) {
            AppSettings.set.lastFolder(folder.getAbsolutePath());
            tfFolder.setText(folder.getAbsolutePath());
        }
    }

    private long startTime, endTime;
    private int completeCount = 0;

    private TimerTask reportQueSize() {
        return new TimerTask() {
            @Override
            public void run() {
                endTime = System.currentTimeMillis();
                long seconds = (endTime - startTime) / 1000;
                jobQueSize = jobQue.size();
                double total = Core.getTotal();
                double downloaded = Core.getDownloaded();
                double downloadedThisSession = Core.getDownloadedThisSession();
                double progress = downloaded / total;
                double bps = downloadedThisSession / seconds;
                String bpsString = getAmount(bps) + "B/s";
                String totalString = getAmount(total);
                String downloadedString = getAmount(downloaded);
                String percent = String.format("%.2f", progress * 100) + "%";
                String message = "Total Progress - " + downloadedString + " / " + totalString + " = " + percent + " @ " + bpsString;
                Platform.runLater(() -> {
                    progressMap.get(0).setProgress(message, progress);
                    lblQue.setText("Job Que: " + jobQueSize + " (" + Core.getFilesDownloaded() + " downloaded)");
                });
                if (stop.getValue().equals(true) && jobQueSize == 0) {
                    timer.cancel();
                }
                if (progress >= 1.0) {
                    completeCount++;
                }
                else {
                    completeCount = 0;
                }
                if (completeCount > 10) {
                    timer.cancel();
                    stop.setValue(true);
                    Core.reset();
                    Platform.runLater(() -> {
                        progressMap.get(0).setProgress("",0.0);
                        lblQue.setText("Job Que: " + 0 + " (" + 0 + " downloaded)");
                    });
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

    public void addDownload(Link link) {
        if (stop.getValue().equals(true))
            return;
        Download download = new Download(link);
        jobQue.add(download);
        exec.submit(download.start());
    }

    public static void startTreeDownloads() {
        INSTANCE.startSelectDownloads();
    }

    private boolean setDownload = false;
    private void startSelectDownloads() {
        setDownload = true;
        int tSize = spinThreads.getValue();
        int jobSize = Core.downloadSet.size();
        if (initDownloads()) {
            for (Download download : Core.downloadSet) {
                if (stop.getValue().equals(true))
                    return;
                jobQue.add(download);
                exec.submit(download.start());
            }
            Core.downloadSet.clear();
            setDownload = false;
        }
    }

    private boolean initDownloads() {
        jobQue.clear();
        Core.baseFolder = tfFolder.getText();
        if (!Core.baseFolderExists()) {
            log(MessageType.ALERT, "BASE FOLDER GONE", Core.baseFolder, TabType.ERROR);
            return false;
        }
        exec = new ThreadPoolExecutor(max, max, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        stop.setValue(false);
        started.setValue(true);
        timer.scheduleAtFixedRate(reportQueSize(), 1000, 750);
        startTime = System.currentTimeMillis();
        String urlLink = tfURL.getText();
        AppSettings.set.lastURL(urlLink);
        setHistory();
        Platform.runLater(() -> btnGo.setDisable(true));
        return true;
    }

    private void start() {
        if(setDownload) {
            startSelectDownloads();
            return;
        }
        if (initDownloads()) {
            try {
                String urlLink = tfURL.getText();
                URL url = new URL(urlLink);
                if (url.openConnection() != null) {
                    Element element = new Element(urlLink).html(urlLink);
                    Link lnk = new Link(element);
                    Links links = lnk.getLinks();
                    new Thread(() -> {
                        for (Link link : links) {
                            if (stop.getValue().equals(true))
                                return;
                            if (link.isFolder()) {
                                getContent(link);
                            }
                            else if (link.isFile()) {
                                addDownload(link);
                            }
                        }
                    }).start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void getContent(Link src) {
        if (src.isFolder()) {
            Element element = new Element(src.getUrlString()).html(src.getUrlString());
            Link lnk = new Link(element);
            Links links = lnk.getLinks();
            for (Link link : links) {
                if (stop.getValue().equals(true)) {
                    return;
                }
                if (link.isValid()) {
                    if (link.isFolder()) {
                        getContent(link);
                    }
                    else {
                        addDownload(link);
                    }
                }
            }
        }
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

    public static void sendLog(Log log) {
        INSTANCE.consoleOutput.send(log);
    }
}
