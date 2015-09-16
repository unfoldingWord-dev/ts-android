package com.door43.translationstudio.fast;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;

import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by joel on 9/15/2015.
 */
public class TranslatorTest extends InstrumentationTestCase {

    private File mTranslatorDir;
    private Translator mTranslator;

    protected void setUp() throws Exception {
        MainApplication app = AppContext.context();
        mTranslatorDir = new File(app.getFilesDir(), "test_translations");
        mTranslator = new Translator(app, mTranslatorDir);
    }

    public void test01Clean() throws Exception {
        FileUtils.deleteQuietly(mTranslatorDir);
    }

    public void test02CreateTargetTranslation() throws Exception {

    }

    public void test03GetTargetTranslationsList() throws Exception {

    }

    public void test04GetTargetTranslation() throws Exception {

    }
}
