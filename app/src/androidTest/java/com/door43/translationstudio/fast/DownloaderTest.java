package com.door43.translationstudio.fast;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Downloader;
import com.door43.translationstudio.core.Indexer;
import com.door43.translationstudio.core.IndexerSQLiteHelper;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.AppContext;

import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by joel on 8/27/2015.
 */
public class DownloaderTest extends InstrumentationTestCase {

    private Downloader mDownloader;
    private Indexer mIndex;
    private File mIndexRoot;

    @Override
    protected void setUp() throws Exception {
        MainApplication app = AppContext.context();
        mIndexRoot = new File(app.getCacheDir(), "test_index");
        IndexerSQLiteHelper downloadIndexHelper = new IndexerSQLiteHelper(app, "downloads");
        mIndex = new Indexer(app, "downloads", mIndexRoot, downloadIndexHelper);
        String server = app.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, app.getResources().getString(R.string.pref_default_media_server));
        mDownloader = new Downloader(mIndex, server + app.getResources().getString(R.string.root_catalog_api));
    }

    public void test1DownloadProjects() throws Exception {
        mIndex.delete();
        mIndex.rebuild();
        FileUtils.deleteQuietly(mIndexRoot);
        mDownloader.getIndex().beginTransaction();
        assertTrue(mDownloader.downloadProjectList());
        mDownloader.getIndex().endTransaction(true);
        String[] projectIds = mIndex.getProjectSlugs();
        assertTrue(projectIds.length > 0);
    }

    public void test2DownloadSourceLanguages() throws Exception {
        mDownloader.getIndex().beginTransaction();
        assertTrue(mDownloader.downloadSourceLanguageList("obs"));
        mDownloader.getIndex().endTransaction(true);
        String[] languageIds = mIndex.getSourceLanguageSlugs("obs");
        assertTrue(languageIds.length > 0);
    }

    public void test3DownloadResources() throws Exception {
        mDownloader.getIndex().beginTransaction();
        assertTrue(mDownloader.downloadResourceList("obs", "en"));
        mDownloader.getIndex().endTransaction(true);
        String[] resourceIds = mIndex.getResourceSlugs("obs", "en");
        assertTrue(resourceIds.length > 0);
    }

    public void test4DownloadSource() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        mDownloader.getIndex().beginTransaction();
        assertTrue(mDownloader.downloadSource(translation, mDownloader.getIndex()));
        mDownloader.getIndex().endTransaction(true);
        String[] chapterIds = mIndex.getChapterSlugs(translation);
        assertTrue(chapterIds.length > 0);
        String[] frameIds = mIndex.getFrameSlugs(translation, "01");
        assertTrue(frameIds.length > 0);
    }

    public void test5DownloadTerms() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        mDownloader.getIndex().beginTransaction();
        assertTrue(mDownloader.downloadTerms(translation, mDownloader.getIndex()));
        mDownloader.getIndex().endTransaction(true);
        String[] allTermIds = mIndex.getWords(translation);
        assertTrue(allTermIds.length > 0);
        assertNotNull(mIndex.getWord(translation, allTermIds[0]));
        mDownloader.getIndex().beginTransaction();
        assertTrue(mDownloader.downloadTermAssignments(translation, mDownloader.getIndex()));
        mDownloader.getIndex().endTransaction(true);
        String[] termsIds = mIndex.getWordsForFrame(translation, "01", "01");
        assertTrue(termsIds.length > 0);
    }

    public void test6DownloadNotes() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        mDownloader.getIndex().beginTransaction();
        assertTrue(mDownloader.downloadNotes(translation, mDownloader.getIndex()));
        mDownloader.getIndex().endTransaction(true);
        String[] noteIds = mIndex.getNoteSlugs(translation, "01", "01");
        assertTrue(noteIds.length > 0);
    }

    public void test7DownloadQuestions() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        mDownloader.getIndex().beginTransaction();
        assertTrue(mDownloader.downloadCheckingQuestions(translation, mDownloader.getIndex()));
        mDownloader.getIndex().endTransaction(true);
        String[] questionIds = mIndex.getQuestions(translation, "01", "01");
        assertTrue(questionIds.length > 0);
    }

    public void test999999Cleanup() throws Exception {
        FileUtils.deleteQuietly(mIndexRoot);
    }
}
