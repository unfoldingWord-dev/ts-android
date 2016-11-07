package com.door43.translationstudio.ui.home;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TargetTranslationMigrator;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.tasks.CloneRepositoryTask;
import com.door43.translationstudio.tasks.GetUserRepositoriesTask;
import com.door43.translationstudio.tasks.RegisterSSHKeysTask;
import org.unfoldingword.tools.taskmanager.SimpleTaskWatcher;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import com.door43.util.FileUtilities;
import com.door43.widget.ViewUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.unfoldingword.gogsclient.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 11/6/2015.
 */
public class RestoreFromDoor43Dialog extends DialogFragment implements SimpleTaskWatcher.OnFinishedListener {
    public static final String TAG = RestoreFromDoor43Dialog.class.getSimpleName();
    private static final String STATE_REPOSITORIES = "state_repositories";
    private static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    private SimpleTaskWatcher taskWatcher;
    private TranslationRepositoryAdapter adapter;
    private Translator translator;
    private List<Repository> repositories = new ArrayList<>();
    private String cloneSSHUrl;
    private File cloneDestDir;
    private eDialogShown mDialogShown = eDialogShown.NONE;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_restore_from_door43, container, false);

        this.taskWatcher = new SimpleTaskWatcher(getActivity(), R.string.loading);
        this.taskWatcher.setOnFinishedListener(this);

        this.translator = App.getTranslator();

        Button dismissButton = (Button) v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskWatcher.stop();
                GetUserRepositoriesTask task = (GetUserRepositoriesTask) TaskManager.getTask(GetUserRepositoriesTask.TASK_ID);
                if (task != null) {
                    task.stop();
                    TaskManager.cancelTask(task);
                    TaskManager.clearTask(task);
                }
                dismiss();
            }
        });

        ListView list = (ListView) v.findViewById(R.id.list);
        adapter = new TranslationRepositoryAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Repository repo = adapter.getItem(position);
                String repoName = repo.getFullName().replace("/", "-");
                cloneDestDir = new File(App.context().getCacheDir(), repoName + System.currentTimeMillis() + "/");
                cloneSSHUrl = repo.getSshUrl();
                CloneRepositoryTask task = new CloneRepositoryTask(cloneSSHUrl, cloneDestDir);
                taskWatcher.watch(task);
                TaskManager.addTask(task, CloneRepositoryTask.TASK_ID);
            }
        });

        // restore state
        if(savedInstanceState != null) {
            String[] repoJsonArray = savedInstanceState.getStringArray(STATE_REPOSITORIES);
            if(repoJsonArray != null) {
                for (String json : repoJsonArray) {
                    try {
                        Repository repo = Repository.fromJSON(new JSONObject(json));
                        if (json != null) {
                            repositories.add(repo);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                adapter.setRepositories(repositories);
            }
            mDialogShown = eDialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, eDialogShown.NONE.getValue()));
        }

        // connect to existing task
        GetUserRepositoriesTask reposTask = (GetUserRepositoriesTask) TaskManager.getTask(GetUserRepositoriesTask.TASK_ID);
        CloneRepositoryTask cloneTask = (CloneRepositoryTask) TaskManager.getTask(CloneRepositoryTask.TASK_ID);
        if (reposTask != null) {
            taskWatcher.watch(reposTask);
        } else if (cloneTask != null) {
            taskWatcher.watch(cloneTask);
        } else if(repositories.size() == 0) {
            // start task
            reposTask = new GetUserRepositoriesTask();
            taskWatcher.watch(reposTask);
            TaskManager.addTask(reposTask, GetUserRepositoriesTask.TASK_ID);
        }

        return v;
    }

    @Override
    public void onResume() {
        restoreDialogs();
        super.onResume();
    }

    /**
     * restore the dialogs that were displayed before rotation
     */
    private void restoreDialogs() {
        //recreate dialog last shown
        switch(mDialogShown) {
            case RESTORE_FAILED:
                notifyRestoreFailed();
                break;

            case AUTH_FAILURE:
                showAuthFailure();
                break;

            case NONE:
                break;

            default:
                Logger.e(TAG,"Unsupported restore dialog: " + mDialogShown.toString());
                break;
        }
    }

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        TaskManager.clearTask(task);

        if (task instanceof GetUserRepositoriesTask) {
            this.repositories = ((GetUserRepositoriesTask) task).getRepositories();
            adapter.setRepositories(repositories);
        } else if (task instanceof CloneRepositoryTask) {
            if (!task.isCanceled()) {
                CloneRepositoryTask.Status status = ((CloneRepositoryTask)task).getStatus();
                File tempPath = ((CloneRepositoryTask) task).getDestDir();
                String cloneUrl = ((CloneRepositoryTask) task).getCloneUrl();

                if(status == CloneRepositoryTask.Status.SUCCESS) {
                    Logger.i(this.getClass().getName(), "Repository cloned from " + cloneUrl);
                    tempPath = TargetTranslationMigrator.migrate(tempPath);
                    TargetTranslation tempTargetTranslation = TargetTranslation.open(tempPath);
                    boolean restoreFailed = false;
                    if (tempTargetTranslation != null) {
                        TargetTranslation existingTargetTranslation = translator.getTargetTranslation(tempTargetTranslation.getId());
                        // create orphaned backup of existing target translation
                        if (existingTargetTranslation != null) {
                            try {
                                App.backupTargetTranslation(existingTargetTranslation, true);
                            } catch (Exception e) {
                                Logger.e(this.getClass().getName(), "Failed to backup the target translation", e);
                            }
                        }

                        // restore the new target translation
                        try {
                            translator.restoreTargetTranslation(tempTargetTranslation);
                        } catch (IOException e) {
                            Logger.e(this.getClass().getName(), "Failed to import the target translation " + tempTargetTranslation.getId(), e);
                            notifyRestoreFailed();
                            restoreFailed = true;
                        }
                    } else {
                        Logger.e(this.getClass().getName(), "Failed to open the online backup");
                        notifyRestoreFailed();
                        restoreFailed = true;
                    }
                    FileUtilities.deleteQuietly(tempPath);

                    if(!restoreFailed) {
                        Handler hand = new Handler(Looper.getMainLooper());
                        hand.post(new Runnable() {
                            @Override
                            public void run() {
                                // todo: terrible hack. We should instead register a listener with the dialog
                                ((HomeActivity) getActivity()).notifyDatasetChanged();

                                Snackbar snack = Snackbar.make(RestoreFromDoor43Dialog.this.getView(), R.string.success, Snackbar.LENGTH_SHORT);
                                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                                snack.show();
                            }
                        });
                    }
                } else if(status == CloneRepositoryTask.Status.AUTH_FAILURE) {
                    Logger.i(this.getClass().getName(), "Authentication failed");
                    // if we have already tried ask the user if they would like to try again
                    if(App.hasSSHKeys()) {
                        showAuthFailure();
                        return;
                    }

                    RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(false);
                    taskWatcher.watch(keyTask);
                    TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
                } else {
                    notifyRestoreFailed();
                }
            }
        } else if(task instanceof RegisterSSHKeysTask) {
            if(((RegisterSSHKeysTask)task).isSuccess()) {
                Logger.i(this.getClass().getName(), "SSH keys were registered with the server");
                // try to clone again
                CloneRepositoryTask pullTask = new CloneRepositoryTask(cloneSSHUrl, cloneDestDir);
                taskWatcher.watch(pullTask);
                TaskManager.addTask(pullTask, CloneRepositoryTask.TASK_ID);
            } else {
                notifyRestoreFailed();
            }
        }
    }

    public void showAuthFailure() {
        mDialogShown = eDialogShown.AUTH_FAILURE;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.error).setMessage(R.string.auth_failure_retry)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(true);
                        taskWatcher.watch(keyTask);
                        TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        notifyRestoreFailed();
                    }
                })
                .show();
    }

    public void notifyRestoreFailed() {
        mDialogShown = eDialogShown.RESTORE_FAILED;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.error)
                .setMessage(R.string.restore_failed)
                .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                    }
                })
                .show();
    }


    @Override
    public void onSaveInstanceState(Bundle out) {
        List<String> repoJsonList = new ArrayList<>();
        for (Repository r : repositories) {
            repoJsonList.add(r.toJSON().toString());
        }
        out.putStringArray(STATE_REPOSITORIES, repoJsonList.toArray(new String[repoJsonList.size()]));
        out.putInt(STATE_DIALOG_SHOWN, mDialogShown.getValue());
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        taskWatcher.stop();
        super.onDestroy();
    }

    /**
     * for keeping track which dialog is being shown for orientation changes (not for DialogFragments)
     */
    public enum eDialogShown {
        NONE(0),
        RESTORE_FAILED(1),
        AUTH_FAILURE(2),
        MERGE_FAILED(3),
        MERGE_CONFLICT(4);

        private int value;

        eDialogShown(int Value) {
            this.value = Value;
        }

        public int getValue() {
            return value;
        }

        public static eDialogShown fromInt(int i) {
            for (eDialogShown b : eDialogShown.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }
}