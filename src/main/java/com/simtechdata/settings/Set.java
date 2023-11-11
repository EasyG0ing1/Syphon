package com.simtechdata.settings;

import java.util.prefs.Preferences;

import static com.simtechdata.settings.LABEL.*;

public class Set {

    public static final Set INSTANCE = new Set();

    private Set() {
    }

    private final Preferences prefs = LABEL.prefs;

    public void lastFolder(String value) {
        AppSettings.CLEAR.lastFolder();
        prefs.put(LAST_FOLDER.name(), value);
    }

    public void lastURL(String value) {
        AppSettings.CLEAR.lastURL();
        prefs.put(LAST_URL.name(), value);
    }

    public void threads(int value) {
        AppSettings.CLEAR.threads();
        prefs.putInt(THREADS.name(), value);
    }
}
