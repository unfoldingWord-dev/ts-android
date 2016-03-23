package com.door43.translationstudio.newui.home;

import android.app.Activity;
import android.app.Application;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TargetTranslationMigrator;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.tasks.CloneTargetTranslationTask;
import com.door43.translationstudio.tasks.GetCloudBackupsTask;
import com.door43.translationstudio.tasks.KeyRegistration;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by joel on 11/6/2015.
 */
public class RestoreFromCloudDialog extends DialogFragment implements GenericTaskWatcher.OnFinishedListener {
    private static final String STATE_TARGET_TRANSLATIONS = "state_target_translations";
//    private static final String STATE_RESTORE_HEAD = "state_restore_head";
    private GenericTaskWatcher taskWatcher;
    private RestoreFromCloudAdapter adapter;
    private String[] targetTranslationSlugs = new String[0];
    private Translator translator;
//    private Library library;
//    private boolean restoreHEAD;
    private OnNewKeyRegistrationListener mListener;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_restore_from_cloud, container, false);

        this.taskWatcher = new GenericTaskWatcher(getActivity(), R.string.loading);
        this.taskWatcher.setOnFinishedListener(this);

        this.translator = AppContext.getTranslator();
//        this.library = AppContext.getLibrary();

//        if(savedInstanceState != null) {
//            restoreHEAD = savedInstanceState.getBoolean(STATE_RESTORE_HEAD, false);
//        }

        Button dismissButton = (Button)v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskWatcher.stop();
                GetCloudBackupsTask task = (GetCloudBackupsTask)TaskManager.getTask(GetCloudBackupsTask.TASK_ID);
                if(task != null) {
                    task.stop();
                    TaskManager.cancelTask(task);
                    TaskManager.clearTask(task);
                }
                dismiss();
            }
        });

        ListView list = (ListView) v.findViewById(R.id.list);
        adapter = new RestoreFromCloudAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String targetTranslationSlug = adapter.getItem(position);
                File tempDir = new File(AppContext.context().getCacheDir(), targetTranslationSlug + System.currentTimeMillis() + "/");
                tempDir.mkdirs();
                CloneTargetTranslationTask task = new CloneTargetTranslationTask(targetTranslationSlug, tempDir);
                taskWatcher.watch(task);
                TaskManager.addTask(task, CloneTargetTranslationTask.TASK_ID);
            }
        });

        // connect to existing task
        GetCloudBackupsTask checkTask = (GetCloudBackupsTask)TaskManager.getTask(GetCloudBackupsTask.TASK_ID);
        CloneTargetTranslationTask cloneTask = (CloneTargetTranslationTask)TaskManager.getTask(CloneTargetTranslationTask.TASK_ID);
        if(checkTask != null) {
            taskWatcher.watch(checkTask);
            // display loading icon
        } else if(cloneTask != null) {
            taskWatcher.watch(cloneTask);
        }

        if(savedInstanceState == null) {
            if(cloneTask == null && checkTask == null) {
                // start task
                checkTask = new GetCloudBackupsTask();
                taskWatcher.watch(checkTask);
                TaskManager.addTask(checkTask, GetCloudBackupsTask.TASK_ID);
            }
        } else if (checkTask == null) {
            // load existing data
            targetTranslationSlugs = savedInstanceState.getStringArray(STATE_TARGET_TRANSLATIONS);
            adapter.setTargetTranslations(targetTranslationSlugs);
        }

        return v;
    }

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        TaskManager.clearTask(task);

        if(task instanceof GetCloudBackupsTask) {
            if(!((GetCloudBackupsTask)task).uploadAuthFailure()) {

                targetTranslationSlugs = ((GetCloudBackupsTask) task).getTargetTranslationSlugs();
                Handler hand = new Handler(Looper.getMainLooper());
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        if (targetTranslationSlugs.length > 0) {
                            adapter.setTargetTranslations(targetTranslationSlugs);
                        } else {
                            notifyReadOnlineBackupsFailed(getActivity());
                        }
                    }
                });
            } else {
                dismiss();
                showAuthFailure();
            }
        } else if(task instanceof CloneTargetTranslationTask) {
            File tempPath = ((CloneTargetTranslationTask)task).getLocalPath();
            tempPath = TargetTranslationMigrator.migrate(tempPath);
            TargetTranslation tempTargetTranslation = TargetTranslation.open(tempPath);
            if(tempTargetTranslation != null) {
                TargetTranslation existingTargetTranslation = translator.getTargetTranslation(tempTargetTranslation.getId());
                // create orphaned backup of existing target translation
                if(existingTargetTranslation != null) {
                    try {
                        AppContext.backupTargetTranslation(existingTargetTranslation, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // restore the new target translation
                try {
                    translator.restoreTargetTranslation(tempTargetTranslation);
                } catch (IOException e) {
                    Logger.e(this.getClass().getName(), "Failed to import the target translation " + tempTargetTranslation.getId(), e);
                }
            } else {
                Logger.e(this.getClass().getName(), "Failed to open the online backup");
            }
            FileUtils.deleteQuietly(tempPath);

            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    // todo: terrible hack.
                    ((HomeActivity) getActivity()).notifyDatasetChanged();
                }
            });
        }
    }

    public void notifyReadOnlineBackupsFailed(Activity activity) {
        FragmentManager fm = this.getFragmentManager();
        if(null == fm) {
            fm = activity.getFragmentManager();
        }
        CustomAlertDialog.Create(activity)
                .setTitle(R.string.import_from_online)
                .setMessage(R.string.no_backups_online)
                .setNeutralButton(R.string.dismiss, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                })
                .show(fm, "NoBackups");
    }

    public void handleRegistrationResults(final Activity activity, boolean success) {
        if(success) {
            if(mListener != null) {
                mListener.onNewKeyRegistration();
            }
        } else {
            CustomAlertDialog.Create(activity)
                    .setTitle(R.string.import_from_online)
                    .setMessage(R.string.registration_failure)
                    .setNeutralButton(R.string.dismiss, null)
                    .show("RegistrationResults");
        }
    }

    public void showAuthFailure() {
        final Activity activity = getActivity();
        final CustomAlertDialog dlg = CustomAlertDialog.Create(activity);
        dlg.setTitle(R.string.import_from_online)
            .setMessage(R.string.auth_failure_retry)
            .setAutoDismiss(false)
            .setPositiveButton(R.string.yes, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doKeyRegistration(activity, dlg);
                }
            })
            .setNegativeButton(R.string.no, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notifyReadOnlineBackupsFailed(activity);
                    dlg.dismiss();
                }
            })
            .show("PubAuthFailure");
    }

    public void doKeyRegistration(final Activity activity, final CustomAlertDialog dlg) {

        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle(R.string.registering_keys);
        progressDialog.setMessage("");
        progressDialog.show();

        AppContext.context().generateKeys();
        final Handler hand = new Handler(Looper.getMainLooper());
        Thread thread = new Thread() {
            @Override
            public void run() {
                KeyRegistration keyReg = new KeyRegistration();
                keyReg.registerKeys(new KeyRegistration.OnRegistrationFinishedListener() {
                    @Override
                    public void onRestoreFinish(final boolean registrationSuccess) {
                        hand.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                dlg.dismiss();
                                handleRegistrationResults(activity, registrationSuccess);
                            }
                        });
                    }
                });
            }
        };
        thread.start();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putStringArray(STATE_TARGET_TRANSLATIONS, this.targetTranslationSlugs);
//        out.putBoolean(STATE_RESTORE_HEAD, this.restoreHEAD);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        taskWatcher.stop();
        super.onDestroy();
    }

    /**
     * Sets the listener that will be called when new keys have been successfully registered.
     * @param listener
     */
    public void setNewKeyRegistrationListener(OnNewKeyRegistrationListener listener) {
        mListener = listener;
    }

    public static interface OnNewKeyRegistrationListener {
        public void onNewKeyRegistration();
    }
}
