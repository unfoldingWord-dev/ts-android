package com.door43.translationstudio.uploadwizard.steps.review;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.RenderedTextDialog;
import com.door43.translationstudio.projects.CheckingQuestionChapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.tasks.LoadCheckingQuestionsTask;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.translationstudio.user.Profile;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.util.DummyDialogListener;
import com.door43.util.Logger;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.util.wizard.WizardActivity;
import com.door43.util.wizard.WizardFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/14/2015.
 */
public class ReviewFragment extends WizardFragment implements GenericTaskWatcher.OnFinishedListener, GenericTaskWatcher.OnCanceledListener {
    private static final String STATE_NUM_QUESTIONS = "num_questions";
    private static final String STATE_NUM_VIEWED = "num_viewed_questions";
    private Button mNextBtn;
    private CheckingQuestionAdapter mAdapter;
    private ExpandableListView mList;
    private TextView mRemainingText;
    private TextView mPercentText;
    private int mNumViewed = 0;
    private int mNumQuestions = 0;
    private static List<CheckingQuestionChapter> mQuestions = new ArrayList<>();
    private GenericTaskWatcher mTaskWatcher;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_review, container, false);
        mList = (ExpandableListView)v.findViewById(R.id.reviewListView);
        mRemainingText = (TextView)v.findViewById(R.id.remainingQuestionsTextView);
        mPercentText = (TextView)v.findViewById(R.id.percentCompleteTextView);
        Button backBtn = (Button)v.findViewById(R.id.backButton);
        mNextBtn = (Button)v.findViewById(R.id.nextButton);
        mRemainingText.setText("0/0");
        mPercentText.setText("0%");

        if(savedInstanceState != null) {
            mNumQuestions = savedInstanceState.getInt(STATE_NUM_QUESTIONS, 0);
            mNumViewed = savedInstanceState.getInt(STATE_NUM_VIEWED, 0);
            updateStats();
        } else {
            // reset everything
            mQuestions = new ArrayList<>();
        }

        mAdapter = new CheckingQuestionAdapter(getActivity(), new CheckingQuestionAdapter.OnClickListener() {
            @Override
            public void onItemClick(int groupPosition, int childPosition) {
                mAdapter.getChild(groupPosition, childPosition).setViewed(!mAdapter.getChild(groupPosition, childPosition).isViewed());
                if (mAdapter.getChild(groupPosition, childPosition).isViewed()) {
                    mNumViewed++;
                } else {
                    mNumViewed--;
                }
                mAdapter.getGroup(groupPosition).saveQuestionStatus(mAdapter.getChild(groupPosition, childPosition));
                mAdapter.getGroup(groupPosition).isViewed(); // generate the view status cache
                updateStats();
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

        mTaskWatcher = new GenericTaskWatcher(getActivity(), R.string.loading);
        mTaskWatcher.setOnFinishedListener(this);
        mTaskWatcher.setOnCanceledListener(this);

        LoadCheckingQuestionsTask task = (LoadCheckingQuestionsTask) TaskManager.getTask(LoadCheckingQuestionsTask.TASK_ID);
        if(savedInstanceState == null) {
            if(task == null) {
                // load questions
                Project project = ((UploadWizardActivity)getActivity()).getTranslationProject();
                SourceLanguage source = ((UploadWizardActivity)getActivity()).getTranslationSource();
                Language target = ((UploadWizardActivity)getActivity()).getTranslationTarget();
                Resource resource = ((UploadWizardActivity)getActivity()).getTranslationResource();
                task = new LoadCheckingQuestionsTask(project, source, resource, target);
                mTaskWatcher.watch(task);
                TaskManager.addTask(task, LoadCheckingQuestionsTask.TASK_ID);
            }
        } else if(mQuestions.size() > 0) {
            mAdapter.changeDataset(mQuestions);
        } else {
            mTaskWatcher.watch(task);
        }

        return v;
    }

    /**
     * Updates the display for the number of completed questions and the total % till complete
     */
    public void updateStats() {
        mRemainingText.setText(mNumViewed + "/" + mNumQuestions);
        mPercentText.setText(Math.round((double) mNumViewed / (double) mNumQuestions * 100d) + "%");
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

    @Override
    public void onFinished(ManagedTask task) {
        mTaskWatcher.stop();
        LoadCheckingQuestionsTask t = (LoadCheckingQuestionsTask)task;
        if(t.getNumQuestions() > 0) {
            mQuestions = t.getQuestions();
            mAdapter.changeDataset(t.getQuestions());
            mNumQuestions = t.getNumQuestions();
            mNumViewed = t.getNumCompleted();
            updateStats();
        } else {
            // there are no checking questions
            if(((UploadWizardActivity)getActivity()).getStepDirection() == WizardActivity.StepDirection.NEXT) {
                goToNext();
            } else {
                onPrevious();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_NUM_QUESTIONS, mNumQuestions);
        outState.putInt(STATE_NUM_VIEWED, mNumViewed);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        mTaskWatcher.stop();
        super.onDestroy();
    }

    @Override
    public void onCanceled(ManagedTask task) {
        mTaskWatcher.stop();
        onPrevious();
    }
}
