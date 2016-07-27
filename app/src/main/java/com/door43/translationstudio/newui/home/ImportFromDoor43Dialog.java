package com.door43.translationstudio.newui.home;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TargetTranslationMigrator;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.FeedbackDialog;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.translationstudio.tasks.AdvancedGogsRepoSearchTask;
import com.door43.translationstudio.tasks.CloneRepositoryTask;
import com.door43.translationstudio.tasks.PullTargetTranslationTask;
import com.door43.translationstudio.tasks.RegisterSSHKeysTask;
import com.door43.translationstudio.tasks.SearchGogsRepositoriesTask;
import org.unfoldingword.tools.taskmanager.SimpleTaskWatcher;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import com.door43.util.FileUtilities;
import com.door43.widget.ViewUtil;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.merge.MergeStrategy;
import org.json.JSONException;
import org.json.JSONObject;
import org.unfoldingword.gogsclient.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/10/16.
 */
public class ImportFromDoor43Dialog extends DialogFragment implements SimpleTaskWatcher.OnFinishedListener {
    public static final String TAG = ImportFromDoor43Dialog.class.getSimpleName();
    private static final String STATE_REPOSITORIES = "state_repositories";
    private static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    private static final String STATE_TARGET_TRANSLATION_ID = "state_target_translation_id";
    private static final String STATE_IMPORT_COMPARE_STATUS = "state_import_compare_status";
    private static final String STATE_MERGE_FROM_URL = "state_merge_from_url";
    private static final String STATE_MANUAL_MERGE = "state_manual_merge";
    public static final String STATE_CLONE_URL = "state_clone_url";
    public static final String STATE_QUICK_LOAD_PATH = "state_quick_load_path";
    private SimpleTaskWatcher taskWatcher;
    private RestoreFromCloudAdapter adapter;
    private Translator translator;
    private List<Repository> repositories = new ArrayList<>();
    private String mClonelUrl;
    private File cloneDestDir;
    private EditText repoEditText;
    private EditText userEditText;
    private String mQuickLoadPath;
    private String targetTranslationId;
    private eDialogShown mDialogShown = eDialogShown.NONE;
    private boolean mMergeFromSpecificUrl = false;
    private boolean mManualMerge = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_import_from_door43, container, false);

        this.taskWatcher = new SimpleTaskWatcher(getActivity(), R.string.loading);
        this.taskWatcher.setOnFinishedListener(this);

        this.translator = App.getTranslator();

        Button dismissButton = (Button) v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskWatcher.stop();
                SearchGogsRepositoriesTask task = (SearchGogsRepositoriesTask) TaskManager.getTask(SearchGogsRepositoriesTask.TASK_ID);
                if (task != null) {
                    task.stop();
                    TaskManager.cancelTask(task);
                    TaskManager.clearTask(task);
                }
                dismiss();
            }
        });
        userEditText = (EditText)v.findViewById(R.id.username);
        repoEditText = (EditText)v.findViewById(R.id.translation_id);

        v.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userQuery = userEditText.getText().toString();
                String repoQuery = repoEditText.getText().toString();

                App.closeKeyboard(getActivity()); // this doesn't seem to work here

                Profile profile = App.getProfile();
                if(profile != null && profile.gogsUser != null) {
                    AdvancedGogsRepoSearchTask task = new AdvancedGogsRepoSearchTask(profile.gogsUser, userQuery, repoQuery, 50);
                    TaskManager.addTask(task, AdvancedGogsRepoSearchTask.TASK_ID);
                    taskWatcher.watch(task);
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), getResources().getString(R.string.login_doo43), Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                    dismiss();
                }
            }
        });

        ListView list = (ListView) v.findViewById(R.id.list);
        adapter = new RestoreFromCloudAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Repository repo = adapter.getItem(position);
                String repoName = repo.getFullName().replace("/", "-");
                cloneDestDir = getTempCloneDirectory(repoName);
                mClonelUrl = repo.getHtmlUrl();
                doCloneRepository();
            }
        });

        // restore state
        if(savedInstanceState != null) {
            mDialogShown = eDialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, eDialogShown.NONE.getValue()));
            targetTranslationId = savedInstanceState.getString(STATE_TARGET_TRANSLATION_ID, null);
            mMergeFromSpecificUrl = savedInstanceState.getBoolean(STATE_MERGE_FROM_URL, false);
            mClonelUrl = savedInstanceState.getString(STATE_CLONE_URL, null);
            mManualMerge = savedInstanceState.getBoolean(STATE_MANUAL_MERGE, false);
            mQuickLoadPath = savedInstanceState.getString(STATE_QUICK_LOAD_PATH, null);

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
        } else {
            if(mQuickLoadPath != null) { // check if we already have a project to merge
                mClonelUrl = mQuickLoadPath;
                notifyMergeConflictDelayed(targetTranslationId);
            }
        }

        // connect to existing task
        AdvancedGogsRepoSearchTask searchTask = (AdvancedGogsRepoSearchTask) TaskManager.getTask(AdvancedGogsRepoSearchTask.TASK_ID);
        CloneRepositoryTask cloneTask = (CloneRepositoryTask) TaskManager.getTask(CloneRepositoryTask.TASK_ID);
        if (searchTask != null) {
            taskWatcher.watch(searchTask);
        } else if (cloneTask != null) {
            taskWatcher.watch(cloneTask);
        }

        restoreDialogs();
        return v;
    }

    /**
     * restore the dialogs that were displayed before rotation
     */
    private void restoreDialogs() {

        //recreate dialog last shown
        switch(mDialogShown) {
            case IMPORT_FAILED:
                notifyImportFailed();
                break;

            case AUTH_FAILURE:
                showAuthFailure();
                break;

            case MERGE_FAILED:
                TargetTranslation targetTranslation = App.getTranslator().getTargetTranslation(targetTranslationId);
                notifyMergeFailed(targetTranslation);
                break;

            case MERGE_CONFLICT:
                notifyMergeConflictDelayed(targetTranslationId);
                break;

            case NONE:
                break;

            default:
                Logger.e(TAG,"Unsupported restore dialog: " + mDialogShown.toString());
                break;
        }
    }

    /**
     * displays the merge conflict dialog but delays for main dialog to finish initializing.
     * @param targetTranslationId
     */
    private void notifyMergeConflictDelayed(final String targetTranslationId) {
        hideKeyboard();

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                TargetTranslation targetTranslation = App.getTranslator().getTargetTranslation(targetTranslationId);
                doRecursiveMerge(targetTranslation);
//                showMergeConflict(targetTranslation);
            }
        });
    }

    private void hideKeyboard() {
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN); // stop annoying keyboard from popping up
    }

    /**
     * this will skip the search when dialog opens and go direct to merge of target
     * @param targetTranslationID
     */
    public void doQuickLoad(String targetTranslationID) {
        this.targetTranslationId = targetTranslationID;
        Profile profile = App.getProfile();
        if(profile != null && profile.gogsUser != null) {
            String server = App.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, App.context().getResources().getString(R.string.pref_default_git_server));
            mQuickLoadPath = server + ":" + profile.gogsUser.getUsername() + "/" + targetTranslationID;
            cloneDestDir = getTempCloneDirectory(targetTranslationID);
        } else {
            Logger.e(ImportFromDoor43Dialog.class.getSimpleName(), "doQuickLoad failed, no gogsUser");
        }
    }

    /**
     * generate a temp folder to load project into for later merge
     * @param repoName
     * @return
     */
    private File getTempCloneDirectory(String repoName) {
        return new File(App.context().getCacheDir(), repoName + System.currentTimeMillis() + "/");
    }

    /**
     * starts task to clone repository
     */
    private void doCloneRepository() {
        CloneRepositoryTask task = new CloneRepositoryTask(mClonelUrl, cloneDestDir);
        taskWatcher.watch(task);
        TaskManager.addTask(task, CloneRepositoryTask.TASK_ID);
    }

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        TaskManager.clearTask(task);

        if (task instanceof AdvancedGogsRepoSearchTask) {
            this.repositories = ((AdvancedGogsRepoSearchTask) task).getRepositories();
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
                    boolean importFailed = false;
                    if (tempTargetTranslation != null) {
                        TargetTranslation existingTargetTranslation = translator.getTargetTranslation(tempTargetTranslation.getId());
                        if (existingTargetTranslation != null) {
                            // merge target translation
                            try {
                                mMergeFromSpecificUrl = true;
                                mClonelUrl = cloneUrl;
                                importFailed = true; // we haven't imported yet
                                showMergeConflict(existingTargetTranslation);
                            } catch (Exception e) {
                                Logger.e(this.getClass().getName(), "Failed to merge the target translation", e);
                                notifyImportFailed();
                                importFailed = true;
                            }
                        } else {
                            // restore the new target translation
                            try {
                                translator.restoreTargetTranslation(tempTargetTranslation);
                            } catch (IOException e) {
                                Logger.e(this.getClass().getName(), "Failed to import the target translation " + tempTargetTranslation.getId(), e);
                                notifyImportFailed();
                                importFailed = true;
                            }
                        }
                    } else {
                        Logger.e(this.getClass().getName(), "Failed to open the online backup");
                        notifyImportFailed();
                        importFailed = true;
                    }
                    FileUtilities.deleteQuietly(tempPath);

                    if(!importFailed) {
                        Handler hand = new Handler(Looper.getMainLooper());
                        hand.post(new Runnable() {
                            @Override
                            public void run() {
                                // todo: terrible hack. We should instead register a listener with the dialog
                                ((HomeActivity) getActivity()).notifyDatasetChanged();

                                Snackbar snack = Snackbar.make(ImportFromDoor43Dialog.this.getView(), R.string.success, Snackbar.LENGTH_SHORT);
                                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                                snack.show();

                                if(mQuickLoadPath != null) {
                                    dismiss();
                                }
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
                    notifyImportFailed();
                }
            }

        } else if(task instanceof RegisterSSHKeysTask) {
            if(((RegisterSSHKeysTask)task).isSuccess()) {
                Logger.i(this.getClass().getName(), "SSH keys were registered with the server");
                doCloneRepository(); // try to clone again
            } else {
                notifyImportFailed();
            }
        } else if (task instanceof PullTargetTranslationTask) {
            PullTargetTranslationTask.Status status = ((PullTargetTranslationTask) task).getStatus();
            TargetTranslation targetTranslation = App.getTranslator().getTargetTranslation(targetTranslationId);
            if (mManualMerge) {
                doManualMerge(targetTranslation);
            } else {
                //  TRICKY: we continue to push for unknown status in case the repo was just created (the missing branch is an error)
                // the pull task will catch any errors
                if (status == PullTargetTranslationTask.Status.UP_TO_DATE
                        || status == PullTargetTranslationTask.Status.UNKNOWN) {
                    Logger.i(this.getClass().getName(), "Changes on the server were synced with " + targetTranslation.getId());

                } else if (status == PullTargetTranslationTask.Status.MERGE_CONFLICTS) {
                    Logger.i(this.getClass().getName(), "The server contains conflicting changes for " + targetTranslation.getId());
                    doManualMerge(targetTranslation);
                }
            }
            this.dismiss();
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
                        notifyImportFailed();
                    }
                }).show();
    }

    public void notifyImportFailed() {
        mDialogShown = eDialogShown.IMPORT_FAILED;
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
        if(targetTranslationId != null) {
            out.putString(STATE_TARGET_TRANSLATION_ID, targetTranslationId);
        }
        out.putBoolean(STATE_MERGE_FROM_URL,  mMergeFromSpecificUrl);
        if(mClonelUrl != null) {
            out.putString(STATE_CLONE_URL, mClonelUrl);
        }
        if(mQuickLoadPath != null) {
            out.putString(STATE_QUICK_LOAD_PATH, mQuickLoadPath);
        }
        out.putBoolean(STATE_MANUAL_MERGE,  mManualMerge);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        taskWatcher.stop();
        super.onDestroy();
    }

    private void showMergeConflict(final TargetTranslation targetTranslation) {
        mDialogShown = eDialogShown.MERGE_CONFLICT;
        this.targetTranslationId = targetTranslation.getId();
        String targetLanguageName = targetTranslation.getTargetLanguageName();
        String projectID = targetTranslation.getProjectId();
        Project project = App.getLibrary().getProject(projectID, targetTranslation.getTargetLanguageName());
        if(project == null) {
            Logger.e(TAG, "invalid project id:" + projectID);
            return;
        }

        String bookName = project.name;
        String message = String.format(getResources().getString(R.string.merge_request),bookName, targetLanguageName);

        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.import_failed_title)
                .setMessage(message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        doRecursiveMerge(targetTranslation);
                    }

                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        ImportFromDoor43Dialog.this.dismiss();
                    }
                }).show();
    }

    /**
      * do pull request with manual merge
     * @param targetTranslation
     */
    private void doRecursiveMerge(TargetTranslation targetTranslation) {
        mManualMerge = true;
        try {
            final String sourceUrl = mMergeFromSpecificUrl ? mClonelUrl : null; // if not a specific url, then will merge from default for target
            pullFromServer(targetTranslation, MergeStrategy.RECURSIVE, sourceUrl);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to merge during import", e);
            notifyMergeFailed(targetTranslation);
        }
    }

    /**
     * Initiate a pull request with specific merge strategy
     * @param targetTranslation
     * @param mergeStrategy
     * @param sourceURL - if null then load from current user and default server
     * @throws Exception
     */
    private void pullFromServer(TargetTranslation targetTranslation, MergeStrategy mergeStrategy, String sourceURL) throws Exception {
        Git git = targetTranslation.getRepo().getGit();
        targetTranslation.commitSync();

        // pull from server
        PullTargetTranslationTask pullTask = new PullTargetTranslationTask(targetTranslation, mergeStrategy, sourceURL);
        taskWatcher.watch(pullTask);
        TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
    }

    private void doManualMerge(TargetTranslation targetTranslation) {
        // ask parent activity to navigate to a new activity
        Intent intent = new Intent(getActivity(), TargetTranslationActivity.class);
        Bundle args = new Bundle();
        args.putString(App.EXTRA_TARGET_TRANSLATION_ID, targetTranslation.getId());
        // TODO: 4/20/16 it woulid be nice to navigate directly to the first conflict
//                args.putString(App.EXTRA_CHAPTER_ID, chapterId);
//                args.putString(App.EXTRA_FRAME_ID, frameId);
        args.putString(App.EXTRA_VIEW_MODE, TranslationViewMode.REVIEW.toString());
        intent.putExtras(args);
        startActivity(intent);
        getActivity().finish();
    }

    /**
     * Displays a dialog to the user indicating the merge failed.
     * Includes an option to submit a bug report
     * @param targetTranslation
     */
    private void notifyMergeFailed(final TargetTranslation targetTranslation) {
        mDialogShown = eDialogShown.MERGE_FAILED;

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.error)
                .setMessage(R.string.import_failed_title)
                .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                    }
                })
                .setNeutralButton(R.string.menu_bug, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        showFeedbackDialog(targetTranslation);
                    }
                }).show();
    }

    private void showFeedbackDialog(TargetTranslation targetTranslation) {
        Project project = App.getLibrary().getProject(targetTranslation.getProjectId(), "en");

        FeedbackDialog feebackDlg = new FeedbackDialog();
        Bundle args = new Bundle();
        String message = "Failed to publish the translation of " +
                project.name + " into " +
                targetTranslation.getTargetLanguageName()
                + ".\ntargetTranslation: " + targetTranslation.getId() +
                "\n--------\n\n";
        args.putString(FeedbackDialog.ARG_MESSAGE, message);
        feebackDlg.setArguments(args);
        showDialogFragment(feebackDlg, "feedback-dialog");
    }

    /**
     * this is to fix old method which when called in onResume() would create a
     * second dialog overlaying the first.  The first was actually not removed.
     * Doing a commit after the remove() and starting a second FragmentTransaction
     * seems to fix the duplicate dialog bug.
     *
     * @param dialog
     * @param tag
     */
    private void showDialogFragment(android.app.DialogFragment dialog, String tag) {
        FragmentTransaction backupFt = getFragmentManager().beginTransaction();
        Fragment backupPrev = getFragmentManager().findFragmentByTag(tag);
        if (backupPrev != null) {
            backupFt.remove(backupPrev);
            backupFt.commit(); // apply the remove
            backupFt = getFragmentManager().beginTransaction(); // start a new transaction
        }
        backupFt.addToBackStack(null);

        dialog.show(backupFt, tag);
    }

    /**
     * for keeping track which dialog is being shown for orientation changes (not for DialogFragments)
     */
    public enum eDialogShown {
        NONE(0),
        IMPORT_FAILED(1),
        AUTH_FAILURE(2),
        MERGE_FAILED(3),
        MERGE_CONFLICT(4);

        private int _value;

        eDialogShown(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
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
