package com.door43.translationstudio.util;

import android.app.Activity;
import android.app.Application;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Custom application class so we can effectively handle state accross activities and other classes
 */
public class MainApplication extends Application {
    private Activity mCurrentActivity = null;
    private Toast mToast = null;

    /**
     * Sets the current activity so we can access it throughout the app.
     * @param mCurrentActivity
     */
    public void setmCurrentActivity(Activity mCurrentActivity) {
        this.mCurrentActivity = mCurrentActivity;
    }

    /**
     * Returns the currently active activity
     * @return
     */
    public Activity getmCurrentActivity() {
        return mCurrentActivity;
    }

    /**
     * Displays a standard toast message in the ui
     * @param message The message to display to the user
     */
    public void setNotice(final String message) {
        if(mCurrentActivity != null) {
            mCurrentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if(mToast != null) {
                        mToast.cancel();
                    }
                    mToast = Toast.makeText(mCurrentActivity, message, Toast.LENGTH_SHORT);
                    mToast.setGravity(Gravity.TOP, 0, 0);
                    mToast.show();
                }
            });
        }
    }
}
