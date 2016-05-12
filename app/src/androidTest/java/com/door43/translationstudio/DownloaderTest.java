package com.door43.translationstudio;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.core.CheckingQuestion;
import com.door43.translationstudio.core.Downloader;
import com.door43.translationstudio.core.Indexer;
import com.door43.translationstudio.core.IndexerSQLiteHelper;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TranslationWord;

/**
 * Created by joel on 8/27/2015.
 */
public class DownloaderTest extends InstrumentationTestCase {

    private Downloader mDownloader;
    private Indexer mIndex;

    @Override
    protected void setUp() throws Exception {
        MainApplication app = AppContext.context();
        IndexerSQLiteHelper downloadIndexHelper = new IndexerSQLiteHelper(app, "downloader_test_downloads");
        mIndex = new Indexer(app, "downloader_test_downloads", downloadIndexHelper);
        String server = app.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, app.getResources().getString(R.string.pref_default_media_server));
        mDownloader = new Downloader(server + app.getResources().getString(R.string.root_catalog_api));
    }

    public void test1DownloadProjects() throws Exception {
        mIndex.delete();
        mIndex.rebuild();
        mIndex.beginTransaction();
        assertTrue(mDownloader.downloadProjectList(mIndex));
        mIndex.endTransaction(true);
        String[] projectIds = mIndex.getProjectSlugs();
        assertTrue(projectIds.length > 0);
    }

    public void test2DownloadSourceLanguages() throws Exception {
        mIndex.beginTransaction();
        assertTrue(mDownloader.downloadSourceLanguageList("obs", mIndex));
        mIndex.endTransaction(true);
        String[] languageIds = mIndex.getSourceLanguageSlugs("obs");
        assertTrue(languageIds.length > 0);
    }

    public void test3DownloadResources() throws Exception {
        mIndex.beginTransaction();
        assertTrue(mDownloader.downloadResourceList("obs", "en", mIndex));
        mIndex.endTransaction(true);
        String[] resourceIds = mIndex.getResourceSlugs("obs", "en");
        assertTrue(resourceIds.length > 0);
    }

    public void test4DownloadSource() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        mIndex.beginTransaction();
        assertTrue(mDownloader.downloadSource(translation, mIndex));
        mIndex.endTransaction(true);
        String[] chapterIds = mIndex.getChapterSlugs(translation);
        assertTrue(chapterIds.length > 0);
        String[] frameIds = mIndex.getFrameSlugs(translation, "01");
        assertTrue(frameIds.length > 0);
    }

    public void test5DownloadTerms() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        mIndex.beginTransaction();
        assertTrue(mDownloader.downloadWords(translation, mIndex));
        mIndex.endTransaction(true);
        String[] allTermIds = mIndex.getWordSlugs(translation);
        assertTrue(allTermIds.length > 0);
        assertNotNull(mIndex.getWord(translation, allTermIds[0]));
        mIndex.beginTransaction();
        assertTrue(mDownloader.downloadWordAssignments(translation, mIndex));
        mIndex.endTransaction(true);
        TranslationWord[] frameWords = mIndex.getWordsForFrame(translation, "01", "01");
        assertTrue(frameWords.length > 0);
    }

    public void test6DownloadNotes() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        mIndex.beginTransaction();
        assertTrue(mDownloader.downloadNotes(translation, mIndex));
        mIndex.endTransaction(true);
        String[] noteIds = mIndex.getNoteSlugs(translation, "01", "01");
        assertTrue(noteIds.length > 0);
    }

    public void test7DownloadQuestions() throws Exception {
        SourceTranslation translation = SourceTranslation.simple("obs", "en", "obs");
        mIndex.beginTransaction();
        assertTrue(mDownloader.downloadCheckingQuestions(translation, mIndex));
        mIndex.endTransaction(true);
        CheckingQuestion[] questions = mIndex.getCheckingQuestions(translation, "01", "01");
        assertTrue(questions.length > 0);
    }

    public void test8DownloadTargetLanguages() throws Exception {
        mIndex.beginTransaction();
        mDownloader.downloadTargetLanguages(mIndex);
        mIndex.endTransaction(true);
        assertTrue(mIndex.getTargetLanguages().length > 0);
    }
}
