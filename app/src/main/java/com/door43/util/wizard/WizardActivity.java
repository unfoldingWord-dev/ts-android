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
    private int mCurrentFragmentIndex = -1;
    private int mContainerViewId = -1;
    private static final String STATE_INDEX = "fragment_index";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // restore state
        if(savedInstanceState != null) {
            mCurrentFragmentIndex = savedInstanceState.getInt(STATE_INDEX) - 1;
        }
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
        if(mContainerViewId == -1) {
            throw new IllegalArgumentException(toString() + " must call setReplacementContainerViewId before navigating between steps");
        }
        mCurrentFragmentIndex ++;
        if(mCurrentFragmentIndex >= 0 && mCurrentFragmentIndex < mFragments.size()) {
            getFragmentManager().beginTransaction().replace(mContainerViewId, mFragments.get(mCurrentFragmentIndex)).addToBackStack(null).commit();
        } else if(mCurrentFragmentIndex >= mFragments.size()) {
            // we are done
            onFinish();
        } else {
            Log.w("Step", "Invalid wizard fragment index: " + mCurrentFragmentIndex);
            finish();
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
        if(mContainerViewId == -1) {
            throw new IllegalArgumentException(toString() + " must call setReplacementContainerViewId before navigating between steps");
        }
        if(mCurrentFragmentIndex > 0) {
            mCurrentFragmentIndex--;
            if (mCurrentFragmentIndex >= 0 && mCurrentFragmentIndex < mFragments.size()) {
                getFragmentManager().beginTransaction().replace(mContainerViewId, mFragments.get(mCurrentFragmentIndex)).addToBackStack(null).commit();
            } else {
                Log.w("Step", "Invalid wizard fragment index: " + mCurrentFragmentIndex);
                finish();
            }
        }
    }

    /**
     * Navigates to the next step if one exists after skipping the number of steps.
     * @param numSteps The number of steps to be skipped. Positive numbers will skip forward. Negative numbers will skip backwards.
     */
    @Override
    public void onSkip(int numSteps) {
        mCurrentFragmentIndex += numSteps;
        onNext();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_INDEX, mCurrentFragmentIndex);
        super.onSaveInstanceState(outState);
    }
}
