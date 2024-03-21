package utils;

import org.example.document.MailMergeODT;
import org.example.exceptions.MailMergeException;
import org.example.utils.MailMerge;
import org.example.utils.Utils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class MailMergeODTTest {
    private final MailMergeODT mailMergeODT = new MailMergeODT();
    private final MailMerge mailMerge = new MailMerge(null);
    private Document content;
    private Document manifest;
    private Document chartContent;

    @Before
    public void setUp()
            throws ParserConfigurationException, IOException, SAXException {
        try (
                var contentResource = ClassLoader.getSystemResourceAsStream(
                        "documents/xml/content.xml"
                );
                var manifestResource = ClassLoader.getSystemResourceAsStream(
                        "documents/xml/manifest.xml"
                );
                var chartContentResource = ClassLoader.getSystemResourceAsStream(
                        "documents/xml/chart_content.xml"
                )
        ) {
            content = Utils.parseDocument(contentResource);
            manifest = Utils.parseDocument(manifestResource);
            chartContent = Utils.parseDocument(chartContentResource);
            mailMerge.getDocuments().put("content.xml", content);
            mailMerge.getDocuments().put("META-INF/manifest.xml", manifest);
            mailMerge.getDocuments().put("Object 1/content.xml", chartContent);
        }
    }

    private String[][] readTable(final Document document, final String name) {
        var tableElement = Utils.getElementByTagAndAttribute(
                document,
                "table:table",
                "table:name",
                name
        );
        var rows = tableElement.getElementsByTagName("table:table-row");
        var table = new String[rows.getLength()][];
        for (int i = 0; i < rows.getLength(); i++) {
            var cells =
                    ((Element) rows.item(i)).getElementsByTagName("table:table-cell");
            table[i] = new String[cells.getLength()];
            for (int j = 0; j < cells.getLength(); j++) {
                table[i][j] = cells.item(j).getTextContent().trim();
            }
        }
        return table;
    }

    @Test
    public void testProcessTexts() throws MailMergeException {
        mailMerge.getTexts().put("<name>", "John");
        mailMerge.getTexts().put("<email>", "john@example.com");
        mailMergeODT.processTexts(mailMerge);

        Assert.assertEquals(
                "John",
                Utils
                        .getElementByTagAndAttribute(
                                content,
                                "text:database-display",
                                "text:column-name",
                                "name"
                        )
                        .getTextContent()
                        .trim()
        );
    }

    @Test(expected = MailMergeException.class)
    public void testProcessTextsMissingFields() throws MailMergeException {
        mailMerge.getTexts().put("<name>", "John");
        mailMergeODT.processTexts(mailMerge);
    }

    @Test(expected = MailMergeException.class)
    public void testProcessTextsNotFound() throws MailMergeException {
        mailMerge.getTexts().put("<Anything>", "John");
        mailMergeODT.processTexts(mailMerge);
    }

    @Test
    public void testProcessTables() throws MailMergeException {
        var table = new String[][]{
                {"name", "email"},
                {"John", "john@example.com"}
        };

        mailMerge.getTables().put("Table1", table);
        mailMergeODT.processTables(mailMerge);

        Assert.assertArrayEquals(table, readTable(content, "Table1"));
    }

    @Test
    public void testProcessTablesAppendRows() throws MailMergeException {
        var table = new String[][]{
                {"name", "email"},
                {"John", "john@example.com"},
                {"Marry", "marry@example.com"}
        };

        mailMerge.getTables().put("Table1", table);
        mailMergeODT.processTables(mailMerge);
        Assert.assertArrayEquals(table, readTable(content, "Table1"));
    }

    @Test(expected = MailMergeException.class)
    public void testProcessTablesDifferentColumnNumbers()
            throws MailMergeException {
        mailMerge
                .getTables()
                .put("Table1", new String[][]{{"name", "email"}, {"John"}});
        mailMergeODT.processTables(mailMerge);
    }

    @Test(expected = MailMergeException.class)
    public void testProcessTablesNotFound() throws MailMergeException {
        mailMerge.getTables().put("Anything", new String[][]{});
        mailMergeODT.processTables(mailMerge);
    }

    @Test
    public void testProcessImagesChangeImagePath() throws MailMergeException {
        mailMerge.getImages().put("Image1", new byte[0]);
        mailMergeODT.processImages(mailMerge);
        Assert.assertNotNull(
                Utils.getElementByTagAndAttribute(
                        content,
                        "draw:image",
                        "xlink:href",
                        "Pictures/Image1.png"
                )
        );
    }

    @Test
    public void testProcessImagesAddImagePathToManifest()
            throws MailMergeException {
        mailMerge.getImages().put("Image1", new byte[0]);
        mailMergeODT.processImages(mailMerge);
        Assert.assertNotNull(
                Utils.getElementByTagAndAttribute(
                        manifest,
                        "manifest:file-entry",
                        "manifest:full-path",
                        "Pictures/Image1.png"
                )
        );
    }

    @Test
    public void testProcessImagesAddToFiles() throws MailMergeException {
        mailMerge.getImages().put("Image1", new byte[0]);
        mailMergeODT.processImages(mailMerge);
        Assert.assertNotNull(mailMerge.getFiles().get("Pictures/Image1.png"));
    }

    @Test(expected = MailMergeException.class)
    public void testProcessImagesNotFound() throws MailMergeException {
        mailMerge.getImages().put("Anything", new byte[0]);
        mailMergeODT.processImages(mailMerge);
    }

    @Test
    public void testProcessCharts() throws MailMergeException {
        var table = new String[][]{
                {"", "Column1", "Column2"},
                {"Row1", "1", "2"},
                {"Row2", "3", "4"}
        };

        mailMerge.getCharts().put("Object1", table);
        mailMergeODT.processCharts(mailMerge);
        Assert.assertArrayEquals(table, readTable(chartContent, "local-table"));
    }

    @Test
    public void testProcessChartsFloat() throws MailMergeException {
        var table = new String[][]{
                {"", "Column1", "Column2"},
                {"Row1", "1.1", "2.2"},
                {"Row2", "3.3", "4.4"}
        };

        mailMerge.getCharts().put("Object1", table);
        mailMergeODT.processCharts(mailMerge);
        Assert.assertArrayEquals(table, readTable(chartContent, "local-table"));
    }

    @Test
    public void testProcessChartsAppendRows() throws MailMergeException {
        var table = new String[][]{
                {"", "Column1", "Column2"},
                {"Row1", "1.1", "2.2"},
                {"Row2", "3.3", "4.4"},
                {"Row3", "5.5", "6.6"}
        };

        mailMerge.getCharts().put("Object1", table);
        mailMergeODT.processCharts(mailMerge);
        Assert.assertArrayEquals(
                table[3],
                readTable(chartContent, "local-table")[3]
        );
    }

    @Test(expected = MailMergeException.class)
    public void testProcessChartsDifferentColumnNumbers()
            throws MailMergeException {
        mailMerge
                .getCharts()
                .put(
                        "Object1",
                        new String[][]{
                                {"", "Column1"},
                                {"Row1", "1.1"},
                                {"Row2", "3.3"}
                        }
                );
        mailMergeODT.processCharts(mailMerge);
    }

    @Test(expected = MailMergeException.class)
    public void testProcessChartsNotFound() throws MailMergeException {
        mailMerge.getCharts().put("Anything", new String[][]{});
        mailMergeODT.processCharts(mailMerge);
    }
}
