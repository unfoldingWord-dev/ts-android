package com.door43.translationstudio;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Translator;

import org.unfoldingword.door43client.Door43Client;

import java.io.File;

/**
 * Created by joel on 10/27/2015.
 */
@MediumTest
public class ImportExportTest extends InstrumentationTestCase {

    private Context context;
    private Translator translator;
    private File testsDir;
    private Door43Client library;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.context = getInstrumentation().getContext();
        this.testsDir = new File(App.context().getCacheDir(), "import_export_tests");
        this.translator = new Translator(this.context, null, new File(this.testsDir, "translator"));
        this.library = App.getLibrary();
    }

    public void test01ImportV2Archive() throws Exception {
        String targetTranslationId = TargetTranslation.generateTargetTranslationId("aa", "obs", TranslationType.TEXT, "obs");
        this.translator.deleteTargetTranslation(targetTranslationId);
        assertNull(this.translator.getTargetTranslation(targetTranslationId));
        File file = new File(this.testsDir, "3.0.1_uw-obs-aa.tstudio");
        file.getParentFile().mkdirs();
        Util.copyStreamToCache(this.context, this.context.getAssets().open("exports/3.0.1_uw-obs-aa.tstudio"), file);
        String[] importedIDs = this.translator.importArchive(file);
        assertTrue(importedIDs.length == 1);
        assertEquals(importedIDs[0], targetTranslationId);
        TargetTranslation targetTranslation = this.translator.getTargetTranslation(targetTranslationId);
        assertNotNull(targetTranslation);
        assertTrue(targetTranslation.getChapterTranslations().length > 0);
    }

    public void test02ExportArchive() throws Exception {
        String targetTranslationId = TargetTranslation.generateTargetTranslationId("aa", "obs", TranslationType.TEXT, "obs");
        File outputPath = new File(testsDir, "exported_translation." + Translator.ARCHIVE_EXTENSION);
        if(outputPath.exists()) outputPath.delete();
        assertTrue(!outputPath.exists());
        TargetTranslation targetTranslation = this.translator.getTargetTranslation(targetTranslationId);
        this.translator.exportArchive(targetTranslation, outputPath);
        assertTrue(outputPath.exists());

        // test ability to import
        this.translator.deleteTargetTranslation(targetTranslationId);
        assertNull(this.translator.getTargetTranslation(targetTranslationId));
        this.translator.importArchive(outputPath);
        targetTranslation = this.translator.getTargetTranslation(targetTranslationId);
        assertNotNull(targetTranslation);
        assertTrue(targetTranslation.getChapterTranslations().length > 0);
        this.translator.deleteTargetTranslation(targetTranslationId);
    }

    // TODO: 3/25/2016 test importing usfm file
    // TODO: 3/25/2016 test importing usfm archive
}
