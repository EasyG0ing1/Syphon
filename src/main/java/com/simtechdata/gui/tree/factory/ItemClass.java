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

import static com.simtechdata.enums.NodeType.FILE;
import static com.simtechdata.enums.NodeType.FOLDER;

public class ItemClass implements Serializable {

    private final String label;
    private String append = "";
    private final NodeType type;
    private Link link;
    private Download download;
    private int fileSize = -1;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public ItemRecord getRecord() {
        if (link != null)
            return new ItemRecord(link.getUrlString(), append, label, fileSize, type);
        else
            return new ItemRecord(append, label, fileSize, type);
    }

    private ItemClass(ItemRecord record) {
        this.append = record.getAppend();
        this.fileSize = record.getFileSize();
        this.label = record.getLabel();
        this.type = record.getNodeType();
        this.link = new Link(record.getLink(), this.type);
        setSelected();
    }

    public static ItemClass getFromRecord(ItemRecord record) {
        if(record.isFull())
            return new ItemClass(record);
        return new ItemClass(record.getLabel());
    }

    public ItemClass(String label) {
        type = FOLDER;
        this.label = label;
        setSelected();
    }

    public ItemClass(Link link, NodeType nodeType) {
        this.type = nodeType;
        this.link = link;
        label = link.getEnd();
        setSelected();
        finish();
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
        if(type.equals(FILE)) {
            new Thread(() -> {
                try {
                    Core.sleep(10);
                    if(fileSize == -1) {
                        URL url = new URL(link.getUrlString());
                        URLConnection con = url.openConnection();
                        this.fileSize = con.getContentLength();
                    }
                    if (this.fileSize >= 0) {
                        String fileSize = Core.f(this.fileSize);
                        append = " (" + fileSize + ")";
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        else {
            new Thread(() -> {
                if(link != null) {
                    int folderSize = link.getLinks().size();
                    append = " (" + folderSize + " items)";
                }
            }).start();
        }
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
        return label + append;
    }
}
