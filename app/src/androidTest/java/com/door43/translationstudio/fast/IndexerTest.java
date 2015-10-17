package com.door43.translationstudio.fast;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.Util;
import com.door43.translationstudio.core.Indexer;
import com.door43.translationstudio.core.IndexerSQLiteHelper;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.AppContext;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;

/**
 * Created by joel on 8/27/2015.
 */
public class IndexerTest extends InstrumentationTestCase {

    private Indexer mIndex;
    private Context mContext;
    private File mIndexRoot;
    private MainApplication mApp;

    @Override
    protected void setUp() throws Exception {
        mApp = AppContext.context();
        mIndexRoot = new File(mApp.getCacheDir(), "test_index");
        IndexerSQLiteHelper indexHelper = new IndexerSQLiteHelper(mApp, "indexer_test_app");
        mIndex = new Indexer(mApp, "indexer_test_app", mIndexRoot, indexHelper);
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
        assertTrue(mIndex.indexNotes(translation, catalog));
        mIndex.endTransaction(true);
        String[] noteIds = mIndex.getNoteSlugs(translation, "01", "01");
        assertTrue(noteIds.length > 0);
        assertNotNull(mIndex.getNote(translation, "01", "01", noteIds[0]));
    }

    public void test06IndexTerms() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        String catalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/terms.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexWords(translation, catalog));
        mIndex.endTransaction(true);
        String[] allTermIds = mIndex.getWords(translation);
        assertTrue(allTermIds.length > 0);
        assertNotNull(mIndex.getWord(translation, allTermIds[0]));
        String associationscatalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/tw_cat.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexTermAssignments(translation, associationscatalog));
        mIndex.endTransaction(true);
        String[] termIds = mIndex.getWordsForFrame(translation, "01", "01");
        assertTrue(termIds.length > 0);
    }

    public void test07IndexQuestions() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        String catalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/checking_questions.json"));
        mIndex.beginTransaction();
        assertTrue(mIndex.indexQuestions(translation, catalog));
        mIndex.endTransaction(true);
        String[] questionIds = mIndex.getQuestions(translation, "01", "01");
        assertTrue(questionIds.length > 0);
        assertNotNull(mIndex.getQuestion(translation, "01", "01", questionIds[0]));
    }

    public void test08LoadExistingIndex() throws Exception {
        InputStream is = mContext.getAssets().open("indexer/sample_index.zip");
        File asset = new File(AppContext.context().getCacheDir(), "indexer/sample_index.zip");
        Util.copyStreamToCache(mContext, is, asset);
        File indexDir = asset.getParentFile();
        FileUtils.deleteQuietly(new File(indexDir, "sample_index"));
        indexDir.mkdirs();
        Zip.unzip(asset, indexDir);
        File dbPath = mApp.getDatabasePath("sample_index");
        FileUtils.deleteQuietly(dbPath);
        FileUtils.moveFile(new File(indexDir, "sample_index"), dbPath);
        IndexerSQLiteHelper indexHelper = new IndexerSQLiteHelper(mApp, "sample_index");
        Indexer index = new Indexer(AppContext.context(), "sample_index", indexDir, indexHelper);
        assertTrue(index.getProjectSlugs().length > 0);
    }

//    public void test09MergeIndexShallow() throws Exception {
//        IndexerSQLiteHelper indexHelper = new IndexerSQLiteHelper(mApp, "merged_app");
//        Indexer mergedIndex = new Indexer(AppContext.context(), "merged_app", mIndexRoot, indexHelper);
//        mergedIndex.delete();
//        mergedIndex.mergeIndex(mIndex, true);
//        assertTrue(mergedIndex.getProjectSlugs().length > 0);
//        assertNotNull(mergedIndex.getProject("obs"));
//        assertNotNull(mergedIndex.getProject("gen"));
//        assertTrue(mergedIndex.getSourceLanguageSlugs("obs").length > 0);
//        assertTrue(mergedIndex.getSourceLanguageSlugs("gen").length > 0);
//        assertTrue(mergedIndex.getResourceSlugs("obs", "en").length == 1);
//        assertTrue(mergedIndex.getResourceSlugs("gen", "en").length > 1);
//        SourceTranslation obsTranslation = SourceTranslation.simple("obs", "en", "obs");
//        SourceTranslation genTranslation = SourceTranslation.simple("gen", "en", "ulb");
//        assertTrue(mergedIndex.getChapterSlugs(obsTranslation).length == 0);
//        assertTrue(mergedIndex.getChapterSlugs(genTranslation).length == 0);
//        indexHelper.close();
//    }

//    public void test10MergeIndexDeep() throws Exception {
//        IndexerSQLiteHelper indexHelper = new IndexerSQLiteHelper(mApp, "merged_app");
//        Indexer mergedIndex = new Indexer(AppContext.context(), "merged_app", mIndexRoot, indexHelper);
//        mergedIndex.delete();
//        mergedIndex.mergeIndex(mIndex);
//        assertTrue(mergedIndex.getProjectSlugs().length  > 1);
//        assertNotNull(mergedIndex.getProject("obs"));
//        assertNotNull(mergedIndex.getProject("gen"));
//        assertTrue(mergedIndex.getSourceLanguageSlugs("obs").length > 0);
//        assertTrue(mergedIndex.getSourceLanguageSlugs("gen").length > 0);
//        assertTrue(mergedIndex.getResourceSlugs("obs", "en").length == 1);
//        assertTrue(mergedIndex.getResourceSlugs("gen", "en").length > 0);
//        SourceTranslation obsTranslation = SourceTranslation.simple("obs", "en", "obs");
//        SourceTranslation genTranslation = SourceTranslation.simple("gen", "en", "ulb");
//        assertTrue(mergedIndex.getChapterSlugs(obsTranslation).length > 0);
//        String[] genChapters = mergedIndex.getChapterSlugs(genTranslation);
//        assertTrue(genChapters.length > 0);
//        String[] genFrames = mergedIndex.getFrameSlugs(genTranslation, genChapters[0]);
//        assertTrue(genFrames.length > 0);
//        JSONObject frameJson = mergedIndex.getFrame(genTranslation, genChapters[0], genFrames[0]);
//        Frame frame = Frame.generate(frameJson);
//        assertTrue(!frame.getText().isEmpty());
//        indexHelper.close();
//    }

//    public void test11MergeIndexProjectShallow() throws Exception {
//        IndexerSQLiteHelper indexHelper = new IndexerSQLiteHelper(mApp, "merged_app");
//        Indexer mergedIndex = new Indexer(AppContext.context(), "merged_app", mIndexRoot, indexHelper);
//        mergedIndex.delete();
//        mergedIndex.mergeProject("obs", mIndex, true);
//        assertTrue(mergedIndex.getProjectSlugs().length == 1);
//        assertNotNull(mergedIndex.getProject("obs"));
//        assertTrue(mergedIndex.getSourceLanguageSlugs("obs").length > 0);
//        assertTrue(mergedIndex.getResourceSlugs("obs", "en").length == 1);
//        SourceTranslation obsTranslation = SourceTranslation.simple("obs", "en", "obs");
//        assertTrue(mergedIndex.getChapterSlugs(obsTranslation).length == 0);
//        indexHelper.close();
//    }

//    public void test12MergeIndexProjectDeep() throws Exception {
//        IndexerSQLiteHelper indexHelper = new IndexerSQLiteHelper(mApp, "merged_app");
//        Indexer mergedIndex = new Indexer(AppContext.context(), "merged_app", mIndexRoot, indexHelper);
//        mergedIndex.delete();
//        mergedIndex.mergeProject("obs", mIndex);
//        assertTrue(mergedIndex.getProjectSlugs().length == 1);
//        assertNotNull(mergedIndex.getProject("obs"));
//        assertTrue(mergedIndex.getSourceLanguageSlugs("obs").length > 0);
//        assertTrue(mergedIndex.getResourceSlugs("obs", "en").length == 1);
//        SourceTranslation obsTranslation = SourceTranslation.simple("obs", "en", "obs");
//        assertTrue(mergedIndex.getChapterSlugs(obsTranslation).length > 0);
//
//        mergedIndex.delete();
//        mergedIndex.mergeProject("gen", mIndex);
//        assertTrue(mergedIndex.getProjectSlugs().length == 1);
//        assertNotNull(mergedIndex.getProject("gen"));
//        assertTrue(mergedIndex.getSourceLanguageSlugs("gen").length > 0);
//        assertTrue(mergedIndex.getResourceSlugs("gen", "en").length > 0);
//        SourceTranslation genTranslation = SourceTranslation.simple("gen", "en", "ulb");
//        assertTrue(mergedIndex.getChapterSlugs(genTranslation).length > 0);
//        indexHelper.close();
//    }

    public void test999999Cleanup() throws Exception {
        //mIndex.delete();
    }
}
