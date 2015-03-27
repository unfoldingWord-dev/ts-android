package com.door43.util.threads;

/**
 * This is the base class for creating a managed thread.
 * Managed threads are designed to be used in the ThreadManager.
 * Managing threads allows you to keep track of threads throughout your application during activity de/re-construction
 */
public abstract class ManagedTask implements Runnable {
    private Thread mThread;
    private boolean mFinished;
    private OnFinishedListener mFinishListener;
    private Object mTaskId;
    private OnProgressListener mProgressListener;
    private double mProgress = -1;
    private String mProgressMessage = "";

    @Override
    public final void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mThread = Thread.currentThread();
        if(!mThread.isInterrupted()) {
            start();
        }
        mFinished = true;
        if(mFinishListener != null) {
            try {
                mFinishListener.onFinished(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets the task id
     * @param id
     */
    public void setTaskId(Object id) {
        mTaskId = id;
    }

    /**
     * Returns the task id
     * @return
     */
    public Object getTaskId() {
        return mTaskId;
    }

    /**
     * Called when progress has been made.
     * This method should be called manually by the implementing class to update the progress
     * @param progress the progress being made between 1 and 0
     * @param message the progress message
     */
    protected void onProgress(double progress, String message) {
        mProgress = progress;
        mProgressMessage = message;
        if(mProgressListener != null) {
            try {
                mProgressListener.onProgress(this, mProgress, mProgressMessage);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets the listener to be called on progress updates
     * @param listener
     */
    public void setOnProgressListener(OnProgressListener listener) {
        mProgressListener = listener;
        if(mProgressListener != null) {
            try {
                mProgressListener.onProgress(this, mProgress, mProgressMessage);
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
        mFinishListener = listener;
        if(mFinishListener != null && isFinished()) {
            try {
                mFinishListener.onFinished(this);
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

    public static interface OnProgressListener {
        public void onProgress(ManagedTask task, double progress, String message);
    }
}
