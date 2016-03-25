package com.door43.translationstudio;

import android.test.InstrumentationTestCase;

public class TranslationImportTest extends InstrumentationTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

//        if(!AppContext.isLoaded()) {
//             load everything
//            Util.runTask(new LoadTargetLanguagesTask());
//            Util.runTask(new LoadProjectsTask());
//            Util.runTask(new IndexProjectsTask(AppContext.projectManager().getProjectSlugs()));
//            AppContext.setLoaded(true);
//        } else {
//            Project[] projects = AppContext.projectManager().getProjectSlugs();
//            for(Project p:projects) {
//                FileUtilities.deleteRecursive(new File(ProjectManager.getRepositoryPath(p, p.getSelectedSourceLanguage())));
//                p.flush();
//            }
//        }
    }

    /**
     * import legacy dokuwiki with a single language translation
     */
    public void testLegacyDokuwikiImport() throws Exception {
//        File asset = AppContext.context().getAssetAsFile("tests/exports/1.0_deutsch.txt");
//        assertTrue(Sharing.importDokuWiki(asset));

        // TODO: verify content imported correctly
    }

    /**
     * import legacy dokuwiki with multiple language translations
     */
    public void testLegacyDokuwikiMultipleImport() throws Exception {
//        File asset = AppContext.context().getAssetAsFile("tests/exports/1.0_afaraf_deutsch.txt");
//        assertTrue(Sharing.importDokuWiki(asset));

        // TODO: verify content imported correctly
    }

    /**
     * import dokuwiki
     */
    public void testDokuwikiImport() throws Exception {
//        File asset = AppContext.context().getAssetAsFile("tests/exports/2.0.0_uw-obs-de_dokuwiki.zip");
//        assertTrue(Sharing.importDokuWikiArchive(asset));

        // TODO: verify content imported correctly
    }

    /**
     * import legacy translation studio project archive through the dokuwiki archive import.
     * The archive should be redirected to the proper prepareLegacyArchiveImport method
     * @throws Exception
     */
    public void testLegacyProjectRedirectsFromDokuwikiArchiveImport() throws Exception {
//        File asset = AppContext.context().getAssetAsFile("tests/exports/2.0.0_uw-obs-de.zip");
//        assertTrue(Sharing.importDokuWikiArchive(asset));

        // TODO: verify content imported correctly
    }

    /**
     * import legacy translation studio project archive
     * @throws Exception
     */
    public void testLegacyProjectImport() throws Exception {
//        File asset = AppContext.context().getAssetAsFile("tests/exports/2.0.0_uw-obs-de.zip");
//        assertTrue(Sharing.prepareLegacyArchiveImport(asset));

        // TODO: verify content imported correctly
    }

    /**
     * import translation studio project archive
     * @throws Exception
     */
    public void testProjectImport() throws Exception {
//        File dokuwiki = AppContext.context().getAssetAsFile("tests/exports/2.0.3_uw-obs-de.tstudio");
//        ProjectImport[] projects = Sharing.prepareArchiveImport(dokuwiki);
//        assertTrue(projects.length > 0);
//        for(ProjectImport p:projects) {
//            assertTrue(Sharing.importProject(p));
//        }

        // TODO: verify content imported correctly
    }
}