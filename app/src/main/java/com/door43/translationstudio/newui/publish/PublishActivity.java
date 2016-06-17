package com.door43.translationstudio.newui.publish;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.translationstudio.AppContext;
import com.door43.widget.ViewUtil;

import java.security.InvalidParameterException;
import java.util.Locale;

public class PublishActivity extends BaseActivity implements PublishStepFragment.OnEventListener {

    public static final int STEP_VALIDATE = 0;
    public static final int STEP_PROFILE = 1;
    public static final int STEP_REVIEW = 2;
    public static final int STEP_PUBLISH = 3;
    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    public static final String EXTRA_CALLING_ACTIVITY = "extra_calling_activity";
    public static final String EXTRA_PUSH_REJECTED = "extra_push_failed";
    private static final String STATE_STEP = "state_step";
    private static final String STATE_PUBLISH_FINISHED = "state_publish_finished";
    private PublishStepFragment mFragment;
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private int mCurrentStep = 0;
    private ViewHolder mValidationIndicator;
    private ViewHolder mProfileIndicator;
    private ViewHolder mReviewIndicator;
    private ViewHolder mPublishIndicator;
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

        mTranslator = AppContext.getTranslator();

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
        mValidationIndicator = new ViewHolder((LinearLayout)findViewById(R.id.validation_button),
                (ImageView)findViewById(R.id.validation_active),
                (ImageView)findViewById(R.id.validation_done),
                (TextView)findViewById(R.id.validation_step),
                (TextView)findViewById(R.id.validation_title),
                (ImageView)findViewById(R.id.validation_circle));
        mProfileIndicator = new ViewHolder((LinearLayout)findViewById(R.id.profile_button),
                (ImageView)findViewById(R.id.profile_active),
                (ImageView)findViewById(R.id.profile_done),
                (TextView)findViewById(R.id.profile_step),
                (TextView)findViewById(R.id.profile_title),
                (ImageView)findViewById(R.id.profile_circle));
        mReviewIndicator = new ViewHolder((LinearLayout)findViewById(R.id.review_button),
                (ImageView)findViewById(R.id.review_active),
                (ImageView)findViewById(R.id.review_done),
                (TextView)findViewById(R.id.review_step),
                (TextView)findViewById(R.id.review_title),
                (ImageView)findViewById(R.id.review_circle));
        mPublishIndicator = new ViewHolder((LinearLayout)findViewById(R.id.publish_button),
                (ImageView)findViewById(R.id.publish_active),
                (ImageView)findViewById(R.id.publish_done),
                (TextView)findViewById(R.id.publish_step),
                (TextView)findViewById(R.id.publish_title),
                (ImageView)findViewById(R.id.publish_circle));

        if(savedInstanceState != null) {
            mCurrentStep = savedInstanceState.getInt(STATE_STEP, 0);
            mPublishFinished = savedInstanceState.getBoolean(STATE_PUBLISH_FINISHED, false);
        }
        updateIndicatorsForStep(mCurrentStep);

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (PublishStepFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                mFragment = new ValidationFragment();
                String sourceTranslationId = AppContext.getSelectedSourceTranslationId(targetTranslationId);
                if(sourceTranslationId == null) {
                    // use the default target translation if they have not chosen one.
                    Library library = AppContext.getLibrary();
                    SourceLanguage sourceLanguage = library.getPreferredSourceLanguage(mTargetTranslation.getProjectId(), Locale.getDefault().getLanguage());
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

        // step click listeners
        mValidationIndicator.mButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(STEP_VALIDATE, false);
            }
        });
        mProfileIndicator.mButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(STEP_PROFILE, false);
            }
        });
        mReviewIndicator.mButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(STEP_REVIEW, false);
            }
        });
        mPublishIndicator.mButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(STEP_PUBLISH, false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            if(mCallingActivity == ACTIVITY_TRANSLATION) {
                // TRICKY: the translation activity is finished after opening the publish activity
                // because we may have to go back and forth and don't want to fill up the stack
                Intent intent = new Intent(this, TargetTranslationActivity.class);
                Bundle args = new Bundle();
                args.putString(AppContext.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
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
            args.putString(AppContext.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
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
        mPublishIndicator.setDone(true);
        mPublishIndicator.setActive(true);
        mPublishFinished = true;
    }

    @Override
    public void postFailure() {
        String targetTranslationId = null;
        if(mTargetTranslation != null) {
            targetTranslationId = mTargetTranslation.getId();
        }
        AppContext.setNotifyTargetTranslationWithUpdates(targetTranslationId);
        finish();
    }

    /**
     * checks if the user can go to this step yet
     * @param step
     * @param force forces the step to be opened even if it has never been opened before
     * @return
     */
    private boolean validateStep(int step, boolean force) {
        if(step > STEP_PUBLISH) {
            step = STEP_PUBLISH;
        }
        if(force) {
            // allow users to open this step if all of the previous steps have been visited
            while (step > 0) {
                step--;
                switch (step) {
                    case STEP_VALIDATE:
                        if (!mValidationIndicator.isVisited()) {
                            return false;
                        }
                        break;
                    case STEP_PROFILE:
                        if (!mProfileIndicator.isVisited()) {
                            return false;
                        }
                        break;
                    case STEP_REVIEW:
                        if (!mReviewIndicator.isVisited()) {
                            return false;
                        }
                        break;
                    case STEP_PUBLISH:
                        // never gets called
                        break;
                }
            }
        } else {
            // allow the user to open a previously opened step
            switch (step) {
                case STEP_VALIDATE:
                    if (!mValidationIndicator.isVisited()) {
                        return false;
                    }
                    break;
                case STEP_PROFILE:
                    if (!mProfileIndicator.isVisited()) {
                        return false;
                    }
                    break;
                case STEP_REVIEW:
                    if (!mReviewIndicator.isVisited()) {
                        return false;
                    }
                    break;
                case STEP_PUBLISH:
                    if (!mPublishIndicator.isVisited()) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    /**
     * Moves to the a stage in the publish process
     * @param step
     * @param force forces the step to be opened even if it has never been opened before
     */
    private void goToStep(int step, boolean force) {
        if(!validateStep(step, force) || step == mCurrentStep) {
            return;
        }

        if(step > STEP_PUBLISH) {
            mCurrentStep = STEP_PUBLISH;
            // mark the publish step as done
            mPublishIndicator.setDone(true);
        } else {
            mCurrentStep = step;
        }
        updateIndicatorsForStep(mCurrentStep);
        switch(mCurrentStep) {
            case STEP_PROFILE:
                mFragment = new TranslatorsFragment();
                break;
            case STEP_REVIEW:
                mFragment = new ReviewFragment();
                break;
            case STEP_PUBLISH:
                mFragment = new PublishFragment();
                break;
            case STEP_VALIDATE:
            default:
                mFragment = new ValidationFragment();
                break;
        }

        Bundle args = getIntent().getExtras();
        String sourceTranslationId = AppContext.getSelectedSourceTranslationId(mTargetTranslation.getId());
        // TRICKY: if the user has not chosen a source translation (this is an empty translation) the id will be null
        if(sourceTranslationId == null) {
            SourceTranslation sourceTranslation = AppContext.getLibrary().getDefaultSourceTranslation(mTargetTranslation.getProjectId(), Locale.getDefault().getLanguage());
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

    /**
     * Correctly renders the state indicators
     * @param step
     */
    private void updateIndicatorsForStep(int step) {
        // reset
        if(mPublishFinished) {
            mValidationIndicator.setDone(true);
            mProfileIndicator.setDone(true);
            mReviewIndicator.setDone(true);
            mPublishIndicator.setDone(true);
        } else {
            mValidationIndicator.setVisited(false);
            mProfileIndicator.setVisited(false);
            mReviewIndicator.setVisited(false);
            mPublishIndicator.setVisited(false);
        }

        switch (step) {
            case STEP_VALIDATE:
                mValidationIndicator.setActive(true);
                mProfileIndicator.setActive(false);
                mReviewIndicator.setActive(false);
                mPublishIndicator.setActive(false);
                break;
            case STEP_PROFILE:
                mValidationIndicator.setDone(true);
                mProfileIndicator.setActive(true);
                mReviewIndicator.setActive(false);
                mPublishIndicator.setActive(false);
                break;
            case STEP_REVIEW:
                mValidationIndicator.setDone(true);
                mProfileIndicator.setDone(true);
                mReviewIndicator.setActive(true);
                mPublishIndicator.setActive(false);
                break;
            case STEP_PUBLISH:
                mValidationIndicator.setDone(true);
                mProfileIndicator.setDone(true);
                mReviewIndicator.setDone(true);
                mPublishIndicator.setActive(true);
                break;

        }

    }

    private class ViewHolder {
        private final LinearLayout mButtonLayout;
        private final ImageView mActiveView;
        private final ImageView mDoneView;
        private final TextView mStepView;
        private final TextView mTitleView;
        private final ImageView mCircleView;
        private boolean mVisited = false;
        private boolean mDone;

        public ViewHolder(LinearLayout buttonLayout, ImageView activeView, ImageView doneView, TextView stepView, TextView titleView, ImageView circleView) {
            mButtonLayout = buttonLayout;
            mActiveView = activeView;
            mDoneView = doneView;
            mStepView = stepView;
            mTitleView = titleView;
            mCircleView = circleView;
        }

        /**
         * Checks if the step has been finished at least once durring this session.
         * @return
         */
        public boolean isDone() {
            return mDone;
        }

        /**
         * Checks if the step has been visited at least once durring this session.
         * @return
         */
        public boolean isVisited() {
            return mVisited;
        }

        /**
         * Marks the step has active or not
         * @param active
         */
        public void setActive(boolean active) {
            if(active) {
                setVisited(true);
                mActiveView.setVisibility(View.VISIBLE);
                if(!mDone) {
                    mStepView.setVisibility(View.VISIBLE);
                    mDoneView.setVisibility(View.GONE);
                } else {
                    mDoneView.setVisibility(View.VISIBLE);
                }
            } else {
                mActiveView.setVisibility(View.GONE);
            }
        }

        /**
         * Marks the step has done or not
         * @param done
         */
        public void setDone(boolean done) {
            if(done) {
                mDone = true;
                setVisited(true);
                mActiveView.setVisibility(View.GONE);
                mStepView.setVisibility(View.GONE);
                mDoneView.setVisibility(View.VISIBLE);
            } else if(!mDone) {
                // finished steps cannot be un-finished
                mStepView.setVisibility(View.VISIBLE);
                mDoneView.setVisibility(View.GONE);
            }
        }

        /**
         * Sets the step as visisted or not
         * @param visited
         */
        public void setVisited(boolean visited) {
            // TODO: tint the drawables as well
            if(visited) {
                mVisited = true;
                mStepView.setTextColor(getResources().getColor(R.color.light_primary_text));
                mTitleView.setTextColor(getResources().getColor(R.color.light_primary_text));
                ViewUtil.tintViewDrawable(mCircleView, getResources().getColor(R.color.light_primary_text));
            } else if(!mVisited) {
                // visited steps cannot be un-visited
                setDone(false);
                setActive(false);
                mStepView.setTextColor(getResources().getColor(R.color.light_secondary_text));
                mTitleView.setTextColor(getResources().getColor(R.color.light_secondary_text));
                ViewUtil.tintViewDrawable(mCircleView, getResources().getColor(R.color.light_secondary_text));
            }
        }
    }

    public void onSaveInstanceState(Bundle out) {
        out.putInt(STATE_STEP, mCurrentStep);
        out.putBoolean(STATE_PUBLISH_FINISHED, mPublishFinished);

        super.onSaveInstanceState(out);
    }
}
