package com.door43.translationstudio.fast;

import android.test.AndroidTestCase;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Downloader;
import com.door43.translationstudio.core.Indexer;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.AppContext;

import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by joel on 8/27/2015.
 */
public class DownloaderTest extends AndroidTestCase {
    private Downloader mDownloader;
    private Indexer mIndex;
    private File mIndexRoot;

    @Override
    protected void setUp() throws Exception {
        MainApplication app = AppContext.context();
        mIndexRoot = new File(app.getCacheDir(), "test_index");
        mIndex = new Indexer(app, "downloads", mIndexRoot);
        String server = app.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, app.getResources().getString(R.string.pref_default_media_server));
        mDownloader = new Downloader(mIndex, server + app.getResources().getString(R.string.root_catalog_api));
    }

    public void test1DownloadProjects() throws Exception {
        FileUtils.deleteQuietly(mIndexRoot);
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

    public void test4DownloadSource() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        assertTrue(mDownloader.downloadSource(translation));
        String[] chapterIds = mIndex.getChapters(translation);
        assertTrue(chapterIds.length > 0);
        String[] frameIds = mIndex.getFrames(translation, "01");
        assertTrue(frameIds.length > 0);
    }

    public void test5DownloadTerms() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        assertTrue(mDownloader.downloadTerms(translation));
        String[] allTermIds = mIndex.getWords(translation);
        assertTrue(allTermIds.length > 0);
        assertNotNull(mIndex.getWord(translation, allTermIds[0]));
        assertTrue(mDownloader.downloadTermAssignments(translation));
        String[] termsIds = mIndex.getWords(translation, "01", "01");
        assertTrue(termsIds.length > 0);
    }

    public void test6DownloadNotes() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        assertTrue(mDownloader.downloadNotes(translation));
        String[] noteIds = mIndex.getNotes(translation, "01", "01");
        assertTrue(noteIds.length > 0);
    }

    public void test7DownloadQuestions() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        assertTrue(mDownloader.downloadCheckingQuestions(translation));
        String[] questionIds = mIndex.getQuestions(translation, "01", "01");
        assertTrue(questionIds.length > 0);
    }

    public void test999999Cleanup() throws Exception {
        FileUtils.deleteQuietly(mIndexRoot);
    }
}
