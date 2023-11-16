package com.simtechdata.gui.tree;

import com.simtechdata.gui.GUI;
import com.simtechdata.gui.tree.factory.Cell;
import com.simtechdata.gui.tree.factory.ItemClass;
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

import java.util.LinkedList;
import java.util.Set;

import static com.simtechdata.enums.NodeType.FILE;
import static com.simtechdata.enums.NodeType.FOLDER;
import static com.simtechdata.settings.SETTING.EXCLUDED_EXTENSIONS;
import static com.simtechdata.settings.SETTING.REMOVE_DUPLICATES;

public class TreeForm {

    private final TreeItem<ItemClass> root;
    private final TreeView<ItemClass> treeView;
    private final Link parentLink;
    private boolean stop = false;
    private Label lblMsg;
    private Stage stage;
    private Scene scene;
    private CheckBox cbRemoveDupes;
    private VBox vbRight;
    private SplitPane splitPane;
    private final IntegerProperty SELECTED_COUNT = new SimpleIntegerProperty();
    private final LongProperty SELECTED_BYTES = new SimpleLongProperty();
    private ProgressBar pBar;
    private double totalItems = 0.0;
    private double itemsAdded = 0.0;
    private String activeThread = "";


    public TreeForm(Link parentLink) {
        this.parentLink = parentLink;
        this.root = new TreeItem<>(new ItemClass(""));
        this.treeView = new TreeView<>(root);
        buildControls();
        new Thread(show()).start();
    }

    private void buildControls() {
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

    private Runnable selectFiles(TreeItem<ItemClass> treeItem) {
        return () -> {
            for (TreeItem<ItemClass> leaf : treeItem.getChildren()) {
                if (leaf.getValue().isFile())
                    leaf.getValue().toggleSelected();
                bumpProgress();
                Core.sleep(10);
            }
        };
    }

    public void buildTree(TreeItem<ItemClass> treeItem, Link topLink) {
        Links links = topLink.getLinks();
        boolean dontAddDuplicates = cbRemoveDupes.isSelected();
        for (Link link : links) {
            if (stop)
                return;
            if (link.isFile()) {
                String name = link.getEnd();
                Core.addCount(name);
                repeats = Core.getRepeatOffenders();
                boolean isDuplicate = repeats.contains(name);
                if (isDuplicate && dontAddDuplicates) {
                    continue;
                }
                bumpProgress();
                TreeItem<ItemClass> fileItem = createTreeItem(new ItemClass(link, FILE));
                Platform.runLater(() -> treeItem.getChildren().add(fileItem));
            }
            else {
                TreeItem<ItemClass> folderItem = createTreeItem(new ItemClass(link, FOLDER));
                folderItem.expandedProperty().addListener(((observable, oldValue, newValue) -> {
                    if (!oldValue && newValue) {
                        new Thread(() -> {
                            Link lnk = folderItem.getValue().getLink();
                            if (folderItem.getChildren().isEmpty()) {
                                Platform.runLater(() -> lblMsg.setText("BUILDING BRANCH: " + link.getEnd()));
                                if (pBar.getProgress() == 0) {
                                    activeThread = Thread.currentThread().getName();
                                    itemsAdded = 0.0;
                                    totalItems = folderItem.getValue().getLink().getLinks().size();
                                }
                                buildTree(folderItem, lnk);
                                clearProgressBar();
                                Platform.runLater(() -> lblMsg.setText("BRANCH BUILT: " + link.getEnd()));
                            }
                        }).start();
                    }
                }));
                bumpProgress();
                Platform.runLater(() -> treeItem.getChildren().add(folderItem));
            }
        }
        if (dontAddDuplicates)
            removeDuplicates(treeItem);
        sortTree(treeItem);
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
        topItem.getChildren().removeIf(treeItem -> repeats.contains(treeItem.getValue().getLabel()));
        for (TreeItem<ItemClass> treeItem : topItem.getChildren()) {
            removeDuplicateItems(treeItem);
        }
    }

    private Set<String> repeats;

    private void removeDuplicates(TreeItem<ItemClass> treeItem) {
        repeats = Core.getRepeatOffenders();
        Platform.runLater(() -> removeDuplicateItems(treeItem));
    }

    private double width;
    private double height;

    private Runnable show() {
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
            Core.startMonitor();
            itemsAdded = 0.0;
            totalItems = parentLink.getLinks().size();
            activeThread = Thread.currentThread().getName();
            buildTree(root, parentLink);
            clearProgressBar();
            Platform.runLater(() -> lblMsg.setText("BASE TREE BUILT"));
        };
    }


    private VBox getLeftVBox() {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(15));
        vbox.setSpacing(20);
        vbox.setAlignment(Pos.CENTER);
        Button btnDupes = newButton("Remove Duplicates");
        btnDupes.setOnAction(e -> removeDuplicates(root));
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
        vbox.getChildren().addAll(lblMsg, pBar, boxItems, boxDupes, btnDupes, btnStart, btnDupeExclusion);
        vbox.setAlignment(Pos.TOP_CENTER);
        return vbox;
    }

    private void bumpProgress() {
        if (Thread.currentThread().getName().equals(activeThread)) {
            itemsAdded++;
            double progress = itemsAdded / totalItems;
            Platform.runLater(() -> pBar.setProgress(progress));
        }
    }

    private void clearProgressBar() {
        Platform.runLater(() -> pBar.setProgress(0.0));
    }

    private Label lblItemCount;
    private Label lblTotalSize;

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
            String exclusions = taExt.getText().replaceAll("\\n", ";").replaceAll(";+", ";").replaceAll("[^a-zA-Z0-9;]+", "");
            exclusions = (exclusions.endsWith(";")) ? exclusions.substring(0, exclusions.length() - 1) : exclusions;
            EXCLUDED_EXTENSIONS.setString(exclusions);
        });
    }
}
