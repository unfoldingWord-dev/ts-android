package com.door43.translationstudio;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import com.door43.translationstudio.core.Downloader;
import com.door43.translationstudio.core.Indexer;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.util.AppContext;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by joel on 8/31/2015.
 */
public class LibraryTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private Library mLibrary;
    private Indexer mDownloadIndex;
    private Downloader mDownloader;
    private Indexer mServerIndex;
    private Indexer mAppIndex;
    private File mIndexRoot;
    private File mTempIndexRoot;

    public LibraryTest() {
        super(MainActivity.class);
    }

    protected void setUp() throws Exception {
        MainApplication app = AppContext.context();
        mTempIndexRoot = new File(app.getCacheDir(), "library_temp_test_index");
        mIndexRoot = new File(app.getCacheDir(), "library_test_index");
        mDownloadIndex = new Indexer("downloads", mTempIndexRoot);
        mServerIndex = new Indexer("server", mTempIndexRoot);
        mAppIndex = new Indexer("app", mIndexRoot);
        String server = app.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, app.getResources().getString(R.string.pref_default_media_server));
        mDownloader = new Downloader(mDownloadIndex, server + app.getResources().getString(R.string.root_catalog_api));
        mLibrary = new Library(mDownloader, mServerIndex, mAppIndex);
    }

    public void test1CheckForAvailableUpdates() throws Exception {
        FileUtils.deleteQuietly(mTempIndexRoot);
        FileUtils.deleteQuietly(mIndexRoot);
        LibraryUpdates updates = mLibrary.getAvailableLibraryUpdates();

        // cache updates
        FileOutputStream fos = AppContext.context().openFileOutput("library_updates", Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeObject(updates);
        os.close();
        fos.close();

        assertTrue(updates.getUpdatedProjects().length > 0);
        String pid = updates.getUpdatedProjects()[0];
        assertTrue(updates.getUpdatedSourceLanguages(pid).length > 0);
        String lid = updates.getUpdatedSourceLanguages(pid)[0];
        assertTrue(updates.getUpdatedResources(pid, lid).length > 0);
    }

    public void test2DownloadUpdates() throws Exception {
        FileInputStream fis = AppContext.context().openFileInput("library_updates");
        ObjectInputStream is = new ObjectInputStream(fis);
        LibraryUpdates updates = (LibraryUpdates) is.readObject();
        is.close();
        fis.close();

        assertTrue(mLibrary.downloadUpdates(updates));
    }

    public void test3Export() throws Exception {
        File archive = mLibrary.export(AppContext.getPublicDownloadsDirectory());
        assertNotNull(archive);
        assertTrue(archive.exists());
    }

    public void test999999Cleanup() throws Exception {
        FileUtils.deleteQuietly(mTempIndexRoot);
//        FileUtils.deleteQuietly(mIndexRoot);
    }
}
