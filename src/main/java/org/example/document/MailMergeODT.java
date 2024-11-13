package org.example.document;

import org.example.exceptions.MailMergeException;
import org.example.utils.MailMerge;
import org.example.utils.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MailMergeODT implements MailMergeDocument {
    private static final String PICTURES_PATH = "Pictures";
    private static final String CONTENT_XML_PATH = "content.xml";
    private static final String MANIFEST_XML_PATH = "META-INF/manifest.xml";

    @Override
    public void processTexts(final MailMerge mailMerge)
            throws MailMergeException {
        if (mailMerge.getTexts().isEmpty()) {
            return;
        }
        Map<String, String> texts = mailMerge.getTexts();
        Map<String, Document> documents = mailMerge.getDocuments();
        Document content = documents.get(CONTENT_XML_PATH);
        NodeList nodeList = content.getElementsByTagName("text:database-display");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String textContent = node.getTextContent();
            if (texts.containsKey(textContent)) {
                node.setTextContent(texts.get(textContent));
            } else {
                throw new MailMergeException(
                        String.format("Field %s is missing", textContent)
                );
            }
        }
    }

    @Override
    public void processTables(final MailMerge mailMerge)
            throws MailMergeException {
        processTables(
                mailMerge.getDocuments().get(CONTENT_XML_PATH),
                mailMerge.getTables()
        );
    }

    private void processTables(Document document, Map<String, String[][]> tables)
            throws MailMergeException {
        if (tables.isEmpty()) {
            return;
        }
        Set<String> notProcessed = new HashSet<>(tables.keySet());
        NodeList tablesNodeList = document.getElementsByTagName("table:table");
        for (int i = 0; i < tablesNodeList.getLength(); i++) {
            Node tableNode = tablesNodeList.item(i);
            String tableName = tableNode
                    .getAttributes()
                    .getNamedItem("table:name")
                    .getTextContent();
            String[][] table = tables.get(tableName);

            if (table != null) {
                NodeList rows =
                        ((Element) tableNode).getElementsByTagName("table:table-row");
                appendRows(rows, tableNode, table.length);
                checkIfTableHasSameNumberOfColumns(table, rows, tableName);
                writeTable(table, rows);
                notProcessed.remove(tableName);
            }
        }

        if (!notProcessed.isEmpty()) {
            throw new MailMergeException(
                    String.format("Table %s not found", notProcessed.stream().findFirst())
            );
        }
    }

    @Override
    public void processImages(final MailMerge mailMerge)
            throws MailMergeException {
        Map<String, byte[]> images = mailMerge.getImages();
        if (images.isEmpty()) {
            return;
        }
        Set<String> notProcessed = new HashSet<>(images.keySet());
        Map<String, Document> documents = mailMerge.getDocuments();
        Document content = documents.get(CONTENT_XML_PATH);
        Document manifest = documents.get(MANIFEST_XML_PATH);
        NodeList parents = content.getElementsByTagName("draw:frame");

        for (int i = 0; i < parents.getLength(); i++) {
            Node parent = parents.item(i);
            String drawName = parent
                    .getAttributes()
                    .getNamedItem("draw:name")
                    .getTextContent();

            if (images.containsKey(drawName)) {
                Node child = parent.getFirstChild();
                Node xlinkHref = child.getAttributes().getNamedItem("xlink:href");
                String imagePath = PICTURES_PATH + "/" + drawName + ".png";
                xlinkHref.setTextContent(imagePath);
                Element element = manifest.createElement("manifest:file-entry");
                element.setAttribute("manifest:full-path", imagePath);
                element.setAttribute("manifest:media-type", "image/png");
                manifest
                        .getElementsByTagName("manifest:manifest")
                        .item(0)
                        .appendChild(element);

                mailMerge.getFiles().put(imagePath, images.get(drawName));
                notProcessed.remove(drawName);
            }
        }

        if (!notProcessed.isEmpty()) {
            throw new MailMergeException(
                    String.format("Image %s not found", notProcessed.stream().findFirst())
            );
        }
    }

    @Override
    public void processCharts(final MailMerge mailMerge)
            throws MailMergeException {
        Map<String, String[][]> charts = mailMerge.getCharts();
        if (charts.isEmpty()) {
            return;
        }
        Set<String> notProcessed = new HashSet<>(charts.keySet());
        Map<String, Document> documents = mailMerge.getDocuments();
        Document content = documents.get(CONTENT_XML_PATH);
        NodeList parents = content.getElementsByTagName("draw:frame");
        for (int i = 0; i < parents.getLength(); i++) {
            Node parent = parents.item(i);
            String drawName = parent
                    .getAttributes()
                    .getNamedItem("draw:name")
                    .getTextContent();
            String[][] table = charts.get(drawName);

            if (table != null) {
                Node child = parent.getFirstChild();
                Node xlinkHref = child.getAttributes().getNamedItem("xlink:href");
                String chartContextPath =
                        xlinkHref.getTextContent().replace("./", "") + "/" + CONTENT_XML_PATH;

                processTables(
                        documents.get(chartContextPath),
                        Map.of("local-table", table)
                );
                notProcessed.remove(drawName);
            }
        }

        if (!notProcessed.isEmpty()) {
            throw new MailMergeException(
                    String.format("Chart %s not found", notProcessed.stream().findFirst())
            );
        }
    }

    private void appendRows(
            NodeList rows,
            Node tableNode,
            int tableRowNumber
    ) {
        for (int i = rows.getLength(); i < tableRowNumber; i++) {
            Node lastRow = rows.item(rows.getLength() - 1);
            tableNode.appendChild(lastRow.cloneNode(true));
        }
    }

    private void checkIfTableHasSameNumberOfColumns(
            String[][] table,
            NodeList rows,
            String tableName
    )
            throws MailMergeException {
        for (int i = 0; i < table.length; i++) {
            Node rowNode = rows.item(i);
            NodeList cells =
                    ((Element) rowNode).getElementsByTagName("table:table-cell");

            if (cells.getLength() != table[i].length) {
                throw new MailMergeException(
                        String.format(
                                "Expected %d columns but got %d columns in table %s",
                                cells.getLength(),
                                table[i].length,
                                tableName
                        )
                );
            }
        }
    }

    private void writeTable(final String[][] table, final NodeList rows) {
        for (int i = 0; i < table.length; i++) {
            Node row = rows.item(i);
            NodeList cells = ((Element) row).getElementsByTagName("table:table-cell");

            for (int j = 0; j < table[0].length; j++) {
                Element cell = (Element) cells.item(j);
                String type = Utils.isNumeric(table[i][j]) ? "float" : "string";
                cell.setAttribute("office:value", table[i][j]);
                cell.setAttribute("office:value-type", type);
                cell.getElementsByTagName("text:p").item(0).setTextContent(table[i][j]);
            }
        }
    }
}
