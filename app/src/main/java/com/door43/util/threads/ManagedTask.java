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
    private boolean mIsStopped = false;
    private OnStartListener mStartListener;
    private boolean mIsRunning = false;

    @Override
    public final void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mThread = Thread.currentThread();
        try {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            mIsRunning = true;
            if(mStartListener != null) {
                try {
                    mStartListener.onStart(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            start();
        } catch (InterruptedException e) {

        } finally {
            // clears the thread's interrupt flag
            Thread.interrupted();
        }

        mIsRunning = false;
        mFinished = true;
        if(mFinishListener != null) {
            try {
                mFinishListener.onFinished(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        onStop();
    }

    /**
     * Causes the task to sleep
     * @param time
     * @throws InterruptedException
     */
    protected final void sleep(long time) {
        try {
            mThread.sleep(time);
        } catch (InterruptedException e) {

        } finally {
            // clear the thread's interrupt flag
            Thread.interrupted();
        }
    }

    /**
     * Sets the task id
     * @param id
     */
    public final void setTaskId(Object id) {
        mTaskId = id;
    }

    /**
     * Returns the task id
     * @return
     */
    public final Object getTaskId() {
        return mTaskId;
    }

    /**
     * Called when progress has been made.
     * This method should be called manually by the implementing class to update the progress
     * @param progress the progress being made between 1 and 0
     * @param message the progress message
     */
    protected final void publishProgress(double progress, String message) {
        mProgress = progress;
        mProgressMessage = message;
        if(mProgressListener != null && !isFinished()) {
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
    public final void setOnProgressListener(OnProgressListener listener) {
        mProgressListener = listener;
        if(mProgressListener != null && !isFinished()) {
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
    public final void setOnFinishedListener(OnFinishedListener listener) {
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
     * Sets the listener to be called when the task starts
     * @param listener
     */
    public final void setOnStartListener(OnStartListener listener) {
        mStartListener = listener;
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

    /**
     * Checks if the task is running
     * @return
     */
    public final boolean isRunning() {
        return mIsRunning;
    }

    /**
     * Checks if the task was canceled
     * @return
     */
    public final boolean isCanceled() {
        return mIsStopped;
    }

    public final void stop() {
        mIsStopped = true;
    }

    /**
     * Called whenever the task stops completely.
     * This allows tasks to perform any cleanup operations
     */
    protected void onStop() {

    }

    /**
     * Checks if the task has been interrupted.
     * Interrupting threads is not reliable so we need to set a flag.
     * @return
     */
    public final boolean interrupted() {
        return mThread.isInterrupted() || mIsStopped;
    }

    public interface OnFinishedListener {
        void onFinished(ManagedTask task);
    }

    public interface OnProgressListener {
        void onProgress(ManagedTask task, double progress, String message);
    }

    public interface OnStartListener {
        void onStart(ManagedTask task);
    }
}
