package com.door43.translationstudio.slow;

import android.content.Context;
import android.test.AndroidTestCase;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.ProjectCategory;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
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
public class LibraryTest extends AndroidTestCase {
    private Library mLibrary;
    private File mLibraryDir;

    protected void setUp() throws Exception {
        MainApplication app = AppContext.context();
        mLibraryDir = new File(app.getFilesDir(), "test_library");
        String server = app.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, app.getResources().getString(R.string.pref_default_media_server));
        String rootApi = server + app.getResources().getString(R.string.root_catalog_api);
        mLibrary = new Library(app, mLibraryDir, mLibraryDir, rootApi);
    }

    public void test01Clean() throws Exception {
        FileUtils.deleteQuietly(mLibraryDir);
    }

    public void test02ExtractLibrary() throws Exception {
        // NOTE: the default library is large so we don't include in the repo. So this test should always fall through
        assertFalse(mLibrary.exists());
        mLibrary.deployDefaultLibrary();
        assertTrue(mLibrary.exists());
    }

    public void test03CheckForAvailableUpdates() throws Exception {
        // pre-populate download index with shallow copy
        mLibrary.seedDownloadIndex();

        LibraryUpdates updates = mLibrary.getAvailableLibraryUpdates(null);

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

        assertTrue(mLibrary.downloadUpdates(updates, null));
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

    public void test07DownloadSourceTranslation() throws Exception {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslations("obs")[0];
        assertTrue(mLibrary.downloadSourceTranslation(sourceTranslation, null));
    }

    public void test08GetProjectCategories() throws Exception {
        ProjectCategory[] projectCategories = mLibrary.getProjectCategories("en");
        // for now we just have obs, nt, and ot
        assertEquals(3, projectCategories.length);
        ProjectCategory category = null;
        for(ProjectCategory projectCategory:projectCategories) {
            if(!projectCategory.isProject()) {
                category = projectCategory;
                break;
            }
        }
        assertNotNull(category);
        ProjectCategory[] subCategories = mLibrary.getProjectCategories(category);
        assertTrue(subCategories.length > 0);

    }

    public void test09GetSourceLanguages() throws Exception {
        SourceLanguage[] sourceLanguages = mLibrary.getSourceLanguages("obs");
        assertTrue(sourceLanguages.length > 0);

        SourceLanguage sourceLanguage = mLibrary.getSourceLanguage("obs", "en");
        assertNotNull(sourceLanguage);
        assertEquals("en", sourceLanguage.getId());
    }

    public void test10GetResources() throws Exception {
        Resource[] resources = mLibrary.getResources("obs", "en");
        assertTrue(resources.length > 0);
    }

    public void test11GetProject() throws Exception {
        Project p = mLibrary.getProject("obs", "en");
        assertNotNull(p);
        assertEquals("obs", p.getId());
    }

    public void test13GetTargetLanguage() throws Exception {
        TargetLanguage targetLanguage = mLibrary.getTargetLanguage("en");
        assertEquals("en", targetLanguage.getId());
    }

    public void test14GetSourceTranslation() throws Exception {
        SourceTranslation[] sourceTranslations = mLibrary.getSourceTranslations("obs");
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(sourceTranslations[0].getId());
        assertNotNull(sourceTranslation);
        assertEquals(sourceTranslations[0].getId(), sourceTranslation.getId());

        SourceTranslation anotherSourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        assertNotNull(anotherSourceTranslation);
    }

    public void test999999Cleanup() throws Exception {
        FileUtils.deleteQuietly(mLibraryDir);
    }
}
