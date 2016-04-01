package com.door43.translationstudio;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.NativeSpeaker;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.AppContext;

import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by joel on 9/15/2015.
 */
public class TranslatorTest extends InstrumentationTestCase {

    private File mTranslatorDir;
    private Translator mTranslator;
    private Library library;
    private MainApplication app;
    private String rootApiUrl;

    protected void setUp() throws Exception {
        app = AppContext.context();
        mTranslatorDir = new File(app.getFilesDir(), "translator_test_translations");
        mTranslator = new Translator(app, mTranslatorDir);
        String server = app.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, app.getResources().getString(R.string.pref_default_media_server));
        rootApiUrl = server + app.getResources().getString(R.string.root_catalog_api);
        library = new Library(app, rootApiUrl);
    }

    public void test01Clean() throws Exception {
        library.delete();
        FileUtils.deleteQuietly(mTranslatorDir);
        AppContext.deployDefaultLibrary();
        library = new Library(app, rootApiUrl);
        assertTrue(library.exists());
    }

    public void test02CreateTargetTranslation() throws Exception {
        TargetLanguage[] targetLanguages = library.getTargetLanguages();
        assertTrue(targetLanguages.length > 0);
        int numTargetTranslations  = 5;
        NativeSpeaker speaker = new NativeSpeaker("me");
        for(int i = 0; i < numTargetTranslations; i ++) {
            mTranslator.createTargetTranslation(speaker, targetLanguages[i], "obs", TranslationType.TEXT, Resource.REGULAR_SLUG, TranslationFormat.MARKDOWN);
        }
        assertEquals(numTargetTranslations, mTranslator.getTargetTranslations().length);
    }

    public void test03GetTargetTranslation() throws Exception {
        TargetLanguage[] targetLanguages = library.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[0];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs", TranslationType.TEXT, Resource.REGULAR_SLUG));
        assertEquals("obs", targetTranslation.getProjectId());
        assertEquals(targetLanguage.getId(), targetTranslation.getTargetLanguageId());
        assertEquals(targetLanguage.name, targetTranslation.getTargetLanguageName());

        TargetTranslation sameTargetTranslation = mTranslator.getTargetTranslation(targetTranslation.getId());
        assertEquals(targetTranslation.getId(), sameTargetTranslation.getId());
    }

    public void test04DeleteTargetTranslation() throws Exception {
        TargetLanguage[] targetLanguages = library.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[0];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs", TranslationType.TEXT, Resource.REGULAR_SLUG));
        TargetTranslation[] targetTranslations = mTranslator.getTargetTranslations();

        mTranslator.deleteTargetTranslation(targetTranslation.getId());
        AppContext.clearTargetTranslationSettings(targetTranslation.getId());
        TargetTranslation deletedTargetTranslation = mTranslator.getTargetTranslation(targetTranslation.getId());
        assertNull(deletedTargetTranslation);

        TargetTranslation[] newTargetTranslations = mTranslator.getTargetTranslations();
        assertEquals(targetTranslations.length - 1, newTargetTranslations.length);
    }

    public void test05AddSourceTranslation() throws Exception {
        TargetLanguage[] targetLanguages = library.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[1];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs", TranslationType.TEXT, Resource.REGULAR_SLUG));

        SourceTranslation[] sourceTranslations = library.getSourceTranslations(targetTranslation.getProjectId());

        AppContext.addOpenSourceTranslation(targetTranslation.getId(), sourceTranslations[0].getId());
        targetTranslation.addSourceTranslation(sourceTranslations[0]);
        AppContext.addOpenSourceTranslation(targetTranslation.getId(), sourceTranslations[1].getId());
        targetTranslation.addSourceTranslation(sourceTranslations[1]);

        String[] sourceTranslationIds = AppContext.getOpenSourceTranslationIds(targetTranslation.getId());
        assertEquals(2, sourceTranslationIds.length);

        // set/get selected translation
        String selectedSourceTranslationId = AppContext.getSelectedSourceTranslationId(targetTranslation.getId());
        assertNotNull(selectedSourceTranslationId);
        assertTrue(selectedSourceTranslationId.equals(sourceTranslationIds[0]) || selectedSourceTranslationId.equals(sourceTranslationIds[1]));
    }

    public void test06RemoveSourceTranslation() throws Exception {
        TargetLanguage[] targetLanguages = library.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[1];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs", TranslationType.TEXT, Resource.REGULAR_SLUG));

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
        TargetLanguage[] targetLanguages = library.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[1];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs", TranslationType.TEXT, Resource.REGULAR_SLUG));

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
