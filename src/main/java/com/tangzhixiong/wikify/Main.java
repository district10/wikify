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

class Config {
    public static String srcDirPath = null;
    public static String dstDirPath = null;
}

public class Main {

    public static String relativePath(String basedir, String path) {
        if (basedir.length()+1 >= path.length()) {
            return "";
        }
        return path.substring(1+basedir.length()).replace("\\", "/");
    }

    public static void writeXml(org.w3c.dom.Document doc, String path) {
        try (
            Writer searchFullXmlWriter = new FileWriter(path);
        ) {
            XMLSerializer serializer = new XMLSerializer(searchFullXmlWriter, new OutputFormat(doc, "UTF-8", true));
            serializer.serialize(doc);
            searchFullXmlWriter.close();
            System.out.println("XML search file saved to [" + path + "].");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createSearchXml(String srcDirPath) throws Exception {
        File srcDirFile = new File(srcDirPath);
        if (!srcDirFile.exists() || !srcDirFile.isDirectory()) {
            return;
        }
        final ArrayDeque<File> queue = new ArrayDeque<>();
        queue.add(srcDirFile);

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // search.full.xml
        org.w3c.dom.Document searchFullXml = docBuilder.newDocument();
        org.w3c.dom.Element searchFullRootElement = searchFullXml.createElement("search");
        searchFullXml.appendChild(searchFullRootElement);

        // search.note.xml
        org.w3c.dom.Document searchNoteXml = docBuilder.newDocument();
        org.w3c.dom.Element searchNoteXmlRootElement = searchNoteXml.createElement("search");
        searchNoteXml.appendChild(searchNoteXmlRootElement);

        while (!queue.isEmpty()) {
            File pwd = queue.poll();
            final File[] entries;
            try {
                entries = pwd.listFiles();
            } catch (NullPointerException e) { continue; }

            for (final File htmlDocumentFile: entries) {
                if (htmlDocumentFile.isFile() && htmlDocumentFile.getAbsolutePath().endsWith(".html")) {
                    System.out.print("Processing [" + htmlDocumentFile.getCanonicalPath() + "]...");
                    Document htmlDocument = Jsoup.parse(htmlDocumentFile, "UTF-8");
                    String urlPath = relativePath(srcDirFile.getCanonicalPath(), htmlDocumentFile.getCanonicalPath());

                    // search.full.xml
                    {
                        // url
                        org.w3c.dom.Element url = searchFullXml.createElement("url");
                        url.setTextContent(urlPath);

                        // title
                        org.w3c.dom.Element title = searchFullXml.createElement("title");
                        if (!htmlDocument.title().isEmpty()) {
                            title.setTextContent(htmlDocument.title());
                        } else {
                            title.setTextContent("(No Title) " + urlPath);
                        }

                        // content
                        org.w3c.dom.Element content = searchFullXml.createElement("content");
                        content.setAttribute("type", "html");
                        content.appendChild(searchFullXml.createCDATASection(htmlDocument.body().html()));

                        org.w3c.dom.Element entry = searchFullXml.createElement("entry");
                        entry.appendChild(title);
                        entry.appendChild(url);
                        entry.appendChild(content);
                        searchFullRootElement.appendChild(entry);
                    }

                    // note fragments
                    Elements terms = htmlDocument.select("body > ul > li > dl");
                    for (Element ele: terms) {
                        org.w3c.dom.Element entry = searchNoteXml.createElement("entry");

                        org.w3c.dom.Element url = searchNoteXml.createElement("url");
                        url.setTextContent(urlPath);

                        org.w3c.dom.Element content = searchNoteXml.createElement("content");
                        content.setAttribute("type", "html");
                        content.appendChild(searchNoteXml.createCDATASection(ele.html()));

                        entry.appendChild(url);
                        entry.appendChild(content);
                        searchNoteXmlRootElement.appendChild(entry);
                    }
                    System.out.print(" done.\n");
                } else if (htmlDocumentFile.isDirectory()) {
                    queue.add(htmlDocumentFile);
                }
            }
        }
        // save to disk
        writeXml(searchFullXml, Config.dstDirPath+"search.full.xml");
        writeXml(searchNoteXml, Config.dstDirPath+"search.note.xml");
    }

    public static void main(String[] args) {

        for (int i = 0; i < args.length; ++i) {
            if (false) {
            } else if (args[i].equals("-i") || args[i].equals("--input")) {
                if (++i < args.length) { Config.srcDirPath = args[i]; }
            } else if (args[i].equals("-o") || args[i].equals("--output")) {
                if (++i < args.length) { Config.dstDirPath = args[i]; }
            }
        }

        if (Config.srcDirPath == null) { Config.srcDirPath = "."; }
        final File srcDirFile = new File(Config.srcDirPath);
        if (!srcDirFile.exists() || !srcDirFile.isDirectory()) {
            System.err.println("Invalid input directory: "+Config.srcDirPath);
            System.exit(1);
        }

        if (Config.dstDirPath == null) { Config.dstDirPath = "."; }
        final File dstDirFile = new File(Config.dstDirPath);
        if (dstDirFile.exists() && !dstDirFile.isDirectory()) {
            System.err.println("Invalid output directory: "+Config.dstDirPath);
            System.exit(2);
        }
        if (!dstDirFile.exists()) { dstDirFile.mkdirs(); }

        // normalize srcDirPath/dstDirPath
        try {
            Config.srcDirPath = srcDirFile.getCanonicalPath();
            Config.dstDirPath = dstDirFile.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(3);
        }

        try {
            createSearchXml(Config.srcDirPath);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
