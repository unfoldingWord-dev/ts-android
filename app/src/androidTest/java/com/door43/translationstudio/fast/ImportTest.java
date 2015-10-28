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
        File file = new File(this.testsDir, "multilanguage.txt");
        file.getParentFile().mkdirs();
        Util.copyStreamToCache(this.context, this.context.getAssets().open("exports/1.0_afaraf_deutsch.txt"), file);
        this.translator.importDokuWiki(this.library, file);
        assertNotNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("de", "obs")));
        assertNotNull(this.translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId("aa", "obs")));
    }

    public void test02ImportDokuWikiFile() throws Exception {

    }

    public void test03ImportLegacyArchive() throws Exception {

    }

    public void test04ImportDokuwikiArchive() throws Exception {

    }

    public void test05ImportV1Archive() throws Exception {

    }

    public void test06ImportV2Archive() throws Exception {

    }
}
