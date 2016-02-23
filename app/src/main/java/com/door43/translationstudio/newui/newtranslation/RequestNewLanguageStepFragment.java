package com.door43.translationstudio.newui.newtranslation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.newui.BaseFragment;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;

import org.json.JSONObject;

/**
 * Created by blm on 2/23/16.
 */
public abstract class RequestNewLanguageStepFragment extends BaseFragment {
    public static final String ARG_SOURCE_TRANSLATION_ID = "arg_source_translation_id";
    public static final String ARG_NEW_LANG_FINISHED = "arg_publish_finished";
    public static final String TAG = RequestNewLanguageStepFragment.class.getSimpleName();
    private OnEventListener mListener;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnEventListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnEventListener");
        }
    }

    protected OnEventListener getListener() {
        return mListener;
    }

    public interface OnEventListener {
        void nextStep(String answersJson);

        void previousStep(String answersJson);

        void finishLanguageRequest();
    }

    protected JSONObject parseAnswers(String answersJson) {
        JSONObject answers;
        try {
            answers = new JSONObject(answersJson);
        } catch (Exception e) {
            Logger.w(TAG, "could not parse answers", e);
            answers = new JSONObject();
        }
        return answers;
    }

    protected JSONObject getAnswersFromArgs(Bundle args) {
        String answersJson = args.getString(RequestNewLanguage.EXTRA_NEW_LANGUAGE_ANSWERS);
        JSONObject answers = parseAnswers(answersJson);
        return answers;
    }
}

