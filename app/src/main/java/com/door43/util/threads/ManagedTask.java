package com.door43.util.threads;

/**
 * This is the base class for creating a managed thread.
 * Managed threads are designed to be used in the ThreadManager.
 * Managing threads allows you to keep track of threads throughout your application during activity de/re-construction
 */
public abstract class ManagedTask implements Runnable {
    private Thread mThread;
    private boolean mFinished;
    private OnFinishedListener mListener;

    @Override
    public final void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mThread = Thread.currentThread();
        if(!mThread.isInterrupted()) {
            start();
        }
        mFinished = true;
        if(mListener != null) {
            try {
                mListener.onFinished(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets the listener to be called when the task is finished
     * @param listener
     */
    public void setOnFinishedListener(OnFinishedListener listener) {
        mListener = listener;
        if(mListener != null && isFinished()) {
            try {
                mListener.onFinished(this);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Perform any threadable tasks here
     */
    public abstract void start();

    /**
     * Returns the thread on which this runnable is being executed
     * @return
     */
    public final Thread getThread() {
        return mThread;
    }

    /**
     * Checks if the task has finished running.
     * @return
     */
    public final boolean isFinished() {
        return mFinished;
    }

    public static interface OnFinishedListener {
        public void onFinished(ManagedTask task);
    }
}
