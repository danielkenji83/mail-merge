package controller;

import org.example.controller.MailMergeController;
import org.example.dto.MailMergeDTO;
import org.example.exceptions.MailMergeException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class MailMergeControllerTest {

    private MailMergeController mailMergeController;
    private InputStream template;
    private InputStream image;

    @Before
    public void setUp() {
        mailMergeController = new MailMergeController();
        template = ClassLoader.getSystemResourceAsStream("documents/odt/template.odt");
        image = ClassLoader.getSystemResourceAsStream("images/png/image.png");
    }

    @Test
    public void testProcess() throws IOException, MailMergeException, InterruptedException, TransformerException {
        byte[] response = mailMergeController.process(
                getMailMergeDTO(),
                new MockMultipartFile("template", template),
                new MultipartFile[]{ new MockMultipartFile("images", "image.png", null, image) }
        );
        Assert.assertTrue(response.length > 0);
    }

    @Test
    public void testProcessBatch() throws IOException, MailMergeException, InterruptedException, TransformerException {
        byte[] response = mailMergeController.processBatch(
                List.of(getMailMergeDTO(), getMailMergeDTO()),
                new MockMultipartFile("template", template),
                new MultipartFile[]{ new MockMultipartFile("images", "image.png", null, image) }
        );
        Assert.assertTrue(response.length > 0);
    }

    private MailMergeDTO getMailMergeDTO() {
        MailMergeDTO mailMergeDTO = new MailMergeDTO();
        mailMergeDTO.setTexts(
                Map.of("name", "anything", "email", "anything@anything.com")
        );
        mailMergeDTO.setTables(
                Map.of(
                        "Table1",
                        List.of(
                                List.of("anything1", "anything2"),
                                List.of("anything3", "anything4")
                        )
                )
        );
        mailMergeDTO.setImages(Map.of("Image1", "image.png"));
        mailMergeDTO.setCharts(
                Map.of(
                        "Object1",
                        List.of(
                                List.of("", "Column1", "Column2"),
                                List.of("Row1", "1", "2"),
                                List.of("Row2", "3", "4")
                        )
                )
        );
        return mailMergeDTO;
    }
}
