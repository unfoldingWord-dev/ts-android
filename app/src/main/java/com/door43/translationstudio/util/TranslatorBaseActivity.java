package com.door43.translationstudio.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.repo.GitSyncAsyncTask;

/**
 * Custom activity class to provide some of the heavy lifting.
 * Every activity within the app should extend this base activity.
 */
public class TranslatorBaseActivity extends ActionBarActivity {

    protected void onResume() {
        super.onResume();
        // set the current activity so that core classes can access the ui when nessesary.
        app().setCurrentActivity(this);
    }

    protected void onPause() {
        clearReferences();
        super.onPause();
    }

    protected void onDestroy() {
        clearReferences();
        super.onDestroy();
    }

    /**
     * Returns the MainApplication context
     * @return
     */
    public MainApplication app() {
        return ((MainApplication) this.getApplication());
    }

    /**
     * Removes references to self to avoid memory leaks
     */
    private void clearReferences() {
        Activity currActivity = app().getCurrentActivity();
        if(currActivity != null && currActivity.equals(this)) {
            app().setCurrentActivity(null);
        }
    }
}
