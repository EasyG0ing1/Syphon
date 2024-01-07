package com.simtechdata.gui.tree.factory;

import com.simtechdata.enums.NodeType;

import java.util.LinkedList;

public class ItemRecord {


    public ItemRecord(String link, String append, String label, int fileSize, NodeType nodeType) {
        this.link = link;
        this.append = append;
        this.label = label;
        this.fileSize = fileSize;
        this.nodeType = nodeType;
    }

    public ItemRecord(String append, String label, int fileSize, NodeType nodeType) {
        this.append = append;
        this.label = label;
        this.fileSize = fileSize;
        this.nodeType = nodeType;
    }

    private final LinkedList<ItemRecord> children = new LinkedList<>();
    private String link;
    private final String label;
    private final String append;
    private final int fileSize;
    private final NodeType nodeType;


    public String getLink() {
        return link;
    }

    public String getAppend() {
        return append;
    }

    public int getFileSize() {
        return fileSize;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void addChild(ItemRecord child) {
        children.addLast(child);
    }

    public String getLabel() {
        return label;
    }

    public LinkedList<ItemRecord> getChildren() {
        return children;
    }

    public boolean isFull() {
        return (link != null && !link.isEmpty()) && !label.isEmpty() && append != null && nodeType != null;
    }

}
