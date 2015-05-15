package com.door43.translationstudio.uploadwizard.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.door43.translationstudio.R;
import com.door43.util.wizard.WizardFragment;

/**
 * Created by joel on 5/14/2015.
 */
public class ReviewFragment extends WizardFragment {
    private Button mNextBtn;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_review, container, false);
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
                // TODO: if the user has already provided contact info we should skip one.
                onNext();
            }
        });

        // TODO: populate the ui

        return v;
    }
}
