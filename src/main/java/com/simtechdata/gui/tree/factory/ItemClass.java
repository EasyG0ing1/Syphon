package com.simtechdata.gui.tree.factory;

import com.simtechdata.enums.NodeType;
import com.simtechdata.utility.Core;
import com.simtechdata.utility.Download;
import com.simtechdata.utility.Link;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;

import static com.simtechdata.enums.NodeType.*;

public class ItemClass implements Serializable {

    private final String label;
    private final NodeType type;
    private Link link;
    private Download download;
    private int fileSize = -1;
    private int itemCount = -1;
    private int linkCount = -1;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public ItemClass(Link link) {
        this.type = link.isFile() ? FILE : FOLDER;
        this.link = link;
        label = link.getEnd();
        setSelected();
        finish();
    }

    public ItemClass(String label) {
        type = NODE;
        this.label = label;
        setSelected();
    }

    public ItemRecord getRecord() {
        if (link != null)
            return new ItemRecord(link.getUrlString(), label, fileSize, itemCount, type);
        else
            return new ItemRecord(label, fileSize, type);
    }

    public static ItemClass getFromRecord(ItemRecord record) {
        if(record.isFull())
            return new ItemClass(record);
        return new ItemClass(record.getLabel());
    }

    private ItemClass(ItemRecord record) {
        this.fileSize = record.getFileSize();
        this.itemCount = record.getFolderSize();
        this.label = record.getLabel();
        this.type = record.getNodeType();
        this.link = new Link(record.getLink(), this.type);
        setSelected();
    }


    private void setSelected() {
        selected.addListener(((observable, wasChecked, isChecked) -> {
            if (!wasChecked && isChecked) {
                if (download == null) {
                    download = new Download(link);
                }
                if (selected.getValue().equals(true)) {
                    Core.downloadSet.add(download);
                    int count = Core.SELECTED_COUNT.get();
                    long totalBytes = Core.SELECTED_BYTES.getValue();
                    Core.addSelectedCount(1);
                    Core.addBytesSelected(fileSize);
                }
                else {
                    download = null;
                }
            }
            if (wasChecked && !isChecked) {
                if (download != null) {
                    Core.downloadSet.remove(download);
                    download = null;
                    int count = Core.SELECTED_COUNT.get();
                    long totalBytes = Core.SELECTED_BYTES.getValue();
                    Core.addSelectedCount(-1);
                    Core.addBytesSelected(fileSize * -1);
                }
            }
        }));
    }

    private void finish() {
        new Thread(() -> {
            if(type.equals(FILE)) {
                try {
                    Core.sleep(10);
                    if (fileSize == -1) {
                        URL url = new URL(link.getUrlString());
                        URLConnection con = url.openConnection();
                        this.fileSize = con.getContentLength();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                if (link != null) {
                    linkCount = link.getLinks().size();
                    itemCount = linkCount;
                }
            }
        }).start();
    }

    public void setItemCount(int count) {
        itemCount = count;
    }
    public void toggleSelected() {
        selected.setValue(selected.not().getValue());
    }

    public boolean isSelected() {
        return selected.getValue().equals(true);
    }

    public boolean isFile() {
        return type.equals(FILE);
    }

    public boolean isFolder() {
        return type.equals(FOLDER);
    }

    public String getName() {
        return label;
    }

    public Link getLink() {
        return link;
    }

    public String getLabel() {
        return label;
    }
    @Override
    public String toString() {
        String append = "";
        switch(type) {
            case FILE -> {
                if(fileSize > -1) {
                    append = " (" + Core.f(fileSize) + ")";
                }
            }
            case FOLDER -> {
                if(itemCount > -1)
                    append = " (" + itemCount + " items)";
            }
        }
        return label + append;
    }
}
