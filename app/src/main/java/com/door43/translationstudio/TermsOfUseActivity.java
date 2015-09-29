package com.door43.translationstudio;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.door43.translationstudio.dialogs.LicenseDialog;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.util.AppContext;

/**
 * This activity checks if the user has accepted the terms of use before continuing to load the app
 */
public class TermsOfUseActivity extends BaseActivity {
    private LicenseDialog mLicenseDialog;
    private Boolean mDialogIsOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AppContext.context().hasAcceptedTerms()) {
            // skip to the splash screen if they have already accepted the terms of use
            startSplashActivity();
        } else {
            setContentView(R.layout.activity_terms);
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
                    AppContext.context().setHasAcceptedTerms(true);
                    startSplashActivity();
                }
            });
            Button licenseBtn = (Button)findViewById(R.id.license_btn);
            licenseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLicenseDialog(R.string.license);
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
    private void startSplashActivity() {
        Intent splashIntent = new Intent(this, SplashScreenActivity.class);
        startActivity(splashIntent);
        finish();
    }

    /**
     * Displays a license dialog with the given resource as the text
     * @param stringResource the string resource to display in the dialog.
     */
    private void showLicenseDialog(int stringResource) {
        if(mLicenseDialog == null) {
            mLicenseDialog = new LicenseDialog();
        }

        if(mDialogIsOpen) {
            return;
        } else {
            mDialogIsOpen = true;
        }

        mLicenseDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mDialogIsOpen = false;
            }
        });

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        Bundle args = new Bundle();
        args.putInt("resourceId", stringResource);
        mLicenseDialog.setArguments(args);
        mLicenseDialog.show(ft, "dialog");
    }
}
