package com.door43.translationstudio;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.AndroidTestCase;

import com.door43.translationstudio.core.Downloader;
import com.door43.translationstudio.core.Indexer;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by joel on 8/31/2015.
 */
public class LibraryTest extends AndroidTestCase {
    private Library mLibrary;
    private Indexer mDownloadIndex;
    private Downloader mDownloader;
    private Indexer mServerIndex;
    private Indexer mAppIndex;
    private File mIndexRoot;
    private File mTempIndexRoot;

    protected void setUp() throws Exception {
        MainApplication app = AppContext.context();
        mTempIndexRoot = new File(app.getCacheDir(), "library_temp_test_index");
        mIndexRoot = new File(app.getCacheDir(), "library_test_index");
        mAppIndex = new Indexer("app", mIndexRoot);
        mDownloadIndex = new Indexer("downloads", mTempIndexRoot);
        mServerIndex = new Indexer("server", mTempIndexRoot);
        String server = app.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, app.getResources().getString(R.string.pref_default_media_server));
        mDownloader = new Downloader(mDownloadIndex, server + app.getResources().getString(R.string.root_catalog_api));
        mLibrary = new Library(app, mDownloader, mServerIndex, mAppIndex);
    }

    public void test01Clean() throws Exception {
        FileUtils.deleteQuietly(mTempIndexRoot);
        FileUtils.deleteQuietly(mIndexRoot);
    }

    public void test02ExtractLibrary() throws Exception {
        // NOTE: the default library is large so we don't include in the repo. So this test should always fall through
        mLibrary.extractDefaultLibrary();
    }

    public void test03CheckForAvailableUpdates() throws Exception {
        // pre-populate download index with shallow copy
        mDownloadIndex.mergeIndex(mAppIndex, true);

        LibraryUpdates updates = mLibrary.getAvailableLibraryUpdates();

        // cache updates
        FileOutputStream fos = AppContext.context().openFileOutput("library_updates", Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeObject(updates);
        os.close();
        fos.close();

        if(updates.getUpdatedProjects().length > 0) {
            String pid = updates.getUpdatedProjects()[0];
            assertTrue(updates.getUpdatedSourceLanguages(pid).length > 0);
            String lid = updates.getUpdatedSourceLanguages(pid)[0];
            assertTrue(updates.getUpdatedResources(pid, lid).length > 0);
        }
    }

    public void test04DownloadUpdates() throws Exception {
        FileInputStream fis = AppContext.context().openFileInput("library_updates");
        ObjectInputStream is = new ObjectInputStream(fis);
        LibraryUpdates updates = (LibraryUpdates) is.readObject();
        is.close();
        fis.close();

        assertTrue(mLibrary.downloadUpdates(updates));
    }

    public void test05Export() throws Exception {
        File archive = mLibrary.export(AppContext.getPublicDownloadsDirectory());
        assertNotNull(archive);
        assertTrue(archive.exists());
    }

    public void test06LoadTargetLanguages() throws Exception {
        TargetLanguage[] languages = mLibrary.getTargetLanguages();
        assertNotNull(languages);
        assertTrue(languages.length > 0);
    }

    public void test999999Cleanup() throws Exception {
        FileUtils.deleteQuietly(mTempIndexRoot);
        FileUtils.deleteQuietly(mIndexRoot);
    }
}
