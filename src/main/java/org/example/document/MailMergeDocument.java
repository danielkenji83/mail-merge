package org.example.document;

import org.example.exceptions.MailMergeException;
import org.example.utils.MailMerge;

public interface MailMergeDocument {
    void processTexts(MailMerge mailMerge) throws MailMergeException;

    void processTables(MailMerge mailMerge) throws MailMergeException;

    void processImages(MailMerge mailMerge) throws MailMergeException;

    void processCharts(MailMerge mailMerge) throws MailMergeException;
}
