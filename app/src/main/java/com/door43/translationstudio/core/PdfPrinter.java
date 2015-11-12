package com.door43.translationstudio.core;

import android.content.Context;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by joel on 11/12/2015.
 */
public class PdfPrinter {
    private final TargetTranslation targetTranslation;
    private final Context context;
    private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
    private static Font redFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL, BaseColor.RED);
    private static Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
    private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);

    public PdfPrinter(Context context, TargetTranslation targetTranslation) {
        this.targetTranslation = targetTranslation;
        this.context = context;
    }

    public File print() throws Exception {
        File tempFile = File.createTempFile(targetTranslation.getId(), "pdf");

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(tempFile));
        document.open();
        addMetaData(document);
        addTitlePage(document);
        addContent(document);
        document.close();

        return tempFile;
    }

    /**
     * Adds file meta data
     * @param document
     */
    private void addMetaData(Document document) {
        ProjectTranslation projectTranslation = targetTranslation.getProjectTranslation();
        document.addTitle(projectTranslation.getTitle());
        document.addSubject(projectTranslation.getDescription());
        for(NativeSpeaker ns:targetTranslation.getTranslators()) {
            document.addAuthor(ns.name);
            document.addCreator(ns.name);
        }
        document.addCreationDate();
        document.addLanguage(targetTranslation.getTargetLanguageName());
    }

    /**
     * Adds the title page
     * @param document
     * @throws DocumentException
     */
    private void addTitlePage(Document document) throws DocumentException {
        Paragraph preface = new Paragraph();
        addEmptyLine(preface, 1);

        // book title
        ProjectTranslation projectTranslation = targetTranslation.getProjectTranslation();
        preface.add(new Paragraph(projectTranslation.getTitle(), catFont));

        addEmptyLine(preface, 1);

        // book description
        preface.add(new Paragraph(projectTranslation.getDescription(), subFont));

        document.add(preface);
        document.newPage();
    }

    /**
     * Adds the content of the book
     * @param document
     */
    private void addContent(Document document) throws DocumentException {
        for(ChapterTranslation c:targetTranslation.getChapterTranslations()) {
            // chapter title
            Anchor anchor = new Anchor(c.title, catFont);
            anchor.setName(c.title);
            com.itextpdf.text.Chapter catPart = new com.itextpdf.text.Chapter(new Paragraph(anchor), Integer.parseInt(c.getId()));

            // chapter body
            // TODO: 11/12/2015 the translation format should be stored in the target translation
            for(FrameTranslation f:targetTranslation.getFrameTranslations(c.getId(), c.getFormat())) {
                catPart.add(new Paragraph(f.body));
            }

            document.add(catPart);
        }
    }

    private static void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }
}
