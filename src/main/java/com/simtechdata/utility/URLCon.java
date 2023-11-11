package com.simtechdata.utility;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class URLCon {

    private final String urlPath;
    private URLConnection con;
    private final long conLength;
    private URL url = null;
    private final String filePath;
    private final long fileLength;
    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";
    private boolean resume = false;

    public URLCon(String urlPath, String filePath) {
        this.filePath = filePath;
        this.fileLength = setFileLength();
        this.urlPath = urlPath;
        this.con = getURLConnection();
        this.conLength = con.getContentLength();
        if(filePath == null) {
            System.err.println("File Path is null");
        }
    }

    private URLConnection getURLConnection() {
        try {
            url = new URL(urlPath);
            URLConnection con = url.openConnection();
            con.setRequestProperty("User-Agent", USER_AGENT);
            return con;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long setFileLength() {
        File file = new File(filePath);
        long fileLength = 0;
        try {
            if (file.exists()) {
                fileLength = file.length();
                if (fileLength <= 0) {
                    FileUtils.forceDelete(file);
                    fileLength = 0;
                }
            }
            else {
                FileUtils.createParentDirectories(file);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileLength;
    }

    public long getFileLength() {
        return fileLength;
    }

    private boolean resume() {
        return fileLength > 0 && (fileLength < conLength);
    }

    public long getConLength() {
        return conLength;
    }

    public void checkResume() {
        try {
            if (resume()) {
                con = url.openConnection();
                con.setRequestProperty("Range", "bytes=" + fileLength + "-");
                resume = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean fileResuming() {
        return resume;
    }

    public InputStream getInputStream() throws IOException {
        return con.getInputStream();
    }

}
