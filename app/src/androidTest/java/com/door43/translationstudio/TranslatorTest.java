package com.door43.translationstudio;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.NativeSpeaker;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Translator;
import com.door43.util.FileUtilities;

import java.io.File;

/**
 * Created by joel on 9/15/2015.
 */
public class TranslatorTest extends InstrumentationTestCase {

    private File mTranslatorDir;
    private Translator mTranslator;
    private Library library;
    private String rootApiUrl;

    protected void setUp() throws Exception {
        mTranslatorDir = new File(App.context().getFilesDir(), "translator_test_translations");
        mTranslator = new Translator(App.context(), null, mTranslatorDir);
        String server = App.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, App.context().getResources().getString(R.string.pref_default_media_server));
        rootApiUrl = server + App.context().getResources().getString(R.string.root_catalog_api);
        library = new Library(App.context(), rootApiUrl, null);
    }

    public void test01Clean() throws Exception {
        library.delete();
        FileUtilities.deleteQuietly(mTranslatorDir);
        App.deployDefaultLibrary();
        library = new Library(App.context(), rootApiUrl, null);
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
        App.clearTargetTranslationSettings(targetTranslation.getId());
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

        App.addOpenSourceTranslation(targetTranslation.getId(), sourceTranslations[0].getId());
        targetTranslation.addSourceTranslation(sourceTranslations[0]);
        App.addOpenSourceTranslation(targetTranslation.getId(), sourceTranslations[1].getId());
        targetTranslation.addSourceTranslation(sourceTranslations[1]);

        String[] sourceTranslationIds = App.getOpenSourceTranslationIds(targetTranslation.getId());
        assertEquals(2, sourceTranslationIds.length);

        // set/get selected translation
        String selectedSourceTranslationId = App.getSelectedSourceTranslationId(targetTranslation.getId());
        assertNotNull(selectedSourceTranslationId);
        assertTrue(selectedSourceTranslationId.equals(sourceTranslationIds[0]) || selectedSourceTranslationId.equals(sourceTranslationIds[1]));
    }

    public void test06RemoveSourceTranslation() throws Exception {
        TargetLanguage[] targetLanguages = library.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[1];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs", TranslationType.TEXT, Resource.REGULAR_SLUG));

        String selectedSourceTranslationId = App.getSelectedSourceTranslationId(targetTranslation.getId());
        String[] sourceTranslationIds = App.getOpenSourceTranslationIds(targetTranslation.getId());

        // the loop below requires two items
        assertEquals(2, sourceTranslationIds.length);

        // delete selected source translation
        String newSelectedSourceTranslationId = null;
        for(String id:sourceTranslationIds) {
            if(id.equals(selectedSourceTranslationId)) {
                App.removeOpenSourceTranslation(targetTranslation.getId(), id);
            } else {
                newSelectedSourceTranslationId = id;
            }
        }
        String[] updatedSourceTranslationIds = App.getOpenSourceTranslationIds(targetTranslation.getId());
        assertEquals(1, updatedSourceTranslationIds.length);
        // should auto select the next source translation
        String actualNewSelectedSourceTranslationId = App.getSelectedSourceTranslationId(targetTranslation.getId());
        assertEquals(newSelectedSourceTranslationId, actualNewSelectedSourceTranslationId);
        // finish emptying
        App.removeOpenSourceTranslation(targetTranslation.getId(), newSelectedSourceTranslationId);
        assertEquals(0, App.getOpenSourceTranslationIds(targetTranslation.getId()).length);
    }

    public void test07SetSelectedSourceTranslation() throws Exception {
        TargetLanguage[] targetLanguages = library.getTargetLanguages();
        TargetLanguage targetLanguage = targetLanguages[1];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs", TranslationType.TEXT, Resource.REGULAR_SLUG));

        String selectedSourceTranslationid = App.getSelectedSourceTranslationId(targetTranslation.getId());
        assertNull(selectedSourceTranslationid);

        // set dummy source translation
        String dummySourceTranslationid = "dummy_id";
        App.setSelectedSourceTranslation(targetTranslation.getId(), dummySourceTranslationid);
        String newSelectedSourceTranslationId = App.getSelectedSourceTranslationId(targetTranslation.getId());
        assertEquals(dummySourceTranslationid, newSelectedSourceTranslationId);

        // remove dummy source translation
        App.setSelectedSourceTranslation(targetTranslation.getId(), null);
        assertNull(App.getSelectedSourceTranslationId(targetTranslation.getId()));
    }
}
