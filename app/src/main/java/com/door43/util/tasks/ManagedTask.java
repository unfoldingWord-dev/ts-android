package com.door43.util.tasks;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is the base class for creating a managed thread.
 * Managed threads are designed to be used in the ThreadManager.
 * Managing threads allows you to keep track of threads throughout your application during activity de/re-construction
 */
public abstract class ManagedTask implements Runnable {
    private Thread mThread;
    private boolean mFinished;
    private List mFinishListeners = Collections.synchronizedList(new ArrayList<>());
    private Object mTaskId;
    private List mProgressListeners = Collections.synchronizedList(new ArrayList<>());
    private double mProgress = -1;
    private String mProgressMessage = "";
    private boolean mIsStopped = false;
    private List mStartListeners = Collections.synchronizedList(new ArrayList<>());
    private boolean mIsRunning = false;
    private List mOnIdChangedListeners = Collections.synchronizedList(new ArrayList<>());
    protected int mThreadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND;
    private Bundle mArgs = null;

    public ManagedTask ManagedTask() {
        return this;
    }

    /**
     * Passes optional arguments to the task
     * @param args
     */
    public void setArgs(Bundle args) {
        mArgs = args;
    }

    /**
     * Returns arguments from the task
     * @return
     */
    public Bundle getArgs() {
        return mArgs;
    }

    /**
     * Changes the thread priority. e.g. android.os.Process.THREAD_PRIORITY_BACKGROUND
     * @param priority the priority of this task default is THREAD_PRIORITY_BACKGROUND
     */
    protected final void setThreadPriority(int priority) {
        mThreadPriority = priority;
    }

    @Override
    public final void run() {
        android.os.Process.setThreadPriority(mThreadPriority);
        mThread = Thread.currentThread();
        try {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            mIsRunning = true;
            synchronized (mStartListeners) {
                Iterator<OnStartListener> it = mStartListeners.iterator();
                while(it.hasNext()) {
                    try {
                        it.next().onStart(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
        synchronized (mFinishListeners) {
            Iterator<OnFinishedListener> it = mFinishListeners.iterator();
            while(it.hasNext()) {
                try {
                    it.next().onFinished(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        onStop();
    }

    public final void run(Thread thread) {
        mThread = thread;
        start();
    }

    /**
     * Causes the task to sleep
     * @param time The time to sleep in milliseconds.
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
     * Utility method to chain several tasks together
     * @param task
     */
    public ManagedTask then(ManagedTask task) {
        delegate(task);
        return task;
    }

    /**
     * Executes another task on the same thread.
     * Progress listeners are inherited from the delegating task
     * @param task
     */
    protected final void delegate(ManagedTask task) {
        synchronized (mProgressListeners) {
            Iterator<OnProgressListener> it = mProgressListeners.iterator();
            while(it.hasNext()) {
                task.addOnProgressListener(it.next());
            }
        }
        task.run(mThread);
    }

    /**
     * Sets the task id
     * @param id
     */
    public final void setTaskId(Object id) {
        mTaskId = id;
        synchronized (mOnIdChangedListeners) {
            Iterator<OnIdChangedListener> it = mOnIdChangedListeners.iterator();
            while(it.hasNext()) {
                try {
                    it.next().onChanged(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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
            synchronized (mProgressListeners) {
                Iterator<OnProgressListener> it = mProgressListeners.iterator();
                while(it.hasNext()) {
                    try {
                        it.next().onProgress(this, mProgress, mProgressMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
     * Sets the listener to be called when the task's id changes
     * This is useful when you need to begin working with a task object before it has been
     * loaded into the task manager
     * @param listener
     */
    public final void addOnIdChangedListener(OnIdChangedListener listener) {
        if(!mOnIdChangedListeners.contains(listener) && listener != null) {
            mOnIdChangedListeners.add(listener);
            try {
                listener.onChanged(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Removes the on id changed listener
     * @param listener
     */
    public final void removeOnIdChangedListener(OnIdChangedListener listener) {
        mOnIdChangedListeners.remove(listener);
    }

    /**
     * Removes all the on id changed listener
     */
    public final void removeAllOnIdChangedListener() {
        mOnIdChangedListeners.clear();
    }

    /**
     * Removes the on progress listener
     * @param listener
     */
    public final void removeOnProgressListener(OnProgressListener listener) {
        mProgressListeners.remove(listener);
    }

    /**
     * Removes all the on progress listener
     */
    public final void removeAllOnProgressListener() {
        mProgressListeners.clear();
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
     * Removes all the on finished listener
     */
    public final void removeAllOnFinishedListener() {
        mFinishListeners.clear();
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
     * Removes all the on start listener
     */
    public final void removeAllOnStartListener() {
        mStartListeners.clear();
    }

    /**
     * Perform any threadable tasks here
     */
    public abstract void start();

    /**
     * Returns the maximum progress threshold
     * Useful for setting up progress bars
     * @return
     */
    public int maxProgress() {
        return 100;
    }

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
        mIsStopped = true;
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
        if(mThread != null) {
            return mThread.isInterrupted() || mIsStopped;
        } else {
            return mIsStopped;
        }
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

    public interface OnIdChangedListener {
        void onChanged(ManagedTask task);
    }
}
