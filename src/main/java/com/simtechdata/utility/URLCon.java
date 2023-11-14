package com.simtechdata.utility;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class URLCon {

    private final Link link;
    private final String urlPath;
    private URLConnection con;
    private final long conLength;
    private URL url = null;
    private final String filePath;
    private final long fileLength;
    private boolean fileExists = false;
    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

    public URLCon(Link link) {
        this.link = link;
        this.filePath = link.getFilePath();
        this.fileLength = setFileLength();
        this.urlPath = link.getUrlString();
        this.con = getURLConnection();
        this.conLength = getServerFileLength();
        Core.addDownloaded(this.fileLength);
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

    private long getServerFileLength() {
        long length = con.getContentLength();
        return length <= 0 ? 0 : length;
    }

    private void setCon() {
        try {
            if (resume()) {
                con = url.openConnection();
                con.setRequestProperty("Range", "bytes=" + fileLength + "-");
            }
            else if (fileNeedsDownloading()) {
                con = url.openConnection();
            }
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
                fileExists = true;
                if (fileLength <= 0) {
                    FileUtils.forceDelete(file);
                    fileExists = false;
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

    public boolean fileNeedsDownloading() {
        return (resume() || !fileExists);
    }

    private boolean resume() {
        return fileExists && (conLength == 0 || (conLength > fileLength));
    }

    public long getFileLength() {
        return fileLength;
    }

    public long getConLength() {
        return conLength;
    }

    public boolean resumeDownload() {
        return resume();
    }

    public InputStream getInputStream() throws IOException {
        setCon();
        return con.getInputStream();
    }

}
