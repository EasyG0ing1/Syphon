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

    public ItemClass(String label) {
        type = FOLDER;
        this.label = label;
        finish();
    }

    public ItemClass(Link link, NodeType nodeType) {
        this.type = nodeType;
        this.link = link;
        label = link.getEnd();
        finish();
    }

    private void finish() {
        if(type.equals(FILE)) {
            selected.addListener(((observable, wasChecked, isChecked) -> {
                if (!wasChecked && isChecked) {
                    if (download == null) {
                        download = new Download(link);
                    }
                    if (selected.getValue().equals(true)) {
                        Core.downloadSet.add(download);
                        int count = Core.SELECTED_COUNT.get();
                        long totalBytes = Core.SELECTED_BYTES.getValue();
                        Core.SELECTED_COUNT.setValue(count + 1);
                        Core.SELECTED_BYTES.setValue(totalBytes + fileSize);
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
                        Core.SELECTED_COUNT.setValue(count - 1);
                        Core.SELECTED_BYTES.setValue(totalBytes - fileSize);
                    }
                }
            }));
            new Thread(() -> {
                try {
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
