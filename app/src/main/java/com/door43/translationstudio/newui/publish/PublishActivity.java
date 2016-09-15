package com.door43.translationstudio.newui.publish;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.BackupDialog;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.widget.ViewUtil;

import java.security.InvalidParameterException;

public class PublishActivity extends BaseActivity implements PublishStepFragment.OnEventListener {

    public static final int STEP_VALIDATE = 0;
    public static final int STEP_PROFILE = 1;
    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    public static final String EXTRA_CALLING_ACTIVITY = "extra_calling_activity";
    private static final String STATE_STEP = "state_step";
    private static final String STATE_PUBLISH_FINISHED = "state_publish_finished";
    private PublishStepFragment mFragment;
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private int mCurrentStep = 0;
    public static final int ACTIVITY_HOME = 1001;
    public static final int ACTIVITY_TRANSLATION = 1002;
    private boolean mPublishFinished = false;
    private int mCallingActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTranslator = App.getTranslator();

        // validate parameters
        Bundle args = getIntent().getExtras();
        final String targetTranslationId = args.getString(EXTRA_TARGET_TRANSLATION_ID, null);
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        if(mTargetTranslation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        // identify calling activity
        mCallingActivity = args.getInt(EXTRA_CALLING_ACTIVITY, 0);
        if(mCallingActivity == 0) {
            throw new InvalidParameterException("you must specify the calling activity");
        }

        // stage indicators

        if(savedInstanceState != null) {
            mCurrentStep = savedInstanceState.getInt(STATE_STEP, 0);
            mPublishFinished = savedInstanceState.getBoolean(STATE_PUBLISH_FINISHED, false);
        }

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (PublishStepFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                mFragment = new ValidationFragment();
                String sourceTranslationId = App.getSelectedSourceTranslationId(targetTranslationId);
                if(sourceTranslationId == null) {
                    // use the default target translation if they have not chosen one.
                    Library library = App.getLibrary();
                    SourceLanguage sourceLanguage = library.getPreferredSourceLanguage(mTargetTranslation.getProjectId(), App.getDeviceLanguageCode());
                    if(sourceLanguage != null) {
                        SourceTranslation sourceTranslation = library.getDefaultSourceTranslation(mTargetTranslation.getProjectId(), sourceLanguage.getId());
                        if (sourceTranslation != null) {
                            sourceTranslationId = sourceTranslation.getId();
                        }
                    }
                }
                if(sourceTranslationId != null) {
                    args.putSerializable(PublishStepFragment.ARG_SOURCE_TRANSLATION_ID, sourceTranslationId);
                    mFragment.setArguments(args);
                    getFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                } else {
                    // the user must choose a source translation before they can publish
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.choose_source_translations, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                    finish();
                }
            }
        }

        // add step button listeners

        Button validationButton = (Button)findViewById(R.id.validation_button);
        validationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(STEP_VALIDATE, false);
            }
        });

        Button profileButton = (Button)findViewById(R.id.profile_button);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(STEP_PROFILE, false);
            }
        });

        Button uploadButton = (Button)findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBackupDialog();
            }
        });
    }

    /**
     * display Backup dialog
     */
    private void showBackupDialog() {
        FragmentTransaction backupFt = getFragmentManager().beginTransaction();
        Fragment backupPrev = getFragmentManager().findFragmentByTag(BackupDialog.TAG);
        if (backupPrev != null) {
            backupFt.remove(backupPrev);
        }
        backupFt.addToBackStack(null);

        BackupDialog backupDialog = new BackupDialog();
        Bundle args = new Bundle();
        args.putString(BackupDialog.ARG_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
        backupDialog.setArguments(args);
        backupDialog.show(backupFt, BackupDialog.TAG);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            if(mCallingActivity == ACTIVITY_TRANSLATION) {
                // TRICKY: the translation activity is finished after opening the publish activity
                // because we may have to go back and forth and don't want to fill up the stack
                Intent intent = new Intent(this, TargetTranslationActivity.class);
                Bundle args = new Bundle();
                args.putString(App.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                intent.putExtras(args);
                startActivity(intent);
            }
            finish();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // TRICKY: the translation activity is finished after opening the publish activity
        // because we may have to go back and forth and don't want to fill up the stack
        if(mCallingActivity == ACTIVITY_TRANSLATION) {
            Intent intent = new Intent(this, TargetTranslationActivity.class);
            Bundle args = new Bundle();
            args.putString(App.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
            intent.putExtras(args);
            startActivity(intent);
        }
        finish();
    }


    @Override
    public void nextStep() {
        goToStep(mCurrentStep + 1, true);
    }

    @Override
    public void finishPublishing() {
        mPublishFinished = true;
    }

    /**
     * Moves to the a stage in the publish process
     * @param step
     * @param force forces the step to be opened even if it has never been opened before
     */
    private void goToStep(int step, boolean force) {
        if( step == mCurrentStep) {
            return;
        }

        if(step > STEP_PROFILE) { // if we are ready to upload
            mCurrentStep = STEP_PROFILE;
            showBackupDialog();
            return;
        } else {
            mCurrentStep = step;
        }

        switch(mCurrentStep) {
            case STEP_PROFILE:
                mFragment = new TranslatorsFragment();
                break;
            case STEP_VALIDATE:
            default:
                mFragment = new ValidationFragment();
                break;
        }

        Bundle args = getIntent().getExtras();
        String sourceTranslationId = App.getSelectedSourceTranslationId(mTargetTranslation.getId());
        // TRICKY: if the user has not chosen a source translation (this is an empty translation) the id will be null
        if(sourceTranslationId == null) {
            SourceTranslation sourceTranslation = App.getLibrary().getDefaultSourceTranslation(mTargetTranslation.getProjectId(), App.getDeviceLanguageCode());
            if(sourceTranslation != null) {
                sourceTranslationId = sourceTranslation.getId();
            }
        }
        args.putSerializable(PublishStepFragment.ARG_SOURCE_TRANSLATION_ID, sourceTranslationId);
        args.putBoolean(PublishStepFragment.ARG_PUBLISH_FINISHED, mPublishFinished);
        mFragment.setArguments(args);
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
        // TODO: animate
    }

    public void onSaveInstanceState(Bundle out) {
        out.putInt(STATE_STEP, mCurrentStep);
        out.putBoolean(STATE_PUBLISH_FINISHED, mPublishFinished);

        super.onSaveInstanceState(out);
    }
}
