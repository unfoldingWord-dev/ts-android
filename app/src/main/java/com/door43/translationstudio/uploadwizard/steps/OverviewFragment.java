package com.door43.translationstudio.uploadwizard.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.util.wizard.WizardFragment;

/**
 * This fragment shows the user what project will be uploaded and allows them to change the project
 * or continue to the next step.
 */
public class OverviewFragment extends WizardFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_overview, container, false);
        TextView titleTextView = (TextView)v.findViewById(R.id.projectTitle);
        TextView sourceTextView = (TextView)v.findViewById(R.id.sourceLanguageTextView);
        TextView targetTextView = (TextView)v.findViewById(R.id.targetLanguageTextView);
        LinearLayout languageLayout = (LinearLayout)v.findViewById(R.id.languageInfoLayout);
        CheckBox publishCheckBox = (CheckBox)v.findViewById(R.id.publishTranslationCheckBox);
        LinearLayout chooseProjectBtn = (LinearLayout)v.findViewById(R.id.chooseProjectButton);

        final Project p = ((UploadWizardActivity)getActivity()).getTranslationProject();

        Button cancelBtn = (Button)v.findViewById(R.id.cancelButton);
        Button nextBtn = (Button)v.findViewById(R.id.nextButton);

        publishCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: mark the project as ready for publication
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
                if(p == null) {
                    // choose a project
                    onNext();
                } else if (p.translationIsReady()) {
                    // project is being published
                    onSkip(1); // see UploadWizardActivity.onCreate() for step order
                } else {
                    // project is being backed up
                    onFinish();
                }
            }
        });

        if(p != null) {
            titleTextView.setText(p.getTitle());
            // TODO: we will want the user to be able to choose the source language and target language without them being the selected source and target.
            sourceTextView.setText(p.getSelectedSourceLanguage().getName());
            targetTextView.setText(p.getSelectedTargetLanguage().getName());
            publishCheckBox.setVisibility(View.VISIBLE);
            languageLayout.setVisibility(View.VISIBLE);
        } else {
            titleTextView.setText(R.string.choose_a_project);
            publishCheckBox.setVisibility(View.GONE);
            languageLayout.setVisibility(View.GONE);
        }

        return v;
    }
}
