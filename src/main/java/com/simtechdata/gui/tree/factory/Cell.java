package com.simtechdata.gui.tree.factory;

import com.simtechdata.utility.Core;
import javafx.application.Platform;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.Serializable;

public class Cell extends TreeCell<ItemClass> implements Serializable {
    private final Image FOLDER = Core.FOLDER_IMG;
    private final Image FILE_UNCHECKED = Core.FILE1_IMG;
    private final Image FILE_CHECKED = Core.FILE2_IMG;
    private ImageView iv;

    public Cell() {

        itemProperty().addListener(((observable, oldValue, itemClass) -> {
            if (itemClass != null) {
                if (itemClass.isFile())
                    iv = new ImageView(getItem().isSelected() ? FILE_CHECKED : FILE_UNCHECKED);
                else
                    iv = new ImageView(FOLDER);
            }
        }));

        setOnMouseClicked(e -> {
            if (getItem() != null) {
                if (getItem().isFile()) {
                    getItem().toggleSelected();
                    iv.setImage(getItem().isSelected() ? FILE_CHECKED : FILE_UNCHECKED);
                }
            }
        });

    }

    @Override
    protected void updateItem(ItemClass item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            Platform.runLater(() -> {
                setText(null);
                setGraphic(null);
            });
        }
        else {
            iv.setPreserveRatio(true);
            iv.setFitWidth(15);
            Platform.runLater(() -> {
                setGraphic(iv);
                setText(item.toString());
            });
        }
    }
}
