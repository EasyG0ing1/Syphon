package com.simtechdata;

import com.simtechdata.enums.OS;
import com.simtechdata.gui.GUI;
import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

public class Main extends Application {

    public static void main(String[] args) throws IOException {
        for(String arg : args) {
            if(arg.toLowerCase().startsWith("linkfile=")) {
                LinkedList<String> linkList = new LinkedList<>();
                String folder = System.getProperty("user.dir");
                String filename = arg.split("=")[1];
                File file = Paths.get(filename).toFile();
                if(!file.exists()) {
                    file = Paths.get(folder, filename).toFile();
                }
                File linkFile = OS.getLinkFile();
                String links;
                if(linkFile.exists()) {
                    links = FileUtils.readFileToString(linkFile, Charset.defaultCharset());
                    linkList.addAll(Arrays.asList(links.split("\\n")));
                }
                if(file.exists()) {
                    String newLinks = FileUtils.readFileToString(file, Charset.defaultCharset());
                    String[] lnks = newLinks.split("\\n");
                    for(String link : lnks) {
                        if(!linkList.contains(link)) {
                            linkList.addLast(link);
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    for(String link : linkList) {
                        sb.append(link);
                        sb.append(System.lineSeparator());
                    }
                    String fileString = sb.toString().replaceAll("\\n{2,}","\n");
                    FileUtils.writeStringToFile(linkFile, fileString, Charset.defaultCharset());
                    System.out.println("Links added. All links:\n\n" + fileString);
                }
                else {
                    System.out.println("Invalid path: " + filename + "\ntry again only use the files absolute path in the argument.");
                }
                System.exit(0);
            }
        }
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        GUI.show();
    }

}
