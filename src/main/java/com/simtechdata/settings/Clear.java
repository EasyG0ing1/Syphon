package com.simtechdata.settings;

import java.util.prefs.Preferences;

import static com.simtechdata.settings.LABEL.*;

public class Clear {

    public static final Clear INSTANCE = new Clear();

    private Clear() {}

    private final Preferences prefs = LABEL.prefs;

    public void lastFolder() {
        prefs.remove(LAST_FOLDER.name());
    }

    public void lastURL() {
        prefs.remove(LAST_URL.name());
    }

    public void threads() {
        prefs.remove(THREADS.name());
    }

    public void urlHistory() {
        prefs.remove(URL_HISTORY.name());
    }
    public void removeDuplicates() {
        prefs.remove(REMOVE_DUPLICATES.name());
    }
    public void duplicateExclusions() {
        prefs.remove(EXCLUSION_EXT.name());
    }
}
