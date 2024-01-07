module Syphon {
    requires org.apache.commons.io;
    requires org.jsoup;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.prefs;
    requires java.base;
    requires com.google.gson;


    opens com.simtechdata.gui.tree to com.google.gson;
    opens com.simtechdata.gui.tree.factory to com.google.gson;

    exports com.simtechdata;
    exports com.simtechdata.enums;
    exports com.simtechdata.gui;
    exports com.simtechdata.gui.tree;
    exports com.simtechdata.gui.tree.factory;
    exports com.simtechdata.utility;

}
