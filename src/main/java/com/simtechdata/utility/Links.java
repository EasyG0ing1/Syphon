package com.simtechdata.utility;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Links extends LinkedList<Link> {

    private long longestNumber = 0;

    public Links() {
        super();
    }

    public Links(Elements elements) {
        super();
        if(elements.size() < 4) {
            for (Element element : elements) {
                Link link = new Link(element);
                Core.logFile(element.wholeText() + ": " + link.getUrlString());
                addLast(link);
            }
        }
        else {
            this.longestNumber = getLongestStartNumber(elements);
            for (Element element : elements) {
                Link link = new Link(element, longestNumber);
                Core.logFile(element.wholeText() + ": " + link.getUrlString());
                addLast(link);
            }
            sortLinks();
        }
    }

    private long getLongestStartNumber(Elements rawElements) {
        if (rawElements == null)
            return 0;
        AtomicLong longest = new AtomicLong(0);
        int listLen = rawElements.size();
        AtomicLong count = new AtomicLong(1);
        for (Element eLink : rawElements) {
            new Thread(() -> {
                String endString = Link.getEnd(eLink);
                int len = getNumberLength(endString);
                if (endString.matches("^\\d+.+")) {
                    long longestNum = Math.max(longest.get(), len);
                    longest.set(longestNum);
                }
                count.addAndGet(1);
            }).start();
        }
        while (count.get() < listLen) {
            Core.sleep(5);
        }
        return longest.get();
    }

    private int getNumberLength(String string) {
        String regex = "(^\\d+)";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(string);
        if (m.find()) {
            return m.group(1).length();
        }
        return 0;
    }

    private void sortLinks() {
        sort(Comparator.comparing(Link::getSortableLink));
    }
}
