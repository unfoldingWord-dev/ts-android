package com.door43.translationstudio.fast;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.Util;
import com.door43.translationstudio.core.Indexer;
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

    @Override
    protected void setUp() throws Exception {
        MainApplication app = AppContext.context();
        mIndexRoot = new File(app.getCacheDir(), "test_index");
        mIndex = new Indexer(app, "indexer_test_app", mIndexRoot);
        mContext = getInstrumentation().getContext();
    }

    public void test01IndexProjects() throws Exception {
        mIndex.destroy();
        String catalog = Util.readStream(mContext.getAssets().open("indexer/catalog.json"));
        assertTrue(mIndex.indexProjects(catalog));
        assertTrue(mIndex.getProjects().length > 0);
    }

    public void test02IndexSourceLanguages() throws Exception {
        String genCatalog = Util.readStream(mContext.getAssets().open("indexer/gen/languages.json"));
        assertTrue(mIndex.indexSourceLanguages("gen", genCatalog));
        assertTrue(mIndex.getSourceLanguages("gen").length > 0);

        String obsCatalog = Util.readStream(mContext.getAssets().open("indexer/obs/languages.json"));
        assertTrue(mIndex.indexSourceLanguages("obs", obsCatalog));
        assertTrue(mIndex.getSourceLanguages("obs").length > 0);
    }

    public void test03IndexResources() throws Exception {
        String genCatalog = Util.readStream(mContext.getAssets().open("indexer/gen/en/resources.json"));
        assertTrue(mIndex.indexResources("gen", "en", genCatalog));
        assertTrue(mIndex.getResources("gen", "en").length > 0);

        String obsCatalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/resources.json"));
        assertTrue(mIndex.indexResources("obs", "en", obsCatalog));
        assertTrue(mIndex.getResources("obs", "en").length > 0);
    }

    public void test04IndexSource() throws Exception {
        SourceTranslation bibleTranslation = SourceTranslation.simple("gen", "en", "ulb");
        String genCatalog = Util.readStream(mContext.getAssets().open("indexer/gen/en/ulb/source.json"));
        assertTrue(mIndex.indexSource(bibleTranslation, genCatalog));
        String[] bibleChapterIds = mIndex.getChapters(bibleTranslation);
        assertTrue(bibleChapterIds.length > 0);
        for(String id:bibleChapterIds) {
            assertTrue(mIndex.getFrames(bibleTranslation, id).length > 0);
        }

        SourceTranslation obsTranslation = SourceTranslation.simple("obs", "en", "obs");
        String obsCatalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/source.json"));
        assertTrue(mIndex.indexSource(obsTranslation, obsCatalog));
        String[] obsChapterIds = mIndex.getChapters(obsTranslation);
        assertTrue(obsChapterIds.length > 0);
        for(String id:obsChapterIds) {
            assertTrue(mIndex.getFrames(obsTranslation, id).length > 0);
        }
    }

    public void test05IndexNotes() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        String catalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/notes.json"));
        assertTrue(mIndex.indexNotes(translation, catalog));
        String[] noteIds = mIndex.getNotes(translation, "01", "01");
        assertTrue(noteIds.length > 0);
        assertNotNull(mIndex.getNote(translation, "01", "01", noteIds[0]));
    }

    public void test06IndexTerms() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        String catalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/terms.json"));
        assertTrue(mIndex.indexTerms(translation, catalog));
        String[] allTermIds = mIndex.getWords(translation);
        assertTrue(allTermIds.length > 0);
        assertNotNull(mIndex.getWord(translation, allTermIds[0]));
        String associationscatalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/tw_cat.json"));
        assertTrue(mIndex.indexTermAssignments(translation, associationscatalog));
        String[] termIds = mIndex.getWords(translation, "01", "01");
        assertTrue(termIds.length > 0);
    }

    public void test07IndexQuestions() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        String catalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/checking_questions.json"));
        assertTrue(mIndex.indexQuestions(translation, catalog));
        String[] questionIds = mIndex.getQuestions(translation, "01", "01");
        assertTrue(questionIds.length > 0);
        assertNotNull(mIndex.getQuestion(translation, "01", "01", questionIds[0]));
    }

    public void test08LoadExistingIndex() throws Exception {
        InputStream is = mContext.getAssets().open("indexer/sample_index.zip");
        File asset = new File(AppContext.context().getCacheDir(), "indexer/sample_index.zip");
        Util.copyStreamToCache(mContext, is, asset);
        File indexDir = asset.getParentFile();
        Zip.unzip(asset, indexDir);
        Indexer index = new Indexer(AppContext.context(), "sample_index", indexDir);
        index.destroy();
        assertTrue(index.getProjects().length > 0);
    }

    public void test09MergeIndexShallow() throws Exception {
        Indexer mergedIndex = new Indexer(AppContext.context(), "merged_app", mIndexRoot);
        mergedIndex.destroy();
        mergedIndex.mergeIndex(mIndex, true);
        assertTrue(mergedIndex.getProjects().length > 0);
        assertNotNull(mergedIndex.getProject("obs"));
        assertNotNull(mergedIndex.getProject("gen"));
        assertTrue(mergedIndex.getSourceLanguages("obs").length > 0);
        assertTrue(mergedIndex.getSourceLanguages("gen").length > 0);
        assertTrue(mergedIndex.getResources("obs", "en").length == 1);
        assertTrue(mergedIndex.getResources("gen", "en").length > 1);
        SourceTranslation obsTranslation = SourceTranslation.simple("obs", "en", "obs");
        SourceTranslation genTranslation = SourceTranslation.simple("gen", "en", "ulb");
        assertTrue(mergedIndex.getChapters(obsTranslation).length == 0);
        assertTrue(mergedIndex.getChapters(genTranslation).length == 0);
    }

    public void test10MergeIndexDeep() throws Exception {
        Indexer mergedIndex = new Indexer(AppContext.context(), "merged_app", mIndexRoot);
        mergedIndex.destroy();
        mergedIndex.mergeIndex(mIndex);
        assertTrue(mergedIndex.getProjects().length  > 1);
        assertNotNull(mergedIndex.getProject("obs"));
        assertNotNull(mergedIndex.getProject("gen"));
        assertTrue(mergedIndex.getSourceLanguages("obs").length > 0);
        assertTrue(mergedIndex.getSourceLanguages("gen").length > 0);
        assertTrue(mergedIndex.getResources("obs", "en").length == 1);
        assertTrue(mergedIndex.getResources("gen", "en").length > 0);
        SourceTranslation obsTranslation = SourceTranslation.simple("obs", "en", "obs");
        SourceTranslation genTranslation = SourceTranslation.simple("gen", "en", "ulb");
        assertTrue(mergedIndex.getChapters(obsTranslation).length > 0);
        assertTrue(mergedIndex.getChapters(genTranslation).length > 0);
    }

    public void test11MergeIndexProjectShallow() throws Exception {
        Indexer mergedIndex = new Indexer(AppContext.context(), "merged_app", mIndexRoot);
        mergedIndex.destroy();
        mergedIndex.mergeProject("obs", mIndex, true);
        assertTrue(mergedIndex.getProjects().length == 1);
        assertNotNull(mergedIndex.getProject("obs"));
        assertTrue(mergedIndex.getSourceLanguages("obs").length > 0);
        assertTrue(mergedIndex.getResources("obs", "en").length == 1);
        SourceTranslation obsTranslation = SourceTranslation.simple("obs", "en", "obs");
        assertTrue(mergedIndex.getChapters(obsTranslation).length == 0);
    }

    public void test12MergeIndexProjectDeep() throws Exception {
        Indexer mergedIndex = new Indexer(AppContext.context(), "merged_app", mIndexRoot);
        mergedIndex.destroy();
        mergedIndex.mergeProject("obs", mIndex);
        assertTrue(mergedIndex.getProjects().length == 1);
        assertNotNull(mergedIndex.getProject("obs"));
        assertTrue(mergedIndex.getSourceLanguages("obs").length > 0);
        assertTrue(mergedIndex.getResources("obs", "en").length == 1);
        SourceTranslation obsTranslation = SourceTranslation.simple("obs", "en", "obs");
        assertTrue(mergedIndex.getChapters(obsTranslation).length > 0);

        mergedIndex.destroy();
        mergedIndex.mergeProject("gen", mIndex);
        assertTrue(mergedIndex.getProjects().length == 1);
        assertNotNull(mergedIndex.getProject("gen"));
        assertTrue(mergedIndex.getSourceLanguages("gen").length > 0);
        assertTrue(mergedIndex.getResources("gen", "en").length > 0);
        SourceTranslation genTranslation = SourceTranslation.simple("gen", "en", "ulb");
        assertTrue(mergedIndex.getChapters(genTranslation).length > 0);
    }

    public void test999999Cleanup() throws Exception {
        //mIndex.destroy();
    }
}
