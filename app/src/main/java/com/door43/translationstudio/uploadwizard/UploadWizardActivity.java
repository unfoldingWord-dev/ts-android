package com.door43.translationstudio.uploadwizard;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ContactFormDialog;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.TranslationManager;
import com.door43.translationstudio.uploadwizard.steps.ContactInfoFragment;
import com.door43.translationstudio.uploadwizard.steps.OverviewFragment;
import com.door43.translationstudio.uploadwizard.steps.PreviewFragment;
import com.door43.translationstudio.uploadwizard.steps.ProjectChooserFragment;
import com.door43.translationstudio.uploadwizard.steps.ReviewFragment;
import com.door43.translationstudio.uploadwizard.steps.validate.VerifyFragment;
import com.door43.translationstudio.user.Profile;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;
import com.door43.util.wizard.WizardActivity;


public class UploadWizardActivity extends WizardActivity {

    private boolean mFinished = false;
    private final static String STATE_FINISHED = "finished";
    // project to upload
    private static Project mProject;
    private static SourceLanguage mSource;
    private static Language mTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_wizard);
        setReplacementContainerViewId(R.id.upload_wizard_content);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null) {
            mFinished = savedInstanceState.getBoolean(STATE_FINISHED);
        } else {
            // initialize things for the upload wizard
            mProject = null;
            mSource = null;
            mTarget = null;
        }

        addStep(new OverviewFragment());
        addStep(new ProjectChooserFragment());
        addStep(new VerifyFragment());
        addStep(new ReviewFragment());
        addStep(new ContactInfoFragment());
        addStep(new PreviewFragment());

        if(mFinished) {
            // TODO: attach to tasks
        }
    }

    @Override
    protected void loadStep() {
        closeKeyboard();
        super.loadStep();
    }

    /**
     * Closes the keyboard
     */
    public void closeKeyboard() {
        if(getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } else {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    /**
     * Sets the translation that will be uploaded
     * @param project
     * @param source
     * @param target
     */
    public void setTranslationToUpload(Project project, SourceLanguage source, Language target) {
        mProject = project;
        mSource = source;
        mTarget = target;
    }

    /**
     * Returns the source language of the translation that will be uploaded
     * @return
     */
    public SourceLanguage getTranslationSource() {
        return mSource;
    }

    /**
     * Returns the target language for the translation that will be uploaded
     * @return
     */
    public Language getTranslationTarget() {
        return mTarget;
    }

    /**
     * Returns the project that will be uploaded
     * @return will return null if no project is selected
     */
    public Project getTranslationProject() {
        if(mProject == null) {
            mSource = null;
            mTarget = null;
            mProject = AppContext.projectManager().getSelectedProject();
            if(mProject != null && (mProject.isTranslatingGlobal() || mProject.isTranslating())) {
                mSource = mProject.getSelectedSourceLanguage();
                if(mProject.isTranslating()) {
                    mTarget = mProject.getSelectedTargetLanguage();
                } else {
                    // get the first available translation from this project
                    Language[] targets = mProject.getActiveTargetLanguages();
                    if(targets.length > 0) {
                        mTarget = targets[0];
                    } else {
                        // this should never happen
                        Logger.e(null, "expecting an active target language but found none.");
                    }
                }
            } else {
                mProject = null;
            }
        }
        return mProject;
    }

    @Override
    public void onFinish() {
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
