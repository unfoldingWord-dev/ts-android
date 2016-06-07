package com.door43.translationstudio;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.door43.translationstudio.core.CheckingQuestion;
import com.door43.translationstudio.core.LibraryData;
import com.door43.translationstudio.core.LibrarySQLiteHelper;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TranslationWord;
import com.door43.translationstudio.core.Util;

/**
 * Created by joel on 8/27/2015.
 */
public class LibraryDataTest extends InstrumentationTestCase {

    private LibraryData mIndex;
    private Context mContext;
    private MainApplication mApp;

    @Override
    protected void setUp() throws Exception {
        mApp = AppContext.context();
        LibrarySQLiteHelper indexHelper = new LibrarySQLiteHelper(mApp, "indexer_test_app");
        mIndex = new LibraryData(mApp, "indexer_test_app", indexHelper);
        mContext = getInstrumentation().getContext();
    }

    public void test01IndexProjects() throws Exception {
        mIndex.delete();
        mIndex.rebuild();
        String catalog = Util.readStream(mContext.getAssets().open("indexer/catalog.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexProjects(catalog));
        mIndex.endTransaction(true);
        assertTrue(mIndex.getProjectSlugs().length > 0);
    }

    public void test02IndexSourceLanguages() throws Exception {
        String genCatalog = Util.readStream(mContext.getAssets().open("indexer/gen/languages.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexSourceLanguages("gen", genCatalog));
        mIndex.endTransaction(true);
        assertTrue(mIndex.getSourceLanguageSlugs("gen").length > 0);

        String obsCatalog = Util.readStream(mContext.getAssets().open("indexer/obs/languages.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexSourceLanguages("obs", obsCatalog));
        mIndex.endTransaction(true);
        assertTrue(mIndex.getSourceLanguageSlugs("obs").length > 0);
    }

    public void test03IndexResources() throws Exception {
        String genCatalog = Util.readStream(mContext.getAssets().open("indexer/gen/en/resources.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexResources("gen", "en", genCatalog));
        mIndex.endTransaction(true);
        assertTrue(mIndex.getResourceSlugs("gen", "en").length > 0);

        String obsCatalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/resources.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexResources("obs", "en", obsCatalog));
        mIndex.endTransaction(true);
        assertTrue(mIndex.getResourceSlugs("obs", "en").length > 0);
    }

    public void test04IndexSource() throws Exception {
        SourceTranslation bibleTranslation = SourceTranslation.simple("gen", "en", "ulb");
        String genCatalog = Util.readStream(mContext.getAssets().open("indexer/gen/en/ulb/source.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexSource(bibleTranslation, genCatalog));
        mIndex.endTransaction(true);
        String[] bibleChapterIds = mIndex.getChapterSlugs(bibleTranslation);
        assertTrue(bibleChapterIds.length > 0);
        for(String chapterId:bibleChapterIds) {
            assertNotNull(mIndex.getChapter(bibleTranslation, chapterId));
            String[] bibleFrameIds = mIndex.getFrameSlugs(bibleTranslation, chapterId);
            assertTrue(bibleFrameIds.length > 0);
            for(String frameId:bibleFrameIds) {
                assertNotNull(mIndex.getFrame(bibleTranslation, chapterId, frameId));
            }
        }

        SourceTranslation obsTranslation = SourceTranslation.simple("obs", "en", "obs");
        String obsCatalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/source.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexSource(obsTranslation, obsCatalog));
        mIndex.endTransaction(true);
        String[] obsChapterIds = mIndex.getChapterSlugs(obsTranslation);
        assertTrue(obsChapterIds.length > 0);
        for(String chapterId:obsChapterIds) {
            assertTrue(mIndex.getFrameSlugs(obsTranslation, chapterId).length > 0);
            String[] obsFrameIds = mIndex.getFrameSlugs(obsTranslation, chapterId);
            assertTrue(obsFrameIds.length > 0);
            for(String frameId:obsFrameIds) {
                assertNotNull(mIndex.getFrame(obsTranslation, chapterId, frameId));
            }
        }
    }

    public void test05IndexNotes() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        String catalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/notes.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexTranslationNotes(translation, catalog));
        mIndex.endTransaction(true);
        String[] noteIds = mIndex.getNoteSlugs(translation, "01", "01");
        assertTrue(noteIds.length > 0);
        assertNotNull(mIndex.getNote(translation, "01", "01", noteIds[0]));
    }

    public void test06IndexTerms() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        String catalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/terms.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexTranslationWords(translation, catalog));
        mIndex.endTransaction(true);
        String[] allTermIds = mIndex.getWordSlugs(translation);
        assertTrue(allTermIds.length > 0);
        assertNotNull(mIndex.getWord(translation, allTermIds[0]));
        String associationscatalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/tw_cat.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexTermAssignments(translation, associationscatalog));
        mIndex.endTransaction(true);
        TranslationWord[] frameWords = mIndex.getWordsForFrame(translation, "01", "01");
        assertTrue(frameWords.length > 0);
    }

    public void test07IndexQuestions() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        String catalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/checking_questions.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexQuestions(translation, catalog));
        mIndex.endTransaction(true);
        CheckingQuestion[] questions = mIndex.getCheckingQuestions(translation, "01", "01");
        assertTrue(questions.length > 0);
        assertNotNull(mIndex.getCheckingQuestion(translation, "01", "01", questions[0].getId()));
    }

    public void test08IndexTranslationAcademy() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("gen", "en", "ulb");
        String catalog = Util.readStream(mApp.getAssets().open("ta.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexTranslationAcademy(translation, catalog));
        mIndex.endTransaction(true);
        // TODO: 12/4/2015 test retrieving an article
    }
}
