package com.door43.translationstudio.util;

import android.app.Activity;

/**
 * Custom activity class to provide some of the heavy lifting.
 */
public class TranslatorActivity extends Activity {

    protected void onResume() {
        super.onResume();
        app().setmCurrentActivity(this);
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
    protected MainApplication app() {
        return ((MainApplication) this.getApplication());
    }


    /**
     * Removes references to self to avoid memory leaks
     */
    private void clearReferences() {
        Activity currActivity = app().getmCurrentActivity();
        if(currActivity != null && currActivity.equals(this)) {
            app().setmCurrentActivity(null);
        }
    }
}
