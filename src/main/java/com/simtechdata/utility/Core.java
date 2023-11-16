package com.simtechdata.utility;

import com.simtechdata.enums.OS;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

import static com.simtechdata.settings.SETTING.*;

public class Core {

    private static final String FILE1_PNG = Core.class.getResource("/File1.png").toExternalForm();
    private static final String FILE2_PNG = Core.class.getResource("/File2.png").toExternalForm();
    private static final String FOLDER_PNG = Core.class.getResource("/Folder.png").toExternalForm();
    public static Image FOLDER_IMG = new Image(FOLDER_PNG);
    public static Image FILE1_IMG = new Image(FILE1_PNG);
    public static Image FILE2_IMG = new Image(FILE2_PNG);
    private static final Screen SCREEN = Screen.getPrimary();
    private static final Rectangle2D BOUNDS = SCREEN.getVisualBounds();
    private static final DoubleAdder TOTAL_SITE_DATA = new DoubleAdder();
    private static final DoubleAdder TOTAL_DOWNLOADED = new DoubleAdder();
    private static final DoubleAdder DOWNLOADED_THIS_SESSION = new DoubleAdder();
    private static final AtomicLong FILES_DOWNLOADED = new AtomicLong();
    public static final double SCREEN_HEIGHT = BOUNDS.getHeight();
    public static final double SCREEN_WIDTH = BOUNDS.getWidth();
    public static final double WIDTH = SCREEN_WIDTH * .8;
    public static final double HEIGHT = SCREEN_HEIGHT * .8;
    public static String baseFolder = LAST_FOLDER.getString();
    public static final IntegerProperty SELECTED_COUNT = new SimpleIntegerProperty(0);
    public static final LongProperty SELECTED_BYTES = new SimpleLongProperty(0);
    private static final AtomicLong Selected_Count = new AtomicLong();
    private static final AtomicLong Selected_Bytes = new AtomicLong();
    private static final Map<String, Integer> countMap = new HashMap<>();
    public static Set<Download> downloadSet = new HashSet<>();

    private static boolean monitorStarted = false;

    public static void startMonitor() {
        if (!monitorStarted) {
            new Thread(() -> {
                while (true) {
                    SELECTED_BYTES.setValue(Selected_Bytes.get());
                    SELECTED_COUNT.setValue(Selected_Count.get());
                    sleep(500);
                }
            }).start();
            monitorStarted = true;
        }
    }

    public static void addSelectedCount(long num) {
        Selected_Count.addAndGet(num);
    }

    public static void addBytesSelected(long num) {
        Selected_Bytes.addAndGet(num);
    }

    public static void reset() {
        FILES_DOWNLOADED.set(0);
        DOWNLOADED_THIS_SESSION.reset();
        TOTAL_DOWNLOADED.reset();
        TOTAL_SITE_DATA.reset();
    }

    public static void addGrandTotal(long value) {
        TOTAL_SITE_DATA.add(value);
    }

    public static void addDownloaded(long value) {
        TOTAL_DOWNLOADED.add(value);
    }

    public static void addDownloadedThisSession(long value) {
        DOWNLOADED_THIS_SESSION.add(value);
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

    public static boolean baseFolderExists() {
        if (baseFolder.isEmpty())
            return false;
        return Paths.get(baseFolder).toFile().exists();
    }

    public static double getDownloaded() {
        return TOTAL_DOWNLOADED.doubleValue();
    }

    public static double getDownloadedThisSession() {
        return DOWNLOADED_THIS_SESSION.doubleValue();
    }

    private static String getURLString(Element link) {
        String baseURI = link.baseUri();
        if (baseURI.endsWith("/")) {
            baseURI = baseURI.substring(0, baseURI.length() - 1);
        }
        String fullURL = baseURI + "/" + link.wholeText();
        if (fullURL.charAt(0) == '/')
            return fullURL.replaceFirst("/", "");
        return fullURL;
    }

    public static void addCount(String name) {
        String ext = FilenameUtils.getExtension(name).toLowerCase();
        Set<String> excludes = EXCLUDED_EXTENSIONS.duplicateExclusionSet();
        if (!excludes.contains(ext)) {
            boolean replace = false;
            int num = 1;
            for (String word : countMap.keySet()) {
                if (word.equals(name)) {
                    num = countMap.get(word) + 1;
                    replace = true;
                    break;
                }
            }
            if (replace) {
                countMap.remove(name);
            }
            countMap.put(name, num);
        }
    }

    public static Set<String> getRepeatOffenders() {
        Set<String> names = new HashSet<>();
        for (String name : countMap.keySet()) {
            Integer num = countMap.get(name);
            if (num > 2) {
                names.add(name);
            }
        }
        return names;
    }

    public static void sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException ignored) {
        }
    }

    public static void logFile(String logText) {
        String filePath = OS.getDataFilePath("LogFile.txt");
        File logFile = new File(filePath);
        try {
            FileUtils.writeStringToFile(logFile, logText + "\n", Charset.defaultCharset(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static String f(long number) {
        double num = (double) number;
        int count = 0;
        while (num > 1000) {
            num /= 1000;
            count++;
        }
        String numString = String.format("%.2f", num);
        return numString + switch (count) {
            case 1 -> "K";
            case 2 -> "M";
            case 3 -> "G";
            default -> "B";
        };
    }

}
