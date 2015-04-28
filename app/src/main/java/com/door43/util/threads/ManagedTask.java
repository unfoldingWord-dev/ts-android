package com.door43.util.threads;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the base class for creating a managed thread.
 * Managed threads are designed to be used in the ThreadManager.
 * Managing threads allows you to keep track of threads throughout your application during activity de/re-construction
 */
public abstract class ManagedTask implements Runnable {
    private Thread mThread;
    private boolean mFinished;
    private List<OnFinishedListener> mFinishListeners = new ArrayList<>();
    private Object mTaskId;
    private List<OnProgressListener> mProgressListeners = new ArrayList<>();
    private double mProgress = -1;
    private String mProgressMessage = "";
    private boolean mIsStopped = false;
    private List<OnStartListener> mStartListeners = new ArrayList<>();
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
            for(OnStartListener listener:mStartListeners) {
                try {
                    listener.onStart(this);
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
        for(OnFinishedListener listener:mFinishListeners) {
            try {
                listener.onFinished(this);
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
        if(!isFinished()) {
            for(OnProgressListener listener:mProgressListeners) {
                try {
                    listener.onProgress(this, mProgress, mProgressMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sets the listener to be called on progress updates
     * @param listener
     */
    public final void addOnProgressListener(OnProgressListener listener) {
        if(!mProgressListeners.contains(listener) && listener != null) {
            mProgressListeners.add(listener);
            if(!isFinished()) {
                try {
                    listener.onProgress(this, mProgress, mProgressMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Removes the on progress listener
     * @param listener
     */
    public final void removeOnProgressListener(OnProgressListener listener) {
        mProgressListeners.remove(listener);
    }

    /**
     * Sets the listener to be called when the task is finished
     * @param listener
     */
    public final void addOnFinishedListener(OnFinishedListener listener) {
        if(!mFinishListeners.contains(listener) && listener != null) {
            mFinishListeners.add(listener);
            if (isFinished()) {
                try {
                    listener.onFinished(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Removes the on finished listener
     * @param listener
     */
    public final void removeOnFinishedListener(OnFinishedListener listener) {
        mFinishListeners.remove(listener);
    }

    /**
     * Sets the listener to be called when the task starts
     * @param listener
     */
    public final void addOnStartListener(OnStartListener listener) {
        if(!mStartListeners.contains(listener) && listener != null) {
            mStartListeners.add(listener);
        }
    }

    /**
     * Removes the on start listener
     * @param listener
     */
    public final void removeOnStartListener(OnStartListener listener) {
        mStartListeners.remove(listener);
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

    /**
     * Notifies the task that it should stop as soon as possible.
     */
    public final void stop() {
        mIsStopped = true;
    }

    /**
     * Same as stop except it also removes all of the listeners
     */
    public final void destroy() {
        mStartListeners.clear();
        mFinishListeners.clear();
        mProgressListeners.clear();
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
