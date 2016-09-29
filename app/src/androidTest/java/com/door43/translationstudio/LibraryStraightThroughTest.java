package com.door43.translationstudio;


import android.util.Log;

import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.CheckingQuestion;
import com.door43.translationstudio.core.ChunkMarker;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.LanguageDirection;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryData;
import com.door43.translationstudio.core.NativeSpeaker;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.ProjectCategory;
import com.door43.translationstudio.core.Questionnaire;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationArticle;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationNote;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Translator;
import com.door43.util.FileUtilities;

import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by Andrew on 6/29/2016.
 */
public class LibraryStraightThroughTest {
    private Library mLibrary;
    private String TAG = "LibraryStraightThroughTest";
    private Translator mTranslator;

    @Before
    public void setup() {
        try {
            App.deployDefaultLibrary();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mLibrary = App.getLibrary();
        assertTrue(mLibrary.exists());
        mTranslator = App.getTranslator();
    }

    @After
    public void tearDown() {

    }

    /**
     * it should return chunk markers for a single project
     */
    @Test
    public void getChunkMarkers() {
        ChunkMarker[] markers = mLibrary.getChunkMarkers("gen");
        assertTrue(markers.length > 0);
    }

    /**
     * it should return all target languages
     */
    @Test
    public void getTargetLanguages() {
        TargetLanguage[] languages = mLibrary.getTargetLanguages();
        assertTrue(languages.length > 0);
    }

    /**
     * it should return a temp target language
     */
    @Test
    public void getTempTargetLanguage() {
        TargetLanguage t = new TargetLanguage("qaa-x-cheeseburger", "testString", "usa", LanguageDirection.LeftToRight);

        mLibrary.deleteTempTargetLanguage(t.slug);
        assertNull(mLibrary.getTempTargetLanguage(t.slug));

        mLibrary.addTempTargetLanguage(t);
        assertNotNull(mLibrary.getTempTargetLanguage(t.slug));
    }

    /**
     * it should return categories for a single project
     */
    @Test
    public void getProjectCategories() {
        ProjectCategory[] projectCategories = mLibrary.getProjectCategories("en");
        // for now we just have obs, nt, and ot
        assertTrue(projectCategories.length > 0);
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

    /**
     * it shoulid return a single target language
     */
    @Test
    public void getTargetLanguage() {
        TargetLanguage targetLanguage = mLibrary.getTargetLanguage("en");
        assertEquals("en", targetLanguage.getId());
    }

    /**
     * it should return a single questionnaire
     */
    @Test
    public void getQuestionaire() {
        assertNotNull(mLibrary.getQuestionnaire(0));
    }

    /**
     * it should export the library to a location on the disk
     */
    @Test
    public void exportLibrary() {
        File fileLocation = new File(App.context().getCacheDir(), "sharing/");
        if(fileLocation.listFiles() != null) {
            FileUtilities.deleteQuietly(fileLocation);
        }
        assertNull(fileLocation.listFiles());

        assertNotNull(mLibrary.export(fileLocation));

        FileUtilities.deleteQuietly(fileLocation);

        assertNull(fileLocation.listFiles());   //verify that created file is deleted
    }

    /**
     * it should return a single project
     */
    @Test
    public void getProject() {
        Project p = mLibrary.getProject("obs", "en");
        assertNotNull(p);
        assertEquals("obs", p.getId());
    }

    /**
     * it should return all projects
     */
    @Test
    public void getProjects() {
        Project [] project = mLibrary.getProjects("en");
        assertTrue(project.length > 0);
    }

    /**
     * it should return the preferrred source language for a project
     */
    @Test
    public void getPreferredSourceLanguage() {
        SourceLanguage sourceLanguage = mLibrary.getPreferredSourceLanguage("obs", "en");
        assertNotNull(sourceLanguage);
    }

    /**
     * it should return all the source languages in a project
     */
    @Test
    public void getSourceLanguages() {
        SourceLanguage[] sourceLanguages = mLibrary.getSourceLanguages("obs");
        assertTrue(sourceLanguages.length > 0);

        SourceLanguage sourceLanguage = mLibrary.getSourceLanguage("obs", "en");
        assertNotNull(sourceLanguage);
        assertEquals("en", sourceLanguage.getId());
    }

    /**
     * it should return all the resources in a source language + project
     */
    @Test
    public void getResources() {
        Resource[] resources = mLibrary.getResources("obs", "en");
        assertTrue(resources.length > 0);
    }

    /**
     * it should return a single resource
     */
    @Test
    public void getResource() {
        SourceTranslation anotherSourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs"); //"ulb"
        assertNotNull(mLibrary.getResource(anotherSourceTranslation));
    }

//    /**
//     * it should return positive progress on a translation
//     */
//    @Test
//    public void getTranslationProgress() {
//        TargetLanguage targetLanguage = mLibrary.getTargetLanguage("en");
//        TargetTranslation targetTranslation = mTranslator.createTargetTranslation(new NativeSpeaker("demo speaker"), targetLanguage, "obs", TranslationType.TEXT, Resource.REGULAR_SLUG, TranslationFormat.USFM);
//        ChapterTranslation ct = targetTranslation.getChapterTranslation("01");
//        targetTranslation.applyChapterTitleTranslation(ct, "Some chapter title");
//        targetTranslation.finishChapterTitle(new Chapter("", "", "01"));
//        assertTrue(mLibrary.getTranslationProgress(targetTranslation) > 0);
//    }

    /**
     * it should return all the chapters in a source translation
     */
    @Test
    public void getChapters() {
        SourceTranslation sourceTranslations = mLibrary.getSourceTranslation("obs", "en", "obs");
        Chapter[] c = mLibrary.getChapters(sourceTranslations);
        assertTrue(c.length > 0);
    }

    /**
     * it should return a single chapter
     */
    @Test
    public void getChapter() {
        SourceTranslation sourceTranslations = mLibrary.getSourceTranslation("obs", "en", "obs");
        Chapter c = mLibrary.getChapter(sourceTranslations, "01");
        assertNotNull(c);
    }

    /**
     * it should return all the frames in a chapter
     */
    @Test
    public void getFrames() {
        SourceTranslation sourceTranslations = mLibrary.getSourceTranslation("obs", "en", "obs");
        Chapter[] chapters = mLibrary.getChapters(sourceTranslations);
        Frame[] frame = mLibrary.getFrames(sourceTranslations, chapters[0].getId());
        assertTrue(frame.length > 0);
    }

    /**
     * it should return all the frame slugs in a chapter
     */
    @Test
    public void getFrameSlugs() {
        SourceTranslation sourceTranslations = mLibrary.getSourceTranslation("obs", "en", "obs");
        Chapter[] chapters = mLibrary.getChapters(sourceTranslations);
        String[] frameSlugs = mLibrary.getFrameSlugs(sourceTranslations, chapters[0].getId());
        assertTrue(frameSlugs.length > 0);
    }

    /**
     * it should return a single frame
     */
    @Test
    public void getFrame() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        Frame localFrame = mLibrary.getFrame(sourceTranslation, "01", "01");
        assertNotNull(localFrame);
    }

    /**
     * it should return a found target language
     */
    @Test
    public void findTargetLanguage() {
        TargetLanguage[] targetLanguages = mLibrary.findTargetLanguage("English");
        assertTrue(targetLanguages.length > 0);
    }

    /**
     * it should return a single source translation
     */
    @Test
    public void getSourceTranslation() {
        SourceTranslation[] sourceTranslations = mLibrary.getSourceTranslations("obs");
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(sourceTranslations[0].getId());
        assertNotNull(sourceTranslation);

        assertNotNull(mLibrary.getSourceTranslation("obs", "en", "obs"));

        assertNotNull(mLibrary.getDefaultSourceTranslation("obs", "en"));
    }

    /**
     * it should return a single draft translation
     */
    @Test
    public void getDraftTranslation() throws IOException {
        // fabricate draft
        LibraryData data = new LibraryData(App.context());
        long sourceLanguageDBId = data.getSourceLanguageDBId("en", data.getProjectDBId("obs"));
        long resourceDBId = data.addResource("hi", sourceLanguageDBId, "Hello World", 1, "v1", 0, "", 0, "", 0, "", 0, "", 0, "", 0);

        // test
        assertTrue(mLibrary.getDraftTranslations("obs").length > 0);
        assertTrue(mLibrary.getDraftTranslations("obs", "en").size() > 0);
        assertNotNull(mLibrary.getDraftTranslation("obs", "en", "hi"));
        SourceTranslation st = SourceTranslation.simple("obs", "en", "hi");
        assertNotNull(mLibrary.getDraftTranslation(st.getId()));

        // clean
        data.deleteResource(resourceDBId);
    }

    /**
     * it should return translation notes on a frame
     */
    @Test
    public void getTranslationNote() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        TranslationNote[] translationNotes = mLibrary.getTranslationNotes(sourceTranslation, "01", "01");
        assertTrue(translationNotes.length > 0);

        assertNotNull(mLibrary.getTranslationNote(sourceTranslation, "01", "01", translationNotes[0].getId()));
    }

    /**
     * it should return translation words for a frame
     */
    @Test
    public void getTranslationWords() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        assertTrue(mLibrary.getTranslationWords(sourceTranslation).length > 0);

        assertTrue(mLibrary.getTranslationWords(sourceTranslation, "01", "01").length > 0);

        assertNotNull(mLibrary.getTranslationWord(sourceTranslation, "heaven"));
    }

    /**
     * it should return a single article
     */
    @Test
    public void getTranslationArticle() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        TranslationArticle translationArticle = mLibrary.getTranslationArticle(sourceTranslation, "vol1", "intro", "vol1_intro_ta_intro");
        assertNotNull(translationArticle);
    }

    /**
     * it should return translation questions for a frame
     */
    @Test
    public void getCheckingQuestion() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        CheckingQuestion[] checkingQuestions = mLibrary.getCheckingQuestions(sourceTranslation, "01", "01");
        assertTrue(checkingQuestions.length > 0);

        assertNotNull(mLibrary.getCheckingQuestion(sourceTranslation, "01", "01", checkingQuestions[0].getId()));
    }

    /**
     * it should check if the project has source content
     */
    @Test
    public void getProjectHasSource() {
        assertTrue(mLibrary.projectHasSource("obs"));
    }

    /**
     * it should check if the source language has source content
     */
    @Test
    public void getSourceLanguageHasSource() {
        assertTrue(mLibrary.sourceLanguageHasSource("obs", "en"));
    }

    /**
     * it should check if the source translation has source content
     */
    @Test
    public void getSourceTranslationHasSource() {
        assertTrue(mLibrary.sourceTranslationHasSource(SourceTranslation.simple("obs", "en", "obs")));
    }

    /**
     * it should delete a single project
     */
    @Test
    public void deleteProject() throws IOException {
        // set up test data
        LibraryData data = new LibraryData(App.context());
        String slug = "yoyoyoyo";
        long projectDBID = data.addProject(slug, 0, 0, "", 0, new String[]{});
        long sourceLanguageDBId = data.addSourceLanguage("en", projectDBID, "English", "YO project", "", "ltr", 0, "", 0, new String[]{});
        long resourceDBId = data.addResource("hi", sourceLanguageDBId, "Hello World", 1, "v1", 0, "", 0, "", 0, "", 0, "", 0, "", 0);
        long chapterDBId = data.addChapter("01", resourceDBId, "reference", "title");
        data.addFrame("01", chapterDBId, "this is the body", "usfm", "");

        // test
        assertTrue(mLibrary.projectHasSource(slug));
        mLibrary.deleteProject(slug);
        assertFalse(mLibrary.projectHasSource(slug));

        // clean up test data
        data.deleteProject(slug);
    }

    /**
     * it should return the format of the chapter body
     */
    @Test
    public void getChapterBodyFormat() {
        SourceTranslation sourceTranslation = SourceTranslation.simple("gen", "en", "ulb");
        assertNotEquals(TranslationFormat.DEFAULT, mLibrary.getChapterBodyFormat(sourceTranslation, "01"));
    }
}