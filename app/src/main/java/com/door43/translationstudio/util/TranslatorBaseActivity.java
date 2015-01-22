package com.door43.translationstudio.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.door43.translationstudio.BugReporterActivity;
import com.door43.translationstudio.CrashReporterActivity;
import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.SplashScreenActivity;
import com.door43.translationstudio.TermsActivity;

/**
 * Custom activity class to provide some of the heavy lifting.
 * Every activity within the app should extend this base activity.
 */
public abstract class TranslatorBaseActivity extends ActionBarActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // make sure the projects were correctly loaded
        String className = this.getClass().getName();
        if(!className.equals(TermsActivity.class.getName()) &&
            !className.equals(SplashScreenActivity.class.getName()) &&
            !className.equals(CrashReporterActivity.class.getName()) &&
            !className.equals(BugReporterActivity.class.getName()) &&
            app().getSharedProjectManager().getProjects().length == 0) {

            app().getSharedProjectManager().reset();
            Intent intent = new Intent(this, SplashScreenActivity.class);
            startActivity(intent);
            finish();
        }
    }

    protected void onResume() {
        super.onResume();
//        MainContext.getEventBus().register(this);
        // set the current activity so that core classes can access the ui when nessesary.
//        app().setCurrentActivity(this);
    }

    public void onStart() {
        super.onStart();
        MainContext.getEventBus().register(this);
        app().setCurrentActivity(this);
    }

    public void onPause() {
        // don't receive events when in the background
//        MainContext.getEventBus().unregister(this);
        super.onPause();
    }

    protected void onDestroy() {
//        clearReferences();
        super.onDestroy();
    }

    public void onStop() {
        clearReferences();
        super.onStop();
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
        MainContext.getEventBus().unregister(this);
        Activity currActivity = app().getCurrentActivity();
        if(currActivity != null && currActivity.equals(this)) {
            app().setCurrentActivity(null);
        }
    }
}
