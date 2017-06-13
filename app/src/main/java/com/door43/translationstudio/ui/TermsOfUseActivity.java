package com.door43.translationstudio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.ui.home.HomeActivity;
import com.door43.translationstudio.ui.legal.LegalDocumentActivity;

/**
 * This activity checks if the user has accepted the terms of use before continuing to load the app
 */
public class TermsOfUseActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Profile profile = App.getProfile();
        final int termsVersion = App.getTermsOfUseVersion();

        if(profile == null) {
            finish();
            return;
        }

        if (termsVersion == profile.getTermsOfUseLastAccepted()) {
            // skip terms if already accepted
            startMainActivity();
        } else {
            setContentView(R.layout.activity_terms);
            Button rejectBtn = (Button)findViewById(R.id.reject_terms_btn);
            rejectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // log out
                    App.setProfile(null);

                    // return to login
                    Intent intent = new Intent(TermsOfUseActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
            Button acceptBtn = (Button)findViewById(R.id.accept_terms_btn);
            acceptBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    profile.setTermsOfUseLastAccepted(termsVersion);
                    App.setProfile(profile);
                    startMainActivity();
                }
            });
            Button licenseBtn = (Button)findViewById(R.id.license_btn);
            licenseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLicenseDialog(R.string.license_pdf);
                }
            });
            Button guidelinesBtn = (Button)findViewById(R.id.translation_guidelines_btn);
            guidelinesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLicenseDialog(R.string.translation_guidlines);
                }
            });
            Button faithBtn = (Button)findViewById(R.id.statement_of_faith_btn);
            faithBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLicenseDialog(R.string.statement_of_faith);
                }
            });
        }
    }

    /**
     * Continues to the splash screen where local resources will be loaded
     */
    private void startMainActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Displays a license dialog with the given resource as the text
     * @param stringResource the string resource to display in the dialog.
     */
    private void showLicenseDialog(int stringResource) {
        Intent intent = new Intent(this, LegalDocumentActivity.class);
        intent.putExtra(LegalDocumentActivity.ARG_RESOURCE, stringResource);
        startActivity(intent);
    }
}
