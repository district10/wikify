package com.tangzhixiong.wikify;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayDeque;

/**
 * Created by tzx on 2016/12/4.
 */

public class Main {

    public static void fillBundle(String srcDirPath) throws Exception {
        final ArrayDeque<File> queue = new ArrayDeque<>();
        queue.add(new File(srcDirPath));
        while (!queue.isEmpty()) {
            File pwd = queue.poll();
            final File[] entries;
            try {
                entries = pwd.listFiles();
            } catch (NullPointerException e) { continue; }
            for (final File entry: entries) {
                if (entry.isFile()) {
                } else if (entry.isDirectory()) {
                    queue.add(entry);
                }
            }
        }
    }
    public static void main(String[] args) {

        // TODO: directory listing
        // search full text: file.html -> /wikify.d/file.xml
        // search dl=dt+dd: file.html -> xml node (entry -> html fragment)
        final File readme = new File("README.html");
        if (!readme.exists() || !readme.isFile()) {
            System.out.println("No 'README.html'.");
            System.exit(1);
        }

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            org.w3c.dom.Document searchXml = docBuilder.newDocument();
            org.w3c.dom.Element rootElement = searchXml.createElement("search");
            searchXml.appendChild(rootElement);

            Document doc = Jsoup.parse(readme, "UTF-8");
            Elements terms = doc.select("li > dl > dt");
            for (Element ele: terms) {
                // String eleText = ele.text() + ele.nextElementSibling().text();
                // eleText = eleText.replaceAll("\\s", "");
                // System.out.println(eleText);
                org.w3c.dom.Element description = searchXml.createElement("entry");
                description.appendChild(searchXml.createCDATASection(ele.parent().outerHtml()));
                // TODO: description <- title, url
                rootElement.appendChild(description);
            }

            Writer out = new FileWriter("search.xml"); // Writer out = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(out, new OutputFormat(searchXml, "UTF-8", true));
            serializer.serialize(searchXml);
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

