package com.door43.translationstudio.core;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.door43.translationstudio.App;
import com.door43.util.FileUtilities;
import com.door43.util.Zip;

import org.unfoldingword.tools.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unfoldingword.door43client.models.TargetLanguage;

/**
 * Created by blm on 7/25/16.
 */
public class ExportUsfmTest extends InstrumentationTestCase {

    File mTempFolder;
    private Context mTestContext;
    private Context mAppContext;
    private TargetLanguage mTargetLanguage;
    private Library mLibrary;
    private ImportUsfm mUsfm;
    private File mOutputFolder;
    private TargetTranslation mTargetTranslation;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        mLibrary = App.getLibrary();
        Logger.flush();
        mTestContext = getInstrumentation().getContext();
        mAppContext = App.context();
        mTargetLanguage = mLibrary.getTargetLanguage("aae");
    }

    @Override
    public void tearDown() throws Exception {
        if (mUsfm != null) {
            mUsfm.cleanup();
        }
        FileUtilities.deleteQuietly(mTempFolder);
    }

    public void test01ValidExportMarkSingle() throws Exception {
        //given
        String zipFileName = null;
        boolean separateChapters = false;
        String source = "mrk.usfm";
        importTestTranslation(source);

        //when
        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);

        //then
        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
    }

    public void test02ValidExportMarkZip() throws Exception {
        //given
        String zipFileName = "test_usfm.zip";
        boolean separateChapters = true;
        String source = "mrk.usfm";
        importTestTranslation(source);

        //when
        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);

        //then
        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
    }

    public void test03ValidExportPsalmSingle() throws Exception {
        //given
        String zipFileName = null;
        boolean separateChapters = false;
        String source = "19-PSA.usfm";
        importTestTranslation(source);

        //when
        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);

        //then
        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
    }

    public void test04ValidExportPsalmZip() throws Exception {
        //given
        String zipFileName = "test_usfm.zip";
        boolean separateChapters = true;
        String source = "19-PSA.usfm";
        importTestTranslation(source);

        //when
        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);

        //then
        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
    }

    public void test05ValidExportJudeSingle() throws Exception {
        //given
        String zipFileName = null;
        boolean separateChapters = false;
        String source = "66-JUD.usfm";
        importTestTranslation(source);

        //when
        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);

        //then
        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
    }

    public void test06ValidExportJudeZip() throws Exception {
        //given
        String zipFileName = "test_usfm.zip";
        boolean separateChapters = true;
        String source = "66-JUD.usfm";
        importTestTranslation(source);

        //when
        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);

        //then
        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
    }

    /**
     * match all the book identifiers
     * @param input
     * @param output
     */
    private void verifyBookID(String input, String output) {
        String bookTitle = extractString(input, ImportUsfm.PATTERN_BOOK_TITLE_MARKER);
        String bookLongName = extractString(input, ImportUsfm.PATTERN_BOOK_LONG_NAME_MARKER);
        String bookShortName = extractString(input, ImportUsfm.PATTERN_BOOK_ABBREVIATION_MARKER);
        String bookTitleOut = extractString(output, ImportUsfm.PATTERN_BOOK_TITLE_MARKER);
        String bookLongNameOut = extractString(output, ImportUsfm.PATTERN_BOOK_LONG_NAME_MARKER);
        String bookShortNameOut = extractString(output, ImportUsfm.PATTERN_BOOK_ABBREVIATION_MARKER);

        String bookID = extractString(input, ImportUsfm.ID_TAG_MARKER);
        String[] bookIdParts = bookID.split(" ");
        String bookIDOut = extractString(output, ImportUsfm.ID_TAG_MARKER);
        String[] bookIdOutParts = bookIDOut.split(" ");

        assertEquals("Input and output book titles (\\toc1) should equal", bookTitle.toLowerCase(), bookTitleOut.toLowerCase());
        assertEquals("Input and output book codes (\\toc3) should equal", bookShortName.toLowerCase(), bookShortNameOut.toLowerCase());
        assertEquals("Input and output book long name (\\toc2) should equal", bookLongName.toLowerCase(), bookLongNameOut.toLowerCase());
        assertEquals("Input and output book ID code (\\id) should equal", bookIdParts[0].toLowerCase(), bookIdOutParts[0].toLowerCase());
    }

    /**
     * match regexPattern and get string in group 1 if present
     *
     * @param text
     * @param regexPattern
     * @return
     */
    private String extractString(CharSequence text, Pattern regexPattern) {
        if (text.length() > 0) {
            // find instance
            Matcher matcher = regexPattern.matcher(text);
            String foundItem = null;
            if (matcher.find()) {
                foundItem = matcher.group(1);
                return foundItem.trim();
            }
        }

        return null;
    }

    /**
     * handles validation of exported USFM file by comparing to original imported USFM file
     * @param zipFileName - to determine if zip file was expected
     * @param separateChapters
     * @param source
     * @param usfmOutput - actual output file
     * @throws IOException
     */
    private void verifyExportedUsfmFile(String zipFileName, boolean separateChapters, String source, File usfmOutput) throws IOException {
        assertNotNull("exported file", usfmOutput);
        if (zipFileName == null) {
            if (!separateChapters) {
                verifySingleUsfmFile(source, usfmOutput);
            } else {
                fail("separate chapters without zip is not supported");
            }
        } else {
            if (separateChapters) {
                verifyUsfmZipFile(source, usfmOutput);
            } else {
                fail("single book with zip is not supported");
            }
        }
    }

    /**
     * handles validation of exported USFM zip file containing chapters by comparing to original imported USFM file
     * @param source
     * @param usfmOutput
     * @throws IOException
     */
    private void verifyUsfmZipFile(String source, File usfmOutput) throws IOException {

        File unzipFolder = new File(mTempFolder,"scratch_test_unzip");
        FileUtilities.forceMkdir(unzipFolder);

        InputStream zipStream = new FileInputStream(usfmOutput);
        Zip.unzipFromStream(zipStream, unzipFolder);
        File[] usfmFiles = unzipFolder.listFiles();

        InputStream usfmStream = mTestContext.getAssets().open("usfm/" + source);
        String usfmInputText = FileUtilities.readStreamToString(usfmStream);

        Matcher inputMatcher = ImportUsfm.PATTERN_CHAPTER_NUMBER_MARKER.matcher(usfmInputText);

        int lastInputChapterStart = -1;
        String chapterIn = "";
        int chapterInInt = -1;
        while (inputMatcher.find()) {
            chapterIn = inputMatcher.group(1); // chapter number in input
            chapterInInt = Integer.valueOf(chapterIn);

            assertTrue("chapter count should be greater than or equal to chapter", usfmFiles.length >= chapterInInt);

           if (chapterInInt > 1) {
                // verify verses in last chapter
                String inputChapter = usfmInputText.substring(lastInputChapterStart, inputMatcher.start());
                String outputChapter = FileUtilities.readFileToString(usfmFiles[chapterInInt-2]);
                verifyBookID(usfmInputText, outputChapter);
                compareVersesInChapter(chapterInInt-1, inputChapter, outputChapter);
            }

            lastInputChapterStart = inputMatcher.end();
        }

        assertTrue("chapter count should equal last chapter", usfmFiles.length == chapterInInt);

        // verify verses in last chapter
        String inputChapter = usfmInputText.substring(lastInputChapterStart);
        String outputChapter = FileUtilities.readFileToString(usfmFiles[chapterInInt-1]);
        verifyBookID(usfmInputText, outputChapter);
        compareVersesInChapter(chapterInInt, inputChapter, outputChapter);
    }

    /**
     * handles validation of exported USFM file by comparing to original imported USFM file
     * @param source
     * @param usfmOutput
     * @throws IOException
     */
    private void verifySingleUsfmFile(String source, File usfmOutput) throws IOException {
        String usfmOutputText = FileUtilities.readFileToString(usfmOutput);

        InputStream usfmStream = mTestContext.getAssets().open("usfm/" + source);
        String usfmInputText = FileUtilities.readStreamToString(usfmStream);

        verifyBookID(usfmInputText, usfmOutputText);

        Matcher inputMatcher = ImportUsfm.PATTERN_CHAPTER_NUMBER_MARKER.matcher(usfmInputText);
        Matcher outputMatcher = ImportUsfm.PATTERN_CHAPTER_NUMBER_MARKER.matcher(usfmOutputText);

        int lastInputChapterStart = -1;
        int lastOutputChapterStart = -1;
        String chapterIn = "";
        int chapterInInt = -1;
        while (inputMatcher.find()) {
            chapterIn = inputMatcher.group(1); // chapter number in input
            chapterInInt = Integer.valueOf(chapterIn);

            if (outputMatcher.find()) {
                String chapterOut = outputMatcher.group(1); // chapter number in output
                int chapterOutInt = Integer.valueOf(chapterOut);
                assertEquals("chapter input should match chapter output", chapterInInt, chapterOutInt);
            } else {
                fail("chapter '" + chapterIn + "' missing in output");
            }

            if (chapterInInt > 1) {
                // verify verses in last chapter
                String inputChapter = usfmInputText.substring(lastInputChapterStart, inputMatcher.start());
                String outputChapter = usfmOutputText.substring(lastOutputChapterStart, outputMatcher.start());
                compareVersesInChapter(chapterInInt-1, inputChapter, outputChapter);
            }

            lastInputChapterStart = inputMatcher.end();
            lastOutputChapterStart = outputMatcher.end();
        }

        if (outputMatcher.find()) {
            fail("extra chapter in output: " + outputMatcher.group(1));
        }

        // verify verses in last chapter
        String inputChapter = usfmInputText.substring(lastInputChapterStart);
        String outputChapter = usfmOutputText.substring(lastOutputChapterStart);
        compareVersesInChapter(chapterInInt, inputChapter, outputChapter);
    }

    /**
     * compares the verses in exported chapter to make sure they are in same order and have same
     *  contents as imported chapter
     * @param chapter
     * @param inputChapter
     * @param outputChapter
     */
    private void compareVersesInChapter(int chapter, String inputChapter, String outputChapter) {
        Matcher inputVerseMatcher = ImportUsfm.PATTERN_USFM_VERSE_SPAN.matcher(inputChapter);
        Matcher outputVerseMatcher = ImportUsfm.PATTERN_USFM_VERSE_SPAN.matcher(outputChapter);
        int lastInputVerseStart = -1;
        int lastOutputVerseStart = -1;
        String verseIn = "";
        while (inputVerseMatcher.find()) {
            verseIn = inputVerseMatcher.group(1); // verse number in input
            if (outputVerseMatcher.find()) {
                String verseOut = outputVerseMatcher.group(1); // verse number in output
                assertEquals("in chapter '" + chapter + "' verse input should match verse output", verseIn, verseOut);
            } else {
                fail("verse '" + verseIn + "' missing in output");
            }

            if (lastInputVerseStart > 0) {
                String inputVerse = inputChapter.substring(lastInputVerseStart, inputVerseMatcher.start());
                String outputVerse = outputChapter.substring(lastOutputVerseStart, outputVerseMatcher.start());
                compareVerses(chapter, verseIn, inputVerse, outputVerse);
            }

            lastInputVerseStart = inputVerseMatcher.end();
            lastOutputVerseStart = outputVerseMatcher.end();
        }

        if (outputVerseMatcher.find()) {
            fail("In chapter '" + chapter + "' extra verse in output: " + outputVerseMatcher.group(1));
        }

        String inputVerse = inputChapter.substring(lastInputVerseStart);
        String outputVerse = outputChapter.substring(lastOutputVerseStart);
        compareVerses(chapter, verseIn, inputVerse, outputVerse);
    }

    /**
     * compares contents of verses
     * @param chapterNum
     * @param verseIn
     * @param inputVerse
     * @param outputVerse
     */
    private void compareVerses(int chapterNum, String verseIn, String inputVerse, String outputVerse) {

        String input = inputVerse;
        String output = outputVerse;

        if (input.equals(output)) {
            return;
        }

        //if not exact match, try stripping section marker and removing double new-lines

        //remove extra white space
        input = cleanUpVerse(input);
        output = cleanUpVerse(output);

        if (input.equals(output)) {
            return;
        }

        assertEquals("In chapter '" + chapterNum + "' verse '" + verseIn + "' verse content should match", input, output);
    }

    public static final String CHAPTER_LABEL_MARKER = "\\\\cl\\s([^\\n]*)";
    public static final Pattern PATTERN_CHAPTER_LABEL_MARKER = Pattern.compile(CHAPTER_LABEL_MARKER);

    /**
     * clean up by stripping section marker and removing double new-lines
     * @param text
     * @return
     */
    private String cleanUpVerse(String text) {

        Matcher chapterLabelMatcher = PATTERN_CHAPTER_LABEL_MARKER.matcher(text);
        if(chapterLabelMatcher.find()) {
            text = text.substring(0,chapterLabelMatcher.start());
        }

        text = text.replace("\\s5\n", "\n"); // remove section markers
        text = replaceAll(text,"\n\n", "\n"); // remove double new-lines
        return text;
    }

    /**
     * repeatedly replaces strings - useful
     * @param text
     * @param target
     * @param replacement
     * @return
     */
    private String replaceAll(String text, CharSequence target, CharSequence replacement)  {
        String oldText = null;
        String newText = text;

        while(newText != oldText) {
            oldText = newText;
            newText = newText.replace(target, replacement);
        }

        return newText;
    }

    /**
     * import a usfm file to be used for export testing.
     * @param source
     * @throws IOException
     */
    private void importTestTranslation(String source) throws IOException {
        //import USFM file to be used for testing
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);
        assertTrue("import usfm test file should succeed", success);
        File[] imports = mUsfm.getImportProjects();
        assertEquals("import usfm test file should succeed", 1, imports.length);

        //open import as targetTranslation
        File projectFolder = imports[0];
        mTempFolder = projectFolder.getParentFile();
        mOutputFolder = new File(mTempFolder,"scratch_test");
        FileUtilities.forceMkdir(mOutputFolder);
        mTargetTranslation = TargetTranslation.open(projectFolder);
    }
}