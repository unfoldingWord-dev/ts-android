package com.door43.translationstudio.uploadwizard.old;

import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.util.wizard.WizardFragment;
import com.door43.translationstudio.util.AppContext;

public class OverviewFragment extends WizardFragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_upload_intro_old, container, false);

        final Project p = AppContext.projectManager().getSelectedProject();

        Button cancelBtn = (Button)rootView.findViewById(R.id.upload_wizard_cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancel();
            }
        });
        Button continueBtn = (Button)rootView.findViewById(R.id.upload_wizard_continue_btn);
        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(p.translationIsReady()) {
                    // continue to validation
                    onNext();
                } else {
                    // just upload a.k.a. backup
                    ((UploadWizardActivity)getActivity()).startUpload();
                }
            }
        });
        final SwitchCompat completeSwitch = (SwitchCompat)rootView.findViewById(R.id.translationIsCompleteSwitch);
        final TextView translationChecksNotice = (TextView)rootView.findViewById(R.id.translationChecksNoticeTextView);
        translationChecksNotice.setVisibility(View.INVISIBLE);

        if(p.translationIsReady()) {
            completeSwitch.setChecked(true);
            Animation in = new AlphaAnimation(0.0f, 1.0f);
            in.setDuration(100);
            in.setFillAfter(true);
            translationChecksNotice.startAnimation(in);
        }
        completeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                p.setTranslationIsReady(completeSwitch.isChecked());
                if(completeSwitch.isChecked()) {
                    Animation in = new AlphaAnimation(0.0f, 1.0f);
                    in.setDuration(100);
                    in.setFillAfter(true);
                    translationChecksNotice.startAnimation(in);
                } else {
                    Animation out = new AlphaAnimation(1.0f, 0.0f);
                    out.setDuration(100);
                    out.setFillAfter(false);
                    translationChecksNotice.startAnimation(out);
                }
            }
        });

        TextView detailsText = (TextView)rootView.findViewById(R.id.project_details_text);
        detailsText.setText(String.format(getResources().getString(R.string.project_details), p.getTitle(), p.getSelectedSourceLanguage().getName(), p.getSelectedTargetLanguage().getName()));

        return rootView;
    }


}
