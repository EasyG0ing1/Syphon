package com.simtechdata.gui.tree;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simtechdata.enums.OS;
import com.simtechdata.gui.GUI;
import com.simtechdata.gui.tree.factory.Cell;
import com.simtechdata.gui.tree.factory.ItemClass;
import com.simtechdata.gui.tree.factory.ItemRecord;
import com.simtechdata.utility.Core;
import com.simtechdata.utility.Link;
import com.simtechdata.utility.Links;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static com.simtechdata.settings.SETTING.EXCLUDED_EXTENSIONS;
import static com.simtechdata.settings.SETTING.REMOVE_DUPLICATES;

public class TreeForm {

    private TreeItem<ItemClass> root;
    private TreeView<ItemClass> treeView;
    private final Link parentLink;
    private boolean stop = false;
    private File jsonFile;
    private Label lblMsg;
    private Stage stage;
    private Scene scene;
    private CheckBox cbRemoveDupes;
    private VBox vbRight;
    private SplitPane splitPane;
    private final IntegerProperty SELECTED_COUNT = new SimpleIntegerProperty();
    private final LongProperty SELECTED_BYTES = new SimpleLongProperty();
    private ProgressBar pBar;
    private final AtomicInteger totalItemsToAdd = new AtomicInteger(0);
    private final AtomicInteger totalItemsAdded = new AtomicInteger(0);
    private final Timer timer;
    private Set<String> repeats;
    private double width;
    private double height;
    private Label lblItemCount;
    private Label lblTotalSize;
    private boolean finalProgress = true;


    public TreeForm(Link parentLink, boolean skipFileCheck) {
        this.parentLink = parentLink;
        buildControls();
        Platform.runLater(() -> new Thread(show(skipFileCheck)).start());
        timer = new Timer();
        timer.scheduleAtFixedRate(updateProgress(), 1000, 200);
    }

    private void buildControls() {
        this.root     = new TreeItem<>(new ItemClass(""));
        this.treeView = new TreeView<>(root);
        root.setExpanded(true);
        treeView.setShowRoot(false);
        treeView.setCellFactory(param -> new Cell());
        width = Core.SCREEN_WIDTH * .8;
        height = Core.SCREEN_HEIGHT * .9;
        treeView.setPrefWidth(width);
        treeView.setPrefHeight(height);
        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 2) {
                TreeItem<ItemClass> treeItem = treeView.getSelectionModel().getSelectedItem();
                if(treeItem == null)
                    return;
                Platform.runLater(() -> lblMsg.setText("Selecting Items"));
                for (TreeItem<ItemClass> leaf : treeItem.getChildren()) {
                    if (leaf.getValue().isFile())
                        new Thread(() -> leaf.getValue().toggleSelected()).start();
                }
                treeItem.setExpanded(true);
                Platform.runLater(() -> lblMsg.setText("Selection Done"));
            }
        });
        splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().addAll(getLeftVBox(), treeView);
        splitPane.setPrefSize(width, height);
        splitPane.setDividerPosition(0, .18);
        SELECTED_COUNT.bind(Core.SELECTED_COUNT);
        SELECTED_BYTES.bind(Core.SELECTED_BYTES);
        SELECTED_COUNT.addListener(((observable, oldValue, newValue) -> Platform.runLater(() -> lblItemCount.setText(String.valueOf(newValue)))));
        SELECTED_BYTES.addListener(((observable, oldValue, newValue) -> Platform.runLater(() -> lblTotalSize.setText(Core.f((long) newValue)))));
    }

    private void addBranchToItem(TreeItem<ItemClass> treeItem, TreeItem<ItemClass> branch) {
        Platform.runLater(() -> {
            treeItem.getChildren().add(branch);
            totalItemsAdded.incrementAndGet();
        });
    }

    public void buildTree(TreeItem<ItemClass> treeItem, Link topLink) {
        if (topLink == null) return;
        Links links = topLink.getLinks();
        for (Link link : links) {
            if (stop) {
                return;
            }
            if (link.isFile()) {
                Core.addCount(link.getEnd());
            }
            ItemClass itemClass = new ItemClass(link);
            final TreeItem<ItemClass> branch = createTreeItem(itemClass);
            if(itemClass.isFolder())
                setExpandedProperty(branch, link);
            addBranchToItem(treeItem, branch);
        }
        sortTree(treeItem);
    }

    private TreeItem<ItemClass> buildTreeItemFromItemRecord(ItemRecord itemRecord) {
        if(itemRecord != null) {
            ItemClass itemClass = ItemClass.getFromRecord(itemRecord);
            TreeItem<ItemClass> treeItem = createTreeItem(itemClass);
            if (itemClass.isFolder()) {
                setExpandedProperty(treeItem, itemClass.getLink());
            }
            for (ItemRecord childRecord : itemRecord.getChildren()) {
                addBranchToItem(treeItem, buildTreeItemFromItemRecord(childRecord));
            }
            return treeItem;
        }
        return null;
    }

    private void setExpandedProperty(TreeItem<ItemClass> folderItem, Link link) {
        folderItem.expandedProperty().addListener(((observable, wasExpanded, isExpanded) -> {
            if (!wasExpanded && isExpanded) {
                if (folderItem.getChildren().isEmpty()) {
                    new Thread(() -> {
                        Link folderLink = folderItem.getValue().getLink();
                        if (folderLink != null) {
                            final String append = link.getEnd();
                            int itemsToAdd = folderLink.getLinks().size();
                            Platform.runLater(() -> lblMsg.setText("BUILDING BRANCH: " + append));
                            totalItemsToAdd.getAndAdd(itemsToAdd);
                            buildTree(folderItem, folderLink);
                            Platform.runLater(() -> lblMsg.setText("BRANCH BUILT: " + append));
                        }
                    }).start();
                }
            }
        }));
    }

    private TreeItem<ItemClass> createTreeItem(final ItemClass itemClass) {
        return new TreeItem<>(itemClass) {
            private boolean isLeaf;
            private boolean isFirstTimeLeaf = true;

            @Override
            public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    ItemClass f = getValue();
                    isLeaf = f.isFile();
                }
                return isLeaf;
            }
        };
    }

    private void sortTree(TreeItem<ItemClass> topItem) {
        LinkedList<TreeItem<ItemClass>> folderList = new LinkedList<>();
        LinkedList<TreeItem<ItemClass>> fileList = new LinkedList<>();
        ObservableList<TreeItem<ItemClass>> list = FXCollections.observableList(topItem.getChildren());
        for (TreeItem<ItemClass> item : list) {
            if (item.getValue().isFile())
                fileList.addLast(item);
            else
                folderList.addLast(item);
        }
        Platform.runLater(() -> {
            topItem.getChildren().clear();
            topItem.getChildren().addAll(folderList);
            topItem.getChildren().addAll(fileList);
        });
    }

    private void removeDuplicateItems(TreeItem<ItemClass> topItem) {
        int total = topItem.getChildren().size();
        if(total > 0) {
            topItem.getChildren().removeIf(treeItem -> repeats.contains(treeItem.getValue().getLabel()));
            topItem.getValue().setItemCount(topItem.getChildren().size());
            for (TreeItem<ItemClass> treeItem : topItem.getChildren()) {
                if(treeItem.getValue().isFolder())
                    removeDuplicateItems(treeItem);
            }
        }
    }

    private void removeDuplicates() {
        repeats = Core.getRepeatOffenders();
        Platform.runLater(() -> {
            removeDuplicateItems(root);
            sortTree(root);
        });
    }

    private Runnable show(boolean skipFileCheck) {
        return () -> {
            Platform.runLater(() -> {
                stage = new Stage();
                scene = new Scene(splitPane);
                stage.setScene(scene);
                stage.setWidth(width);
                stage.setHeight(height);
                stage.setOnCloseRequest(e -> stop = true);
                stage.show();
            });
            createTree(skipFileCheck);
        };
    }

    private void createTree(boolean skipFileCheck) {
        Core.startMonitor();
        Core.reset();
        totalItemsToAdd.addAndGet(parentLink.getLinks().size());
        jsonFile = new File(OS.getDataFilePath(this.parentLink.getServer() + "_tree.json"));
        if (jsonFile.exists() && !skipFileCheck) {
            loadTreeFromJsonFile();
            Platform.runLater(() -> lblMsg.setText("TREE BUILT FROM PREVIOUS SAVE"));
        }
        else {
                buildTree(root, parentLink);
                Platform.runLater(() -> lblMsg.setText("BASE TREE BUILT"));
        }
    }

    private VBox getLeftVBox() {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(15));
        vbox.setSpacing(20);
        vbox.setAlignment(Pos.CENTER);
        Button btnDupes = newButton("Remove Duplicates");
        btnDupes.setOnAction(e -> removeDuplicates());
        Button btnStart = newButton("Start Download");
        btnStart.setOnAction(e -> {
            stage.close();
            GUI.startTreeDownloads();
        });
        Button btnDupeExclusion = new Button("Set Duplicate Exclusions");
        btnDupeExclusion.setOnAction(e -> setDupeExclusions());
        lblMsg = newLabel("BUILDING TREE ROOT, BE PATIENT");
        lblMsg.setPrefWidth(width * .9);
        lblMsg.setStyle("-fx-font-weight: bold; -fx-font-size: 14");
        Button btnSaveTree = new Button("Save Tree");
        btnSaveTree.setOnAction(e -> saveTreeAsJson());
        Label lblItemInfo = newLabel("Selected:");
        lblItemInfo.setPrefWidth(65);
        lblItemInfo.setAlignment(Pos.CENTER_LEFT);
        lblItemCount = newLabel("");
        lblItemCount.setPrefWidth(45);
        lblTotalSize = newLabel("");
        lblTotalSize.setPrefWidth(125);
        Label lblRemoveDupes = newLabel("Remove Duplicates");
        lblRemoveDupes.setPrefWidth(125);
        cbRemoveDupes = new CheckBox();
        cbRemoveDupes.setSelected(REMOVE_DUPLICATES.getBool());
        cbRemoveDupes.setOnAction(e -> REMOVE_DUPLICATES.setBool(cbRemoveDupes.isSelected()));
        pBar = new ProgressBar(0.0);
        pBar.setPrefWidth(200);
        HBox boxDupes = newHBox(lblRemoveDupes, cbRemoveDupes);
        boxDupes.setPrefWidth(200);
        HBox boxItems = newHBox(lblItemInfo, lblItemCount, lblTotalSize);
        boxItems.setPrefWidth(200);
        boxItems.setAlignment(Pos.CENTER_LEFT);
        vbox.getChildren().addAll(lblMsg, pBar, boxItems, boxDupes, btnDupes, btnStart, btnDupeExclusion, btnSaveTree);
        vbox.setAlignment(Pos.TOP_CENTER);
        return vbox;
    }

    private TimerTask updateProgress() {
        return new TimerTask() {
            @Override
            public void run() {
                double total = totalItemsToAdd.doubleValue();
                double added = totalItemsAdded.doubleValue();
                if(total > 0 && added > 0) {
                    double progress = added / total;
                    if(progress < 1) {
                        Platform.runLater(() -> pBar.setProgress(progress));
                        finalProgress = true;
                    }
                    else {
                        if(finalProgress){
                            Platform.runLater(() -> pBar.setProgress(0.0));
                            if(cbRemoveDupes.isSelected()) {
                                removeDuplicates();
                            }
                            totalItemsToAdd.set(0);
                            totalItemsAdded.set(0);
                            finalProgress = false;
                        }
                    }
                }
            }
        };
    }

    private HBox newHBox(Node... nodes) {
        HBox box = new HBox(10, nodes);
        box.setPadding(new Insets(5));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox getDupesBox() {
        Button btn = newButton("Hide Duplicates");

        Button btnStart = newButton("Start Download");
        btnStart.setOnAction(e -> {

        });
        return new HBox();
    }

    private Button newButton(String label) {
        Button btn = new Button(label);
        btn.setPrefWidth(30 + label.length() * 6);
        btn.setPrefHeight(23);
        return btn;
    }

    private Label newLabel(String text) {
        Label label = new Label(text);
        label.setAlignment(Pos.CENTER_LEFT);
        return label;
    }

    private void setDupeExclusions() {
        double width = 420;
        double height = 270;
        Label label = newLabel("Type file extensions each on a new line and those extensions will not be considered removable duplicates files.");
        label.setPrefWidth(width);
        label.setPrefHeight(40);
        label.setWrapText(true);
        TextArea taExt = new TextArea(EXCLUDED_EXTENSIONS.getString().toLowerCase().replaceAll(";", "\n"));
        taExt.setPrefWidth(width);
        taExt.setPrefHeight(height - 50);
        VBox vbox = new VBox(10, label, taExt);
        vbox.setPadding(new Insets(10));
        Platform.runLater(() -> {
            Stage stage = new Stage();
            Scene scene = new Scene(vbox);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.showAndWait();
            String exclusions = taExt.getText().replaceAll("\\n", ";").replaceAll(";+", ";").replaceAll("[^a-zA-Z0-9_;-]+", "");
            exclusions = (exclusions.endsWith(";")) ? exclusions.substring(0, exclusions.length() - 1) : exclusions;
            EXCLUDED_EXTENSIONS.setString(exclusions);
        });
    }

    private ItemRecord buildItemRecordStructure(TreeItem<ItemClass> treeItem) {
        ItemClass itemClass = treeItem.getValue();
        ItemRecord record = itemClass.getRecord();
        for (TreeItem<ItemClass> child : treeItem.getChildren()) {
            record.addChild(buildItemRecordStructure(child));
        }
        return record;
    }

    public void saveTreeAsJson() {
        lblMsg.setText("SAVING TREE ");
        ItemRecord rootRecord = buildItemRecordStructure(root);
        rootRecord.setRepeats(repeats);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(rootRecord);
        try {
            FileUtils.writeStringToFile(jsonFile, json, Charset.defaultCharset());
            lblMsg.setText("SAVING TREE - DONE");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void buildTreeFromJson(ItemRecord itemRecord) {
        root = buildTreeItemFromItemRecord(itemRecord);
        treeView.setRoot(root);
        treeView.refresh();
        sortTree(root);
    }

    public void loadTreeFromJsonFile() {
        try {
            String json = FileUtils.readFileToString(jsonFile, Charset.defaultCharset());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            ItemRecord rootRecord = gson.fromJson(json, ItemRecord.class);
            Core.setRepeatOffenders(rootRecord.getRepeats());
            buildTreeFromJson(rootRecord);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
