package com.door43.translationstudio.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.spannables.Span;
import com.door43.translationstudio.ui.spannables.USFMVerseSpan;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.VerticalPositionMark;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.resourcecontainer.Project;
import org.unfoldingword.resourcecontainer.Resource;
import org.unfoldingword.resourcecontainer.ResourceContainer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
    private final Font boldBodyFont;
    private final Font underlineBodyFont;
    private final Font subFont;
    private final Font headingFont;
    private final TranslationFormat format;
    private final Door43Client library;
    private final ResourceContainer sourceContainer;
    private final Font superScriptFont;
    private final BaseFont baseFont;
    private final File imagesDir;
    private boolean includeMedia = true;
    private boolean includeIncomplete = true;
    private final Map<String, PdfTemplate> tocPlaceholder = new HashMap<>();
    private final Map<String, Integer> pageByTitle = new HashMap<>();
    private final float PAGE_NUMBER_FONT_SIZE = 10;
    private PdfWriter writer;
    private Paragraph mCurrentParagraph;


    public PdfPrinter(Context context, Door43Client library, TargetTranslation targetTranslation, TranslationFormat format, String fontPath, File imagesDir) throws IOException, DocumentException {
        this.targetTranslation = targetTranslation;
        this.context = context;
        this.format = format;
        this.library = library;
        this.imagesDir = imagesDir;
        Project p = library.index().getProject("en", targetTranslation.getProjectId(), true);
        java.util.List<Resource> resources = library.index().getResources(p.languageSlug, p.slug);
        ResourceContainer rc = null;
        try {
            rc = library.open("en", targetTranslation.getProjectId(), resources.get(0).slug);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.sourceContainer = rc;

        baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        titleFont = new Font(baseFont, 25, Font.BOLD);
        chapterFont = new Font(baseFont, 20);
        bodyFont = new Font(baseFont, 10);
        boldBodyFont = new Font(baseFont, 10, Font.BOLD);
        headingFont = new Font(baseFont, 14, Font.BOLD);
        underlineBodyFont = new Font(baseFont, 10, Font.UNDERLINE);
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
        File tempFile = File.createTempFile(targetTranslation.getId(), ".pdf");

        Document document = new Document(PageSize.LETTER, HORIZONTAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, VERTICAL_PADDING);
        writer = PdfWriter.getInstance(document, new FileOutputStream(tempFile));
        writer.setPageEvent(this);
        document.open();
        addMetaData(document);
        addTitlePage(document);
        addLicensePage(document);
        addTOC(document);
        addContent(document);
        document.close();

        return tempFile;
    }

    private void addTOC(Document document) throws DocumentException {
        document.newPage();
        document.resetPageCount(); // disable page numbering for this page (TOC)

        String toc = App.context().getResources().getString(R.string.table_of_contents);
        com.itextpdf.text.Chapter intro = new com.itextpdf.text.Chapter(new Paragraph(toc, chapterFont), 0);
        intro.setNumberDepth(0);
        document.add(intro);

        for(ChapterTranslation c:targetTranslation.getChapterTranslations()) {
            if(!includeIncomplete && !c.isTitleFinished() && !sourceContainer.readChunk(c.getId(), "title").isEmpty()) {
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
        document.resetPageCount(); // disable page numbering for this page (title)

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
        com.itextpdf.text.Chapter chapter = new com.itextpdf.text.Chapter(chapterParagraph, Util.strToInt(c.getId(), 0));
        chapter.setNumberDepth(0);

        // table for vertical alignment
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setMinimumHeight(document.getPageSize().getHeight() - VERTICAL_PADDING * 2);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);

        document.add(chapter);
        document.add(new Paragraph(" ")); // put whitespace between chapter title and text

        // update TOC
        PdfTemplate template = tocPlaceholder.get(title);
        template.beginText();
        template.setFontAndSize(baseFont, PAGE_NUMBER_FONT_SIZE);
        template.setTextMatrix(50 - baseFont.getWidthPoint(String.valueOf(writer.getPageNumber()), PAGE_NUMBER_FONT_SIZE), 0);
        template.showText(String.valueOf(writer.getPageNumber()));
        template.endText();
    }

    /**
     * Adds the content of the book
     * @param document
     */
    private void addContent(Document document) throws DocumentException, IOException {
        for(ChapterTranslation c:targetTranslation.getChapterTranslations()) {
            boolean chapter0 = (Util.strToInt(c.getId(), 0) == 0);
            if(!chapter0) { // if chapter 00, then skip title since that was already printed as first page.
                if (includeIncomplete || c.isTitleFinished() || sourceContainer.readChunk(c.getId(), "title").isEmpty()) {
                    addChapterPage(document, c);
                }
            }

            // get chapter body
            FrameTranslation[] frames = targetTranslation.getFrameTranslations(c.getId(), this.format);
            ArrayList<FrameTranslation> frameList = ExportUsfm.sortFrameTranslations(frames);
            for(FrameTranslation f: frameList) {
                if(includeIncomplete || f.isFinished()) {
                    if(includeMedia && this.format == TranslationFormat.DEFAULT) {
                        // TODO: 11/13/2015 insert frame images if we have them.
                        // TODO: 11/13/2015 eventually we need to provide the directory where to find these images which will be downloaded not in assets
                        try {
                            File imageFile = new File(imagesDir, targetTranslation.getProjectId() + "-" + f.getComplexId() + ".jpg");
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

        String pageNumberShown = "";
        int pageNumber = writer.getPageNumber();
        if(pageNumber > 0) { // only add page number if above zero
            pageNumberShown += pageNumber;
        }

        // place page number just within the margin
        float textBase = document.bottom() - PAGE_NUMBER_FONT_SIZE;

        cb.beginText();
        cb.setFontAndSize(baseFont, PAGE_NUMBER_FONT_SIZE);
        cb.setTextMatrix((document.right() / 2) + HORIZONTAL_PADDING / 2, textBase);
        cb.showText(pageNumberShown);
        cb.endText();
        cb.restoreState();
    }

    /**
     * add the license from resource
     * @param document
     * @throws DocumentException
     */
    private void addLicensePage(Document document) throws DocumentException {
        // title
        String title = "";
        Anchor anchor = new Anchor(title, chapterFont);
        anchor.setName("name");
        Paragraph chapterParagraph = new Paragraph(anchor);
        chapterParagraph.setAlignment(Element.ALIGN_CENTER);
        com.itextpdf.text.Chapter chapter = new com.itextpdf.text.Chapter(chapterParagraph, 0);
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

        // place license title on it's own page
        document.newPage();
        document.resetPageCount(); // disable page numbering for this page (license)
        document.add(chapter);

        // translate simple html to paragraphs
        String license = App.context().getResources().getString(R.string.license_pdf);

        if(includeMedia) {
            license += App.context().getResources().getString(R.string.artwork_attribution_pdf);
        }

        license = license.replace("&#8226;", "\u2022");

        mCurrentParagraph = null;
        parseHtml( document, license, 0);

        nextParagraph(document);
    }

    /**
     * convert basic html to pdf chunks and add to document
     * @param document
     * @param text
     * @param pos
     * @throws DocumentException
     */
    private void parseHtml(Document document, String text, int pos) throws DocumentException {
        if(text == null) {
            return;
        }

        int length = text.length();
        FoundHtml foundHtml;
        while(pos < length) {
            foundHtml = getNextHtml(text, pos);
            if(null == foundHtml) {
                break;
            }

            if(foundHtml.startPos > pos) {
                String beforeText = text.substring(pos, foundHtml.startPos);
                addHtmlChunk(beforeText, bodyFont);
            }

            if("b".equals(foundHtml.html)) { // bold
                addHtmlChunk(foundHtml.enclosed, boldBodyFont);
            } else if((foundHtml.html.length() > 0) && (foundHtml.html.charAt(0) == 'h')) { // header
                nextParagraph(document);
                mCurrentParagraph = new Paragraph(foundHtml.enclosed, headingFont);
                nextParagraph(document);
            } else if((foundHtml.html.length() > 0) && (foundHtml.html.charAt(0) == 'a')) { // anchor
                addHtmlChunk(foundHtml.enclosed, underlineBodyFont);
            } else if("br".equals(foundHtml.html)) { // line break
                nextParagraph(document);
            } else if("p".equals(foundHtml.html)) { // line break
                blankLine(document);
                parseHtml( document, foundHtml.enclosed, 0);
                nextParagraph(document);
            } else { // anything else just strip off the html tag
                parseHtml( document, foundHtml.enclosed, 0);
            }

            pos = foundHtml.htmlFinishPos;
        }

        if(pos < length) {
            String rest = text.substring(pos);
            addHtmlChunk(rest, bodyFont);
        }
    }

    /**
     * add text to current paragraph, trim white space
     * @param text
     * @param font
     * @throws DocumentException
     */
    private void addHtmlChunk(String text, Font font) throws DocumentException {
        if((text != null) && !text.isEmpty()) {
            Character c = text.charAt(0);
            text = text.replace("\n","");
            while((text.length() > 1) && ("  ".equals(text.substring(0,2)))) { // remove extra leading space
                text = text.substring(1);
            }
            while((text.length() > 1) && ("  ".equals(text.substring(text.length() - 2)))) { // remove extra leading space
                text = text.substring(0, text.length() - 1);
            }

            Chunk chunk = new Chunk(text, font);
            addChunkToParagraph(chunk);
        }
    }

    /**
     * start a new paragraph
     * @param document
     * @throws DocumentException
     */
    private void nextParagraph(Document document) throws DocumentException {
        if(mCurrentParagraph != null) {
            document.add(mCurrentParagraph);
        }
        mCurrentParagraph = null;
    }

    /**
     * insert a blank paragraph
     * @param document
     * @throws DocumentException
     */
    private void blankLine(Document document) throws DocumentException {
        nextParagraph(document);
        mCurrentParagraph = new Paragraph(" ", bodyFont);
        nextParagraph(document);
    }

    /**
     * add a chunk to current paragraph
     * @param chunk
     * @throws DocumentException
     */
    private void addChunkToParagraph(Chunk chunk) throws DocumentException {
        if(null == mCurrentParagraph) {
            mCurrentParagraph = new Paragraph("", bodyFont);
        }
        mCurrentParagraph.add(chunk);
    }

    /**
     * get next html tag from start pos
     * @param text
     * @param startPos
     * @return
     */
    private FoundHtml getNextHtml(String text, int startPos) {
        int pos = text.indexOf("<", startPos);
        if(pos < 0) {
            return null;
        }

        int end = text.indexOf(">", pos + 1);
        int length = text.length();
        if(end < 0) {
            return new FoundHtml(text.substring(pos + 1), pos, length, "");
        }

        String token = text.substring(pos + 1, end);
        if(!token.isEmpty() && (token.charAt(token.length() - 1) == '/')) {
            return new FoundHtml(token.substring(0, token.length() - 1), pos, end + 1, "");
        }

        String[] parts = token.split(" "); // ignore attibutes
        String endToken = "</" + parts[0] + ">";
        int finish = text.indexOf(endToken, end + 1);
        if(finish < 0) { // if end token not found, then stop at next
            int next = text.indexOf("<", end + 1);
            if(next < 0) {
                return new FoundHtml(token, pos, length, text.substring(end + 1, length));
            } else {
                return new FoundHtml(token, pos, next, text.substring(end + 1, next));
            }
        }

        int htmlFinishPos = finish + endToken.length();
        return new FoundHtml(token, pos, htmlFinishPos, text.substring(end + 1, finish));
    }

    /**
     * class for keeping track of an html tag that was found, it's name, it's contents, and position
     */
    private class FoundHtml {
        public String html;
        public String enclosed;
        public int startPos;
        public int htmlFinishPos;

        public FoundHtml(String html, int startPos, int htmlFinishPos, String enclosed) {
            this.html = html;
            this.startPos = startPos;
            this.htmlFinishPos = htmlFinishPos;
            this.enclosed = enclosed;
        }
    }
}
