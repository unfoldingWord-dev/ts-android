package com.door43.translationstudio.fast;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.Util;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;

import java.io.File;

/**
 * Created by joel on 10/27/2015.
 */
public class ImportTest extends InstrumentationTestCase {

    private Context context;
    private Translator translator;
    private File testsDir;
    private Library library;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.context = getInstrumentation().getContext();
        this.testsDir = new File(AppContext.context().getCacheDir(), "import_tests");
        this.translator = new Translator(this.context, new File(this.testsDir, "translator"));
        this.library = AppContext.getLibrary();
    }

    public void test01ImportMultiLanguageDokuWikiFile() throws Exception {
        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs"));
        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs"));
        assertNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs")));
        assertNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs")));
        File file = new File(this.testsDir, "multilanguage.txt");
        file.getParentFile().mkdirs();
        Util.copyStreamToCache(this.context, this.context.getAssets().open("exports/1.0_afaraf_deutsch.txt"), file);
        this.translator.importDokuWiki(this.library, file);
        assertNotNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs")));
        assertNotNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs")));
        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs"));
        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs"));
    }

    public void test02ImportDokuWikiFile() throws Exception {
        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs"));
        assertNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs")));
        File file = new File(this.testsDir, "singlelanguage.txt");
        file.getParentFile().mkdirs();
        Util.copyStreamToCache(this.context, this.context.getAssets().open("exports/1.0_deutsch.txt"), file);
        this.translator.importDokuWiki(this.library, file);
        assertNotNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs")));
        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs"));
    }

    public void test03ImportLegacyArchive() throws Exception {
//        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs"));
//        assertNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs")));
//        File file = new File(this.testsDir, "2.0.0_uw-obs-de.zip");
//        file.getParentFile().mkdirs();
//        Util.copyStreamToCache(this.context, this.context.getAssets().open("exports/2.0.0_uw-obs-de.zip"), file);
//        this.translator.importArchive(file);
//        assertNotNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs")));
//        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs"));
    }

    public void test04ImportLegacyDokuWikiArchive() throws Exception {
        // this is when we exported a single project into a zip
    }

    public void test05ImportDokuWikiArchive() throws Exception {
        // multiple projects each as a single text file within a zip.
    }

    public void test06ImportV1Archive() throws Exception {
//        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs"));
//        assertNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs")));
//        File file = new File(this.testsDir, "2.0.3_uw-obs-de.tstudio");
//        file.getParentFile().mkdirs();
//        Util.copyStreamToCache(this.context, this.context.getAssets().open("exports/2.0.3_uw-obs-de.tstudio"), file);
//        this.translator.importArchive(file);
//        assertNotNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs")));
//        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs"));
    }

    public void test07ImportV2Archive() throws Exception {
        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs"));
        assertNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs")));
        File file = new File(this.testsDir, "3.0.1_uw-obs-aa.tstudio");
        file.getParentFile().mkdirs();
        Util.copyStreamToCache(this.context, this.context.getAssets().open("exports/3.0.1_uw-obs-aa.tstudio"), file);
        this.translator.importArchive(file);
        assertNotNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs")));
        this.translator.deleteTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs"));
    }
}
