package com.door43.translationstudio.uploadwizard.steps.review;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.CheckingQuestion;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.DataStore;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.translationstudio.user.Profile;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.util.Logger;
import com.door43.util.wizard.WizardFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/14/2015.
 */
public class ReviewFragment extends WizardFragment {
    private Button mNextBtn;
    private CheckingQuestionAdapter mAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_review, container, false);
        ListView list = (ListView)v.findViewById(R.id.reviewListView);
        Button backBtn = (Button)v.findViewById(R.id.backButton);
        mNextBtn = (Button)v.findViewById(R.id.nextButton);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrevious();
            }
        });
        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: check if the user viewed all of the questions
                goToNext();
            }
        });

        mAdapter = new CheckingQuestionAdapter();
        list.setAdapter(mAdapter);
        loadCheckingQuestions();

        return v;
    }

    /**
     * Proceeds to the next step
     */
    private void goToNext() {
        Profile profile = ProfileManager.getProfile();
        if(profile == null) {
            onNext();
        } else {
            // skip the contact form if already collected
            onSkip(1);
        }
    }

    private void loadCheckingQuestions() {
        Project project = ((UploadWizardActivity)getActivity()).getTranslationProject();
        SourceLanguage source = ((UploadWizardActivity)getActivity()).getTranslationSource();

        // TODO: eventually we'll want the checking questions to be indexed as well

        // TODO: we're not sure if each resource will have it's own checking questions or if we'll just have one.
        // So for now we're checking all the resources for the first available checking questions.
        String questionsRaw = null;
        for(Resource r:source.getResources()) {
            questionsRaw = DataStore.pullCheckingQuestions(project.getId(), source.getId(), r.getId(), false, false);
            if(questionsRaw != null) {
                break;
            }
        }

        if(questionsRaw == null) {
            // there are no checking questions
            goToNext();
        } else {
            List<CheckingQuestion> questions = parseCheckingQuestions(questionsRaw);
            if(questions.size() > 0) {
                mAdapter.changeDataset(questions);
            } else {
                goToNext();
            }
        }
    }

    /**
     * Parses and returns a list of checking questions
     * @param rawQuestions
     * @return
     */
    private List<CheckingQuestion> parseCheckingQuestions(String rawQuestions) {
        List<CheckingQuestion> questions = new ArrayList<>();
        JSONArray json;
        try {
            json = new JSONArray(rawQuestions);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "malformed projects catalog", e);
            return new ArrayList<>();
        }

        for(int i = 0; i < json.length(); i ++) {
            try {
                JSONObject jsonChapter = json.getJSONObject(i);
                String chapterId = jsonChapter.getString("id");
                JSONArray jsonQuestionSet = jsonChapter.getJSONArray("cq");
                for(int j = 0; j < jsonQuestionSet.length(); j ++) {
                    JSONObject jsonQuestion = jsonQuestionSet.getJSONObject(j);
                    questions.add(CheckingQuestion.generate(chapterId, jsonQuestion));
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "failed to load the checking question", e);
                continue;
            }
        }

        return questions;
    }
}
