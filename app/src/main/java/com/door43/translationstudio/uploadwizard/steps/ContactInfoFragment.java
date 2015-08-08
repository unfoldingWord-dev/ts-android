package com.door43.translationstudio.uploadwizard.steps;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.door43.translationstudio.R;
import com.door43.translationstudio.user.Profile;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.util.wizard.WizardFragment;

/**
 * Created by joel on 5/14/2015.
 */
public class ContactInfoFragment extends WizardFragment {
    private Button mNextBtn;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_contact_info, container, false);
        final EditText nameText = (EditText)v.findViewById(R.id.nameEditText);
        final EditText emailText = (EditText)v.findViewById(R.id.emailEditText);
        final EditText phoneText = (EditText)v.findViewById(R.id.phoneEditText);

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
                if(nameText.getText().toString().isEmpty() || emailText.getText().toString().isEmpty()) {
                    // name and email required
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.notice)
                            .setMessage(R.string.complete_required_fields)
                            .show();
                } else {
                    ProfileManager.setProfile( new Profile(nameText.getText().toString(), emailText.getText().toString(), phoneText.getText().toString()));
                    onNext();
                }
            }
        });

        Profile profile = ProfileManager.getProfile();
        if(profile != null) {
            nameText.setText(profile.getName());
            emailText.setText(profile.getEmail());
            phoneText.setText(profile.getPhone());
        } else {
            nameText.setText("");
            emailText.setText("");
            phoneText.setText("");
        }
        return v;
    }
}
