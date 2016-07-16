package com.door43.translationstudio;


import android.util.Log;

import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.CheckingQuestion;
import com.door43.translationstudio.core.ChunkMarker;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.LanguageDirection;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.ProjectCategory;
import com.door43.translationstudio.core.Questionnaire;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationArticle;
import com.door43.translationstudio.core.TranslationNote;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.TranslationWord;
import com.door43.translationstudio.core.Translator;

import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.asn1.x509.Target;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by Andrew on 6/29/2016.
 */
public class LibraryStraightThroughTest implements ManagedTask.OnFinishedListener {
    private Library mLibrary;
    private String TAG = "LibraryStraightThroughTest";
    private Translator mTranslator;

    @Before
    public void setup() {
        mLibrary = App.getLibrary();
        assertTrue(mLibrary.exists());

//        try {
//            App.deployDefaultLibrary();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        mLibrary = App.getLibrary();

//        // NOTE: this will fail when first updating the db version
//        assertTrue(mLibrary.exists());
    }

    @After
    public void tearDown() {

    }

    @Test
    public void getChunkMarkers() {
        ChunkMarker[] markers = mLibrary.getChunkMarkers("gen");
        assertTrue(markers.length > 0);
    }

    @Test
    public void getTargetLanguages() {
        TargetLanguage[] languages = mLibrary.getTargetLanguages();
        assertTrue(languages.length > 0);
    }

    @Test
    public void getTempTargetLanguage() {
        TargetLanguage t = new TargetLanguage("qaa-x-cheeseburger", "testString", "usa", LanguageDirection.LeftToRight);

        mLibrary.deleteTempTargetLanguage(t.code);
        assertNull(mLibrary.getTempTargetLanguage(t.code));

        mLibrary.addTempTargetLanguage(t);
        assertNotNull(mLibrary.getTempTargetLanguage(t.code));
    }

    @Test
    public void getProjectCategories() {
        ProjectCategory[] projectCategories = mLibrary.getProjectCategories("en");
        // for now we just have obs, nt, and ot
        assertEquals(3, projectCategories.length);
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

    @Test
    public void getTargetLanguage() {
        TargetLanguage targetLanguage = mLibrary.getTargetLanguage("en");
        assertEquals("en", targetLanguage.getId());
    }

    @Test
    public void getQuestionaires() {
        Questionnaire [] questionnaires = mLibrary.getQuestionnaires();
        assertTrue(questionnaires.length > 0);
        for(Questionnaire q:questionnaires) {
            Log.e(TAG, "questionaire: "+q.dbId);
        }
    }

    @Test
    public void getQuestionaire() {
        assertNotNull(mLibrary.getQuestionnaire(0));
    }

    @Test
    public void setExport() {
        File fileLocation = new File(App.context().getCacheDir(), "sharing/");
        if(fileLocation.listFiles() != null){
            deleteDirectoryFiles(fileLocation);
        }
        assertNull(fileLocation.listFiles());

        assertNotNull(mLibrary.export(fileLocation));

        deleteDirectoryFiles(fileLocation);  //cleanup

        assertNull(fileLocation.listFiles());   //verify that created file is deleted
    }

    public boolean deleteDirectoryFiles(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDirectoryFiles(children[i]);
                if (!success) {
                    return false; }
            }
        } // either file or an empty directory
        Log.e(TAG, "removing file or directory : " + dir.getName());
        return dir.delete();
    }

    @Test
    public void getLanguages() {
        TargetLanguage [] targetLanguages = mLibrary.getTargetLanguages();
        assertNotNull(targetLanguages);

        Log.e(TAG,"getTargetLanguages: "+targetLanguages.length);

        for(TargetLanguage tl:targetLanguages){
            Log.e(TAG,"Target Language id: " + tl.getId() + ", region: " + tl.region + ", name: " + tl.name + ", code: " + tl.code);
        }
    }

    @Test
    public void getProjects() {
        Project [] project = mLibrary.getProjects("");
        assertNotNull(project);
    }

    @Test
    public void getProjectCategory() {
        ProjectCategory [] pc = mLibrary.getProjectCategories("en");    //there is no default language, need to pass two letter language to return something "en", "fr", etc
        assertNotNull(pc);
        for(ProjectCategory projCat:pc) {
            Log.e(TAG, "projectCategory: " + projCat.getId());
        }
    }

    @Test
    public void getPreferredSourceLanguage() {
        SourceLanguage sourceLanguage = mLibrary.getPreferredSourceLanguage("obs", "en");
        assertNotNull(sourceLanguage);
    }

    @Test
    public void getSourceLanguages() {
        SourceLanguage[] sourceLanguages = mLibrary.getSourceLanguages("obs");
        assertTrue(sourceLanguages.length > 0);

        SourceLanguage sourceLanguage = mLibrary.getSourceLanguage("obs", "en");
        assertNotNull(sourceLanguage);
        assertEquals("en", sourceLanguage.getId());
    }

    @Test
    public void getResources() {
        Resource[] resources = mLibrary.getResources("obs", "en");
        assertTrue(resources.length > 0);
    }

    @Test
    public void getResource() {
        SourceTranslation anotherSourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs"); //"ulb"
        assertNotNull(mLibrary.getResource(anotherSourceTranslation));
    }

    @Test
    public void getProject() {
        Project p = mLibrary.getProject("obs", "en");
        assertNotNull(p);
        assertEquals("obs", p.getId());
    }

    @Test
    public void getTranslationProgress() {
//        TargetTranslation[] targetTranslations = App.getTranslator().getTargetTranslations();
//        for(TargetTranslation t:targetTranslations) {
//            Log.e(TAG, "progress: " + (mLibrary.getTranslationProgress(t) * 100 + "%"));
//            assertNotNull(mLibrary.getTranslationProgress(t));
//        }

        TargetLanguage[] targetLanguages = mLibrary.getTargetLanguages();
        for(TargetLanguage t:targetLanguages) {
            Log.e(TAG, "targetLanguages: " + t.getId());
        }
        TargetLanguage targetLanguage = targetLanguages[0];
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(targetLanguage.getId(), "obs", TranslationType.TEXT, Resource.REGULAR_SLUG));
        Log.e(TAG, "progress: " + (mLibrary.getTranslationProgress(targetTranslation) * 100 + "%"));
        Log.e(TAG, "targetTranslation: " + targetTranslation.getProjectId());
    }

    @Test
    public void getChapters() {
        SourceTranslation sourceTranslations = mLibrary.getSourceTranslation("obs", "en", "obs");
        Chapter[] c = mLibrary.getChapters(sourceTranslations);
        for (Chapter chap : c) {
            Log.e(TAG, "chapters: " +chap.title);
        }
    }

    @Test
    public void getChapter() {
        SourceTranslation sourceTranslations = mLibrary.getSourceTranslation("obs", "en", "obs");
        Chapter c = mLibrary.getChapter(sourceTranslations, "25");  //check 1 - 50 chapterIds
        assertNotNull(c);
        Log.e(TAG, "chapter: " + c.getId() + ", " + c.reference);
    }

    @Test
    public void getFrames() {
        SourceTranslation sourceTranslations = mLibrary.getSourceTranslation("obs", "en", "obs");
        Chapter[] chapters = mLibrary.getChapters(sourceTranslations);
        Frame[] frame = mLibrary.getFrames(sourceTranslations, chapters[49].getId());
        assertNotNull(frame);
        for(Frame f:frame){
            Log.e(TAG, "frame: " + f.getId() + ", " +f.getChapterId() + ", " +f.getStartVerse() + ", " + f.getEndVerse() + ", " + f.body + ", " + f.getTitle());
        }
    }

    @Test
    public void getFrameSlugs() {
        SourceTranslation sourceTranslations = mLibrary.getSourceTranslation("obs", "en", "obs");
        Chapter[] chapters = mLibrary.getChapters(sourceTranslations);
        Log.e(TAG, "chapterSlug: " + chapters[48].getId());
        String[] frameSlugs = mLibrary.getFrameSlugs(sourceTranslations, chapters[48].getId());
        assertNotNull(frameSlugs);
        Log.e(TAG, "frameSlugs: " + frameSlugs.toString());
        for (String s:frameSlugs){
            Log.e(TAG, "frameSlugs: " + s.toString());
        }
    }

    @Test
    public void getFrame() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        Chapter[] chapters = mLibrary.getChapters(sourceTranslation);
        Frame[] frame = mLibrary.getFrames(sourceTranslation, chapters[48].getId());
        Frame localFrame = mLibrary.getFrame(sourceTranslation, chapters[48].getId(), frame[0].getId());
        assertNotNull(localFrame);
        Log.e(TAG, "frame: " + localFrame.getTitle() +", " + localFrame.getChapterId());
    }

//    @Test
//    public void findTargetLanguage() {
//        TargetLanguage[] targetLanguages = mLibrary.findTargetLanguage("en");
//        for(TargetLanguage t:targetLanguages) {
//            Log.e(TAG, "targetLanguages: " + t.getId());
//        }
//    }

    @Test
    public void getSourceTranslation() {
        SourceTranslation[] sourceTranslations = mLibrary.getSourceTranslations("obs");
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(sourceTranslations[0].getId());
        assertNotNull(sourceTranslation);
        assertEquals(sourceTranslations[0].getId(), sourceTranslation.getId());

        assertNotNull(mLibrary.getSourceTranslation("obs", "en", "obs"));

        assertNotNull(mLibrary.getDefaultSourceTranslation("obs", "en"));
    }

    @Test
    public void getDraftTranslation() {
        assertNotNull(mLibrary.getDraftTranslations("obs"));

        assertNotNull(mLibrary.getDraftTranslations("obs", "en"));

        // min checking level issue causing this to fail
        assertNotNull(mLibrary.getDraftTranslation("obs", "en", "obs"));  //am, bn, ceb, en, es, etc...

        assertNotNull(mLibrary.getDraftTranslation("obs"));
    }

    @Test
    public void getTranslationNote() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        Log.e(TAG, "sourceTranslation: " + sourceTranslation.getId());
        TranslationNote[] translationNotes = mLibrary.getTranslationNotes(sourceTranslation, "01", "01");
        Log.e(TAG, "TranslationNote[] length: " + translationNotes.length );
//        + ", getId at 0: " + translationNotes[0].getId());
        assertNotNull(translationNotes);
        for (TranslationNote s:translationNotes) {
            Log.e(TAG, "TranslationNote: " + s.getId());
        }

//        mLibrary.getTranslationNote(sourceTranslation, chapterId, frameId, noteId);
    }

    @Test
    public void getTranslationWords() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        TranslationWord[] tWords = mLibrary.getTranslationWords(sourceTranslation);
        for(TranslationWord t:tWords) {
            Log.e(TAG, "translationWords, id:" + t.getId());
        }
        assertNotNull(tWords);

        TranslationWord[] translationWords = mLibrary.getTranslationWords(sourceTranslation, "01", "04");
        for(TranslationWord t:translationWords) {
            Log.e(TAG, "translationWords2: " + t.getTerm());
        }
        assertNotNull(translationWords);

        TranslationWord tWord = mLibrary.getTranslationWord(sourceTranslation, "heaven");
        Log.e(TAG, "translation word heaven: " + tWord.getDefinition());
        assertNotNull(tWord);
    }

    @Test
    public void getTranslationArticle() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        TranslationArticle translationArticle = mLibrary.getTranslationArticle(sourceTranslation, "01", "01", "01");
        assertNotNull(translationArticle);
    }

    @Test
    public void getCheckingQuestion() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        CheckingQuestion[] checkingQuestions = mLibrary.getCheckingQuestions(sourceTranslation, "12", "12");
        for(CheckingQuestion check:checkingQuestions) {
            Log.e(TAG, "checking question: " + check.getId() + ", " + check.getQuestion()+ ", Answer: " + check.getAnswer());
        }
        assertNotNull(checkingQuestions);

        assertNotNull(mLibrary.getCheckingQuestion(sourceTranslation, "12", "12", "26a12a8643bd53a7c69ab2c4fc7657a1"));
    }

    @Test
    public void getProjectHasSource() {
        assertTrue(mLibrary.projectHasSource("obs"));
    }

    @Test
    public void getSourceLanguageHasSource() {
        assertTrue(mLibrary.sourceLanguageHasSource("obs", "en"));
        assertTrue(mLibrary.sourceLanguageHasSource("obs", "fr"));
    }

    @Test
    public void getSourceTranslationHasSource() {
        assertTrue(mLibrary.sourceTranslationHasSource(mLibrary.getSourceTranslation("obs", "en", "obs")));
    }

    //deleteProject?

    //getTargetLanguagesLength isn't used

    @Test
    public void getChapterBodyFormat() {
        SourceTranslation sourceTranslation = mLibrary.getSourceTranslation("obs", "en", "obs");
        Log.e(TAG, "chapter body format: " + mLibrary.getChapterBodyFormat(sourceTranslation, "01")); //insert anything into second parameter, it always returns 'default' ??
        assertNotEquals("default", mLibrary.getChapterBodyFormat(sourceTranslation, "01"));

        Log.e(TAG, "chapter body: " + mLibrary.getChapterBody(sourceTranslation, "01"));
        assertNotEquals("", mLibrary.getChapterBody(sourceTranslation, "01"));
    }

    @Override
    public void onTaskFinished(ManagedTask task) {
        Log.e(TAG, "finished!");
    }
}