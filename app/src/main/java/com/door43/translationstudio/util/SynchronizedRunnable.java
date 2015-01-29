package com.door43.translationstudio.util;

/**
 * Created by joel on 1/29/2015.
 */
public abstract class SynchronizedRunnable implements Runnable {
    private boolean mFinished = false;

    /**
     * Notifies any sychronized objects that this runnable is finished
     */
    protected void setFinished() {
        synchronized (this) {
            mFinished = true;
            notify();
        }
    }

    /**
     * Checks if this runnable has thrown it's flag indicating other
     * @return
     */
    public boolean isFinished() {
        return mFinished;
    }
}
