package com.simtechdata;

import com.simtechdata.enums.OS;
import com.simtechdata.gui.GUI;
import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
                    if(!file.exists()) {
                        System.out.println("Invalid Path: " + filename + "\ntry again only use the files absolute path in the argument.");
                        System.exit(0);
                    }
                }
                else {
                    String newLinks = FileUtils.readFileToString(file, Charset.defaultCharset());
                    for(String link : newLinks.split(System.lineSeparator())) {
                        addLinkToLinksFile(link);
                    }
                }
                System.exit(0);
            }
        }
        launch(args);
    }

    public static void addLinkToLinksFile(String link) throws IOException {
        File linkFile = OS.getLinkFile();
        String links;
        if(!linkFile.exists()) {
            linkFile.createNewFile();
        }
        if(linkFile.exists()) {
            links = FileUtils.readFileToString(linkFile, Charset.defaultCharset());
            if(links.contains(link))
                return;
            Deque<String> list = new LinkedList<>(Arrays.asList(links.split(System.lineSeparator())));
            Set<String> set = new HashSet<>(list);

            if(!set.contains(link)) {
                set.add(link);
                list.addLast(link);
            }

            String sortedListString = set.stream()
                    .sorted(Comparator.comparing(String::toString))
                    .collect(Collectors.joining(System.lineSeparator()));

            Files.writeString(linkFile.toPath(), sortedListString, Charset.defaultCharset());

            System.out.println("Link Added: " + link);
        }
    }

    @Override
    public void start(Stage stage) {
        GUI.show();
    }

}
