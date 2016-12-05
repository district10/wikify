package com.tangzhixiong.wikify;

import org.w3c.dom.Node;
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

/**
 * Created by tzx on 2016/12/5.
 */
public class Utils {
    public static void main(String[] args) {
        System.out.println("aoeiaoei".indexOf(" "));
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
    }

    public static String normalize(String input) {
        return input.toString()
                .replaceAll("[\\pP\\p{Punct}]", " ")
                .replaceAll("\\r?\\n", " ")
                .replaceAll("\\s+", " ");
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
            System.out.println("XML search file saved to [" + path.replace("\\", "/") + "].");
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
