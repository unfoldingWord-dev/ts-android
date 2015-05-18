package com.door43.translationstudio.uploadwizard.steps.review;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

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
import com.door43.util.DummyDialogListener;
import com.door43.util.Logger;
import com.door43.util.wizard.WizardActivity;
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
    private ListView mList;
    private TextView mRemainingText;
    private TextView mPercentText;
    private int numComplete = 0;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_review, container, false);
        mList = (ListView)v.findViewById(R.id.reviewListView);
        mRemainingText = (TextView)v.findViewById(R.id.remainingQuestionsTextView);
        mPercentText = (TextView)v.findViewById(R.id.percentCompleteTextView);
        Button backBtn = (Button)v.findViewById(R.id.backButton);
        mNextBtn = (Button)v.findViewById(R.id.nextButton);
        mAdapter = new CheckingQuestionAdapter(getActivity());


        mRemainingText.setText("0/0");
        mPercentText.setText("0%");
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrevious();
            }
        });
        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = true;
                for(int i = 0; i < mAdapter.getCount(); i ++) {
                    if(!mAdapter.getItem(i).isViewed()) {
                        checked = false;
                        break;
                    }
                }
                // TODO: it would be nice to keep track of when a frame or chapter was last changed and also when the user views a question.
                // that way we could avoid having the user check the same question all over again.
                if(checked) {
                    goToNext();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.notice)
                            .setMessage(R.string.review_all_checking_questions)
                            .setPositiveButton(R.string.label_ok, new DummyDialogListener())
                            .show();
                }
            }
        });

        mList.setAdapter(mAdapter);

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mAdapter.getItem(position).setViewed(!mAdapter.getItem(position).isViewed());
                if(mAdapter.getItem(position).isViewed()) {
                    numComplete ++;
                } else {
                    numComplete --;
                }
                mRemainingText.setText(numComplete + "/" + mAdapter.getCount());
                mPercentText.setText(Math.round((double)numComplete/(double)mAdapter.getCount() * 100d) + "%");
                mAdapter.notifyDataSetChanged();
            }
        });

        loadCheckingQuestions();

        return v;
    }

    public void onResume() {
        super.onResume();
        mList.setSelectionAfterHeaderView();
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
            if(((UploadWizardActivity)getActivity()).getStepDirection() == WizardActivity.StepDirection.NEXT) {
                goToNext();
            } else {
                onPrevious();
            }
        } else {
            List<CheckingQuestion> questions = parseCheckingQuestions(questionsRaw);
            if(questions.size() > 0) {
                mAdapter.changeDataset(questions);
            } else {
                if(((UploadWizardActivity)getActivity()).getStepDirection() == WizardActivity.StepDirection.NEXT) {
                    goToNext();
                } else {
                    onPrevious();
                }
            }
        }
        mRemainingText.setText("0/"+mAdapter.getCount());
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
                if(jsonChapter.has("id") && jsonChapter.has("cq")) {
                    String chapterId = jsonChapter.getString("id");
                    JSONArray jsonQuestionSet = jsonChapter.getJSONArray("cq");
                    for (int j = 0; j < jsonQuestionSet.length(); j++) {
                        JSONObject jsonQuestion = jsonQuestionSet.getJSONObject(j);
                        questions.add(CheckingQuestion.generate(chapterId, jsonQuestion));
                    }
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "failed to load the checking question", e);
            }
        }

        return questions;
    }
}
