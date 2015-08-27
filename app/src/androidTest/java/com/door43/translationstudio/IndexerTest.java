package com.door43.translationstudio;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import com.door43.translationstudio.core.Indexer;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.util.AppContext;

/**
 * Created by joel on 8/27/2015.
 */
public class IndexerTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Indexer mIndex;
    private Context mContext;

    public IndexerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        MainApplication app = AppContext.context();
        mIndex = new Indexer("app", app.getCacheIndexDir());
        mContext = getInstrumentation().getContext();
    }

    public void test1IndexProjects() throws Exception {
        mIndex.destroy();
        String catalog = Util.readStream(mContext.getAssets().open("indexer/catalog.json"));
        assertTrue(mIndex.indexProjects(catalog));
        assertTrue(mIndex.getProjects().length > 0);
    }

    public void test2IndexSourceLanguages() throws Exception {
        String genCatalog = Util.readStream(mContext.getAssets().open("indexer/gen/languages.json"));
        assertTrue(mIndex.indexSourceLanguages("gen", genCatalog));
        assertTrue(mIndex.getSourceLanguages("gen").length > 0);

        String obsCatalog = Util.readStream(mContext.getAssets().open("indexer/obs/languages.json"));
        assertTrue(mIndex.indexSourceLanguages("obs", obsCatalog));
        assertTrue(mIndex.getSourceLanguages("obs").length > 0);
    }

    public void test3IndexResources() throws Exception {
        String genCatalog = Util.readStream(mContext.getAssets().open("indexer/gen/en/resources.json"));
        assertTrue(mIndex.indexResources("gen", "en", genCatalog));
        assertTrue(mIndex.getResources("gen", "en").length > 0);

        String obsCatalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/resources.json"));
        assertTrue(mIndex.indexResources("obs", "en", obsCatalog));
        assertTrue(mIndex.getResources("obs", "en").length > 0);
    }

    public void test4IndexSource() throws Exception {
        SourceTranslation bibleTranslation = new SourceTranslation("gen", "en", "ulb");
        String genCatalog = Util.readStream(mContext.getAssets().open("indexer/gen/en/ulb/source.json"));
        assertTrue(mIndex.indexSource(bibleTranslation, genCatalog));
        String[] bibleChapterIds = mIndex.getChapters(bibleTranslation);
        assertTrue(bibleChapterIds.length > 0);
        for(String id:bibleChapterIds) {
            assertTrue(mIndex.getFrames(bibleTranslation, id).length > 0);
        }

        SourceTranslation obsTranslation = new SourceTranslation("obs", "en", "obs");
        String obsCatalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/source.json"));
        assertTrue(mIndex.indexSource(obsTranslation, obsCatalog));
        String[] obsChapterIds = mIndex.getChapters(obsTranslation);
        assertTrue(obsChapterIds.length > 0);
        for(String id:obsChapterIds) {
            assertTrue(mIndex.getFrames(obsTranslation, id).length > 0);
        }
    }

    public void test5IndexNotes() throws Exception {
        SourceTranslation obsTranslation = new SourceTranslation("obs", "en", "obs");
        String catalog = Util.readStream(mContext.getAssets().open("indexer/obs/en/obs/notes.json"));
        assertTrue(mIndex.indexNotes(obsTranslation, catalog));
        // TODO: make sure we can retreive notes on a frame.
    }
}
