package com.door43.translationstudio;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import com.door43.translationstudio.core.Downloader;
import com.door43.translationstudio.core.Indexer;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.util.AppContext;

/**
 * Created by joel on 8/27/2015.
 */
public class DownloaderTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private Downloader mDownloader;
    private Indexer mIndex;

    public DownloaderTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        MainApplication app = AppContext.context();
        mIndex = new Indexer("downloads", app.getCacheIndexDir());
        String server = app.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, app.getResources().getString(R.string.pref_default_media_server));
        mDownloader = new Downloader(mIndex, server + app.getResources().getString(R.string.root_catalog_api));
    }

    public void test1DownloadProjects() throws Exception {
        mIndex.destroy();
        assertTrue(mDownloader.downloadProjectList());
        String[] projectIds = mIndex.getProjects();
        assertTrue(projectIds.length > 0);
    }

    public void test2DownloadSourceLanguages() throws Exception {
        assertTrue(mDownloader.downloadSourceLanguageList("obs"));
        String[] languageIds = mIndex.getSourceLanguages("obs");
        assertTrue(languageIds.length > 0);
    }

    public void test3DownloadResources() throws Exception {
        assertTrue(mDownloader.downloadResourceList("obs", "en"));
        String[] resourceIds = mIndex.getResources("obs", "en");
        assertTrue(resourceIds.length > 0);
    }

    public void test4DownloadTerms() throws Exception {
        SourceTranslation translation = new SourceTranslation("obs", "en", "ulb");
        assertTrue(mDownloader.downloadTerms(translation));
        String[] termsIds = mIndex.getTerms(translation);
        assertTrue(termsIds.length > 0);
    }

    public void test5DownloadNotes() throws Exception {
        SourceTranslation translation = new SourceTranslation("obs", "en", "ulb");
        assertTrue(mDownloader.downloadNotes(translation));
        String[] noteIds = mIndex.getNotes(translation, "01", "01");
        assertTrue(noteIds.length > 0);
    }

    public void test6DownloadQuestions() throws Exception {
        SourceTranslation translation = new SourceTranslation("obs", "en", "ulb");
        assertTrue(mDownloader.downloadCheckingQuestions(translation));
        String[] questionIds = mIndex.getQuestions(translation, "01", "01");
        assertTrue(questionIds.length > 0);
    }
}
