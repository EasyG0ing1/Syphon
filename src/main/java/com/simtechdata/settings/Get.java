package com.simtechdata.settings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.prefs.Preferences;

import static com.simtechdata.settings.LABEL.*;

public class Get {

    public static final Get INSTANCE = new Get();

    private Get() {}

    private final Preferences prefs = LABEL.prefs;

    public String lastFolder() {
        return prefs.get(LAST_FOLDER.name(), System.getProperty("user.home"));
    }

    public String lastURL() {
        return prefs.get(LAST_URL.name(), "https://");
    }

    public int threads() {
        return prefs.getInt(THREADS.name(), 20);
    }

    public String urlHistory() {
        return prefs.get(URL_HISTORY.name(), "");
    }

    public boolean removeDuplicates() {
        return prefs.getBoolean(REMOVE_DUPLICATES.name(), false);
    }

    public String duplicateExclusions() {
        return prefs.get(EXCLUSION_EXT.name(), "").toLowerCase();
    }

    public java.util.Set<String> duplicateExclusionSet() {
        String line = duplicateExclusions();
        return new HashSet<>(Arrays.asList(line.split(";")));
    }
}
