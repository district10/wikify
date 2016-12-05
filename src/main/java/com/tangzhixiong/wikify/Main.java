package com.tangzhixiong.wikify;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Created by tzx on 2016/12/4.
 */

class Config {
    public static String srcDirPath = ".";
    public static String dstDirPath = ".";
    public static String bodySelector = "body";     // full text search
    public static String noteSelector = "dl";       // note search
    public static LinkedHashSet<String> lineCodes = new LinkedHashSet<>();
    public static HashMap<String, HashSet<Integer>> codeRef = new HashMap<>();
    public static int index = 0;
    public static int chunksize = 10;
}

public class Main {
    public static void createSearchXml(String srcDirPath) throws Exception {
        final File srcDirFile = new File(srcDirPath);
        if (!srcDirFile.exists() || !srcDirFile.isDirectory()) {
            return;
        }
        final ArrayDeque<File> queue = new ArrayDeque<>();
        queue.add(srcDirFile);

        final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // search.full.xml
        final org.w3c.dom.Document searchFullXml = docBuilder.newDocument();
        final org.w3c.dom.Element searchFullRootElement = searchFullXml.createElement("search");
        searchFullXml.appendChild(searchFullRootElement);

        // search.note.xml
        final org.w3c.dom.Document searchNoteXml = docBuilder.newDocument();
        final org.w3c.dom.Element searchNoteXmlRootElement = searchNoteXml.createElement("search");
        searchNoteXml.appendChild(searchNoteXmlRootElement);

        // search.code.xml
        final org.w3c.dom.Document searchCodeXml = docBuilder.newDocument();
        final org.w3c.dom.Element searchCodeRootElement = searchCodeXml.createElement("search");
        searchCodeXml.appendChild(searchCodeRootElement);
        searchCodeRootElement.setAttribute("chunsize", Integer.toString(Config.chunksize));

        // search.code.block.%d.xml
        final org.w3c.dom.Document searchCodeBlockXml = docBuilder.newDocument();
        final org.w3c.dom.Element searchCodeBlockRootElement = searchCodeBlockXml.createElement("search");
        searchCodeBlockXml.appendChild(searchCodeBlockRootElement);

        while (!queue.isEmpty()) {
            File pwd = queue.poll();
            final File[] entries;
            try {
                entries = pwd.listFiles();
            } catch (NullPointerException e) { continue; }

            for (final File htmlDocumentFile: entries) {
                if (htmlDocumentFile.isDirectory()) {
                    queue.add(htmlDocumentFile);
                } else if (htmlDocumentFile.isFile() && htmlDocumentFile.getAbsolutePath().endsWith(".html")) {
                    System.out.print("Processing [" + htmlDocumentFile.getCanonicalPath().replace("\\", "/") + "]...");

                    Document htmlDocument = Jsoup.parse(htmlDocumentFile, "UTF-8");
                    String urlPath = Utils.relativePath(srcDirFile.getCanonicalPath(), htmlDocumentFile.getCanonicalPath());

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
                        for (Element ele: terms) { sb.append(ele.text()); }

                        String fulltext = Utils.normalize(sb.toString());
                        if (!fulltext.isEmpty()) {
                            content.appendChild(searchFullXml.createCDATASection(Utils.normalize(sb.toString())));

                            org.w3c.dom.Element entry = searchFullXml.createElement("entry");
                            entry.appendChild(title);
                            entry.appendChild(url);
                            entry.appendChild(content);
                            searchFullRootElement.appendChild(entry);
                        }
                    }

                    // search.note.xml
                    Elements notes = htmlDocument.select(Config.noteSelector);
                    for (final Element note: notes) {
                        if (!note.text().isEmpty()) {
                            org.w3c.dom.Element entry = searchNoteXml.createElement("entry");
                            org.w3c.dom.Element url = searchNoteXml.createElement("url");
                            url.appendChild(searchNoteXml.createCDATASection(urlPath));
                            org.w3c.dom.Element content = searchNoteXml.createElement("content");
                            content.setAttribute("type", "html");
                            content.appendChild(searchNoteXml.createCDATASection(note.html()));
                            entry.appendChild(url);
                            entry.appendChild(content);
                            searchNoteXmlRootElement.appendChild(entry);
                        }
                    }

                    // search.code.xml
                    {
                        // NOT: div.sourceCode > pre > code
                        {
                            for (final Element lineCode: htmlDocument.select("code")) {
                                if (lineCode.text().length() < 5) { continue; }
                                Element pa = lineCode.parent();
                                Element gp = pa.parent();
                                if (pa.nodeName().equals("pre") && gp.nodeName().equals("div") && gp.hasClass("sourceCode")) {
                                    continue;
                                }
                                String outerHtml = lineCode.outerHtml();
                                if (!Config.lineCodes.contains(outerHtml)) {
                                    Config.lineCodes.add(outerHtml);
                                }
                            }

                        }
                        // YES: div.sourceCode > pre > code
                        {
                            for (final Element blockCode: htmlDocument.select(".sourceCode > pre > code")) {
                                String text = blockCode.text();
                                if (text.replaceAll("\\s", "").replaceAll("\\r?\\n", "").isEmpty()) { continue; }

                                ++Config.index;

                                String lines[] = text.split("\\r?\\n");
                                for (String line : lines) {
                                    String key = line.replaceAll("\\s", "");
                                    if (key.isEmpty()) { continue; }
                                    if (!Config.codeRef.containsKey(key)) {
                                        Config.codeRef.put(key, new HashSet<>());
                                    }
                                    Config.codeRef.get(key).add(Config.index);
                                }

                                org.w3c.dom.Element entry = searchCodeBlockXml.createElement("entry");

                                org.w3c.dom.Element content = searchCodeBlockXml.createElement("content");
                                content.setAttribute("index", Integer.toString(Config.index));

                                String lang = blockCode.classNames().toString();
                                if (lang.indexOf(' ') < 0) {
                                    lang = "unknown";
                                } else {
                                    lang = lang.substring(lang.indexOf(' ')+1);
                                    if (lang.endsWith("]")) {
                                        lang = lang.substring(0, lang.length()-1);
                                    }
                                }
                                content.setAttribute("lang", lang);
                                content.setAttribute("type", "html");
                                content.appendChild(searchCodeBlockXml.createCDATASection(blockCode.html()));
                                entry.appendChild(content);
                                searchCodeBlockRootElement.appendChild(entry);
                            }
                        }
                    }

                    System.out.print(" done.\n");
                }
            }
        } // all files in dir processed

        // search.code.xml
        {
            // line code
            {
                for (String lineCode : Config.lineCodes) {
                    int start = lineCode.indexOf('>')+1;
                    int end = lineCode.lastIndexOf('<');
                    if (start > lineCode.length()) { start = 0; }
                    if (start >= end) { start = 0; end = lineCode.length(); }
                    String keyString = lineCode.substring(start, end);

                    org.w3c.dom.Element entry = searchCodeXml.createElement("entry");
                    entry.setAttribute("type", "line code");

                    org.w3c.dom.Element key = searchCodeXml.createElement("key");
                    key.appendChild(searchCodeXml.createCDATASection(keyString.replaceAll("\\s", "")));

                    org.w3c.dom.Element code = searchCodeXml.createElement("code");
                    code.appendChild(searchCodeXml.createCDATASection(lineCode));

                    entry.appendChild(key);
                    entry.appendChild(code);
                    searchCodeRootElement.appendChild(entry);
                }
            }
            // block code (block is too big, so this is just ref to real code blocks)
            {
                for (String keyString: Config.codeRef.keySet()) {
                    org.w3c.dom.Element entry = searchCodeXml.createElement("entry");
                    entry.setAttribute("type", "block code");

                    org.w3c.dom.Element key = searchCodeXml.createElement("key");
                    key.appendChild(searchCodeXml.createCDATASection(keyString));

                    org.w3c.dom.Element refs = searchCodeXml.createElement("refs");
                    refs.appendChild(searchCodeXml.createCDATASection(Config.codeRef.get(keyString).toString()));

                    entry.appendChild(key);
                    entry.appendChild(refs);
                    searchCodeRootElement.appendChild(entry);
                }
            }
        }

        // save to disk
        System.out.println();
        Utils.writeXml(searchFullXml, Config.dstDirPath+"/search.full.xml");
        Utils.writeXml(searchNoteXml, Config.dstDirPath+"/search.note.xml");
        Utils.writeXml(searchCodeXml, Config.dstDirPath+"/search.code.xml");
        Utils.writeCodeBlocks(searchCodeBlockXml, Config.dstDirPath+"/search.code.%d.xml", Config.chunksize);
    }

    public static void main(String[] args) {

        Utils.configUtf8();

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
            } else if (args[i].equals("-cs") || args[i].equals("--chunksize")) {
                if (++i < args.length) { Config.chunksize = Integer.valueOf(args[i]); }
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
        } catch (Exception e) {
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
