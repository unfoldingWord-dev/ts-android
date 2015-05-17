package com.door43.translationstudio.uploadwizard.steps;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.util.wizard.WizardFragment;

/**
 * This fragment shows the user what project will be uploaded and allows them to change the project
 * or continue to the next step.
 */
public class OverviewFragment extends WizardFragment {
    private TextView mTitleTextView;
    private TextView mSourceTextView;
    private TextView mTargetTextView;
    private LinearLayout mLanguageLayout;
    private CheckBox mPublishCheckBox;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_overview, container, false);
        mTitleTextView = (TextView)v.findViewById(R.id.projectTitle);
        mSourceTextView = (TextView)v.findViewById(R.id.sourceLanguageTextView);
        mTargetTextView = (TextView)v.findViewById(R.id.targetLanguageTextView);
        mLanguageLayout = (LinearLayout)v.findViewById(R.id.languageInfoLayout);
        mPublishCheckBox = (CheckBox)v.findViewById(R.id.publishTranslationCheckBox);
        LinearLayout chooseProjectBtn = (LinearLayout)v.findViewById(R.id.chooseProjectButton);
        Button cancelBtn = (Button)v.findViewById(R.id.cancelButton);
        Button nextBtn = (Button)v.findViewById(R.id.nextButton);

        mPublishCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Project p = ((UploadWizardActivity)getActivity()).getTranslationProject();
                Language t = ((UploadWizardActivity)getActivity()).getTranslationTarget();
                p.setTranslationIsReady(t, mPublishCheckBox.isChecked());
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel();
            }
        });
        chooseProjectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNext();
            }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Project p = ((UploadWizardActivity)getActivity()).getTranslationProject();
                Language t = ((UploadWizardActivity)getActivity()).getTranslationTarget();
                if(p == null) {
                    // choose a project
                    onNext();
                } else if (p.translationIsReady(t)) {
                    // project is being published
                    onSkip(1); // see UploadWizardActivity.onCreate() for step order
                } else {
                    // project is being backed up
                    onFinish();
                }
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Project project = ((UploadWizardActivity)getActivity()).getTranslationProject();
        SourceLanguage source = ((UploadWizardActivity)getActivity()).getTranslationSource();
        Language target = ((UploadWizardActivity)getActivity()).getTranslationTarget();

        // populate data
        if(project != null) {
            mTitleTextView.setText(project.getTitle());
            mSourceTextView.setText(source.getName());
            mTargetTextView.setText(target.getName());
            mPublishCheckBox.setVisibility(View.VISIBLE);
            mLanguageLayout.setVisibility(View.VISIBLE);
            mPublishCheckBox.setChecked(project.translationIsReady(target));
        } else {
            mTitleTextView.setText(R.string.choose_a_project);
            mPublishCheckBox.setVisibility(View.GONE);
            mPublishCheckBox.setChecked(false);
            mLanguageLayout.setVisibility(View.GONE);
        }
    }
}
