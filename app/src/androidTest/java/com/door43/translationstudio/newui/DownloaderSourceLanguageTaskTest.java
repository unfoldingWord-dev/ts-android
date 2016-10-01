package com.door43.translationstudio.newui;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
//import com.door43.translationstudio.SettingsActivity;
//import com.door43.translationstudio.core.CheckingQuestion;
//import com.door43.translationstudio.core.Downloader;
//import com.door43.translationstudio.core.Library;
//import com.door43.translationstudio.core.LibraryData;
//import com.door43.translationstudio.core.SourceTranslation;
//import com.door43.translationstudio.core.TranslationWord;
import com.door43.translationstudio.newui.library.ServerLibraryDetailFragment;
import com.door43.translationstudio.tasks.DownloadSourceLanguageTask;

import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by joel on 8/27/2015.
 */
@MediumTest
public class DownloaderSourceLanguageTaskTest extends InstrumentationTestCase  implements ManagedTask.OnFinishedListener,  ManagedTask.OnProgressListener {

//    private Downloader mDownloader;
//    private LibraryData mIndex;
    private boolean success = false;
    private int progressCount = 0;
    private final CountDownLatch signal = new CountDownLatch(1);

    @Override
    protected void setUp() throws Exception {
//        mIndex = new LibraryData(App.context());
//        String server = App.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, App.context().getResources().getString(R.string.pref_default_media_server));
//        mDownloader = new Downloader(server + App.context().getResources().getString(R.string.root_catalog_api));
    }

    public void test01DownloadSourceValid() {
        String resource = "obs";
        String languageId = "fr";
        boolean expectSuccess = true;
        doSourceDownload(resource, languageId);

        assertSame(expectSuccess, success);
        assertTrue(progressCount > 1);
    }

    public void test02DownloadLanguageInvalid() {
        String resource = "obs";
        String languageId = "english";
        boolean expectSuccess = false;
        doSourceDownload(resource, languageId);

        assertSame(expectSuccess, success);
    }

    public void test03DownloadResourceInvalid() {
        String resource = "bible";
        String languageId = "fr";
        boolean expectSuccess = false;
        doSourceDownload(resource, languageId);

        assertSame(expectSuccess, success);
    }

    private void doSourceDownload(String resource, String languageId) {
        DownloadSourceLanguageTask task = new DownloadSourceLanguageTask(resource, languageId);
        task.addOnFinishedListener(this);
        task.addOnProgressListener(this);
        String id = resource + "-" + languageId;
        TaskManager.addTask(task, id);
        TaskManager.groupTask(task, ServerLibraryDetailFragment.DOWNLOAD_SOURCE_LANGUAGE_TASK_GROUP);
        success = false;

        try {
            //wait for download
            signal.await(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(DownloaderSourceLanguageTaskTest.class.getSimpleName(),id + " download failed", e);
        }
    }

    /**
     * called when download is finished
     *
     * @param task
     */
    @Override
    public void onTaskFinished(ManagedTask task) {
        DownloadSourceLanguageTask downloadTask = (DownloadSourceLanguageTask) task;
        if (downloadTask.isFinished() && downloadTask.getSuccess()) {
            success = true;
        } else {
            success = false;
        }
    }

    @Override
    public void onTaskProgress(final ManagedTask task, final double progress, final String message, final boolean secondary) {
        this.progressCount++;
    }
}
