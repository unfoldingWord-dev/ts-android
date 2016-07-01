package com.door43.translationstudio;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.door43.translationstudio.core.ChunkMarker;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.ProjectCategory;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;

/**
 * Created by joel on 8/31/2015.
 */
@MediumTest
public class LibraryTest extends InstrumentationTestCase {
    private Library mLibrary;

    protected void setUp() throws Exception {
        mLibrary = App.getLibrary();
    }

    public void test01Clean() throws Exception {
        mLibrary.delete();
    }

    public void test02ExtractLibrary() throws Exception {
        // NOTE: the default library is large so we don't include in the repo. So this test should always fall through
        assertFalse(mLibrary.exists());
        App.deployDefaultLibrary();
        mLibrary = App.getLibrary();

        // NOTE: this will fail when first updating the db version
        assertTrue(mLibrary.exists());
    }

    public void test03ChunkMarkers() throws Exception {
        ChunkMarker[] markers = mLibrary.getChunkMarkers("gen");
        assertTrue(markers.length > 0);
    }

    public void test04ChunkMarkers() throws Exception {
        ChunkMarker[] markers = mLibrary.getChunkMarkers("rev");
        assertTrue(markers.length > 0);
    }

    public void test05ChunkMarkers() throws Exception {
        ChunkMarker[] markers = mLibrary.getChunkMarkers("psa");
        assertTrue(markers.length > 0);
    }

//    public void test03DownloadTargetLanguages() throws Exception {
//        mLibrary.downloadTargetLanguages();
//        assertTrue(mLibrary.getTargetLanguages().length > 0);
//    }

//    public void test04CheckForAvailableUpdates() throws Exception {
//        LibraryUpdates updates = mLibrary.checkServerForUpdates(null);
//
//        // cache updates
//        FileOutputStream fos = App.context().openFileOutput("library_updates", Context.MODE_PRIVATE);
//        ObjectOutputStream os = new ObjectOutputStream(fos);
//        os.writeObject(updates);
//        os.close();
//        fos.close();
//
//        if(updates.numSourceTranslationUpdates() > 0) {
//            String pid = updates.getUpdatedProjects()[0];
//            assertTrue(updates.getUpdatedSourceLanguages(pid).length > 0);
//            String lid = updates.getUpdatedSourceLanguages(pid)[0];
//            assertTrue(updates.getUpdatedResources(pid, lid).length > 0);
//        }
//    }

//    public void test05DownloadUpdates() throws Exception {
//        FileInputStream fis = App.context().openFileInput("library_updates");
//        ObjectInputStream is = new ObjectInputStream(fis);
//        LibraryUpdates updates = (LibraryUpdates) is.readObject();
//        is.close();
//        fis.close();
//
//        // download all available updates
//        assertTrue(mLibrary.downloadUpdates(updates, null));
//    }

//    public void test06DownloadEverything() throws Exception {
//        mLibrary.delete();
//        mLibrary = App.getLibrary();
//        assertFalse(mLibrary.exists());
//        mLibrary.checkServerForUpdates(null);
//        assertTrue(mLibrary.downloadTargetLanguages());
//        assertTrue(mLibrary.downloadAllProjects(null, null));
//    }

//    public void test07Export() throws Exception {
//        File archive = mLibrary.export(App.getPublicDownloadsDirectory());
//        assertNotNull(archive);
//        assertTrue(archive.exists());
//    }

    public void test08LoadTargetLanguages() throws Exception {
        TargetLanguage[] languages = mLibrary.getTargetLanguages();
        assertNotNull(languages);
        assertTrue(languages.length > 0);
    }

//    public void test09DownloadSourceTranslation() throws Exception {
//        SourceTranslation sourceTranslation = mLibrary.getSourceTranslations("obs")[0];
//
//        assertTrue(mLibrary.downloadSourceTranslation(sourceTranslation, null));
//    }

    public void test10GetProjectCategories() throws Exception {
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

    public void test11GetSourceLanguages() throws Exception {
        SourceLanguage[] sourceLanguages = mLibrary.getSourceLanguages("obs");
        assertTrue(sourceLanguages.length > 0);

        SourceLanguage sourceLanguage = mLibrary.getSourceLanguage("obs", "en");
        assertNotNull(sourceLanguage);
        assertEquals("en", sourceLanguage.getId());
    }

    public void test12GetResources() throws Exception {
        Resource[] resources = mLibrary.getResources("obs", "en");
        assertTrue(resources.length > 0);
    }

    public void test13GetProject() throws Exception {
        Project p = mLibrary.getProject("obs", "en");
        assertNotNull(p);
        assertEquals("obs", p.getId());
    }

    public void test14GetTargetLanguage() throws Exception {
        TargetLanguage targetLanguage = mLibrary.getTargetLanguage("en");
        assertEquals("en", targetLanguage.getId());
    }

    public void test15GetSourceTranslation() throws Exception {
        SourceTranslation[] sourceTranslations = mLibrary.getSourceTranslations("obs");
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(sourceTranslations[0].getId());
        assertNotNull(sourceTranslation);
        assertEquals(sourceTranslations[0].getId(), sourceTranslation.getId());

        SourceTranslation anotherSourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        assertNotNull(anotherSourceTranslation);
    }
}
