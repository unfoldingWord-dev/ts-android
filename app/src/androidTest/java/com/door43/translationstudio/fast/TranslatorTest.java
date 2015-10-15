package com.door43.translationstudio.fast;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.core.Util;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by joel on 9/15/2015.
 */
public class TranslatorTest extends InstrumentationTestCase {

    private File mTranslatorDir;
    private Translator mTranslator;
    private File mLibraryDir;
    private Library mLibrary;
    private MainApplication mApp;

    protected void setUp() throws Exception {
        mApp = AppContext.context();
        mTranslatorDir = new File(mApp.getFilesDir(), "test_translations");
        mTranslator = new Translator(mApp, mTranslatorDir);

        mLibraryDir = new File(mApp.getFilesDir(), "test_translator_library");
        String server = mApp.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, mApp.getResources().getString(R.string.pref_default_media_server));
        String rootApi = server + mApp.getResources().getString(R.string.root_catalog_api);
        mLibrary = new Library(mApp, mLibraryDir, mLibraryDir, rootApi);
    }

    public void test01Clean() throws Exception {
        FileUtils.deleteQuietly(mTranslatorDir);
        if(!mLibrary.exists()) {
            AppContext.deployDefaultLibrary(mLibrary);
        }
    }

    public void test02CreateTargetTranslation() throws Exception {
        TargetLanguage[] targetLanguages = mLibrary.getTargetLanguages();
        int numTargetTranslations  = 5;
        for(int i = 0; i < numTargetTranslations; i ++) {
            mTranslator.createTargetTranslation(targetLanguages[i], "obs");
        }
        assertEquals(numTargetTranslations, mTranslator.getTargetTranslations().length);
    }

    public void test03GetTargetTranslation() throws Exception {
        TargetLanguage[] targetLanguages = mLibrary.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[0];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs"));
        assertEquals("obs", targetTranslation.getProjectId());
        assertEquals(targetLanguage.getId(), targetTranslation.getTargetLanguageId());
        assertEquals(targetLanguage.name, targetTranslation.getTargetLanguageName());

        TargetTranslation sameTargetTranslation = mTranslator.getTargetTranslation(targetTranslation.getId());
        assertEquals(targetTranslation.getId(), sameTargetTranslation.getId());
    }

    public void test04DeleteTargetTranslation() throws Exception {
        TargetLanguage[] targetLanguages = mLibrary.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[0];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs"));
        TargetTranslation[] targetTranslations = mTranslator.getTargetTranslations();

        mTranslator.deleteTargetTranslation(targetTranslation.getId());
        AppContext.clearTargetTranslationSettings(targetTranslation.getId());
        TargetTranslation deletedTargetTranslation = mTranslator.getTargetTranslation(targetTranslation.getId());
        assertNull(deletedTargetTranslation);

        TargetTranslation[] newTargetTranslations = mTranslator.getTargetTranslations();
        assertEquals(targetTranslations.length - 1, newTargetTranslations.length);
    }

    public void test05AddSourceTranslation() throws Exception {
        TargetLanguage[] targetLanguages = mLibrary.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[1];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs"));

        SourceTranslation[] sourceTranslations = mLibrary.getSourceTranslations(targetTranslation.getProjectId());

        AppContext.addOpenSourceTranslation(targetTranslation.getId(), sourceTranslations[0].getId());
        targetTranslation.useSourceTranslation(sourceTranslations[0]);
        AppContext.addOpenSourceTranslation(targetTranslation.getId(), sourceTranslations[1].getId());
        targetTranslation.useSourceTranslation(sourceTranslations[1]);

        String[] sourceTranslationIds = AppContext.getOpenSourceTranslationIds(targetTranslation.getId());
        assertEquals(2, sourceTranslationIds.length);

        // set/get selected translation
        String selectedSourceTranslationId = AppContext.getSelectedSourceTranslationId(targetTranslation.getId());
        assertNotNull(selectedSourceTranslationId);
        assertTrue(selectedSourceTranslationId.equals(sourceTranslationIds[0]) || selectedSourceTranslationId.equals(sourceTranslationIds[1]));
    }

    public void test06RemoveSourceTranslation() throws Exception {
        TargetLanguage[] targetLanguages = mLibrary.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[1];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs"));

        String selectedSourceTranslationId = AppContext.getSelectedSourceTranslationId(targetTranslation.getId());
        String[] sourceTranslationIds = AppContext.getOpenSourceTranslationIds(targetTranslation.getId());

        // the loop below requires two items
        assertEquals(2, sourceTranslationIds.length);

        // delete selected source translation
        String newSelectedSourceTranslationId = null;
        for(String id:sourceTranslationIds) {
            if(id.equals(selectedSourceTranslationId)) {
                AppContext.removeOpenSourceTranslation(targetTranslation.getId(), id);
            } else {
                newSelectedSourceTranslationId = id;
            }
        }
        String[] updatedSourceTranslationIds = AppContext.getOpenSourceTranslationIds(targetTranslation.getId());
        assertEquals(1, updatedSourceTranslationIds.length);
        // should auto select the next source translation
        String actualNewSelectedSourceTranslationId = AppContext.getSelectedSourceTranslationId(targetTranslation.getId());
        assertEquals(newSelectedSourceTranslationId, actualNewSelectedSourceTranslationId);
        // finish emptying
        AppContext.removeOpenSourceTranslation(targetTranslation.getId(), newSelectedSourceTranslationId);
        assertEquals(0, AppContext.getOpenSourceTranslationIds(targetTranslation.getId()).length);
    }

    public void test07SetSelectedSourceTranslation() throws Exception {
        TargetLanguage[] targetLanguages = mLibrary.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[1];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs"));

        String selectedSourceTranslationid = AppContext.getSelectedSourceTranslationId(targetTranslation.getId());
        assertNull(selectedSourceTranslationid);

        // set dummy source translation
        String dummySourceTranslationid = "dummy_id";
        AppContext.setSelectedSourceTranslation(targetTranslation.getId(), dummySourceTranslationid);
        String newSelectedSourceTranslationId = AppContext.getSelectedSourceTranslationId(targetTranslation.getId());
        assertEquals(dummySourceTranslationid, newSelectedSourceTranslationId);

        // remove dummy source translation
        AppContext.setSelectedSourceTranslation(targetTranslation.getId(), null);
        assertNull(AppContext.getSelectedSourceTranslationId(targetTranslation.getId()));
    }
}
