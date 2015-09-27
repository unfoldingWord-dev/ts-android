package com.door43.util.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * This class performs a lot of the grunt work for handling managed tasks
 * The progress is displayed in a dialog
 */
public class GenericTaskWatcher implements ManagedTask.OnFinishedListener, ManagedTask.OnProgressListener, ManagedTask.OnStartListener, DialogInterface.OnCancelListener, ManagedTask.OnIdChangedListener {

    private final Context mContext;
    private final int mTitleRes;
    private final int mIconRes;
    private ProgressDialog mProgressDialog;
    private Object mTaskId;
    private boolean mWatching = false;
    private OnFinishedListener mOnFinishedListener;
    private OnCanceledListener mOnCanceledListener;

    /**
     * Create a new task handler
     * @param context
     * @param titleRes
     */
    public GenericTaskWatcher(Context context, int titleRes) {
        mContext = context;
        mTitleRes = titleRes;
        mIconRes = 0;
    }

    /**
     * Create a new task handler
     * @param context
     * @param titleRes
     * @param iconRes
     */
    public GenericTaskWatcher(Context context, int titleRes, int iconRes) {
        mContext = context;
        mTitleRes = titleRes;
        mIconRes = iconRes;
    }

    /**
     * Sets the listener to be fired when the task finishes
     * @param listener
     */
    public void setOnFinishedListener(OnFinishedListener listener) {
        mOnFinishedListener = listener;
    }

    /**
     * Sets the listener to be fired when the task is canceled
     * If not set then the dialog will not be cancelable
     * @param listener
     */
    public void setOnCanceledListener(OnCanceledListener listener) {
        mOnCanceledListener = listener;
    }

    /**
     * Checks if the task watcher is currently watching a task
     * @return
     */
    public boolean isWatching() {
        return mWatching;
    }

    /**
     * Begin watching a task.
     * If you want to begin watching another task you should call disconnect() first
     * WARNING: this method expects the task with the given id to have already been added to the task manager
     * @param taskId
     */
    public boolean watch(Object taskId) {
        if(!mWatching) {
            if(taskId != null) {
                ManagedTask task = TaskManager.getTask(taskId);
                return connectTask(task);
            } else {
                Log.w(null, "You cannot watch a task with id NULL");
                return false;
            }
        } else {
            Log.w(null, "The watcher is already watching a task. Did you forget to call stop() first?");
            return false;
        }
    }

    /**
     * Begin watching a task.
     * If you want to begin watching another task you should call disconnect() first
     * @param task
     */
    public boolean watch(ManagedTask task) {
        if(!mWatching) {
            return connectTask(task);
        } else {
            Log.w(null, "The watcher is already watching a task. Did you forget to call stop() first?");
            return false;
        }
    }

    private boolean connectTask(ManagedTask task) {
        if(task != null) {
            mWatching = true;

            // get task id
            task.addOnIdChangedListener(this);

            // task operation listeners
            task.addOnFinishedListener(this);
            task.addOnProgressListener(this);
            task.addOnStartListener(this);
            return true;
        } else {
            Log.w(null, "The task does not exist");
            return false;
        }
    }

    /**
     * Disconnects the watcher from the task.
     * After closing you may begin watching another task
     */
    public void stop() {
        if(mTaskId != null) {
            ManagedTask task = TaskManager.getTask(mTaskId);
            if(task != null) {
                task.removeOnIdChangedListener(this);
                task.removeOnFinishedListener(this);
                task.removeOnProgressListener(this);
                task.removeOnStartListener(this);
            }
            mTaskId = null;
        }
        if(mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mWatching = false;
    }

    @Override
    public void onFinished(final ManagedTask task) {
        TaskManager.clearTask(task);
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if(mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                if(mOnFinishedListener != null) {
                    mOnFinishedListener.onFinished(task);
                }
            }
        });
    }

    @Override
    public void onProgress(final ManagedTask task, final double progress, final String message, boolean secondary) {
        if(!task.isFinished()) {
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if(task.isFinished()) {
                        if(mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                        return;
                    }
                    if(mProgressDialog == null) {
                        mProgressDialog = new ProgressDialog(mContext);
                        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        if(mOnCanceledListener != null) {
                            mProgressDialog.setCancelable(true);
                            mProgressDialog.setOnCancelListener(GenericTaskWatcher.this);
                        } else {
                            mProgressDialog.setCancelable(false);
                        }
                        mProgressDialog.setCanceledOnTouchOutside(false);
                        mProgressDialog.setMax(task.maxProgress());
                        if(mIconRes != 0) {
                            mProgressDialog.setIcon(mIconRes);
                        }
                        mProgressDialog.setTitle(mTitleRes);
                        mProgressDialog.setMessage("");
                    }
                    if(!mProgressDialog.isShowing()) {
                        mProgressDialog.show();
                    }
                    if(progress == -1) {
                        mProgressDialog.setIndeterminate(true);
                        mProgressDialog.setProgress(mProgressDialog.getMax());
                    } else {
                        mProgressDialog.setIndeterminate(false);
                        mProgressDialog.setProgress((int)Math.ceil(progress * mProgressDialog.getMax()));
                    }
                    if(!message.isEmpty()) {
                        mProgressDialog.setMessage(message);
                    } else {
                        mProgressDialog.setMessage("");
                    }
                }
            });
        }
    }

    @Override
    public void onStart(ManagedTask task) {
        // This is a stub. This isn't used anywhere yet
    }

    /**
     * The task dialog has been canceled
     * @param dialog
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        ManagedTask task = TaskManager.getTask(mTaskId);
        if(task != null) {
            TaskManager.cancelTask(task);
            TaskManager.clearTask(task);
        }
        mOnCanceledListener.onCanceled(task);
    }

    /**
     * The task id has been set
     * @param task
     */
    @Override
    public void onChanged(ManagedTask task) {
        mTaskId = task.getTaskId();
    }

    public interface OnCanceledListener {
        void onCanceled(ManagedTask task);
    }

    public interface OnFinishedListener {
        void onFinished(ManagedTask task);
    }
}
