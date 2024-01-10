package com.simtechdata.settings;

import com.simtechdata.enums.OS;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

public enum SETTING {
    LAST_FOLDER,
    LAST_URL,
    URL_HISTORY,
    THREAD_COUNT,
    REMOVE_DUPLICATES,
    EXCLUDED_EXTENSIONS,
    DUPLICATE_EXCLUSION_SET;
    public static final Preferences prefs = Preferences.userNodeForPackage(SETTING.class);
    private static final String extensionExclusionFilename = "excludedExtensions.txt";
    private void clear() {
        prefs.remove(this.name());
    }

    public void setString(String value) {
        if (value == null)
            return;
        if (this.equals(LAST_URL) || this.equals(URL_HISTORY)) {
            String current = URL_HISTORY.getString();
            if (current != null && current.contains(";")) {
                for (String url : current.split(";")) {
                    if (url.equals(value))
                        return;
                }
            }
            String newHistory = (current == null ? "" : current) + ";" + value;
            newHistory = newHistory.replaceAll(";+", ";");
            URL_HISTORY.clear();
            prefs.put(URL_HISTORY.name(), newHistory);
        }
        clear();
        prefs.put(this.name(), value);
    }

    public void setInt(int value) {
        clear();
        prefs.putInt(this.name(), value);
    }

    public void setBool(boolean value) {
        clear();
        prefs.putBoolean(this.name(), value);
    }

    public void lastFolder(String value) {
        if (this.equals(LAST_FOLDER)) {
            clear();
            prefs.put(this.name(), value);
        }
    }

    public java.util.Set<String> duplicateExclusionSet() {
        Set<String> exclusionSet = null;
        if (this.equals(DUPLICATE_EXCLUSION_SET)) {
            File exclusionListFile = Paths.get(OS.getDataFilePath(extensionExclusionFilename)).toFile();
            if(exclusionListFile.exists()) {
                try {
                    String exclusionFile = FileUtils.readFileToString(exclusionListFile, Charset.defaultCharset());
                    exclusionSet = new HashSet<>(Arrays.asList(exclusionFile.split("\\n")));

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else
                exclusionSet = new HashSet<>(Arrays.asList(getString().toLowerCase().split(";")));
        }
        return exclusionSet;
    }

    public String getString() {
        return switch (this) {
            //First option checks to see if the last saved folder exists. If so, it sends that String and if not, it provides the users home folder.
            case LAST_FOLDER -> (new File(prefs.get(this.name(), System.getProperty("user.home"))).exists() ? prefs.get(this.name(), System.getProperty("user.home")) : System.getProperty("user.home"));
            case LAST_URL -> prefs.get(this.name(), "https://");
            case THREAD_COUNT, EXCLUDED_EXTENSIONS -> prefs.get(this.name(), "");
            case URL_HISTORY -> getLocal();
            case REMOVE_DUPLICATES, DUPLICATE_EXCLUSION_SET -> null;
        };
    }

    public File getDownloadFolder() {
        File folder;
        if (this.equals(LAST_FOLDER)) {
            try {
                String folderString = getString();
                folder = new File(folderString);
                if(folder.exists())
                    return folder;
                folder = Paths.get(System.getProperty("user.home"), "Syphon").toFile();
                FileUtils.forceMkdir(folder);
                return folder;
            } catch (IOException ignored) {}
        }
        return Paths.get(System.getProperty("user.home")).toFile();
    }

    private String getLocal() {
        try {
            if (OS.getLinkFile().exists()) {
                String links = FileUtils.readFileToString(OS.getLinkFile(), Charset.defaultCharset());
                return links.replaceAll("\\n", ";");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    public Integer getInt() {
        return switch (this) {
            case DUPLICATE_EXCLUSION_SET, LAST_FOLDER, LAST_URL, REMOVE_DUPLICATES, EXCLUDED_EXTENSIONS -> null;
            case URL_HISTORY, THREAD_COUNT -> prefs.getInt(this.name(), 20);
        };
    }

    public Boolean getBool() {
        return switch (this) {
            case DUPLICATE_EXCLUSION_SET, LAST_FOLDER, LAST_URL, THREAD_COUNT, URL_HISTORY, EXCLUDED_EXTENSIONS -> null;
            case REMOVE_DUPLICATES -> prefs.getBoolean(this.name(), false);
        };
    }
}
