package com.tangzhixiong.wikify;

import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.HashSet;

/**
 * Created by tzx on 2016/12/5.
 */
public class Utils {
    public static HashSet<String> langs = new HashSet<>();
    public static TransformerFactory tFactory = null;
    public static Transformer transformer = null;
    static {

        tFactory = TransformerFactory.newInstance();
        try {
            transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // got these by `pandoc -v'
        String[] init = {
                "abc", "actionscript", "ada", "agda", "apache", "asn1", "asp", "awk", "bash",
                "bibtex", "boo", "c", "changelog", "clojure", "cmake", "coffee", "coldfusion",
                "commonlisp", "cpp", "cs", "css", "curry", "d", "diff", "djangotemplate",
                "dockerfile", "dot", "doxygen", "doxygenlua", "dtd", "eiffel", "elixir",
                "email", "erlang", "fasm", "fortran", "fsharp", "gcc", "glsl", "gnuassembler",
                "go", "hamlet", "haskell", "haxe", "html", "idris", "ini", "isocpp", "java",
                "javadoc", "javascript", "json", "jsp", "julia", "kotlin", "latex", "lex",
                "lilypond", "literatecurry", "literatehaskell", "llvm", "lua", "m4",
                "makefile", "mandoc", "markdown", "mathematica", "matlab", "maxima",
                "mediawiki", "metafont", "mips", "modelines", "modula2", "modula3",
                "monobasic", "nasm", "noweb", "objectivec", "objectivecpp", "ocaml", "octave",
                "opencl", "pascal", "perl", "php", "pike", "postscript", "prolog", "pure",
                "python", "r", "relaxng", "relaxngcompact", "rest", "rhtml", "roff", "ruby",
                "rust", "scala", "scheme", "sci", "sed", "sgml", "sql", "sqlmysql",
                "sqlpostgresql", "tcl", "tcsh", "texinfo", "verilog", "vhdl", "xml", "xorg",
                "xslt", "xul", "yacc", "yaml", "zsh" };
        for (String s : init) {
            langs.add(s);
        }

    }

    public static void main(String[] args) {
    }

    public static String parseLanguage(String classes) {
        // [sourceCode cpp], [sourceCode java]
        if (classes.startsWith("[")) { classes = classes.substring(1); }
        if (classes.endsWith("]")) { classes = classes.substring(0, classes.length()-1); }
        for (String s : classes.split(" ")) {
            if (langs.contains(s.toLowerCase())) {
                return s;
            }
        }
        return "unknown";
    }

    public static String filterOut(String text, String filter) {
        HashSet<String> dict = new HashSet<>();
        String[] keywords = {
                "abstract", "assert", "auto", "boolean", "break", "byte", "case", "catch",
                "char", "class", "const", "continue", "debugger", "default", "delete", "do",
                "double", "else", "entry", "enum", "extends", "extern", "false", "final",
                "finally", "float", "for", "function", "goto", "if", "implements", "import",
                "in", "instanceof", "int", "interface", "long", "native", "new", "null",
                "package", "protected", "public", "register", "return", "short", "signed",
                "sizeof", "static", "strictfp", "struct", "super", "switch", "synchronized",
                "this", "throw", "throws", "transient", "true", "try", "typeof", "union",
                "unsigned", "unused", "var", "void", "volatile", "while", "with" };
        for (String s : filter.split(" ")) {
            dict.add(s);
        }
        // text = text.replace(" "+s+" ", " ");
        return text;
    }

    public static void configUtf8() {
        try {
            System.setProperty("file.encoding", "UTF-8");
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String relativePath(String basedir, String path) {
        if (basedir.length()+1 >= path.length()) {
            return "";
        }
        return path.substring(1+basedir.length()).replace("\\", "/");
    }

    public static void writeXml(org.w3c.dom.Document doc, String path) {
        try {
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(path));
            transformer.transform(source, result);
            System.err.println("XML search file saved to [" + path.replace("\\", "/") + "].");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeCodeBlocks(org.w3c.dom.Document doc, String format, int chunksize) {
        if (chunksize <= 0) { chunksize = 100; }
        try {

            final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            final org.w3c.dom.Document xml = docBuilder.newDocument();
            final org.w3c.dom.Element root = xml.createElement("search");
            xml.appendChild(root);

            NodeList entries = doc.getElementsByTagName("entry");
            int length = entries.getLength();
            int i = 0;
            for (i = 0; i < length; ) {
                root.appendChild(xml.importNode(entries.item(i), true));
                ++i;
                if (i%chunksize == 0) {
                    writeXml(xml, String.format(format, i/chunksize));
                    while (root.hasChildNodes()) {
                        root.removeChild(root.getFirstChild());
                    }
                }
            }
            if (root.getChildNodes().getLength() > 0) {
                writeXml(xml, String.format(format, 1+i/chunksize));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
