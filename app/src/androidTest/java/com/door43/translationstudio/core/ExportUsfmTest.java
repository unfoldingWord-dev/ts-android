package com.door43.translationstudio.core;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.door43.translationstudio.App;
import com.door43.util.FileUtilities;
import com.door43.util.Zip;

import org.eclipse.jgit.util.StringUtils;
import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.ChunkMarker;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.door43client.models.Versification;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unfoldingword.door43client.models.TargetLanguage;

/**
 * Created by blm on 7/25/16.
 */
public class ExportUsfmTest extends InstrumentationTestCase {

    public static final String TAG = ExportUsfmTest.class.getSimpleName();
    File mTempFolder;
    private Context mTestContext;
    private Context mAppContext;
    private TargetLanguage mTargetLanguage;
    private Door43Client mLibrary;
    private ImportUsfm mUsfm;
    private File mOutputFolder;
    private TargetTranslation mTargetTranslation;
    private String mErrorLog;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        mErrorLog = null;
        mLibrary = App.getLibrary();
        Logger.flush();
        mTestContext = getInstrumentation().getContext();
        mAppContext = App.context();
        if(!App.isLibraryDeployed()) {
            App.deployDefaultLibrary();
        }
        mTargetLanguage = mLibrary.index().getTargetLanguage("aae");
        if(App.getProfile() == null) { // make sure this is initialized
            App.setProfile(new Profile("testing"));
        }
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

    public void test07ValidExportJobSingle() throws Exception {
        //given
        String zipFileName = null;
        boolean separateChapters = false;
        String source = "18-JOB.usfm";
        importTestTranslation(source);

        //when
        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);

        //then
        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
    }

//    public void test08ValidExportIsaiahSingle() throws Exception {
//        //given
//        String zipFileName = null;
//        boolean separateChapters = false;
//        String source = "23-ISA.usfm";
//        importTestTranslation(source);
//
//        //when
//        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);
//
//        //then
//        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
//    }
//
//    public void test09ValidExportJeremiahSingle() throws Exception {
//        //given
//        String zipFileName = null;
//        boolean separateChapters = false;
//        String source = "24-JER.usfm";
//        importTestTranslation(source);
//
//        //when
//        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);
//
//        //then
//        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
//    }
//
//    public void test10ValidExportLukeSingle() throws Exception {
//        //given
//        String zipFileName = null;
//        boolean separateChapters = false;
//        String source = "43-LUK.usfm";
//        importTestTranslation(source);
//
//        //when
//        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);
//
//        //then
//        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
//    }
//
//    public void test11ValidExportJohnSingle() throws Exception {
//        //given
//        String zipFileName = null;
//        boolean separateChapters = false;
//        String source = "44-JHN.usfm";
//        importTestTranslation(source);
//
//        //when
//        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);
//
//        //then
//        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
//    }


    public void test12VerifyBookChunkMarkersAgainstSourceTOC() throws Exception {
        //given
        String[] books = {
                            "gen", "exo", "lev", "num", "deu", "jos", "jud", "rut", "1sa", "2sa",
                            "1ki", "2ki", "1ch", "2ch", "ezr", "neh", "est", "job", "psa", "pro",
                            "ecc", "sng", "isa", "jer", "lam", "ezk", "dan", "hos", "jol", "amo",
                            "oba", "jon", "mic", "nam", "hab", "zep", "hag", "zec", "mal",
                            "mat", "mrk", "luk", "jhn", "act", "rom", "1co", "2co", "gal", "eph",
                            "php", "col", "1th", "2th", "1ti", "2ti", "tit", "phm", "heb", "jas",
                            "1pe", "2pe", "1jn", "2jn", "3jn", "jud", "rev"
        };

        //when

        //then
        verifyTOC(books);
    }

    private void verifyTOC(String[] books) throws IOException {
        String errorLog = "";

        for (String book : books) {
            verifyBookTOC(book);
            if(!mErrorLog.isEmpty()) {
                errorLog += "Errors in book '" + book + "':\n" + mErrorLog + "\n\n";
            }
        }

        if(!errorLog.isEmpty()) {
            Log.d(TAG, "Invalid TOCs found:\n" + errorLog);
            fail("Invalid TOCs found:\n" + errorLog);
        }
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
     * handles validation of book source TOC against versification chunk list
     * @throws IOException
     */
    private void verifyBookTOC(String projectSlug) throws IOException {
        mErrorLog = "";
        String sourceTranslationSlug = getAvailableTargetTranslations(mLibrary, projectSlug);
        List<Map> sourceToc = getResourceToc(mLibrary, sourceTranslationSlug);
        verifyChunking(sourceToc, projectSlug);
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
        mErrorLog = "";
        List<Map> sourceToc = getResourceTOC(mTargetTranslation, mLibrary);
        String projectSlug = mTargetTranslation.getProjectId();
        verifyChunking(sourceToc, projectSlug);

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

        if(!mErrorLog.isEmpty()) {
            Log.d(TAG, "Errors found:\n" + mErrorLog);
            fail("Errors found:\n" + mErrorLog);
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

            if(usfmFiles.length < chapterInInt) {
                addErrorMsg("chapter count " + usfmFiles.length + "' should be greater than or equal to chapter number '" + chapterInInt + "'\n");
            }

           if (chapterInInt > 1) {
                // verify verses in last chapter
                String inputChapter = usfmInputText.substring(lastInputChapterStart, inputMatcher.start());
                String outputChapter = FileUtilities.readFileToString(usfmFiles[chapterInInt-1]);
                verifyBookID(usfmInputText, outputChapter);
                compareVersesInChapter(chapterInInt-1, inputChapter, outputChapter);
            }

            lastInputChapterStart = inputMatcher.end();
        }

        if(usfmFiles.length != chapterInInt + 1) {
            addErrorMsg("chapter count " + usfmFiles.length + "' should be  '" + (chapterInInt + 1) + "'\n");
        }

        // verify verses in last chapter
        String inputChapter = usfmInputText.substring(lastInputChapterStart);
        String outputChapter = FileUtilities.readFileToString(usfmFiles[chapterInInt]);
        verifyBookID(usfmInputText, outputChapter);
        compareVersesInChapter(chapterInInt, inputChapter, outputChapter);
    }

    /**
     * queue up error messages
     * @param error
     */
    private void addErrorMsg(String error) {
        mErrorLog = error + mErrorLog;
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
                int chapterOutInt = Integer.parseInt(chapterOut);
                if(chapterInInt != chapterOutInt) {
                    addErrorMsg("chapter input: " + chapterInInt + "\n does not match chapter output:" + chapterOutInt + "\n");
                }
            } else {
                addErrorMsg("chapter '" + chapterIn + "' missing in output\n");
                break;
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
            addErrorMsg("extra chapter in output: " + outputMatcher.group(1) + "\n");
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
                if(!verseIn.equals(verseOut)) {
                    addErrorMsg("in chapter '" + chapter + "' verse input '" + verseIn + "'\n does not match verse output '" + verseOut + "'\n");
                    return;
                }
            } else {
                addErrorMsg("in chapter '" + chapter + "', verse '" + verseIn + "' missing in output\n");
                return;
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
            addErrorMsg("In chapter '" + chapter + "' extra verse in output: '" + outputVerseMatcher.group(1) + "\n");
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

        if(!input.equals(output)) {
            if(!output.equals(input + "\n")) {
                return;
            }
            if(!input.equals(output + "\n")) {
                return;
            }
            addErrorMsg("In chapter '" + chapterNum + "' verse '" + verseIn + "' verse input:\n" + input + "\n does not match output:\n" + output + "\n");
        }
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
        text = text.replace("\\s5 \n", "\n"); // remove section markers
        text = replaceAll(text,"\n\n", "\n"); // remove double new-lines
        text = replaceAll(text,"\n\n", "\n"); // remove double new-lines
        text = replaceAll(text,"\n \n", "\n"); // remove double new-lines
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
        assertNotNull("mTargetLanguage", mTargetLanguage);
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

    private void verifyChunking(List<Map> sourceToc, String projectSlug) {
        assertNotNull("sourceToc should not be null", sourceToc);
        mErrorLog = "";
        List<Versification> versifications = App.getLibrary().index().getVersifications("en");
        List<ChunkMarker> markers = App.getLibrary().index().getChunkMarkers(projectSlug, versifications.get(0).slug);
        assertTrue("chunk markers should not be empty", markers.size() > 0);

        ImportUsfm.ParsedChunks parsedChunks = ImportUsfm.parseChunks(markers);
        List<String> mChapters = parsedChunks.chapters;
        HashMap<String, List<String>> mChunks = parsedChunks.chunks;

        int versificationIndex = 0;
        int lastChapter = -1;

        for (int i = 0; i < sourceToc.size(); i++) {
            Map tocChapter = sourceToc.get(i);
            String chapterSlug = (String) tocChapter.get("chapter");
            int chapterInt = strToInt(chapterSlug,-1);
            if(chapterInt < 0) { // skip if not number
                continue;
            }

            if(lastChapter >= 0) {
                if(chapterInt <= lastChapter) {
                    addErrorMsg("versification chunks are out of order chapter '" + chapterSlug + "' should be greater than '" + lastChapter + "'\n");
                }
            }
            lastChapter = chapterInt;

            if(versificationIndex >= mChapters.size()) {
                addErrorMsg("missing chunk for versification '" + versificationIndex + "'\n");
                break;
            }

            String versificationChapter = mChapters.get(versificationIndex++);
            if(strToInt(chapterSlug, -1) != strToInt(versificationChapter,-1)) {
                addErrorMsg("Resource chapter '" + chapterSlug + "' does not equal versification chapter '" + versificationChapter + "'\n");
                continue;
            }

            List<String> tocChunks = (List) tocChapter.get("chunks");
            List<String> versificationChunks = mChunks.get(versificationChapter);

            boolean chunkErrors = false;

            int offset = 0;
            while(strToInt(tocChunks.get(offset), -1) < 0) {
                offset++;
            }

            if((tocChunks.size()-offset) != versificationChunks.size()) {
                addErrorMsg("For chapter '" + chapterSlug + "' toc chunk count is '" + (tocChunks.size()-offset) + "' but versification chunk count is '" + versificationChunks.size() + "'\n");
                chunkErrors = true;
            }

            int limit = (tocChunks.size()-offset) < versificationChunks.size() ? tocChunks.size()-offset : versificationChunks.size();
            for(int j = 0; j < limit; j++) {
                String tocVerse = tocChunks.get(j+offset);
                String versificationVerse = versificationChunks.get(j);
                if(strToInt(tocVerse,-1) != strToInt(versificationVerse,-1)) {
                    addErrorMsg("For chapter '" + chapterSlug + "' toc chunk " + i + " is '" + tocVerse + "' but versification chunk is '" + versificationVerse + "'\n");
                    chunkErrors = true;
                }
            }

            if(chunkErrors) {
                addErrorMsg("For chapter '" + chapterSlug + "' toc chunks: " + i + " is: " + StringUtils.join(tocChunks, ", ") + "\nbut versification chunks are " + StringUtils.join(versificationChunks, ", ") + "\n");
            }
        }

        if(versificationIndex != mChapters.size()) {
            addErrorMsg("unprocessed chunk for versification '" + (mChapters.size() - versificationIndex) + "'\n");
        }

        if(!mErrorLog.isEmpty()) {
            addErrorMsg("\n\n==============\nChunking Errors found:\n");
        }
    }

    /**
     * gets the resource TOCs even if user has not selected one yet
     * @param targetTranslation
     * @param library
     * @return
     */
    public static List<Map> getResourceTOC(TargetTranslation targetTranslation, Door43Client library) {
        Translation sourceTranslation;
        ResourceContainer mSourceContainer;
        List<Map> sourceToc;
        String sourceTranslationSlug = App.getSelectedSourceTranslationId(targetTranslation.getId());
        if(sourceTranslationSlug == null) { // if none selected, try list of selected translations
            String[] sourceTranslationSlugs = App.getSelectedSourceTranslations(targetTranslation.getId());
            if((sourceTranslationSlugs != null) && (sourceTranslationSlugs.length > 0)) {
                sourceTranslationSlug = sourceTranslationSlugs[0];
            }
        }

        // last try look for any available that are loaded into memory
        if(sourceTranslationSlug == null) { // if none selected, try list of selected translations
            String projectId = targetTranslation.getProjectId();
            sourceTranslationSlug = getAvailableTargetTranslations(library, projectId);
        }

        return getResourceToc(library, sourceTranslationSlug);
    }

    private static List<Map> getResourceToc(Door43Client library, String sourceTranslationSlug) {
        Translation sourceTranslation;
        ResourceContainer mSourceContainer;
        List<Map> sourceToc;
        sourceTranslation = library.index().getTranslation(sourceTranslationSlug);
        mSourceContainer = ContainerCache.cache(library, sourceTranslation.resourceContainerSlug);
        sourceToc = (List<Map>) mSourceContainer.toc;
        return sourceToc;
    }

    /**
     * find an available translation for project ID
     * @param library
     * @param projectId
     * @return
     */
    private static String getAvailableTargetTranslations(Door43Client library, String projectId) {
        String sourceTranslationSlug = null;
        List<Translation> availableTranslations = library.index().findTranslations(null, projectId, null, "book", "all", App.MIN_CHECKING_LEVEL, -1);
        if((availableTranslations != null) && (availableTranslations.size() > 0)) {
            for (Translation availableTranslation : availableTranslations) {
                final boolean isDownloaded = library.exists(availableTranslation.resourceContainerSlug);
                if(isDownloaded) {
                    sourceTranslationSlug = availableTranslation.resourceContainerSlug;
                    break;
                }
            }
        }
        return sourceTranslationSlug;
    }

    /**
     * right size the file name length.  App expects file names under 100 to be only two digits.
     * @param fileName
     * @return
     */
    public static String getRightFileNameLength(String fileName) {
        Integer numericalValue = strToInt(fileName, -1);
        if((numericalValue >= 0) && (numericalValue < 100) && (fileName.length() != 2)) {
            fileName = "00" + fileName; // make sure has leading zeroes
            fileName = fileName.substring(fileName.length()-2); // trim down extra leading zeros
        }
        return fileName;
    }

    /**
     * do string to integer with default value on conversion error
     * @param value
     * @param defaultValue
     * @return
     */
    public static int strToInt(String value, int defaultValue) {
        try {
            int retValue = Integer.parseInt(value);
            return retValue;
        } catch (Exception e) {
//            Log.d(TAG, "Cannot convert to int: " + value);
        }
        return defaultValue;
    }

}