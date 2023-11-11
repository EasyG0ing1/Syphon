package com.simtechdata.utility;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.jsoup.nodes.Element;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.regex.Pattern;

public class Core {

    private static final Screen SCREEN = Screen.getPrimary();
    private static final Rectangle2D BOUNDS = SCREEN.getVisualBounds();
    private static final DoubleAdder TOTAL_SITE_DATA = new DoubleAdder();
    private static final DoubleAdder TOTAL_DOWNLOADED = new DoubleAdder();
    private static final AtomicLong FILES_DOWNLOADED = new AtomicLong();
    private static final String END_FILE_REGEX = "\\.[A-Za-z0-9_-]+$";
    public static final double SCREEN_HEIGHT = BOUNDS.getHeight();
    public static final double SCREEN_WIDTH = BOUNDS.getWidth();
    public static final double WIDTH = SCREEN_WIDTH * .8;
    public static final double HEIGHT = SCREEN_HEIGHT * .8;
    public static String baseFolder = "";

    public static void addGrandTotal(long value) {
        TOTAL_SITE_DATA.add(value);
    }

    public static void addDownloaded(long value) {
        TOTAL_DOWNLOADED.add(value);
    }

    public static void addFileDownloaded() {
        FILES_DOWNLOADED.addAndGet(1);
    }

    public static long getFilesDownloaded() {
        return FILES_DOWNLOADED.get();
    }

    public static double getTotal() {
        return TOTAL_SITE_DATA.doubleValue();
    }

    public static double getDownloaded() {
        return TOTAL_DOWNLOADED.doubleValue();
    }

    public static boolean isFile(String name) {
        return Pattern.compile(END_FILE_REGEX).matcher(name).find();
    }

    public static boolean isFolder(Element link) {
        return link.wholeText().endsWith("/");
    }

    public static boolean isValidLink(Element link) {
        return !getFullURL(link).isEmpty();
    }

    public static String getFullURL(Element link) {
        String last = link.wholeText();
        String finalLink = "";
        String base = link.baseUri();
        if (last.endsWith("/"))
            finalLink = (base + last).replaceAll(" ", "%20");
        else {
            if (isFile(last))
                finalLink = base + last;
        }
        return finalLink;
    }

    public static String getDownloadLink(Element link) {
        String last = link.wholeText();
        String base = link.baseUri();
        return (base + last).replaceAll(" ", "%20");
    }

    public static void sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException ignored) {
        }
    }
}
