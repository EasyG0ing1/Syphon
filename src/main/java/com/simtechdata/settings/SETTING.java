package com.simtechdata.settings;

import com.simtechdata.enums.OS;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
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
        if (this.equals(DUPLICATE_EXCLUSION_SET)) {
            return new HashSet<>(Arrays.asList(getString().toLowerCase().split(";")));
        }
        return null;
    }

    public String getString() {
        return switch (this) {
            case LAST_FOLDER -> prefs.get(this.name(), System.getProperty("user.home"));
            case LAST_URL -> prefs.get(this.name(), "https://");
            case THREAD_COUNT, EXCLUDED_EXTENSIONS -> prefs.get(this.name(), "");
            case URL_HISTORY -> getLocal();
            case REMOVE_DUPLICATES, DUPLICATE_EXCLUSION_SET -> null;
        };
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
