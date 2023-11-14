package com.simtechdata.utility;

import com.simtechdata.enums.MessageType;
import com.simtechdata.enums.State;
import com.simtechdata.enums.TabType;
import com.simtechdata.gui.GUI;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import static com.simtechdata.enums.MessageType.*;
import static com.simtechdata.enums.State.*;
import static com.simtechdata.enums.TabType.ERROR;
import static com.simtechdata.utility.Core.f;

public class Download {

    public Download(Link link) {
        super();
        this.filename = link.getEnd();
        this.link = link;
        this.buffer = new byte[2048];
        this.state = READY;
        this.filePath = link.getFilePath();
        urlCon = new URLCon(link);
        this.conLength = urlCon.getConLength();
        Core.addGrandTotal(conLength);
    }

    private final Link link;
    private final String filename;
    private String label;
    private final byte[] buffer;
    private State state;
    private Integer index;
    private final URLCon urlCon;
    private final String filePath;
    private long startTime;
    private long endTime;
    private long seconds;
    private long bytesReceived;
    private final long conLength;
    private long bytesReadThisSession;


    private void setLabel() {
        String starting = urlCon.resumeDownload() ? "** RESUMING: " : "DOWNLOADING: ";
        label = starting + filename + " (%s) [%s]";
    }

    public Runnable start() {
        return () -> {
            try {
                if (!urlCon.fileNeedsDownloading()) {
                    log(MEDIUM, "DOWNLOADED OR NO LOCAL FOLDER", filename, TabType.FINISHED);
                    Core.addFileDownloaded();
                    state = FINISHED;
                    return;
                }
                while ((index = GUI.getIndex()) == null && state.equals(READY)) {
                    Core.sleep(100);
                }
                if (!state.equals(READY)) {
                    return;
                }
                setLabel();
                if (urlCon.getConLength() <= 0) {
                    log(ALERT, "** ALERT **", "Server reports 0 size: " + getLink(), ERROR);
                }
                getFile();
                finish();
            } catch (IOException e) {
                log(ALERT, "** FAILED TRANSFER **", filename, ERROR);
            }
        };
    }

    private void finish() {
        if (state.equals(FINISHED)) {
            log(NORMAL, "DOWNLOAD COMPLETE", filePath + " FROM: " + link.getCleanURL(), TabType.FINISHED);
            GUI.release(index);
        }
    }

    public Runnable stop() {
        state = USER_CANCELED;
        return () -> {
            log(MEDIUM, "USER CANCELED", filename, TabType.CANCELED);
            GUI.release(index);
        };
    }

    public void getFile() throws IOException {
        long fileSize = urlCon.getFileLength();
        bytesReceived = fileSize;
        if (bytesReceived > urlCon.getConLength()) {
            Core.logFile("fileSize: " + f(fileSize) + " Server: " + f(urlCon.getConLength()) + ": " + filename);
        }
        try (BufferedInputStream bis = new BufferedInputStream(urlCon.getInputStream());
             RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek(fileSize);
            int bytesIn;
            startTime = System.currentTimeMillis();
            state = RUNNING;
            new Thread(monitorTransfer()).start();
            while (((bytesIn = bis.read(buffer)) != -1) && state.equals(RUNNING)) {
                raf.write(buffer, 0, bytesIn);
                bytesReceived += bytesIn;
                bytesReadThisSession += bytesIn;
                Core.addDownloaded(bytesIn);
                Core.addDownloadedThisSession(bytesIn);
            }
        }
        state = state.equals(RUNNING) ? FINISHED : state;
        Core.addFileDownloaded();
    }

    Random random = new Random(System.currentTimeMillis());

    private long randomTime() {
        long min = 500;
        long max = 1500;
        return random.nextLong(min, max);
    }

    private Runnable monitorTransfer() {
        return () -> {
            Core.sleep(2000);
            double serverFileSize = (conLength == 0) ? 8 * 1024 * 1024 * 3 : conLength;
            while (state.equals(RUNNING)) {
                String read = f(bytesReceived);
                String total = f(conLength);
                endTime = System.currentTimeMillis();
                seconds = (endTime - startTime) / 1000;
                String bps = f(bytesReadThisSession / seconds) + "B/s";
                String cumProgress = read + " / " + total;
                double progress = (double) (bytesReceived) / serverFileSize;
                GUI.setValues(index, String.format(label, bps, cumProgress), progress);
                Core.sleep(randomTime());
            }
        };
    }

    public String getLink() {
        return link.getUrlString();
    }

    private void log(MessageType messageType, String typeMsg, String msg, TabType tabType) {
        Log log = new Log(messageType, typeMsg, msg, tabType);
        GUI.sendLog(log);
    }
}
