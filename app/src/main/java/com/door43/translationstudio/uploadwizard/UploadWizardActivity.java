package com.door43.translationstudio.uploadwizard;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ContactFormDialog;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.TranslationManager;
import com.door43.translationstudio.uploadwizard.steps.ContactInfoFragment;
import com.door43.translationstudio.uploadwizard.steps.OverviewFragment;
import com.door43.translationstudio.uploadwizard.steps.PreviewFragment;
import com.door43.translationstudio.uploadwizard.steps.ProjectChooserFragment;
import com.door43.translationstudio.uploadwizard.steps.ReviewFragment;
import com.door43.translationstudio.uploadwizard.steps.VerifyFragment;
import com.door43.translationstudio.user.Profile;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.wizard.WizardActivity;


public class UploadWizardActivity extends WizardActivity {

    private boolean mFinished = false;
    private final static String STATE_FINISHED = "finished";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_wizard);
        setReplacementContainerViewId(R.id.upload_wizard_content);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null) {
            mFinished = savedInstanceState.getBoolean(STATE_FINISHED);
        }

        addStep(new OverviewFragment());
        addStep(new ProjectChooserFragment());
        addStep(new VerifyFragment());
        addStep(new ReviewFragment());
        addStep(new ContactInfoFragment());
        addStep(new PreviewFragment());

        onNext();

        if(mFinished) {
            // TODO: attach to tasks
        }
    }

    @Override
    protected void onFinish() {
        mFinished = true;
        // TODO: place all of this in a task
        if(AppContext.projectManager().getSelectedProject().translationIsReady() && ProfileManager.getProfile() == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            // Create and show the dialog
            ContactFormDialog newFragment = new ContactFormDialog();
            newFragment.setOkListener(new ContactFormDialog.OnOkListener() {
                @Override
                public void onOk(Profile profile) {
                    ProfileManager.setProfile(profile);
                    upload();
                }
            });
            newFragment.show(ft, "dialog");
        } else {
            upload();
        }
    }

    /**
     * Initiates the actual upload
     */
    private void upload() {
        // TODO: place all of this in a task
        // prepare upload
        AppContext.context().showProgressDialog(R.string.preparing_upload);
        AppContext.projectManager().getSelectedProject().commit(new Project.OnCommitComplete() {
            @Override
            public void success() {
                TranslationManager.syncSelectedProject();
                finish();
            }

            @Override
            public void error(Throwable e) {
                // We don't care. Worst case is the server won't know that the translation is ready.
                TranslationManager.syncSelectedProject();
                finish();
            }
        });
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_FINISHED, mFinished);
        super.onSaveInstanceState(outState);
    }
}
