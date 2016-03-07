package com.door43.translationstudio.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.door43.translationstudio.R;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.USFMVerseSpan;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.VerticalPositionMark;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 11/12/2015.
 */
public class PdfPrinter extends PdfPageEventHelper {
    private static final float VERTICAL_PADDING = 72.0f; // 1 inch
    private static final float HORIZONTAL_PADDING = 72.0f; // 1 inch
    private final TargetTranslation targetTranslation;
    private final Context context;
    private final Font titleFont;
    private final Font chapterFont;
    private final Font bodyFont;
    private final Font subFont;
    private final TranslationFormat format;
    private final Library library;
    private final SourceTranslation sourceTranslation;
    private final Font superScriptFont;
    private final BaseFont baseFont;
    private final File imagesDir;
    private boolean includeMedia = true;
    private boolean includeIncomplete = true;
    private final Map<String, PdfTemplate> tocPlaceholder = new HashMap<>();
    private final Map<String, Integer> pageByTitle = new HashMap<>();
    private final float PAGE_NUMBER_FONT_SIZE = 10;
    private PdfWriter writer;

    public PdfPrinter(Context context, Library library, TargetTranslation targetTranslation, TranslationFormat format, String fontPath, File imagesDir) throws IOException, DocumentException {
        this.targetTranslation = targetTranslation;
        this.context = context;
        this.format = format;
        this.library = library;
        this.imagesDir = imagesDir;
        this.sourceTranslation = library.getDefaultSourceTranslation(targetTranslation.getProjectId(), "en");

        baseFont = BaseFont.createFont(fontPath, "UTF-8", BaseFont.EMBEDDED);
        titleFont = new Font(baseFont, 25, Font.BOLD);
        chapterFont = new Font(baseFont, 20);
        bodyFont = new Font(baseFont, 10);
        subFont = new Font(baseFont, 10, Font.ITALIC);
        superScriptFont = new Font(baseFont, 9);
        superScriptFont.setColor(94, 94, 94);
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
        writer = PdfWriter.getInstance(document, new FileOutputStream(tempFile));
        writer.setPageEvent(this);
        document.open();
        addMetaData(document);
        addTitlePage(document);
        addTOC(document);
        addContent(document);
        document.close();

        return tempFile;
    }

    private void addTOC(Document document) throws DocumentException {
        com.itextpdf.text.Chapter intro = new com.itextpdf.text.Chapter(new Paragraph("Table of Contents ", chapterFont), 0);
        intro.setNumberDepth(0);
        document.add(intro);

        for(ChapterTranslation c:targetTranslation.getChapterTranslations()) {
            if(!includeIncomplete && !c.isTitleFinished() && !library.getChapter(sourceTranslation, c.getId()).title.isEmpty()) {
                continue;
            }
            // write chapter title
            final String title = chapterTitle(c);
            Chunk chunk = new Chunk(title).setLocalGoto(title);
            document.add(new Paragraph(chunk));

            // add placeholder for page reference
            document.add(new VerticalPositionMark() {
                @Override
                public void draw(final PdfContentByte canvas, final float llx, final float lly, final float urx, final float ury, final float y) {
                    final PdfTemplate createTemplate = canvas.createTemplate(50, 50);
                    tocPlaceholder.put(title, createTemplate);
                    canvas.addTemplate(createTemplate, urx - 50, y);
                }
            });
        }
        document.newPage();
    }

    /**
     * Adds file meta data
     * @param document
     */
    private void addMetaData(Document document) {
        ProjectTranslation projectTranslation = targetTranslation.getProjectTranslation();
        document.addTitle(projectTranslation.getTitle());
        document.addSubject(projectTranslation.getDescription());
        for(NativeSpeaker ns:targetTranslation.getContributors()) {
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

    private String chapterTitle(ChapterTranslation c) {
        String title;
        if(c.title.isEmpty()) {
            title = String.format(context.getResources().getString(R.string.label_chapter_title_detailed), "" + Integer.parseInt(c.getId()));
        } else {
            title = c.title;
        }
        return title;
    }

    private void addChapterPage(Document document, ChapterTranslation c) throws DocumentException {
        // title
        String title = chapterTitle(c);
        Anchor anchor = new Anchor(title, chapterFont);
        anchor.setName(c.title);
        Paragraph chapterParagraph = new Paragraph(anchor);
        chapterParagraph.setAlignment(Element.ALIGN_CENTER);
        com.itextpdf.text.Chapter chapter = new com.itextpdf.text.Chapter(chapterParagraph, Integer.parseInt(c.getId()));
        chapter.setNumberDepth(0);

        // table for vertical alignment
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setMinimumHeight(document.getPageSize().getHeight() - VERTICAL_PADDING * 2);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.addElement(chapter);
        table.addCell(cell);

        // place chapter title on it's own page
        document.newPage();
//        document.add(table);
        document.add(chapter);

        // update TOC
        PdfTemplate template = tocPlaceholder.get(title);
        template.beginText();
        template.setFontAndSize(baseFont, PAGE_NUMBER_FONT_SIZE);
        template.setTextMatrix(50 - baseFont.getWidthPoint(String.valueOf(writer.getPageNumber()), PAGE_NUMBER_FONT_SIZE), 0);
        template.showText(String.valueOf(writer.getPageNumber()));
        template.endText();

        document.newPage();
    }

    /**
     * Adds the content of the book
     * @param document
     */
    private void addContent(Document document) throws DocumentException, IOException {
        for(ChapterTranslation c:targetTranslation.getChapterTranslations()) {
            if(!includeIncomplete && !c.isTitleFinished() && !library.getChapter(sourceTranslation, c.getId()).title.isEmpty()) {
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
                            File imageFile = new File(imagesDir, "360px/" + targetTranslation.getProjectId() + "-" + f.getComplexId() + ".jpg");
                            if(imageFile.exists()) {
                                addImage(document, imageFile.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // TODO: 11/13/2015 render body according to the format
                    Paragraph paragraph = new Paragraph("", bodyFont);
                    String body = f.body;
                    if(format == TranslationFormat.USFM) {
                        addUSFM(paragraph, f.body);
                    } else {
                        paragraph.add(body);
                    }
                    document.add(paragraph);
                    document.add(new Paragraph(" "));
                }
            }

            // chapter reference
            if(includeIncomplete || c.isReferenceFinished()) {
                document.add(new Paragraph(c.reference, subFont));
            }
        }
    }

    private void addUSFM(Paragraph paragraph, String usfm) {
        Pattern pattern = Pattern.compile(USFMVerseSpan.PATTERN);
        Matcher matcher = pattern.matcher(usfm);
        int lastIndex = 0;
        while(matcher.find()) {
            // add preceeding text
            paragraph.add(usfm.substring(lastIndex, matcher.start()));

            // add verse
            Span verse = new USFMVerseSpan(matcher.group(1));
            Chunk chunk = new Chunk();
            chunk.setFont(superScriptFont);
            chunk.setTextRise(5f);
            if (verse != null) {
                chunk.append(verse.getHumanReadable().toString());
            } else {
                // failed to parse the verse
                chunk.append(usfm.subSequence(lastIndex, matcher.end()).toString());
            }
            chunk.append(" ");
            paragraph.add(chunk);
            lastIndex = matcher.end();
        }
        paragraph.add(usfm.subSequence(lastIndex, usfm.length()).toString());
    }

    private static void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }

    /**
     * Add image from a file path
     *
     * @param document
     * @param path
     * @throws DocumentException
     * @throws IOException
     */
    public static void addImage(Document document, String path) throws DocumentException, IOException {
        Image image = Image.getInstance(path);
        image.setAlignment(Element.ALIGN_CENTER);
        if(image.getScaledWidth() > pageWidth(document) || image.getScaledHeight() > pageHeight(document)) {
            image.scaleToFit(pageWidth(document), pageHeight(document));
        }
        document.add(new Chunk(image, 0, 0, true));
    }

    /**
     * Add Image from an input stream
     * @param document
     * @param is
     * @throws DocumentException
     * @throws IOException
     */
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

    @Override
    public void onChapter(final PdfWriter writer, final Document document, final float paragraphPosition, final Paragraph title) {
        this.pageByTitle.put(title.getContent(), writer.getPageNumber());
    }

    @Override
    public void onSection(final PdfWriter writer, final Document document, final float paragraphPosition, final int depth, final Paragraph title) {
        this.pageByTitle.put(title.getContent(), writer.getPageNumber());
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();
        String text = "" + writer.getPageNumber();

        // place page number just within the margin
        float textBase = document.bottom() - PAGE_NUMBER_FONT_SIZE;

        cb.beginText();
        cb.setFontAndSize(baseFont, PAGE_NUMBER_FONT_SIZE);
        cb.setTextMatrix((document.right() / 2) + HORIZONTAL_PADDING / 2, textBase);
        cb.showText(text);
        cb.endText();
        cb.restoreState();
    }
}
