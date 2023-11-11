package com.simtechdata.settings;

import java.util.prefs.BackingStoreException;
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
    public void clearAll() {
        try {
            prefs.clear();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }
}
