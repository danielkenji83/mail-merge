package org.example.utils;

import org.apache.commons.io.FileUtils;
import org.example.document.MailMergeODT;
import org.example.exceptions.MailMergeException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class MailMerge {
    private static final String DEFAULT_FILE_NAME = "template";

    private final ZipFile template;
    private final Map<String, String> texts = new HashMap<>();
    private final Map<String, String[][]> tables = new HashMap<>();
    private final Map<String, byte[]> images = new HashMap<>();
    private final Map<String, String[][]> charts = new HashMap<>();

    private final Map<String, Document> documents = new HashMap<>();
    private final Map<String, byte[]> files = new HashMap<>();

    public MailMerge(final ZipFile template) {
        this.template = template;
    }

    public static MailMergeBuilder builder(final ZipFile template) {
        return new MailMergeBuilder(template);
    }

    public Map<String, String> getTexts() {
        return texts;
    }

    public Map<String, String[][]> getTables() {
        return tables;
    }

    public Map<String, byte[]> getImages() {
        return images;
    }

    public Map<String, String[][]> getCharts() {
        return charts;
    }

    public Map<String, Document> getDocuments() {
        return documents;
    }

    public Map<String, byte[]> getFiles() {
        return files;
    }

    public byte[] process()
            throws IOException, TransformerException, InterruptedException, MailMergeException {
        MailMergeODT mailMergeODT = new MailMergeODT();
        initializeDocuments();
        mailMergeODT.processTexts(this);
        mailMergeODT.processTables(this);
        mailMergeODT.processImages(this);
        mailMergeODT.processCharts(this);
        return writeChanges();
    }

    private void initializeDocuments() throws MailMergeException {
        Enumeration<? extends ZipEntry> entries = template.entries();
        documents.clear();
        files.clear();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".xml")) {
                try {
                    Document document = DocumentBuilderFactory
                            .newInstance()
                            .newDocumentBuilder()
                            .parse(template.getInputStream(entry));
                    documents.put(entry.getName(), document);
                } catch (SAXException | IOException | ParserConfigurationException e) {
                    throw new MailMergeException(e.getMessage(), e);
                }
            }
        }
    }

    private void writeDocument(
            ZipOutputStream zipOutputStream,
            Document document
    )
            throws TransformerException {
        TransformerFactory
                .newInstance()
                .newTransformer()
                .transform(new DOMSource(document), new StreamResult(zipOutputStream));
    }

    private byte[] writeChanges()
            throws IOException, TransformerException, InterruptedException {
        Path tempDirectory = Utils.createTempDirectory();
        Path tempFile = tempDirectory.resolve(DEFAULT_FILE_NAME + ".odt");
        try (
                ZipOutputStream mergedTemplate = new ZipOutputStream(
                        Files.newOutputStream(tempFile.toAbsolutePath())
                )
        ) {
            Enumeration<? extends ZipEntry> entries = template.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                mergedTemplate.putNextEntry(entry);
                if (documents.containsKey(entry.getName())) {
                    writeDocument(mergedTemplate, documents.get(entry.getName()));
                } else {
                    template.getInputStream(entry).transferTo(mergedTemplate);
                }
                mergedTemplate.closeEntry();
            }

            for (final Map.Entry<String, byte[]> file : files.entrySet()) {
                mergedTemplate.putNextEntry(new ZipEntry(file.getKey()));
                mergedTemplate.write(file.getValue());
                mergedTemplate.closeEntry();
            }

            mergedTemplate.finish();
            template.close();
        }

        try {
            return Utils.convertToPdf(
                    tempFile,
                    tempDirectory,
                    DEFAULT_FILE_NAME + ".pdf"
            );
        } finally {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    public static class MailMergeBuilder {
        private final MailMerge mailMerge;

        public MailMergeBuilder(final ZipFile template) {
            this.mailMerge = new MailMerge(template);
        }

        public MailMergeBuilder texts(final Map<String, String> texts) {
            texts.forEach((k, v) -> mailMerge.texts.put("<" + k + ">", v));
            return this;
        }

        public MailMergeBuilder tables(Map<String, List<List<String>>> tables) {
            tables.forEach(
                    (k, v) ->
                            mailMerge.tables.put(
                                    k,
                                    v
                                            .stream()
                                            .map(a -> a.toArray(String[]::new))
                                            .toArray(String[][]::new)
                            )
            );
            return this;
        }

        public MailMergeBuilder images(final Map<String, byte[]> images) {
            images.forEach((k, v) -> mailMerge.images.put(k, v.clone()));
            return this;
        }

        public MailMergeBuilder charts(Map<String, List<List<String>>> charts) {
            charts.forEach(
                    (k, v) ->
                            mailMerge.charts.put(
                                    k,
                                    v
                                            .stream()
                                            .map(a -> a.toArray(String[]::new))
                                            .toArray(String[][]::new)
                            )
            );
            return this;
        }

        public MailMerge build() {
            return mailMerge;
        }
    }
}
