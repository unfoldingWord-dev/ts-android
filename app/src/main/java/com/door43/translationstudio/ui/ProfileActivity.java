package com.door43.translationstudio.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;

public class ProfileActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        View loginDoor43 = findViewById(R.id.login_door43);
        View registerDoor43 = findViewById(R.id.register_door43);
        View registerOffline = findViewById(R.id.register_offline);

        loginDoor43.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, LoginDoor43Activity.class);
                startActivity(intent);
            }
        });
        registerDoor43.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, RegisterDoor43Activity.class);
                startActivity(intent);
            }
        });
        registerOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, RegisterOfflineActivity.class);
                startActivity(intent);
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

        if (App.getProfile() != null) {
            Intent intent = new Intent(this, TermsOfUseActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Displays the privacy notice
     * @param listener if set the dialog will become a confirmation dialog
     */
    public static void showPrivacyNotice(Activity context, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder privacy = new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setTitle(R.string.privacy_notice)
                .setIcon(R.drawable.ic_info_black_24dp)
                .setMessage(R.string.publishing_privacy_notice);

        if(listener != null) {
            privacy.setPositiveButton(R.string.label_continue, listener);
            privacy.setNegativeButton(R.string.title_cancel, null);
        } else {
            privacy.setPositiveButton(R.string.dismiss, null);
        }
        privacy.show();
    }
}
