package com.door43.translationstudio;

import android.test.ActivityInstrumentationTestCase2;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.tasks.DownloadAvailableProjectsTask;

/**
 * Created by joel on 3/24/2015.
 */
public class DownloadTest  extends ActivityInstrumentationTestCase2<MainActivity> {

    public DownloadTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    public void testDownloadAvailableProjects() {
        DownloadAvailableProjectsTask task = new DownloadAvailableProjectsTask(true);
        task.start();
        assertEquals(task.getProjects().size() > 0, true);
        for(Project p:task.getProjects()) {
            assertEquals(p.getSourceLanguages().size() > 0, true);
            for(SourceLanguage l:p.getSourceLanguages()) {
                assertEquals(l.getResources().length > 0, true);

            }
        }
    }

    public void testDownloadLanguage() {
//        DownloadLanguageTask task = new DownloadLanguageTask();
    }

    public void testDownloadResourceCatalog() {

    }

    public void testDownloadTerms() {

    }

    public void testDownloadNotes() {

    }

    public void testDownloadSource() {

    }

    public void testImportProject() {

    }

    public void testImportLanguage() {

    }

    public void testImportResource() {

    }

    public void testImportNotes() {

    }

    public void testImportTerms() {

    }

    public void testImportSource() {

    }
}
