package com.door43.translationstudio.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.itextpdf.text.*;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by joel on 11/12/2015.
 */
public class PdfPrinter {
    private static final float VERTICAL_PADDING = 72.0f; // 1 inch
    private static final float HORIZONTAL_PADDING = 72.0f; // 1 inch
    private final TargetTranslation targetTranslation;
    private final Context context;
    private final Font titleFont;
    private final Font chapterFont;
    private final Font bodyFont;
    private final Font subFont;
    private final TranslationFormat format;
    private boolean includeMedia = true;
    private boolean includeIncomplete = true;

    public PdfPrinter(Context context, TargetTranslation targetTranslation, TranslationFormat format, String fontPath) throws IOException, DocumentException {
        this.targetTranslation = targetTranslation;
        this.context = context;
        this.format = format;

        BaseFont baseFont = BaseFont.createFont(fontPath, "UTF-8", BaseFont.EMBEDDED);
        titleFont = new Font(baseFont, 25, Font.BOLD);
        chapterFont = new Font(baseFont, 20);
        bodyFont = new Font(baseFont, 10);
        subFont = new Font(baseFont, 10, Font.ITALIC);
    }

    /**
     * Include media (images) in the pdf
     * @param include
     */
    public void includeMedia(boolean include) {
        this.includeMedia = include;
    }

    /**
     * Include incomplete translations
     * @param include
     */
    public void includeIncomplete(boolean include) {
        this.includeIncomplete = include;
    }

    public File print() throws Exception {
        File tempFile = File.createTempFile(targetTranslation.getId(), "pdf");

        Document document = new Document(PageSize.LETTER, HORIZONTAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, VERTICAL_PADDING);
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
        document.addKeywords("format=" + format.getName());
    }

    /**
     * Adds the title page
     * @param document
     * @throws DocumentException
     */
    private void addTitlePage(Document document) throws DocumentException {
        Paragraph preface = new Paragraph();
        preface.setAlignment(Element.ALIGN_CENTER);
        addEmptyLine(preface, 1);

        // book title
        ProjectTranslation projectTranslation = targetTranslation.getProjectTranslation();
        Paragraph titleParagraph = (new Paragraph(projectTranslation.getTitle(), titleFont));
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        preface.add(titleParagraph);

        addEmptyLine(preface, 1);

        // book description
        preface.add(new Paragraph(projectTranslation.getDescription(), subFont));

        // table for vertical alignment
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setMinimumHeight(document.getPageSize().getHeight() - VERTICAL_PADDING * 2);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.addElement(preface);
        table.addCell(cell);

        document.add(table);
        document.newPage();
    }

    private void addChapterPage(Document document, ChapterTranslation c) throws DocumentException {
        // title
        Anchor anchor = new Anchor(c.title, chapterFont);
        anchor.setName(c.title);
        Paragraph chapterParagraph = new Paragraph(anchor);
        chapterParagraph.setAlignment(Element.ALIGN_CENTER);
        // TODO: 11/13/2015 we'll probably use this in the table of contents
//        com.itextpdf.text.Chapter chapter = new com.itextpdf.text.Chapter(chapterParagraph, Integer.parseInt(c.getId()));

        // table for vertical alignment
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setMinimumHeight(document.getPageSize().getHeight() - VERTICAL_PADDING * 2);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.addElement(chapterParagraph);
        table.addCell(cell);

        // place chapter title on it's own page
        document.newPage();
        document.add(table);
        document.newPage();
    }

    /**
     * Adds the content of the book
     * @param document
     */
    private void addContent(Document document) throws DocumentException, IOException {
        for(ChapterTranslation c:targetTranslation.getChapterTranslations()) {
            if(!includeIncomplete && !c.isTitleFinished()) {
                continue;
            }

            addChapterPage(document, c);

            // chapter body
            for(FrameTranslation f:targetTranslation.getFrameTranslations(c.getId(), this.format)) {
                if(includeIncomplete || f.isFinished()) {
                    if(includeMedia && this.format == TranslationFormat.DEFAULT) {
                        // TODO: 11/13/2015 insert frame images if we have them.
                        // TODO: 11/13/2015 eventually we need to provide the directory where to find these images which will be downloaded not in assets
                        try {
                            InputStream is = context.getAssets().open("project_images/obs/obs-en-" + f.getComplexId() + ".jpg");
                            addImage(document, is);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // TODO: 11/13/2015 render body according to the format
                    document.add(new Paragraph(f.body, bodyFont));
                    document.add(new Paragraph(" "));
                }
            }

            // chapter reference
            if(includeIncomplete || c.isReferenceFinished()) {
                document.add(new Paragraph(c.reference, subFont));
            }
        }
    }

    private static void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }

    public static void addImage(Document document, String path) throws DocumentException, IOException {
        Image image = Image.getInstance(path);
        if(image.getScaledWidth() > pageWidth(document) || image.getScaledHeight() > pageHeight(document)) {
            image.scaleToFit(pageWidth(document), pageHeight(document));
        }
        document.add(new Chunk(image, 0, 0, true));
    }

    public static void addImage(Document document, InputStream is) throws DocumentException, IOException {
        Bitmap bmp = BitmapFactory.decodeStream(is);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        Image image = Image.getInstance(stream.toByteArray());
        image.setAlignment(Element.ALIGN_CENTER);
        if(image.getScaledWidth() > pageWidth(document) || image.getScaledHeight() > pageHeight(document)) {
            image.scaleToFit(pageWidth(document), pageHeight(document));
        }
        document.add(new Chunk(image, 0, 0, true));
    }

    /**
     * Returns the height of the printable area of the page
     * @param document
     * @return
     */
    private static float pageHeight(Document document) {
        return document.getPageSize().getHeight() - VERTICAL_PADDING * 2;
    }

    /**
     * Returns the width of the printable area of the page
     * @param document
     * @return
     */
    private static float pageWidth(Document document) {
        return document.getPageSize().getWidth() - HORIZONTAL_PADDING * 2;
    }
}
