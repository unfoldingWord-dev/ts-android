package com.door43.translationstudio.uploadwizard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.TranslationManager;
import com.door43.translationstudio.tasks.GenericTaskWatcher;
import com.door43.translationstudio.uploadwizard.steps.ContactInfoFragment;
import com.door43.translationstudio.uploadwizard.steps.OverviewFragment;
import com.door43.translationstudio.uploadwizard.steps.PreviewFragment;
import com.door43.translationstudio.uploadwizard.steps.ProjectChooserFragment;
import com.door43.translationstudio.uploadwizard.steps.ReviewFragment;
import com.door43.translationstudio.uploadwizard.steps.validate.VerifyFragment;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;
import com.door43.util.threads.ManagedTask;
import com.door43.util.wizard.WizardActivity;


public class UploadWizardActivity extends WizardActivity implements GenericTaskWatcher.OnFinishedListener {

    private boolean mFinished = false;
    private final static String STATE_FINISHED = "finished";
    private final static String STATE_UPLOADED = "uploaded";
    private static Project mProject;
    private static SourceLanguage mSource;
    private static Language mTarget;
    private GenericTaskWatcher mTaskWatcher;
    private boolean mUploaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_wizard);
        setReplacementContainerViewId(R.id.upload_wizard_content);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null) {
            mFinished = savedInstanceState.getBoolean(STATE_FINISHED);
            mUploaded = savedInstanceState.getBoolean(STATE_UPLOADED);
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

        mTaskWatcher = new GenericTaskWatcher(this, R.string.push_msg_init, R.drawable.ic_cloud_small);
        mTaskWatcher.setOnFinishedListener(this);
        // TODO: do we want to allow users to cancel their uploads?

        if(mUploaded) {
            onUploaded();
        } else if(mFinished) {
            // TODO: attach to tasks
//            mTaskWatcher.watch();
        }
    }

    @Override
    protected void loadStep() {
        closeKeyboard();
        if(!mFinished) {
            super.loadStep();
        }
        // if the wizard has finished we don't need to load any steps
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
        upload();
    }

    /**
     * Uploads the translation to the server
     */
    private void upload() {
        // prepare upload
        getTranslationProject().commit(new Project.OnCommitComplete() {
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
        outState.putBoolean(STATE_UPLOADED, mUploaded);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        mTaskWatcher.stop();
    }

    @Override
    public void onFinished(ManagedTask task) {
        mTaskWatcher.stop();
        mUploaded = true;
        onUploaded();
    }

    /**
     * Displays success notice to the user
     */
    public void onUploaded() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.success)
                .setMessage(R.string.git_push_success)
                .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }
}
