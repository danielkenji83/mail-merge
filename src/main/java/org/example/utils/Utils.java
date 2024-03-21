package org.example.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Utils {

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static Path createTempDirectory() throws IOException {
        return Files.createTempDirectory(UUID.randomUUID().toString());
    }

    public static byte[] convertToPdf(
            Path source,
            Path destination,
            String fileName
    )
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "soffice",
                "--headless",
                "--convert-to",
                "pdf:writer_pdf_Export",
                source.toString(),
                "--outdir",
                destination.toString(),
                fileName
        );
        Process process = processBuilder.start();

        process.waitFor();
        if (process.exitValue() != 0) {
            try (InputStream errorStream = process.getErrorStream()) {
                errorStream.transferTo(System.out);
            }
        }

        return Files.readAllBytes(destination.resolve(fileName));
    }

    public static boolean isNumeric(final String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public static Document parseDocument(final InputStream inputStream)
            throws ParserConfigurationException, IOException, SAXException {
        return DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(inputStream);
    }

    public static Element getElementByTagAndAttribute(
            Document document,
            String tag,
            String attributeKey,
            String attributeValue
    ) {
        NodeList nodeList = document.getElementsByTagName(tag);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            Node attribute = node.getAttributes().getNamedItem(attributeKey);
            if (
                    attribute != null && attribute.getTextContent().equals(attributeValue)
            ) {
                return (Element) node;
            }
        }
        return null;
    }

    public static byte[] zipFiles(byte[][] files) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            int count = 0;
            for (byte[] file : files) {
                ZipEntry zipEntry1 = new ZipEntry(String.format("%04d.pdf", count));
                zipOutputStream.putNextEntry(zipEntry1);
                zipOutputStream.write(file);
                zipOutputStream.closeEntry();
                count++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
