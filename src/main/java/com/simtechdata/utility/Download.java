package com.simtechdata.utility;

import com.simtechdata.enums.MessageType;
import com.simtechdata.enums.State;
import com.simtechdata.enums.TabType;
import com.simtechdata.gui.GUI;
import org.jsoup.nodes.Element;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;

import static com.simtechdata.enums.MessageType.*;
import static com.simtechdata.enums.State.*;
import static com.simtechdata.enums.TabType.ERROR;

public class Download {

    public Download(Element link) {
        super();
        this.filename = link.wholeText();
        this.coreURL = link.baseUri();
        this.link = link;
        this.baseFolder = Core.baseFolder;
        this.buffer = new byte[2048];
        this.state = READY;
        String folderPath = getFolderPath();
        String webServer = getWebServer();
        filePath = Paths.get(baseFolder, webServer, folderPath, filename).toString();
        urlCon = new URLCon(getLink(), filePath);
        Core.addGrandTotal(urlCon.getConLength());
    }

    private final Element link;
    private final String filename;
    private final String coreURL;
    private final String baseFolder;
    private String label;
    private final byte[] buffer;
    private State state;
    private Integer index;
    private final URLCon urlCon;
    private final String filePath;
    private long startTime, endTime, seconds, bytesRead, conLength;


    private void setLabel() {
        String starting = urlCon.fileResuming() ? "** RESUMING: " : "DOWNLOADING: ";
        label = starting + filename + " (%s)";
    }

    public Runnable start() {
        return () -> {
            try {
                while ((index = GUI.getIndex()) == null && state.equals(READY)) {
                    Core.sleep(100);
                }
                if (!state.equals(READY)) {
                    return;
                }
                setLabel();
                conLength = urlCon.getConLength();
                if (conLength <= 0) {
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
            log(NORMAL, filename, "Download Complete", TabType.FINISHED);
            GUI.release(index);
        }
    }
    public Runnable stop() {
        state = USER_CANCELED;
        return ()->{
            log(MEDIUM, "USER CANCELED", filename, TabType.CANCELED);
            GUI.release(index);
        };
    }

    public void getFile() throws IOException {
        urlCon.checkResume();
        bytesRead = urlCon.getFileLength();
        try (BufferedInputStream bis = new BufferedInputStream(urlCon.getInputStream());
             RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek(urlCon.getFileLength());
            int bytesIn;
            startTime = System.currentTimeMillis();
            state = RUNNING;
            new Thread(monitorTransfer()).start();
            while (((bytesIn = bis.read(buffer)) != -1) && state.equals(RUNNING)) {
                raf.write(buffer, 0, bytesIn);
                bytesRead += bytesIn;
                Core.addDownloaded(bytesIn);
            }
        }
        state = state.equals(RUNNING) ? FINISHED : state;
        Core.addFileDownloaded();
    }

    private Runnable monitorTransfer() {
        return () -> {
            Core.sleep(2000);
            double serverFileSize = (conLength == 0) ? 8 * 1024 * 1024 * 3 : conLength;
            while (state.equals(RUNNING)) {
                endTime = System.currentTimeMillis();
                seconds = (endTime - startTime) / 1000;
                String bps = f(bytesRead / seconds) + "B/s";
                double progress = (double) (bytesRead) / serverFileSize;
                GUI.setValues(index, String.format(label, bps), progress);
                Core.sleep(500);
            }
        };
    }

    private String getWebServer() {
        String newBaseURL = coreURL.replaceAll("/+$", "").replaceFirst("[htps:]+//", "");
        return newBaseURL.split("/")[0];
    }

    private String getFolderPath() {
        String newBaseURL = coreURL.replaceAll("/+$", "").replaceFirst("[htps:]+//", "");
        String[] parts = newBaseURL.split("/");
        StringBuilder sb = new StringBuilder();
        if (parts.length > 0) {
            for (int x = 0; x < parts.length; x++) {
                if (x > 0) {
                    sb.append(parts[x]).append("/");
                }
            }
        }
        return sb.replace(sb.length() - 1, sb.length(), "").toString();
    }

    public String getLink() {
        return Core.getDownloadLink(link);
    }

    private String f(long number) {
        double num = (double) number;
        int count = 0;
        while (num > 1000) {
            num /= 1000;
            count++;
        }
        String numString = String.format("%.2f", num);
        return numString + switch (count) {
            case 1 -> "K";
            case 2 -> "M";
            case 3 -> "G";
            default -> "B";
        };
    }

    private void log(MessageType messageType, String typeMsg, String msg, TabType tabType) {
        Log.l(messageType, typeMsg, msg, tabType);
    }
}
