package com.simtechdata.settings;

import java.util.prefs.Preferences;

import static com.simtechdata.settings.LABEL.*;

public class Set {

    public static final Set INSTANCE = new Set();

    private Set() {
    }

    private final Preferences prefs = LABEL.prefs;

    public void lastFolder(String value) {
        AppSettings.clear.lastFolder();
        prefs.put(LAST_FOLDER.name(), value);
    }

    public void lastURL(String value) {
        String last = AppSettings.get.lastURL();
        AppSettings.set.urlHistory(last);
        AppSettings.clear.lastURL();
        prefs.put(LAST_URL.name(), value);
    }

    public void threads(int value) {
        AppSettings.clear.threads();
        prefs.putInt(THREADS.name(), value);
    }

    public void urlHistory(String value) {
        String current = AppSettings.get.urlHistory();
        for(String url : current.split(";")) {
            if(url.equals(value))
                return;
        }
        String newHistory = current + ";" + value;
        newHistory = newHistory.replaceAll(";+",";");
        AppSettings.clear.urlHistory();
        prefs.put(URL_HISTORY.name(), newHistory);
    }

    public void removeDuplicates(boolean value) {
        AppSettings.clear.removeDuplicates();
        prefs.putBoolean(REMOVE_DUPLICATES.name(), value);
    }

    public void duplicateExclusions(String value) {
        AppSettings.clear.duplicateExclusions();
        prefs.put(EXCLUSION_EXT.name(), value);
    }
}
