package com.door43.translationstudio.util;

import android.content.Context;
import android.os.Handler;

/**
 * This class allows you to execute operations in a seperate thread and then finish up by running some code on the ui thread
 */
public abstract class ThreadableUI {
    private Thread mThread;


    public void start(Context context) {
        final Handler handler = new Handler(context.getMainLooper());
        mThread = new Thread() {
            @Override
            public void run() {
                // execute the tasks
                ThreadableUI.this.run();
                // execute cleanup on the ui thread
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onPostExecute();
                    }
                });
            }
        };
        mThread.start();
    }

    /**
     * Check if the thread is alive
     * @return
     */
    public boolean isAlive() {
        if(mThread != null) {
            return mThread.isAlive();
        } else {
            return false;
        }
    }

    /**
     * kills the thread
     * WARNING: this may not be the safest way to terminate the thread
     */
    public void stop() {
        if(mThread != null) {
            onStop();
            mThread.interrupt();
            mThread = null;
        }
    }

    /**
     * Allow the thread to perform some cleanup actions before shutting down
     */
    abstract public void onStop();

    /**
     * Code to be ran when the thread starts up
     */
    abstract public void run();

    /**
     * Code to be ran on the UI thread after the run() is complete
     */
    abstract public void onPostExecute();
}
