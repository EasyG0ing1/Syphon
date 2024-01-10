package com.simtechdata.gui.tree.factory;

import com.simtechdata.enums.NodeType;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class ItemRecord {


    public ItemRecord(String link, String label, int fileSize, int folderSize, NodeType nodeType) {
        this.link = link;
        this.label = label;
        this.fileSize = fileSize;
        this.folderSize = folderSize;
        this.nodeType = nodeType;
    }

    public ItemRecord(String label, int fileSize, NodeType nodeType) {
        this.label = label;
        this.fileSize = fileSize;
        this.folderSize = -1;
        this.nodeType = nodeType;
    }

    private final LinkedList<ItemRecord> children = new LinkedList<>();
    private final LinkedList<String> repeats = new LinkedList<>();
    private String link;
    private final String label;
    private final int fileSize;
    private final int folderSize;
    private final NodeType nodeType;


    public Set<String> getRepeats() {
        return new HashSet<>(repeats);
    }

    public void setRepeats(Set<String> repeats) {
        this.repeats.clear();
        for(String item : repeats) {
            this.repeats.addLast(item);
        }
    }

    public String getLink() {
        return link;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getFolderSize() {
        return folderSize;
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
        return (link != null && !link.isEmpty()) && !label.isEmpty() && nodeType != null;
    }

}
