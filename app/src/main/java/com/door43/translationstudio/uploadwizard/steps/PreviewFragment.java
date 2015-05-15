package com.door43.translationstudio.uploadwizard.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.door43.translationstudio.R;
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
                onPrevious();
            }
        });

        // TODO: populate the ui
        return v;
    }
}
