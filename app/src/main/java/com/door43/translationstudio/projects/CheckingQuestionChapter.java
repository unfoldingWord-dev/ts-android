package com.door43.translationstudio.projects;

import android.content.SharedPreferences;

import com.door43.translationstudio.util.AppContext;
import com.door43.util.Security;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by joel on 5/18/2015.
 */
public class CheckingQuestionChapter {
    private static final String PREFERENCES_TAG = "com.door43.translationstudio.checking_questions";
    private final String mId;
    private final Project mProject;
    private final SourceLanguage mSource;
    private final Language mTarget;
    private final Resource mResource;
    List<CheckingQuestion> mQuestions;
    private boolean mIsViewed = false;

    public CheckingQuestionChapter(Project project, SourceLanguage source, Resource resource, Language target, String chapterId) {
        mProject = project;
        mSource = source;
        mResource = resource;
        mTarget = target;
        mId = chapterId;
    }

    public Project getProject() {
        return mProject;
    }

    public Language getTargetLanguage() {
        return mTarget;
    }

    public Resource getResource() {
        return mResource;
    }

    public SourceLanguage getSourceLanguage() {
        return mSource;
    }

    /**
     * Returns the chapter id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns a checking question
     * @param index
     * @return null if the question does not exist
     */
    public CheckingQuestion getQuestion(int index) {
        if(mQuestions != null) {
            if(index >= 0 && index < mQuestions.size()) {
                return mQuestions.get(index);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the number of checking questions in this chapter
     * @return
     */
    public int getCount() {
        if(mQuestions == null) {
            return 0;
        } else {
            return mQuestions.size();
        }
    }

    /**
     * Saves the state of the checking question.
     *
     * @param question
     */
    public void saveQuestionStatus(CheckingQuestion question) {
        SharedPreferences settings = AppContext.context().getSharedPreferences(PREFERENCES_TAG, AppContext.context().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String selector = "question_state_" + mProject.getId() + "-" + mSource.getId() + "-" + mTarget.getId() + "-" + mId + "-" + question.getId();
        if(!question.isViewed()) {
            editor.remove(selector);
        } else {
            String hashes = "";
            for(String frameId:question.references) {
                String[] parts = frameId.split("-");
                if(parts.length != 2) {
                    continue;
                }
                Translation translation = Frame.getTranslation(mProject.getId(), mTarget.getId(), parts[0], parts[1]);
                hashes += Security.md5(translation.getText());
            }
            editor.putString(selector, Security.md5(hashes));
        }
        editor.apply();
    }

    /**
     * Loads the state of the checking question.
     * If the translation for any reference in the question changes it will be marked as not viewed
     * @param question
     */
    public void loadQuestionStatus(CheckingQuestion question) {
        SharedPreferences settings = AppContext.context().getSharedPreferences(PREFERENCES_TAG, AppContext.context().MODE_PRIVATE);
        String selector = "question_state_" + mProject.getId() + "-" + mSource.getId() + "-" + mTarget.getId() + "-" + mId + "-" + question.getId();
        String cachedHash = settings.getString(selector, null);
        if(cachedHash != null) {
            String hashes = "";
            for(String frameId:question.references) {
                String[] parts = frameId.split("-");
                if(parts.length != 2) {
                    continue;
                }
                Translation translation = Frame.getTranslation(mProject.getId(), mTarget.getId(), parts[0], parts[1]);
                hashes += Security.md5(translation.getText());
            }
            if(cachedHash.equals(Security.md5(hashes))) {
                question.setViewed(true);
                return;
            }
        }
        question.setViewed(false);
    }

    /**
     * Adds a checking question to the chapter
     * @param question
     */
    public void addQuestion(CheckingQuestion question) {
        if(mQuestions == null) {
            mQuestions = new ArrayList<>();
        }
        mQuestions.add(question);
    }

    /**
     * Checks if all the questions in this chapter group have been viewed
     */
    public boolean isViewed() {
        if(mQuestions != null) {
            for(CheckingQuestion q:mQuestions) {
                if(!q.isViewed()) {
                    mIsViewed = false;
                    return false;
                }
            }
        }
        mIsViewed = true;
        return true;
    }

    /**
     * Sets the chapter as being viewed
     * @param viewed
     */
    public void setViewed(boolean viewed) {
        mIsViewed = viewed;
    }

    /**
     * Returns the cached value from isViewed();
     * @return
     */
    public boolean isViewedCached() {
        return mIsViewed;
    }
}
