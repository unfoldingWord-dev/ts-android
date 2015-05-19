package com.door43.translationstudio.uploadwizard.steps.review;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.RenderedTextDialog;
import com.door43.translationstudio.projects.CheckingQuestionChapter;
import com.door43.translationstudio.projects.CheckingQuestion;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.projects.data.DataStore;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.USXRenderer;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/14/2015.
 */
public class ReviewFragment extends WizardFragment {
    private Button mNextBtn;
    private CheckingQuestionAdapter mAdapter;
    private ExpandableListView mList;
    private TextView mRemainingText;
    private TextView mPercentText;
    private int numComplete = 0;
    private int mNumQuestions = 0;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_review, container, false);
        mList = (ExpandableListView)v.findViewById(R.id.reviewListView);
        mRemainingText = (TextView)v.findViewById(R.id.remainingQuestionsTextView);
        mPercentText = (TextView)v.findViewById(R.id.percentCompleteTextView);
        Button backBtn = (Button)v.findViewById(R.id.backButton);
        mNextBtn = (Button)v.findViewById(R.id.nextButton);
        mAdapter = new CheckingQuestionAdapter(getActivity(), new CheckingQuestionAdapter.OnClickListener() {
            @Override
            public void onItemClick(int groupPosition, int childPosition) {
                mAdapter.getChild(groupPosition, childPosition).setViewed(!mAdapter.getChild(groupPosition, childPosition).isViewed());
                if (mAdapter.getChild(groupPosition, childPosition).isViewed()) {
                    numComplete++;
                } else {
                    numComplete--;
                }
                mAdapter.getGroup(groupPosition).saveQuestionStatus(mAdapter.getChild(groupPosition, childPosition));
                mAdapter.getGroup(groupPosition).isViewed(); // generate the view status cache
                mRemainingText.setText(numComplete + "/" + mNumQuestions);
                mPercentText.setText(Math.round((double) numComplete / (double) mNumQuestions * 100d) + "%");
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onReferenceClick(int groupPosition, int childPosition, String reference) {
                String[] parts = reference.split("-");
                if(parts.length == 2) {
                    CheckingQuestionChapter c = mAdapter.getGroup(groupPosition);
                    Frame frame = IndexStore.getFrame(c.getProject().getId(), c.getSourceLanguage().getId(), c.getResource().getId(), parts[0], parts[1]);
                    Translation translation = Frame.getTranslation(c.getProject().getId(), c.getTargetLanguage().getId(), parts[0], parts[1]);
                    if (translation != null && frame != null) {
                        // move other dialogs to backstack
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ft.addToBackStack(null);

                        // create dialog
                        RenderedTextDialog dialog = new RenderedTextDialog();
                        Bundle args = new Bundle();
                        args.putString(RenderedTextDialog.ARG_BODY, translation.getText());
                        args.putString(RenderedTextDialog.ARG_BODY_FORMAT, frame.format.toString());
                        args.putString(RenderedTextDialog.ARG_TITLE, reference);
                        args.putString(RenderedTextDialog.ARG_TITLE_FORMAT, Frame.Format.DEFAULT.toString());
                        dialog.setArguments(args);
                        dialog.show(ft, "dialog");
                    }
                } else {
                    Logger.w(ReviewFragment.class.getName(), "The question reference is invalid: " + reference);
                }
            }
        });

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
                for (int i = 0; i < mAdapter.getGroupCount(); i++) {
                    if (!mAdapter.getGroup(i).isViewed()) {
                        checked = false;
                        break;
                    }
                }
                // TODO: it would be nice to keep track of when a frame or chapter was last changed and also when the user views a question.
                // that way we could avoid having the user check the same question all over again.
                if (checked) {
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

        // TODO: place this in a task
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
        Language target = ((UploadWizardActivity)getActivity()).getTranslationTarget();
        Resource resource = ((UploadWizardActivity)getActivity()).getTranslationResource();

        // TODO: eventually we'll want the checking questions to be indexed as well

        String questionsRaw = DataStore.pullCheckingQuestions(project.getId(), source.getId(), resource.getId(), false, false);

        if(questionsRaw == null) {
            // there are no checking questions
            if(((UploadWizardActivity)getActivity()).getStepDirection() == WizardActivity.StepDirection.NEXT) {
                goToNext();
            } else {
                onPrevious();
            }
        } else {
            List<CheckingQuestionChapter> questions = parseCheckingQuestions(questionsRaw, project, source, resource, target);
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
        mRemainingText.setText("0/"+mNumQuestions);
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
                        chapter.loadQuestionStatus(question);
                        if(!question.isViewed()) {
                            chapter.setViewed(false);
                            numComplete ++;
                        }
                        chapter.addQuestion(question);
                        mNumQuestions ++;
                    }
                    questions.add(chapter);
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "failed to load the checking question", e);
            }
        }

        return questions;
    }
}
