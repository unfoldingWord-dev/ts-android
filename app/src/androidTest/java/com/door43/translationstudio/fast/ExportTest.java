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
public class ExportTest extends InstrumentationTestCase {

    private Context context;
    private Translator translator;
    private File testsDir;
    private Library library;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.context = getInstrumentation().getContext();
        this.testsDir = new File(AppContext.context().getCacheDir(), "export_test");
        this.translator = new Translator(this.context, new File(this.testsDir, "translator"));
        this.library = AppContext.getLibrary();
    }

    public void test01ExportArchive() throws Exception {
        // load content for exporting
        File file = new File(this.testsDir, "3.0.1_uw-obs-aa.tstudio");
        file.getParentFile().mkdirs();
        this.translator.deleteTargetTranslation("uw-obs-aa");
        Util.copyStreamToCache(this.context, this.context.getAssets().open("exports/3.0.1_uw-obs-aa.tstudio"), file);
        this.translator.importArchive(file);
        TargetTranslation targetTranslation = this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs"));
        assertNotNull(targetTranslation);
        assertTrue(targetTranslation.getChapterTranslations().length > 0);

        // export
        File output = new File(testsDir, "archive." + Translator.ARCHIVE_EXTENSION);
        if(output.exists()) output.delete();
        assertTrue(!output.exists());
//        targetTranslation.setDefaultContributor();
        this.translator.exportArchive(targetTranslation, output);
        assertTrue(output.exists());

        // test ability to export
        this.translator.deleteTargetTranslation(targetTranslation.getId());
        assertNull(this.translator.getTargetTranslation(targetTranslation.getId()));
        this.translator.importArchive(output);
        targetTranslation = this.translator.getTargetTranslation(targetTranslation.getId());
        assertNotNull(targetTranslation);
        assertTrue(targetTranslation.getChapterTranslations().length > 0);
    }

    public void test02ExportDokuWiki() throws Exception {
        // load content for exporting
        File file = new File(this.testsDir, "3.0.1_uw-obs-aa.tstudio");
        file.getParentFile().mkdirs();
        Util.copyStreamToCache(this.context, this.context.getAssets().open("exports/3.0.1_uw-obs-aa.tstudio"), file);
        this.translator.importArchive(file);
        TargetTranslation targetTranslation = this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs"));
        assertNotNull(targetTranslation);
        assertTrue(targetTranslation.getChapterTranslations().length > 0);

        // export
        File output = new File(testsDir, "archive.zip");
        if(output.exists()) output.delete();
        assertTrue(!output.exists());
        this.translator.exportDokuWiki(targetTranslation, output);
        assertTrue(output.exists());

        // test ability to import
        this.translator.deleteTargetTranslation(targetTranslation.getId());
        assertNull(this.translator.getTargetTranslation(targetTranslation.getId()));
        this.translator.importDokuWikiArchive(this.library, output);
        targetTranslation  = this.translator.getTargetTranslation(targetTranslation.getId());
        assertNotNull(targetTranslation);
        assertTrue(targetTranslation.getChapterTranslations().length > 0);
    }
}
