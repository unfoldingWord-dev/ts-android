package com.door43.translationstudio.newui.publish;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.util.AppContext;
import com.door43.widget.ViewUtil;

import java.security.InvalidParameterException;

public class PublishActivity extends BaseActivity implements PublishStepFragment.OnEventListener {

    public static final int STEP_VALIDATE = 0;
    public static final int STEP_PROFILE = 1;
    public static final int STEP_REVIEW = 2;
    public static final int STEP_PUBLISH = 3;
    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    private static final String STATE_STEP = "state_step";
    private PublishStepFragment mFragment;
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private int mCurrentStep = 0;
    private ViewHolder mValidationIndicator;
    private ViewHolder mProfileIndicator;
    private ViewHolder mReviewIndicator;
    private ViewHolder mPublishIndicator;

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
        }
        updateIndicatorsForStep(mCurrentStep);

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (PublishStepFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                mFragment = new ValidationFragment();
                String sourceTranslationId = mTranslator.getSelectedSourceTranslationId(targetTranslationId);
                args.putSerializable(PublishStepFragment.ARG_SOURCE_TRANSLATION_ID, sourceTranslationId);
                mFragment.setArguments(args);
                getFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
                // TODO: animate
            }
        }

        // step click listeners
        mValidationIndicator.mButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(STEP_VALIDATE);
            }
        });
        mProfileIndicator.mButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(STEP_PROFILE);
            }
        });
        mReviewIndicator.mButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(STEP_REVIEW);
            }
        });
        mPublishIndicator.mButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(STEP_PUBLISH);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    public void nextStep() {
        goToStep(mCurrentStep + 1);
    }

    /**
     * checks if the user can go to this step yet
     * @param step
     * @return
     */
    private boolean validateStep(int step) {
        if(step > STEP_PUBLISH) {
            step = STEP_PUBLISH;
        }
        while(step > 0) {
            step --;
            switch (step) {
                case STEP_VALIDATE:
                    if(!mValidationIndicator.isVisited()) {
                        return false;
                    }
                    break;
                case STEP_PROFILE:
                    if(!mProfileIndicator.isVisited()) {
                        return false;
                    }
                    break;
                case STEP_REVIEW:
                    if(!mReviewIndicator.isVisited()) {
                        return false;
                    }
                    break;
                case STEP_PUBLISH:
                    // never gets called
                    break;
            }
        }
        return true;
    }

    /**
     * Moves to the a stage in the publish process
     * @param step
     */
    private void goToStep(int step) {
        if(!validateStep(step)) {
            return;
        }

        if(step > STEP_PUBLISH) {
            mCurrentStep = STEP_PUBLISH;
        } else {
            mCurrentStep = step;
        }
        updateIndicatorsForStep(mCurrentStep);
        switch(mCurrentStep) {
            case STEP_PROFILE:
                mFragment = new ProfileFragment();
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
        String sourceTranslationId = mTranslator.getSelectedSourceTranslationId(mTargetTranslation.getId());
        args.putSerializable(PublishStepFragment.ARG_SOURCE_TRANSLATION_ID, sourceTranslationId);
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
        mValidationIndicator.setVisited(false);
        mProfileIndicator.setVisited(false);
        mReviewIndicator.setVisited(false);
        mPublishIndicator.setVisited(false);

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
}
