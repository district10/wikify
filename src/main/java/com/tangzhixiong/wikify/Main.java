package com.tangzhixiong.wikify;

import com.hankcs.hanlp.HanLP;
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
    public static String postSelector = "body";     // full text search
    public static String noteSelector = "dl";       // note search

    public static String srcDirPath = ".";
    public static String dstDirPath = ".";

    public static LinkedHashSet<String> lineCodes = new LinkedHashSet<>();
    public static HashMap<String, HashSet<Integer>> codeRef = new HashMap<>();

    public static int codeIndex = 0;
    public static int codeChunksize = 10;
    public static int postIndex = 0;
    public static int postChunksize = 5;
    public static int noteIndex = 0;
    public static int noteChunksize = 25;
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

        // search.note.xml
        final org.w3c.dom.Document searchNoteXml = docBuilder.newDocument();
        final org.w3c.dom.Element searchNoteXmlRootElement = searchNoteXml.createElement("search");
        searchNoteXmlRootElement.setAttribute("chunksize", Integer.toString(Config.noteChunksize));
        searchNoteXml.appendChild(searchNoteXmlRootElement);
        // search.note.%d.xml
        final org.w3c.dom.Document _searchNoteXml = docBuilder.newDocument();
        final org.w3c.dom.Element _searchNoteXmlRootElement = _searchNoteXml.createElement("search");
        _searchNoteXml.appendChild(_searchNoteXmlRootElement);

        // search.code.xml
        final org.w3c.dom.Document searchCodeXml = docBuilder.newDocument();
        final org.w3c.dom.Element searchCodeRootElement = searchCodeXml.createElement("search");
        searchCodeRootElement.setAttribute("chunksize", Integer.toString(Config.codeChunksize));
        searchCodeXml.appendChild(searchCodeRootElement);
        // search.code.%d.xml
        final org.w3c.dom.Document _searchCodeXml = docBuilder.newDocument();
        final org.w3c.dom.Element _searchCodeXmlRootElement = _searchCodeXml.createElement("search");
        _searchCodeXml.appendChild(_searchCodeXmlRootElement);

        // search.post.xml
        final org.w3c.dom.Document searchPostXml = docBuilder.newDocument();
        final org.w3c.dom.Element searchPostXmlRootElement = searchPostXml.createElement("search");
        searchPostXmlRootElement.setAttribute("chunksize", Integer.toString(Config.postChunksize));
        searchPostXml.appendChild(searchPostXmlRootElement);
        // search.post.%d.xml
        final org.w3c.dom.Document _searchPostXml = docBuilder.newDocument();
        final org.w3c.dom.Element _searchPostXmlRootElement = _searchPostXml.createElement("search");
        _searchPostXml.appendChild(_searchPostXmlRootElement);

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

                    ++Config.postIndex;
                    System.out.print("Processing [" + htmlDocumentFile.getCanonicalPath().replace("\\", "/") + "]...");
                    Document htmlDocument = Jsoup.parse(htmlDocumentFile, "UTF-8");

                    // url & title
                    String urlPath = Utils.relativePath(srcDirFile.getCanonicalPath(), htmlDocumentFile.getCanonicalPath());
                    String docTitle = htmlDocument.title();
                    if (docTitle.isEmpty()) {
                        docTitle = "(No Title) " + urlPath;
                    }

                    // url
                    org.w3c.dom.Element url = _searchPostXml.createElement("url");
                    url.appendChild(_searchPostXml.createCDATASection(urlPath));

                    // title
                    org.w3c.dom.Element title = _searchPostXml.createElement("title");
                    title.appendChild(_searchPostXml.createCDATASection(docTitle));

                    // POST: search.post.%d.xml
                    {
                        // content
                        org.w3c.dom.Element content = _searchPostXml.createElement("content");
                        StringBuilder sb = new StringBuilder();
                        Elements terms = htmlDocument.select(Config.postSelector);
                        for (Element ele: terms) { sb.append(ele.text()); sb.append(" "); }
                        String fulltext = sb.toString().replaceAll("\\r?\\n", " ").replaceAll("\\s+", " ");
                        content.appendChild(_searchPostXml.createCDATASection(fulltext));

                        org.w3c.dom.Element entry = _searchPostXml.createElement("entry");
                        entry.setAttribute("index", Integer.toString(Config.postIndex));
                        entry.appendChild(url);
                        entry.appendChild(title);
                        entry.appendChild(content);
                        _searchPostXmlRootElement.appendChild(entry);
                    }

                    // Note: search.note.xml & search.note.%d.xml
                    {
                        Elements notes = htmlDocument.select(Config.noteSelector);
                        for (final Element note: notes) {
                            if (!note.text().isEmpty()) {
                                ++Config.noteIndex;

                                // search.note.xml
                                {
                                    // headline
                                    org.w3c.dom.Element headline = searchNoteXml.createElement("headline");
                                    if (note.select("dt").size() != 0) {
                                        headline.appendChild(searchNoteXml.createCDATASection(note.select("dt").first().html()));
                                    } else { // extract key sentence
                                        headline.appendChild(searchNoteXml.createCDATASection(HanLP.extractSummary(note.text(), 3).toString()));
                                    }

                                    // keywords
                                    String text = note.text().replaceAll("[\\pP\\p{Punct}]", " ").toLowerCase();
                                    List<String> kws = HanLP.extractPhrase(note.text(), 2);
                                    for (String s : HanLP.extractKeyword(text, 10)) { kws.add(s); }
                                    org.w3c.dom.Element keywords = searchNoteXml.createElement("keywords");
                                    keywords.appendChild(searchNoteXml.createCDATASection(kws.toString()));

                                    // entry
                                    org.w3c.dom.Element entry = searchNoteXml.createElement("entry");
                                    entry.setAttribute("index", Integer.toString(Config.noteIndex));
                                    entry.appendChild(searchNoteXml.importNode(url, true));
                                    entry.appendChild(searchNoteXml.importNode(title, true));
                                    entry.appendChild(headline);
                                    entry.appendChild(keywords);
                                    searchNoteXmlRootElement.appendChild(entry);
                                }

                                // search.note.%d.xml
                                {
                                    org.w3c.dom.Element content = _searchNoteXml.createElement("content");
                                    content.appendChild(_searchNoteXml.createCDATASection(note.html()));

                                    org.w3c.dom.Element entry = _searchNoteXml.createElement("entry");
                                    entry.setAttribute("index", Integer.toString(Config.noteIndex));
                                    entry.appendChild(_searchNoteXml.importNode(url, true));
                                    entry.appendChild(_searchNoteXml.importNode(title, true));
                                    entry.appendChild(content);
                                    _searchNoteXmlRootElement.appendChild(entry);
                                }
                            }
                        }
                    }

                    // CODE: search.code.xml
                    {
                        for (final Element lineCode: htmlDocument.select("code")) {
                            String text = lineCode.text();
                            if (text.length() < 2) { continue; }
                            if (!lineCode.parent().nodeName().equals("pre")) {
                                // NOT: pre > code，line code (行内代码段)
                                String outerHtml = lineCode.outerHtml();
                                if (!Config.lineCodes.contains(outerHtml)) {
                                    Config.lineCodes.add(outerHtml);
                                }
                            } else {
                                // YES: pre > code，block code (块状代码段)
                                ++Config.codeIndex;

                                String lines[] = text.split("\\r?\\n");
                                for (String line : lines) {
                                    String key = line.replaceAll("\\s", "");
                                    if (key.isEmpty()) { continue; }
                                    if (!Config.codeRef.containsKey(key)) {
                                        Config.codeRef.put(key, new HashSet<>());
                                    }
                                    Config.codeRef.get(key).add(Config.codeIndex);
                                }

                                org.w3c.dom.Element content = _searchCodeXml.createElement("content");
                                String lang = lineCode.classNames().toString();
                                content.setAttribute("lang", Utils.parseLanguage(lang));
                                content.appendChild(_searchCodeXml.createCDATASection(lineCode.html()));

                                org.w3c.dom.Element entry = _searchCodeXml.createElement("entry");
                                entry.setAttribute("index", Integer.toString(Config.codeIndex));
                                entry.appendChild(_searchCodeXml.importNode(url, true));
                                entry.appendChild(_searchCodeXml.importNode(title, true));
                                entry.appendChild(content);
                                _searchCodeXmlRootElement.appendChild(entry);
                            }
                        }
                    }

                    // POST: search.post.xml
                    {
                        // remove code, a
                        htmlDocument.select("a,code").remove();

                        Elements terms = htmlDocument.select(Config.postSelector);
                        StringBuilder sb = new StringBuilder();
                        for (Element ele: terms) { sb.append(ele.text()); sb.append(" "); }
                        String fulltext = sb.toString()
                                // .replaceAll("[\\pP\\p{Punct}]", " ")
                                .replaceAll("\\r?\\n", " ")
                                .replace(" -< ", " ")
                                .replace(" +< ", " ")
                                .replaceAll("\\s+", " ")
                                .toLowerCase();

                        // keywords
                        org.w3c.dom.Element keywords = searchPostXml.createElement("keywords");
                        List<String> kws = HanLP.extractPhrase(fulltext, 5);
                        for (String s : HanLP.extractKeyword(fulltext, 25)) { kws.add(s); }
                        // <meta name="keywords" content="pandoc, wikify" />
                        for (Element element : htmlDocument.getElementsByAttributeValue("name", "keywords")) {
                            kws.add(element.attr("content"));
                        }
                        keywords.appendChild(searchPostXml.createCDATASection(kws.toString()));

                        // summary
                        org.w3c.dom.Element summary = searchPostXml.createElement("summary");
                        List<String> sentenceList = HanLP.extractSummary(fulltext, 5);
                        summary.appendChild(searchPostXml.createCDATASection(sentenceList.toString()));

                        org.w3c.dom.Element entry = searchPostXml.createElement("entry");
                        entry.setAttribute("index", Integer.toString(Config.postIndex));
                        entry.appendChild(searchPostXml.importNode(url, true));
                        entry.appendChild(searchPostXml.importNode(title, true));
                        entry.appendChild(keywords);
                        entry.appendChild(summary);
                        searchPostXmlRootElement.appendChild(entry);
                    }

                    System.out.print(" done.\n");
                }
            }
        } // all files in dir processed

        // CODE: search.code.xml
        {
            // line code
            {
                for (final String lineCode: Config.lineCodes) {
                    int start = lineCode.indexOf('>')+1;
                    int end = lineCode.lastIndexOf('<');
                    if (start > lineCode.length()) { start = 0; }
                    if (start >= end) { start = 0; end = lineCode.length(); }
                    String keyString = lineCode.substring(start, end);

                    // key
                    org.w3c.dom.Element key = searchCodeXml.createElement("key");
                    key.appendChild(searchCodeXml.createCDATASection(keyString.replaceAll("\\s", "")));

                    // code
                    org.w3c.dom.Element code = searchCodeXml.createElement("code");
                    code.appendChild(searchCodeXml.createCDATASection(lineCode));

                    // entry
                    org.w3c.dom.Element entry = searchCodeXml.createElement("entry");
                    entry.setAttribute("type", "line code");
                    entry.appendChild(key);
                    entry.appendChild(code);
                    searchCodeRootElement.appendChild(entry);
                }
            }
            // block code (block is too big, so this is just ref to real code blocks)
            {
                for (String keyString: Config.codeRef.keySet()) {
                    org.w3c.dom.Element key = searchCodeXml.createElement("key");
                    key.appendChild(searchCodeXml.createCDATASection(keyString));

                    org.w3c.dom.Element refs = searchCodeXml.createElement("refs");
                    refs.appendChild(searchCodeXml.createCDATASection(Config.codeRef.get(keyString).toString()));

                    org.w3c.dom.Element entry = searchCodeXml.createElement("entry");
                    entry.setAttribute("type", "block code");
                    entry.appendChild(key);
                    entry.appendChild(refs);
                    searchCodeRootElement.appendChild(entry);
                }
            }
        }

        // save to disk
        System.out.println();
        {
            Utils.writeXml(searchCodeXml, Config.dstDirPath+"/search.code.xml");
            Utils.writeCodeBlocks(_searchCodeXml, Config.dstDirPath+"/search.code.%d.xml", Config.codeChunksize);
        }
        {
            Utils.writeXml(searchNoteXml, Config.dstDirPath+"/search.note.xml");
            Utils.writeCodeBlocks(_searchNoteXml, Config.dstDirPath+"/search.note.%d.xml", Config.noteChunksize);
        }
        {
            Utils.writeXml(searchPostXml, Config.dstDirPath+"/search.post.xml");
            Utils.writeCodeBlocks(_searchPostXml, Config.dstDirPath+"/search.post.%d.xml", Config.postChunksize);
        }
    }

    public static void main(String[] args) {

        Utils.configUtf8();

        for (int i = 0; i < args.length; ++i) {
            if (false) {
            } else if (args[i].equals("-i") || args[i].equals("--input")) {
                if (++i < args.length) { Config.srcDirPath = args[i]; }
            } else if (args[i].equals("-o") || args[i].equals("--output")) {
                if (++i < args.length) { Config.dstDirPath = args[i]; }
            } else if (args[i].equals("-ps") || args[i].equals("--postSelector")) {
                if (++i < args.length) { Config.postSelector = args[i]; }
            } else if (args[i].equals("-ns") || args[i].equals("--noteSelector")) {
                if (++i < args.length) { Config.noteSelector = args[i]; }
            } else if (args[i].equals("-ccs") || args[i].equals("--codeChunksize")) {
                if (++i < args.length) { Config.codeChunksize = Integer.valueOf(args[i]); }
            } else if (args[i].equals("-ncs") || args[i].equals("--noteChunksize")) {
                if (++i < args.length) { Config.noteChunksize = Integer.valueOf(args[i]); }
            } else if (args[i].equals("-pcs") || args[i].equals("--postChunksize")) {
                if (++i < args.length) { Config.postChunksize = Integer.valueOf(args[i]); }
            }
        }

        System.err.printf("__________ wikify  configs __________\n");
        System.err.printf("Code chunksize: %d\n", Config.codeChunksize);
        System.err.printf("Note chunksize: %d\n", Config.noteChunksize);
        System.err.printf("Post chunksize: %d\n", Config.postChunksize);
        System.err.printf("Post selector: \"%s\"\n", Config.postSelector);
        System.err.printf("Note selector: \"%s\"\n", Config.noteSelector);
        System.err.printf("-------------------------------------\n");

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
