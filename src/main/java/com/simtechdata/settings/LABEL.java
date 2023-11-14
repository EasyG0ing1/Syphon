package com.simtechdata.settings;

import java.util.prefs.Preferences;

public enum LABEL {
    LAST_FOLDER,
    LAST_URL,
    URL_HISTORY,
    THREADS,
    REMOVE_DUPLICATES,
    EXCLUSION_EXT;
    public static final Preferences prefs = Preferences.userNodeForPackage(LABEL.class);

}
