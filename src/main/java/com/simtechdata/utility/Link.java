package com.simtechdata.utility;


import com.simtechdata.enums.NodeType;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Link {


    private final Element link;
    private final String urlString;
    private final String base;
    private final String end;
    private final String server;
    private String baseURI;
    private boolean isFile;
    private boolean isFolder;
    private final int numLength;
    private final long longestLen;
    private boolean firstFileCheck = true;
    private final boolean startsWithNumber;

    public Link(Element link) {
        this.link = link;
        this.urlString = getFullURL(this.link);
        this.end = getEnd(this.link);
        this.base = urlString.replaceFirst(Pattern.quote(end), "");
        this.numLength = getNumberLength(this.end);
        this.longestLen = 0;
        setBaseURI();
        this.startsWithNumber = false;
        this.server = getSetServer();
        setIsFile();
    }

    public Link(String urlString, NodeType type) {
        if(urlString == null) {
            System.out.println("null");
            System.exit(0);
        }
        if(urlString.isEmpty()) {
            System.out.println("empty");
            System.exit(0);
        }
        this.link = new Element(urlString);
        this.urlString = urlString;
        this.end = getEnd(this.link);
        this.base = urlString.replaceFirst(Pattern.quote(end), "");
        this.numLength = getNumberLength(this.end);
        this.longestLen = 0;
        setBaseURI();
        this.startsWithNumber = false;
        this.server = getSetServer();
        this.isFile = type.equals(NodeType.FILE);
        this.isFolder = type.equals(NodeType.FOLDER);
    }

    public Link(Element link, long longestLen) {
        this.link = link;
        this.urlString = getFullURL(this.link);
        this.end = getEnd(this.link);
        this.base = urlString.replaceFirst(Pattern.quote(end), "");  //Will have slash at the end
        this.numLength = getNumberLength(this.end);
        this.longestLen = longestLen;
        setBaseURI();
        long delta = longestLen - numLength;
        this.startsWithNumber = delta > 0 && numLength > 0;
        this.server = getSetServer();
    }

    public static String getEnd(Element link) {
        String base = getFullURL(link);
        return getEnd(base);
    }

    private static String getEnd(String link) {
        String string = stripSlash(link);
        String[] parts = string.split("/");
        if (parts.length > 0)
            return parts[parts.length - 1];
        return "";
    }

    private void setBaseURI() {
        String protocol = urlString.startsWith("http://") ? "http://" : "https://";
        String url = stripProtocol(stripSlash(urlString));
        this.baseURI = protocol + url.split("/")[0];
    }

    private String stripProtocol(String url) {
        String protocol = url.startsWith("http://") ? "http://" : "https://";
        return url.replaceFirst(Pattern.quote(protocol),"");
    }

    private String getSetServer() {
        return stripProtocol(baseURI);
    }

    private static String getFullURL(Element link) {
        String baseURI = stripSlash(link.baseUri());
        String fullURL = baseURI + "/" + link.wholeText();
        return (fullURL.charAt(0) == '/') ?
                fullURL.replaceFirst("/", "") :
                fullURL;
    }


    private static String stripSlash(String link) {
        if (link.endsWith("/"))
            return link.substring(0, link.length() - 1);
        return link;
    }

    private void setIsFile() {
        if(!urlString.isEmpty()) {
            boolean isFile = true;
            try {
                Connection.Response resp = Jsoup.connect(urlString).method(Connection.Method.HEAD).execute();
                if(!resp.contentType().equals("text/plain")) //If the above line throws an exception this line is skipped
                    isFile = false;

            } catch (IOException ignored) {
            }
            this.isFile = isFile;
            this.isFolder = !isFile;
        }
    }

    private int getNumberLength(String string) {
        if (numStart()) {
            String regex = "(^\\d+)";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(string);
            if (m.find()) {
                return m.group(1).length();
            }
        }
        return 0;
    }

    private boolean numStart() {
        return end.matches("^\\d+.+");
    }

    public Element getLink() {
        return link;
    }

    public String getUrlString() {
        return urlString;
    }

    public String getCleanURL() {
        return urlString.replaceAll("%20", " ");
    }

    public String  getFilePath() {
        String urlString = getCleanURL();
        String protocol = urlString.startsWith("http://") ? "http://" : "https://";
        urlString = urlString.replaceFirst(Pattern.quote(protocol),"");
        urlString = urlString.replaceFirst(server + "/", "");
        String[] items = urlString.split("/");
        String file = items[items.length - 1];
        String folders = urlString.replaceFirst("/" + file, "");
        return Paths.get(Core.baseFolder, server, folders, file).toAbsolutePath().toString();
    }

    public String getEnd() {
        return end;
    }

    public boolean isFile() {
        if(firstFileCheck) {
            firstFileCheck = false;
            setIsFile();
        }
        return isFile;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public String getServer() {
        return server;
    }

    public String getSortableLink() {
        int delta = (int) longestLen - numLength;
        if (startsWithNumber) {
            return base + "0".repeat(delta) + end;
        }
        return urlString;
    }

    public Links getLinks() {
        try {
            Document doc = Jsoup.connect(urlString).get();
            Elements elements = doc.select("a[href]");
            return new Links(elements);
        } catch (IOException ignored) {}
        return new Links();
    }

    public boolean isValid() {
        return !urlString.isEmpty();
    }

}
