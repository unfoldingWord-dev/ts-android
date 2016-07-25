package com.door43.translationstudio.core;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.door43.translationstudio.App;
import com.door43.util.FileUtilities;

import org.unfoldingword.tools.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;

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
        if(mUsfm != null) {
            mUsfm.cleanup();
        }
        FileUtilities.deleteQuietly(mTempFolder);
    }

    public void test01ValidExportMarkSingle() throws Exception {
        //given
        String zipFileName = null;
        boolean separateChapters = false;
        String source = "mrk.usfm";
        int expectedChapters = 16;
        importTestTranslation(source);

        //when
        File usfmOutput = ExportUsfm.saveToUSFM(mTargetTranslation, mOutputFolder, zipFileName, separateChapters);

        //then
        verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput);
    }

    private void verifyExportedUsfmFile(String zipFileName, boolean separateChapters, String source, File usfmOutput) throws IOException {
        assertNotNull("exported file", usfmOutput);
        if(zipFileName == null) {
            if(!separateChapters) {
                String usfmOutputText = FileUtilities.readFileToString(usfmOutput);

                InputStream usfmStream = mTestContext.getAssets().open( "usfm/" + source);
                String usfmInputText = FileUtilities.readStreamToString(usfmStream);

                Matcher inputMatcher = ImportUsfm.PATTERN_CHAPTER_NUMBER_MARKER.matcher(usfmInputText);
                Matcher outputMatcher = ImportUsfm.PATTERN_CHAPTER_NUMBER_MARKER.matcher(usfmOutputText);

                int lastInputChapterStart = -1;
                int lastOutputChapterStart = -1;
                String chapterIn = "";
                while (inputMatcher.find()) {
                    chapterIn = inputMatcher.group(1); // chapter number in input
                    int chapterInInt = Integer.valueOf(chapterIn);

                    if (outputMatcher.find()) {
                        String chapterOut = outputMatcher.group(1); // chapter number in output
                        int chapterOutInt = Integer.valueOf(chapterOut);
                        assertEquals("chapter input should match chapter output", chapterInInt,chapterOutInt);
                    } else {
                        fail("chapter '" + chapterIn + "' missing in output");
                    }

                    if(chapterInInt > 1) {
                        // verify verses in last chapter
                        String inputChapter = usfmInputText.substring(lastInputChapterStart, inputMatcher.start());
                        String outputChapter = usfmOutputText.substring(lastOutputChapterStart, outputMatcher.start());
                        compareVersesInChapter(chapterIn, inputChapter, outputChapter);
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
                compareVersesInChapter(chapterIn, inputChapter, outputChapter);
            }
        }
    }

    private void compareVersesInChapter(String chapterId, String inputChapter, String outputChapter) {
        Matcher inputVerseMatcher = ImportUsfm.PATTERN_USFM_VERSE_SPAN.matcher(inputChapter);
        Matcher outputVerseMatcher = ImportUsfm.PATTERN_USFM_VERSE_SPAN.matcher(outputChapter);
        int lastInputVerseStart = -1;
        int lastOutputVerseStart = -1;
        String verseIn = "";
        while (inputVerseMatcher.find()) {
            verseIn = inputVerseMatcher.group(1); // verse number in input
            if (outputVerseMatcher.find()) {
                String verseOut = outputVerseMatcher.group(1); // verse number in output
                assertEquals("in chapter '" + chapterId + "' verse input should match verse output", verseIn, verseOut);
            } else {
                fail("verse '" + verseIn + "' missing in output");
            }

            if(lastInputVerseStart > 0) {
                String inputVerse = inputChapter.substring(lastInputVerseStart, inputVerseMatcher.start());
                String outputVerse = outputChapter.substring(lastOutputVerseStart, outputVerseMatcher.start());
                compareVerses(chapterId, verseIn, inputVerse, outputVerse);
            }

            lastInputVerseStart = inputVerseMatcher.end();
            lastOutputVerseStart = outputVerseMatcher.end();
        }

        if (outputVerseMatcher.find()) {
            fail("In chapter '" + chapterId + "' extra verse in output: " + outputVerseMatcher.group(1));
        }

        String inputVerse = inputChapter.substring(lastInputVerseStart);
        String outputVerse = outputChapter.substring(lastOutputVerseStart);
        compareVerses(chapterId, verseIn, inputVerse, outputVerse);
    }

    private void compareVerses(String chapterId, String verseIn, String inputVerse, String outputVerse) {
        if(inputVerse.equals(outputVerse)) {
            return;
        }

        //remove extra white space
        inputVerse = inputVerse.replace("\\s5\n", "");
        inputVerse = inputVerse.replace("\n\n", "\n");
        inputVerse = inputVerse.replace("\n\n", "\n");
        outputVerse = outputVerse.replace("\\s5\n", "");
        outputVerse = outputVerse.replace("\n\n", "\n");
        outputVerse = outputVerse.replace("\n\n", "\n");

        assertEquals( "In chapter '" + chapterId + "' verse '" + verseIn + "' verse content should match", inputVerse,outputVerse);
    }

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