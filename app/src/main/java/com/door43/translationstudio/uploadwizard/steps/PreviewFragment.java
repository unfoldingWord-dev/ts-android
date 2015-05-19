package com.door43.translationstudio.uploadwizard.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.translationstudio.user.Profile;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.util.wizard.WizardFragment;

/**
 * Created by joel on 5/14/2015.
 */
public class PreviewFragment extends WizardFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_preview, container, false);

        Button backBtn = (Button)v.findViewById(R.id.backButton);
        Button uploadBtn = (Button)v.findViewById(R.id.uploadButton);
        LinearLayout contactInfoBtn = (LinearLayout)v.findViewById(R.id.contactInfoButton);
        TextView nameText = (TextView)v.findViewById(R.id.nameTextView);
        TextView emailText = (TextView)v.findViewById(R.id.emailTextView);
        TextView phoneText = (TextView)v.findViewById(R.id.phoneTextView);
        TextView projectTitleText = (TextView)v.findViewById(R.id.projectTitleTextView);
        TextView targetTextView = (TextView)v.findViewById(R.id.targetLanguageTextView);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSkip(-1);
            }
        });
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNext();
            }
        });
        contactInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // move to contact form
                onPrevious();
            }
        });

        Project p = ((UploadWizardActivity)getActivity()).getTranslationProject();
        Language l = ((UploadWizardActivity) getActivity()).getTranslationTarget();
        projectTitleText.setText(p.getTitle());
        targetTextView.setText(l.getName());
        // TODO: set up graphite and font size

        Profile profile = ProfileManager.getProfile();
        if(profile != null) {
            nameText.setText(profile.getName());
            emailText.setText(profile.getEmail());
            phoneText.setText(profile.getPhone());
        } else {
            // the profile is missing (should never happen)
            onPrevious();
        }

        return v;
    }
}
