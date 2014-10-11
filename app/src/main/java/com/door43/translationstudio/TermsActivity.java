package com.door43.translationstudio;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.door43.translationstudio.dialogs.LicenseDialog;
import com.door43.translationstudio.util.TranslatorBaseActivity;

/**
 * This activity checks if the user has accepted the terms of use before continuing to load the app
 */
public class TermsActivity extends TranslatorBaseActivity {
    private LicenseDialog licenseDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        if (app().hasAcceptedTerms()) {
            // skip to the splash screen if they have already accepted the terms of use
            startSplashActivity();
        } else {
            Button rejectBtn = (Button)findViewById(R.id.reject_terms_btn);
            rejectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
            Button acceptBtn = (Button)findViewById(R.id.accept_terms_btn);
            acceptBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    app().setHasAcceptedTerms(true);
                    startSplashActivity();
                }
            });
            Button licenseBtn = (Button)findViewById(R.id.license_btn);
            licenseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLicense();
                }
            });
        }
    }

    /**
     * Continues to the splash screen where local resources will be loaded
     */
    private void startSplashActivity() {
        Intent splashIntent = new Intent(this, SplashActivity.class);
        startActivity(splashIntent);
        finish();
    }

    /**
     * Displays the license dialog
     */
    public void showLicense() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        if(licenseDialog == null) {
            licenseDialog = new LicenseDialog();
        }
        licenseDialog.show(ft, "dialog");
    }
}
