package com.door43.translationstudio;

import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.door43.translationstudio.core.Profile;

public class RegisterOfflineActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_offline);

        final EditText fullNameText = (EditText) findViewById(R.id.full_name);
        Button cancelButton = (Button)findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        Button continueButton = (Button)findViewById(R.id.ok_button);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String fullName = fullNameText.getText().toString().trim();
                if(!fullName.equals("")) {
                    ProfileActivity.showPrivacyNotice(RegisterOfflineActivity.this, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppContext.setProfile(new Profile(fullName));
                            finish();
                        }
                    });
                } else {
                    // missing fields
                    AppContext.context().showToastMessage(R.string.complete_required_fields);
                }
            }
        });
        findViewById(R.id.privacy_notice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileActivity.showPrivacyNotice(RegisterOfflineActivity.this, null);
            }
        });
    }
}
