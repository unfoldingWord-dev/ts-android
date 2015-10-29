package com.door43.translationstudio.slow;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.ProjectCategory;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.AppContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by joel on 8/31/2015.
 */
public class LibraryTest extends InstrumentationTestCase {
    private Library mLibrary;

    protected void setUp() throws Exception {
        mLibrary = AppContext.getLibrary();
    }

    public void test01Clean() throws Exception {
        mLibrary.delete();
    }

    public void test02ExtractLibrary() throws Exception {
        // NOTE: the default library is large so we don't include in the repo. So this test should always fall through
        assertFalse(mLibrary.exists());
        AppContext.deployDefaultLibrary();
        mLibrary = AppContext.getLibrary();

        // NOTE: this will fail when first updating the db version
        assertTrue(mLibrary.exists());
    }

    public void test03DownloadTargetLanguages() throws Exception {
        mLibrary.downloadTargetLanguages();
        assertTrue(mLibrary.getTargetLanguages().length > 0);
    }

    public void test04CheckForAvailableUpdates() throws Exception {
        LibraryUpdates updates = mLibrary.checkServerForUpdates(null);

        // cache updates
        FileOutputStream fos = AppContext.context().openFileOutput("library_updates", Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeObject(updates);
        os.close();
        fos.close();

        if(updates.numSourceTranslationUpdates() > 0) {
            String pid = updates.getUpdatedProjects()[0];
            assertTrue(updates.getUpdatedSourceLanguages(pid).length > 0);
            String lid = updates.getUpdatedSourceLanguages(pid)[0];
            assertTrue(updates.getUpdatedResources(pid, lid).length > 0);
        }
    }

    public void test05DownloadUpdates() throws Exception {
        FileInputStream fis = AppContext.context().openFileInput("library_updates");
        ObjectInputStream is = new ObjectInputStream(fis);
        LibraryUpdates updates = (LibraryUpdates) is.readObject();
        is.close();
        fis.close();

        // download all available updates
        assertTrue(mLibrary.downloadUpdates(updates, null));
    }

    public void test06Export() throws Exception {
        File archive = mLibrary.export(AppContext.getPublicDownloadsDirectory());
        assertNotNull(archive);
        assertTrue(archive.exists());
    }

    public void test07LoadTargetLanguages() throws Exception {
        TargetLanguage[] languages = mLibrary.getTargetLanguages();
        assertNotNull(languages);
        assertTrue(languages.length > 0);
    }

    public void test08DownloadSourceTranslation() throws Exception {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslations("obs")[0];

        assertTrue(mLibrary.downloadSourceTranslation(sourceTranslation, null));
    }

    public void test09GetProjectCategories() throws Exception {
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

    public void test10GetSourceLanguages() throws Exception {
        SourceLanguage[] sourceLanguages = mLibrary.getSourceLanguages("obs");
        assertTrue(sourceLanguages.length > 0);

        SourceLanguage sourceLanguage = mLibrary.getSourceLanguage("obs", "en");
        assertNotNull(sourceLanguage);
        assertEquals("en", sourceLanguage.getId());
    }

    public void test11GetResources() throws Exception {
        Resource[] resources = mLibrary.getResources("obs", "en");
        assertTrue(resources.length > 0);
    }

    public void test12GetProject() throws Exception {
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
}
