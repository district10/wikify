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
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;

/**
 * Created by tzx on 2016/12/4.
 */

class Config {
    public static String srcDirPath = ".";
    public static String dstDirPath = ".";
    public static String bodySelector = "body";     // full text search
    public static String noteSelector = "dl";       // note search
    public static LinkedHashSet<String> blockCodes = new LinkedHashSet<>();
    public static LinkedHashSet<String> lineCodes = new LinkedHashSet<>();
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

        // search.code.xml
        org.w3c.dom.Document searchCodeXml = docBuilder.newDocument();
        org.w3c.dom.Element searchCodeRootElement = searchCodeXml.createElement("search");
        searchCodeXml.appendChild(searchCodeRootElement);

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
                        url.appendChild(searchFullXml.createCDATASection(urlPath));

                        // title
                        org.w3c.dom.Element title = searchFullXml.createElement("title");
                        if (!htmlDocument.title().isEmpty()) {
                            title.appendChild(searchFullXml.createCDATASection(htmlDocument.title()));
                        } else {
                            title.appendChild(searchFullXml.createCDATASection("(No Title) " + urlPath));
                        }

                        // content
                        org.w3c.dom.Element content = searchFullXml.createElement("content");
                        content.setAttribute("type", "html");
                        Elements terms = htmlDocument.select(Config.bodySelector);
                        StringBuilder sb = new StringBuilder();
                        for (Element ele: terms) {
                            sb.append(ele.html());
                        }
                        if (!sb.toString().isEmpty()) {
                            content.appendChild(searchFullXml.createCDATASection(sb.toString()));
                            org.w3c.dom.Element entry = searchFullXml.createElement("entry");
                            entry.appendChild(title);
                            entry.appendChild(url);
                            entry.appendChild(content);
                            searchFullRootElement.appendChild(entry);
                        }
                    }

                    // search.note.xml
                    Elements terms = htmlDocument.select(Config.noteSelector);
                    for (Element ele: terms) {
                        if (!ele.html().isEmpty()) {
                            org.w3c.dom.Element entry = searchNoteXml.createElement("entry");
                            org.w3c.dom.Element url = searchNoteXml.createElement("url");
                            url.appendChild(searchNoteXml.createCDATASection(urlPath));
                            org.w3c.dom.Element content = searchNoteXml.createElement("content");
                            content.setAttribute("type", "html");
                            content.appendChild(searchNoteXml.createCDATASection(ele.html()));
                            entry.appendChild(url);
                            entry.appendChild(content);
                            searchNoteXmlRootElement.appendChild(entry);
                        }
                    }

                    /*
                    // search.code.xml
                    for (Element ele: htmlDocument.select("code")) {
                        // NOT: div.sourceCode > pre > code
                        if (ele.hasClass("fold") || ele.hasClass("foldable")) { continue; }
                        Element grandfather = ele.parent().parent();
                        if (!grandfather.nodeName().equals("div") && !grandfather.hasClass("sourceCode")) {
                            String outerHtml = ele.outerHtml();
                            if (outerHtml.isEmpty()) { continue; }
                            if (!Config.blockCodes.contains(outerHtml)) {
                                Config.blockCodes.add(outerHtml);
                                org.w3c.dom.Element entry = searchCodeXml.createElement("entry");
                                org.w3c.dom.Element content = searchCodeXml.createElement("content");
                                content.setAttribute("type", "html");
                                content.appendChild(searchCodeXml.createCDATASection(outerHtml));
                                entry.appendChild(content);
                                searchCodeRootElement.appendChild(entry);
                            }
                        }
                    }
                    for (Element ele: htmlDocument.select("div.sourceCode")) {
                        // YES: div.sourceCode > pre > code
                        String outerHtml = ele.outerHtml();
                        if (!Config.blockCodes.contains(outerHtml)) {
                            Config.blockCodes.add(outerHtml);
                            org.w3c.dom.Element entry = searchCodeXml.createElement("entry");
                            org.w3c.dom.Element content = searchCodeXml.createElement("content");
                            content.setAttribute("type", "html");
                            content.appendChild(searchCodeXml.createCDATASection(outerHtml));
                            entry.appendChild(content);
                            searchCodeRootElement.appendChild(entry);
                        }
                    }
                    */

                    System.out.print(" done.\n");
                } else if (htmlDocumentFile.isDirectory()) {
                    queue.add(htmlDocumentFile);
                }
            }
        }
        // save to disk
        writeXml(searchFullXml, Config.dstDirPath+"/search.full.xml");
        writeXml(searchNoteXml, Config.dstDirPath+"/search.note.xml");
        writeXml(searchCodeXml, Config.dstDirPath+"/search.code.xml");
    }

    public static void main(String[] args) {

        try {
            System.setProperty("file.encoding", "UTF-8");
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < args.length; ++i) {
            if (false) {
            } else if (args[i].equals("-i") || args[i].equals("--input")) {
                if (++i < args.length) { Config.srcDirPath = args[i]; }
            } else if (args[i].equals("-o") || args[i].equals("--output")) {
                if (++i < args.length) { Config.dstDirPath = args[i]; }
            } else if (args[i].equals("-bs") || args[i].equals("--bodySelector")) {
                if (++i < args.length) { Config.bodySelector = args[i]; }
            } else if (args[i].equals("-ns") || args[i].equals("--noteSelector")) {
                if (++i < args.length) { Config.noteSelector = args[i]; }
            }
        }

        final File srcDirFile = new File(Config.srcDirPath);
        if (!srcDirFile.exists() || !srcDirFile.isDirectory()) {
            System.err.println("Invalid input directory: "+Config.srcDirPath);
            System.exit(1);
        }

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
