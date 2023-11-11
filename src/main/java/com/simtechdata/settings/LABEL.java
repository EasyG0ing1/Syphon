package com.simtechdata.settings;

import java.util.prefs.Preferences;

public enum LABEL {
    LAST_FOLDER,
    LAST_URL,
    THREADS;
    public static final Preferences prefs = Preferences.userNodeForPackage(LABEL.class);

}
