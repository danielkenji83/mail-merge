package org.example.controller;

import org.example.dto.MailMergeDTO;
import org.example.exceptions.MailMergeException;
import org.example.utils.MailMerge;
import org.example.utils.Utils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

@RestController
public class MailMergeController {
    @PostMapping(
            value = "/",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public byte[] process(
            @RequestPart("data") MailMergeDTO mailMergeDTO,
            @RequestPart("template") MultipartFile template,
            @RequestPart("images") MultipartFile[] images
    ) throws IOException, MailMergeException, InterruptedException, TransformerException {
        Path templateFile = createTempTemplate(template);
        try {
            return MailMerge
                    .builder(new ZipFile(templateFile.toFile()))
                    .texts(mailMergeDTO.getTexts())
                    .tables(mailMergeDTO.getTables())
                    .images(getImageMap(mailMergeDTO, images))
                    .charts(mailMergeDTO.getCharts())
                    .build()
                    .process();
        } finally {
            Files.deleteIfExists(templateFile);
        }
    }

    @PostMapping(
            value = "/batch",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/zip"
    )
    public byte[] processBatch(
            @RequestPart("data") List<MailMergeDTO> mailMergeDTOs,
            @RequestPart("template") MultipartFile template,
            @RequestPart("images") MultipartFile[] images
    ) throws IOException, MailMergeException, InterruptedException, TransformerException {
        Path templateFile = createTempTemplate(template);
        try {
            byte[][] files = new byte[mailMergeDTOs.size()][];
            for (int i = 0; i < mailMergeDTOs.size(); i++) {
                files[i] = MailMerge
                        .builder(new ZipFile(templateFile.toFile()))
                        .texts(mailMergeDTOs.get(i).getTexts())
                        .tables(mailMergeDTOs.get(i).getTables())
                        .images(getImageMap(mailMergeDTOs.get(i), images))
                        .charts(mailMergeDTOs.get(i).getCharts())
                        .build()
                        .process();
            }
            return Utils.zipFiles(files);
        } finally {
            Files.deleteIfExists(templateFile);
        }
    }

    private Path createTempTemplate(MultipartFile template) throws IOException, MailMergeException {
        if (template.isEmpty()) {
            throw new MailMergeException("template file is missing");
        }

        Path templateFile = Files.createTempFile("mailmerge-", UUID.randomUUID().toString());
        Files.copy(
                template.getInputStream(),
                templateFile,
                StandardCopyOption.REPLACE_EXISTING
        );
        return templateFile;
    }

    private Map<String, byte[]> getImageMap(MailMergeDTO mailMergeDTO, MultipartFile[] images) throws IOException, MailMergeException {
        Map<String, byte[]> imageByOriginalFilename = new HashMap<>();
        for (MultipartFile image : images) {
            if (image.isEmpty()) {
                throw new MailMergeException("image file is missing");
            }

            imageByOriginalFilename.put(
                    image.getOriginalFilename(),
                    image.getBytes()
            );
        }
        return mailMergeDTO
                .getImages()
                .entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> imageByOriginalFilename.get(e.getValue())
                        )
                );
    }
}
