package com.simtechdata.enums;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * This enum class is used to set the OS on which Drifty is running
 */
public enum OS {
    WIN, MAC, LINUX, SOLARIS, FREEBSD;

    private static OS osType;
    private static String osName;
    private static String jsonDataPath;
    private static final String jsonData = "JsonData";

    private static void setOSType() {
        osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            osType = OS.WIN;
        }
        else if (osName.contains("mac")) {
            osType = OS.MAC;
        }
        else if (osName.contains("linux")) {
            osType = OS.LINUX;
        }
        else if (osName.contains("sun")) {
            osType = OS.SOLARIS;
        }
        else if (osName.contains("free")) {
            osType = OS.FREEBSD;
        }
        else {
            osType = OS.LINUX;
        }
    }

    public static String getOSName() {
        if (osType == null) {
            setOSType();
        }
        return osName;
    }

    public static boolean isWindows() {
        if (osType == null) {
            setOSType();
        }
        return osType.equals(OS.WIN);
    }

    public static boolean isMac() {
        if (osType == null) {
            setOSType();
        }
        return osType.equals(OS.MAC);
    }

    public static String getDataFilePath(String filename) {
        return Paths.get(getDataPath(), filename).toAbsolutePath().toString();
    }

    public static String getDataPath() {
        String path = OS.isWindows() ?
                Paths.get(System.getenv("LOCALAPPDATA"), "Syphon").toAbsolutePath().toString() :
                Paths.get(System.getProperty("user.home"), ".syphon").toAbsolutePath().toString();

        jsonDataPath = Paths.get(path, jsonData).toAbsolutePath().toString();

        File folder = new File(path);

        if (!folder.exists()) {
            try {
                FileUtils.createParentDirectories(new File(jsonDataPath));
            } catch (IOException ignored) {
            }
        }
        return path;
    }

}
