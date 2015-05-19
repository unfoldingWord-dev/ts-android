package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.CheckingQuestion;
import com.door43.translationstudio.projects.CheckingQuestionChapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.projects.data.DataStore;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;
import com.door43.util.tasks.ManagedTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/18/2015.
 */
public class LoadCheckingQuestionsTask extends ManagedTask {

    public static final String TASK_ID = "load_checking_questions";
    private final Project mProject;
    private final SourceLanguage mSource;
    private final Resource mResource;
    private final Language mTarget;
    private int mNumCompletedQuestions = 0;
    private int mNumQuestions = 0;
    private List<CheckingQuestionChapter> mQuestions = new ArrayList<>();

    public LoadCheckingQuestionsTask(Project project, SourceLanguage source, Resource resource, Language target) {
        mProject = project;
        mSource = source;
        mResource = resource;
        mTarget = target;
    }

    @Override
    public void start() {
        publishProgress(-1, "");
        // TODO: eventually we'll want the checking questions to be indexed as well
        String questionsRaw = DataStore.pullCheckingQuestions(mProject.getId(), mSource.getId(), mResource.getId(), false, false);
        if(questionsRaw != null) {
            mQuestions = parseCheckingQuestions(questionsRaw, mProject, mSource, mResource, mTarget);
        }
    }

    /**
     * Returns the list of questions grouped by chapter
     * @return
     */
    public List<CheckingQuestionChapter> getQuestions() {
        return mQuestions;
    }

    /**
     * Returns the number of questions that have already been viewed
     * @return
     */
    public int getNumCompleted() {
        return mNumCompletedQuestions;
    }

    /**
     * Returns the total number of questions
     * @return
     */
    public int getNumQuestions() {
        return mNumQuestions;
    }

    /**
     * Parses and returns a list of checking questions
     * @param rawQuestions
     * @return
     */
    private List<CheckingQuestionChapter> parseCheckingQuestions(String rawQuestions, Project project, SourceLanguage source, Resource resource, Language target) {
        List<CheckingQuestionChapter> questions = new ArrayList<>();
        JSONArray json;
        try {
            json = new JSONArray(rawQuestions);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "malformed projects catalog", e);
            return new ArrayList<>();
        }

        for(int i = 0; i < json.length(); i ++) {
            publishProgress((double)i/(double)json.length(), "");
            try {
                JSONObject jsonChapter = json.getJSONObject(i);
                if(jsonChapter.has("id") && jsonChapter.has("cq")) {
                    String chapterId = jsonChapter.getString("id");
                    JSONArray jsonQuestionSet = jsonChapter.getJSONArray("cq");
                    CheckingQuestionChapter chapter = new CheckingQuestionChapter(project, source, resource, target, chapterId);
                    chapter.setViewed(true);
                    for (int j = 0; j < jsonQuestionSet.length(); j++) {
                        JSONObject jsonQuestion = jsonQuestionSet.getJSONObject(j);
                        CheckingQuestion question = CheckingQuestion.generate(chapterId, jsonQuestion);
                        // skip questions who's references have not been translated
                        boolean questionIsNeeded = false;
                        for(String reference:question.references) {
                            String parts[] = reference.split("-");
                            Translation translation = Frame.getTranslation(chapter.getProject().getId(), chapter.getTargetLanguage().getId(), parts[0], parts[1]);
                            if(translation != null) {
                                questionIsNeeded = true;
                                break;
                            }
                        }
                        // only include needed questions
                        if(questionIsNeeded) {
                            chapter.loadQuestionStatus(question);
                            if (!question.isViewed()) {
                                chapter.setViewed(false);
                            } else {
                                mNumCompletedQuestions++;
                            }
                            chapter.addQuestion(question);
                            mNumQuestions++;
                        }
                    }
                    if(chapter.getCount() > 0) {
                        questions.add(chapter);
                    }
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "failed to load the checking question", e);
            }
        }

        return questions;
    }
}
