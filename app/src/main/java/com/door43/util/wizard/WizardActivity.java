package com.door43.util.wizard;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import java.util.ArrayList;

/**
 * This class provides an easy to way to create a step by step wizard within a single activity.
 */
public abstract class WizardActivity extends ActionBarActivity implements WizardFragment.OnFragmentInteractionListener {
    private ArrayList<WizardFragment> mFragments = new ArrayList<>();
    private int mCurrentFragmentIndex = 0;
    private int mContainerViewId = -1;
    private static final String STATE_INDEX = "fragment_index";
    private StepDirection mStepDirection = StepDirection.NEXT;
    private boolean mHasInstanceState = false;

    public enum StepDirection {
        NEXT,
        PREVIOUS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // restore state
        if(savedInstanceState != null) {
            mCurrentFragmentIndex = savedInstanceState.getInt(STATE_INDEX, -1);
            mHasInstanceState = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!mHasInstanceState) {
            // only load the first step when the activity starts up
            loadStep();
        }
    }

    /**
     * Returns the direction in which the wizard step was made
     * @return
     */
    public StepDirection getStepDirection() {
        return mStepDirection;
    }

    /**
     * Sets the container view id that will be replaced with each step
     * @param containerViewId The view container to be replaced e.g. R.id.wizard_content
     */
    protected void setReplacementContainerViewId(int containerViewId) {
        mContainerViewId = containerViewId;
    }

    /**
     * Adds a fragment to the list of steps
     * @param fragment
     */
    protected void addStep(WizardFragment fragment) {
        mFragments.add(fragment);
    }

    /**
     * Called when the user reaches the end of the wizard
     */
    public abstract void onFinish();

    /**
     * Navigates to the next step if one exists
     */
    @Override
    public void onNext() {
        mStepDirection = StepDirection.NEXT;
        if(mContainerViewId == -1) {
            throw new IllegalArgumentException(toString() + " must call setReplacementContainerViewId before navigating between steps");
        }
        mCurrentFragmentIndex ++;
        if(mCurrentFragmentIndex >= mFragments.size()) {
            mCurrentFragmentIndex = mFragments.size() - 1;
            // we are done
            onFinish();
        } else {
            loadStep();
        }
    }

    /**
     * Closes the activity
     */
    @Override
    public void onCancel() {
        finish();
    }

    /**
     * Navigates to the previous step if one exists
     */
    @Override
    public void onPrevious() {
        mStepDirection = StepDirection.PREVIOUS;
        if(mContainerViewId == -1) {
            throw new IllegalArgumentException(toString() + " must call setReplacementContainerViewId before navigating between steps");
        }
        if(mCurrentFragmentIndex > 0) {
            mCurrentFragmentIndex--;
            loadStep();
        }
    }

    /**
     * Inserts the current step fragment into the layout
     */
    protected void loadStep() {
        if (mCurrentFragmentIndex >= 0 && mCurrentFragmentIndex < mFragments.size()) {
            getFragmentManager().beginTransaction().replace(mContainerViewId, mFragments.get(mCurrentFragmentIndex)).commit();
        } else {
            Log.w("Step", "Invalid wizard fragment index: " + mCurrentFragmentIndex);
            finish();
        }
    }

    /**
     * Navigates to the next step if one exists after skipping the number of steps.
     * @param numSteps The number of steps to be skipped. Positive numbers will skip forward. Negative numbers will skip backwards.
     */
    @Override
    public void onSkip(int numSteps) {
        if(numSteps < 0) {
            mCurrentFragmentIndex += numSteps;
            onPrevious();
        } else {
            mCurrentFragmentIndex += numSteps;
            onNext();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_INDEX, mCurrentFragmentIndex);
        super.onSaveInstanceState(outState);
    }
}
