package com.door43.translationstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.home.HomeActivity;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends BaseActivity {

    private EditText mName;
    private EditText mEmail;
    private EditText mPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mName = (EditText)findViewById(R.id.name_edittext);
        mEmail = (EditText)findViewById(R.id.email_edittext);
        mPhone = (EditText)findViewById(R.id.phone_edittext);

        findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Support multiple profiles.
                Profile profile = new Profile(
                        mName.getText().toString(),
                        mEmail.getText().toString(),
                        mPhone.getText().toString());

                if (profile.isValid()) {
                    List<Profile> profiles = new ArrayList<>();
                    profiles.add(profile);
                    AppContext.setProfiles(profiles);
                    openMainActivity();
                } else {
                    CustomAlertDialog.Create(ProfileActivity.this)
                            .setMessage(R.string.profile_information_required)
                            .setPositiveButton(R.string.label_ok, null)
                            .show("Profile");
                }
            }
        });
        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (AppContext.getProfiles() != null) {
            openMainActivity();
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
