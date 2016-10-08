package com.door43.translationstudio.newui.home;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import org.eclipse.jgit.merge.MergeStrategy;
import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.resourcecontainer.Project;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.EventBuffer;
import com.door43.translationstudio.ProfileActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;

import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.Door43LoginDialog;
import com.door43.translationstudio.newui.library.ServerLibraryActivity;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.newtranslation.NewTargetTranslationActivity;
import com.door43.translationstudio.newui.FeedbackDialog;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.translationstudio.rendering.MergeConflictHandler;
import com.door43.translationstudio.tasks.ExamineImportsForCollisionsTask;
import com.door43.translationstudio.tasks.ImportProjectsTask;
import org.unfoldingword.tools.taskmanager.SimpleTaskWatcher;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import com.door43.translationstudio.tasks.PullTargetTranslationTask;
import com.door43.translationstudio.tasks.RegisterSSHKeysTask;
import com.door43.util.FileUtilities;
import com.door43.widget.ViewUtil;


import java.io.File;

public class HomeActivity extends BaseActivity implements SimpleTaskWatcher.OnFinishedListener, WelcomeFragment.OnCreateNewTargetTranslation, TargetTranslationListFragment.OnItemClickListener, EventBuffer.OnEventListener {
    private static final int NEW_TARGET_TRANSLATION_REQUEST = 1;
    public static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    public static final String STATE_DIALOG_TRANSLATION_ID = "state_dialog_translationID";
    public static final String TAG = HomeActivity.class.getSimpleName();
    private Door43Client mLibrary;
    private Translator mTranslator;
    private Fragment mFragment;
    private SimpleTaskWatcher taskWatcher;
    private ExamineImportsForCollisionsTask mExamineTask;
    private eDialogShown mDialogShown = eDialogShown.NONE;
    private String mTargetTranslationWithUpdates;
    private String mTargetTranslationID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        taskWatcher = new SimpleTaskWatcher(this, R.string.loading);
        taskWatcher.setOnFinishedListener(this);

        FloatingActionButton addTranslationButton = (FloatingActionButton) findViewById(R.id.addTargetTranslationButton);
        addTranslationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateNewTargetTranslation();
            }
        });

        mLibrary = App.getLibrary();
        mTranslator = App.getTranslator();

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                // use current fragment
                mFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                if (mTranslator.getTargetTranslations().length > 0) {
                    mFragment = new TargetTranslationListFragment();
                    mFragment.setArguments(getIntent().getExtras());
                } else {
                    mFragment = new WelcomeFragment();
                    mFragment.setArguments(getIntent().getExtras());
                }

                getFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
            }
        }

        ImageButton moreButton = (ImageButton)findViewById(R.id.action_more);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu moreMenu = new PopupMenu(HomeActivity.this, v);
                ViewUtil.forcePopupMenuIcons(moreMenu);
                moreMenu.getMenuInflater().inflate(R.menu.menu_home, moreMenu.getMenu());
                moreMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_update:
                                UpdateLibraryDialog updateDialog = new UpdateLibraryDialog();
                                showDialogFragment(updateDialog, UpdateLibraryDialog.TAG);
                                return true;
                            case R.id.action_import:
                                ImportDialog importDialog = new ImportDialog();
                                showDialogFragment(importDialog, ImportDialog.TAG);
                                return true;
                            case R.id.action_feedback:
                                FeedbackDialog dialog = new FeedbackDialog();
                                showDialogFragment(dialog, "feedback-dialog");
                                return true;
                            case R.id.action_share_apk:
                                try {
                                    PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                                    File apkFile = new File(pinfo.applicationInfo.publicSourceDir);
                                    File exportFile = new File(App.getSharingDir(), pinfo.applicationInfo.loadLabel(getPackageManager()) + "_" + pinfo.versionName + ".apk");
                                    FileUtilities.copyFile(apkFile, exportFile);
                                    if (exportFile.exists()) {
                                        Uri u = FileProvider.getUriForFile(HomeActivity.this, "com.door43.translationstudio.fileprovider", exportFile);
                                        Intent i = new Intent(Intent.ACTION_SEND);
                                        i.setType("application/zip");
                                        i.putExtra(Intent.EXTRA_STREAM, u);
                                        startActivity(Intent.createChooser(i, getResources().getString(R.string.send_to)));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    // todo notify user app could not be shared
                                }
                                return true;
                            case R.id.action_log_out:
                                App.setProfile(null);
                                Intent logoutIntent = new Intent(HomeActivity.this, ProfileActivity.class);
                                startActivity(logoutIntent);
                                finish();
                                return true;
                            case R.id.action_settings:
                                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                                startActivity(intent);
                                return true;
                        }
                        return false;
                    }
                });
                moreMenu.show();
            }
        });

        Intent intent = getIntent(); // check if user is trying to open a tstudio file
        if(intent != null) {
            String action = intent.getAction();
            if(action != null) {
                if (action.compareTo(Intent.ACTION_VIEW) == 0 || action.compareTo(Intent.ACTION_DEFAULT) == 0) {
                    String scheme = intent.getScheme();
                    ContentResolver resolver = getContentResolver();
                    Uri contentUri = intent.getData();
                    Logger.i(TAG,"Opening: " + contentUri.toString());
                    if (scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                        importFromUri(resolver, contentUri);
                        return;
                    } else if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {
                        importFromUri(resolver, contentUri);
                        return;
                    }
                }
            }
        }

        // open last project when starting the first time
        if (savedInstanceState == null) {
            TargetTranslation targetTranslation = getLastOpened();
            if (targetTranslation != null) {
                onItemClick(targetTranslation);
                return;
            }
        } else {
            mDialogShown = eDialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, eDialogShown.NONE.getValue()));
            mTargetTranslationID = savedInstanceState.getString(STATE_DIALOG_TRANSLATION_ID, null);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        App.setLastFocusTargetTranslation(null);

        int numTranslations = mTranslator.getTargetTranslations().length;
        if(numTranslations > 0 && mFragment instanceof WelcomeFragment) {
            // display target translations list
            mFragment = new TargetTranslationListFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();

            // load list after fragment created
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    ((TargetTranslationListFragment) mFragment).reloadList();
                }
            });

        } else if(numTranslations == 0 && mFragment instanceof TargetTranslationListFragment) {
            // display welcome screen
            mFragment = new WelcomeFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
        } else if(numTranslations > 0 && mFragment instanceof TargetTranslationListFragment) {
            // reload list
            ((TargetTranslationListFragment)mFragment).reloadList();
        }

        // re-connect to tasks
        PullTargetTranslationTask pullTask = (PullTargetTranslationTask) TaskManager.getTask(PullTargetTranslationTask.TASK_ID);
        if(pullTask != null) {
            taskWatcher.watch(pullTask);
            TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
        }

        mTargetTranslationWithUpdates = App.getNotifyTargetTranslationWithUpdates();
        if(mTargetTranslationWithUpdates != null && pullTask == null) {
            showTranslationUpdatePrompt(mTargetTranslationWithUpdates);
        }

        restoreDialogs();
    }

   /**
     * restore the dialogs that were displayed before rotation
     */
    private void restoreDialogs() {
        switch(mDialogShown) {
            case IMPORT_VERIFICATION:
                displayImportVerification();
                break;

            case MERGE_CONFLICT:
                showMergeConflict(mTargetTranslationID);
                break;

            case NONE:
                break;

            default:
                Logger.e(TAG,"Unsupported restore dialog: " + mDialogShown.toString());
                break;
        }
    }

    /**
     * Displays a dialog while replacing any duplicate dialog
     *
     * @param dialog
     * @param tag
     */
    private void showDialogFragment(DialogFragment dialog, String tag) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(tag);
        if (prev != null) {
            ft.remove(prev);
            // TODO: 10/7/16 I don't think we need this
            ft.commit();
            ft = getFragmentManager().beginTransaction();
        }
        ft.addToBackStack(null);
        // attach to any available event buffers
        if(dialog instanceof EventBuffer.OnEventTalker) {
            ((EventBuffer.OnEventTalker)dialog).eventBuffer.addOnEventListener(this);
        }
        dialog.show(ft, tag);
    }

    @Override
    public void onFinished(final ManagedTask task) {
        taskWatcher.stop();
        if (task instanceof ExamineImportsForCollisionsTask) {
            final ExamineImportsForCollisionsTask examineTask = (ExamineImportsForCollisionsTask) task;
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if (examineTask.mSuccess) {
                        displayImportVerification();
                    } else {
                        Logger.e(TAG, "Could not process content URI: " + examineTask.mContentUri.toString());
                        showImportResults(mExamineTask.mContentUri.toString(), null, false);
                        examineTask.cleanup();
                    }
                }
            });


        } else if (task instanceof ImportProjectsTask) {

            final ImportProjectsTask importTask = (ImportProjectsTask) task;
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    Translator.ImportResults importResults = importTask.getImportResults();
                    boolean success = importResults.isSuccess();
                    if(success && importResults.mergeConflict) {
                        showMergeConflict(importResults.importedSlug);
                    } else {
                        showImportResults(mExamineTask.mContentUri.toString(), mExamineTask.mProjectsFound, success);
                    }
                }
            });
            mExamineTask.cleanup();
        } else if (task instanceof PullTargetTranslationTask) {
            PullTargetTranslationTask.Status status = ((PullTargetTranslationTask)task).getStatus();
            if(status == PullTargetTranslationTask.Status.UP_TO_DATE || status == PullTargetTranslationTask.Status.UNKNOWN) {
                new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                        .setTitle(R.string.success)
                        .setMessage(R.string.success_translation_update)
                        .setPositiveButton(R.string.dismiss, null)
                        .show();
            } else if (status == PullTargetTranslationTask.Status.AUTH_FAILURE) {
                // regenerate ssh keys
                // if we have already tried ask the user if they would like to try again
                if(App.context().hasSSHKeys()) {
                    showAuthFailure();
                    return;
                }

                RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(false);
                taskWatcher.watch(keyTask);
                TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
            } else if (status == PullTargetTranslationTask.Status.MERGE_CONFLICTS) {
                new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                        .setTitle(R.string.success)
                        .setMessage(R.string.success_translation_update_with_conflicts)
                        .setNeutralButton(R.string.review, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(HomeActivity.this, TargetTranslationActivity.class);
                                intent.putExtra(App.EXTRA_TARGET_TRANSLATION_ID, ((PullTargetTranslationTask)task).targetTranslation.getId());
                                startActivity(intent);
                            }
                        })
                        .show();
            } else {
                notifyTranslationUpdateFailed();
            }
            App.setNotifyTargetTranslationWithUpdates(null);
        } else if (task instanceof RegisterSSHKeysTask) {
            if (((RegisterSSHKeysTask) task).isSuccess()) {
                Logger.i(this.getClass().getName(), "SSH keys were registered with the server");
                // try to pull again
                downloadTargetTranslationUpdates(mTargetTranslationWithUpdates);
            } else {
                notifyTranslationUpdateFailed();
            }
        }
    }

    /**
     * let user know there was a merge conflict
     * @param targetTranslationID
     */
    public void showMergeConflict(String targetTranslationID) {
        mDialogShown = eDialogShown.MERGE_CONFLICT;
        mTargetTranslationID = targetTranslationID;
        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setTitle(R.string.merge_conflict_title).setMessage(R.string.import_merge_conflict)
                .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        doManualMerge();
                    }
                }).show();
    }

    /**
     * open review mode to let user resolve conflict
     */
    private void doManualMerge() {
        // ask parent activity to navigate to a new activity
        Intent intent = new Intent(this, TargetTranslationActivity.class);
        Bundle args = new Bundle();
        args.putString(App.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslationID);

        MergeConflictHandler.CardLocation location = MergeConflictHandler.findFirstMergeConflict( mTargetTranslationID );
        if(location != null) {
            args.putString(App.EXTRA_CHAPTER_ID, location.chapterID);
            args.putString(App.EXTRA_FRAME_ID, location.frameID);
        }

        args.putString(App.EXTRA_VIEW_MODE, TranslationViewMode.REVIEW.toString());
        intent.putExtras(args);
        startActivity(intent);
    }

    public void showAuthFailure() {
        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
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
                        notifyTranslationUpdateFailed();
                    }
                }).show();
    }

    private void notifyTranslationUpdateFailed() {
        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setTitle(R.string.error)
                .setMessage(R.string.update_failed)
                .setNeutralButton(R.string.dismiss, null)
                .show();
    }

    /**
     * display the final import Results.
     */
    private void showImportResults(String projectPath, String projectNames, boolean success) {
        mDialogShown = eDialogShown.IMPORT_RESULTS;
        String message;
        if(success) {
            String format = App.context().getResources().getString(R.string.import_project_success);
            message = String.format(format, projectNames, projectPath);
        } else {
            String format = App.context().getResources().getString(R.string.import_failed);
            message = format + "\n" + projectPath;
        }

        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setTitle(success ? R.string.title_import_Success : R.string.title_import_Failed)
                .setMessage(message)
                .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        mExamineTask.cleanup();
                        HomeActivity.this.finish();
                    }
                })
                .show();
    }

    /**
     * begin the uri import
     * @param resolver
     * @param contentUri
     * @return
     * @throws Exception
     */
    private void importFromUri(ContentResolver resolver, Uri contentUri) {
        mExamineTask = new ExamineImportsForCollisionsTask(resolver, contentUri);
        taskWatcher.watch(mExamineTask);
        TaskManager.addTask(mExamineTask, ExamineImportsForCollisionsTask.TASK_ID);
    }

    /**
     * show dialog to verify that we want to import, restore or cancel.
     */
    private void displayImportVerification() {
        mDialogShown = eDialogShown.IMPORT_VERIFICATION;
        AlertDialog.Builder dlg = new AlertDialog.Builder(this,R.style.AppTheme_Dialog);
            dlg.setTitle(R.string.label_import)
                .setMessage(String.format(getResources().getString(R.string.confirm_import_target_translation), mExamineTask.mProjectsFound))
                .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        mExamineTask.cleanup();
                        HomeActivity.this.finish();
                    }
                })
                .setPositiveButton(R.string.label_restore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        doArchiveImport(true);
                    }
                });

        if(mExamineTask.mAlreadyPresent) { // add merge option
            dlg.setNeutralButton(R.string.label_import, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDialogShown = eDialogShown.NONE;
                    doArchiveImport(false);
                    dialog.dismiss();
                }
            });
        }
        dlg.show();
    }

    /**
     * import specified file
     * @param overwrite
     */
    private void doArchiveImport(boolean overwrite) {
        ImportProjectsTask importTask = new ImportProjectsTask(mExamineTask.mProjectsFolder, overwrite);
        taskWatcher.watch(importTask);
        TaskManager.addTask(importTask, ImportProjectsTask.TASK_ID);
    }

    /**
     * get last project opened and make sure it is still present
     * @return
     */
    @Nullable
    private TargetTranslation getLastOpened() {
        String lastTarget = App.getLastFocusTargetTranslation();
        if (lastTarget != null) {
            TargetTranslation targetTranslation = mTranslator.getTargetTranslation(lastTarget);
            if (targetTranslation != null) {
                return targetTranslation;
            }
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        // display confirmation before closing the app
        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setMessage(R.string.exit_confirmation)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HomeActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(NEW_TARGET_TRANSLATION_REQUEST == requestCode ) {
            if(RESULT_OK == resultCode ) {
                if(mFragment instanceof WelcomeFragment) {
                    // display target translations list
                    mFragment = new TargetTranslationListFragment();
                    mFragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                } else {
                    ((TargetTranslationListFragment) mFragment).reloadList();
                }

                Intent intent = new Intent(HomeActivity.this, TargetTranslationActivity.class);
                intent.putExtra(App.EXTRA_TARGET_TRANSLATION_ID, data.getStringExtra(NewTargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID));
                startActivity(intent);
            } else if( NewTargetTranslationActivity.RESULT_DUPLICATE == resultCode ) {
                // display duplicate notice to user
                String targetTranslationId = data.getStringExtra(NewTargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID);
                TargetTranslation existingTranslation = mTranslator.getTargetTranslation(targetTranslationId);
                if(existingTranslation != null) {
                    Project project = mLibrary.index().getProject(App.getDeviceLanguageCode(), existingTranslation.getProjectId(), true);
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), String.format(getResources().getString(R.string.duplicate_target_translation), project.name, existingTranslation.getTargetLanguageName()), Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            } else if( NewTargetTranslationActivity.RESULT_ERROR == resultCode) {
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.error), Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        }
    }

    /**
     * prompt user that project has changed
     * @param targetTranslationId
     */
    public void showTranslationUpdatePrompt(final String targetTranslationId) {
        TargetTranslation targetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        if(targetTranslation == null) {
            Logger.e(TAG, "invalid target translation id:" + targetTranslationId);
            return;
        }

        String projectID = targetTranslation.getProjectId();
        Project project = App.getLibrary().index().getProject(targetTranslation.getTargetLanguageName(), projectID, true);
        if(project == null) {
            Logger.e(TAG, "invalid project id:" + projectID);
            return;
        }

        String message = String.format(getResources().getString(R.string.merge_request),
                project.name, targetTranslation.getTargetLanguageName());

        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setTitle(R.string.change_detected)
                .setMessage(message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        downloadTargetTranslationUpdates(targetTranslationId);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        App.setNotifyTargetTranslationWithUpdates(null);
                        mDialogShown = eDialogShown.NONE;
                    }
                })
                .show();
    }

    /**
     * Updates a single target translation
     * @param targetTranslationId
     */
    private void downloadTargetTranslationUpdates(String targetTranslationId) {
        if(App.isNetworkAvailable()) {
            if (App.getProfile().gogsUser == null) {
                Door43LoginDialog dialog = new Door43LoginDialog();
                showDialogFragment(dialog, Door43LoginDialog.TAG);
                return;
            }

            PullTargetTranslationTask task = new PullTargetTranslationTask(
                    mTranslator.getTargetTranslation(targetTranslationId),
                    MergeStrategy.RECURSIVE,
                    null);
            taskWatcher.watch(task);
            TaskManager.addTask(task, PullTargetTranslationTask.TASK_ID);
        } else {
            Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.internet_not_available, Snackbar.LENGTH_LONG);
            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
            snack.show();
        }
    }

    @Override
    public void onCreateNewTargetTranslation() {
        Intent intent = new Intent(HomeActivity.this, NewTargetTranslationActivity.class);
        startActivityForResult(intent, NEW_TARGET_TRANSLATION_REQUEST);
    }

    @Override
    public void onItemDeleted(String targetTranslationId) {
        if(mTranslator.getTargetTranslations().length > 0) {
            ((TargetTranslationListFragment) mFragment).reloadList();
        } else {
            // display welcome screen
            mFragment = new WelcomeFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
        }
    }

    @Override
    public void onItemClick(TargetTranslation targetTranslation) {
        // validate project and target language

        Project project = App.getLibrary().index().getProject("en", targetTranslation.getProjectId(), true);
        TargetLanguage language = App.languageFromTargetTranslation(targetTranslation);
        if(project == null || language == null) {
            Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.missing_source, Snackbar.LENGTH_LONG);
            snack.setAction(R.string.check_for_updates, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UpdateLibraryDialog updateDialog = new UpdateLibraryDialog();
                    showDialogFragment(updateDialog, updateDialog.TAG);
                }
            });
            snack.setActionTextColor(getResources().getColor(R.color.light_primary_text));
            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
            snack.show();
        } else {
            Intent intent = new Intent(HomeActivity.this, TargetTranslationActivity.class);
            intent.putExtra(App.EXTRA_TARGET_TRANSLATION_ID, targetTranslation.getId());
            startActivity(intent);
        }
    }

    public void notifyDatasetChanged() {
        onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putInt(STATE_DIALOG_SHOWN, mDialogShown.getValue());
        out.putString(STATE_DIALOG_TRANSLATION_ID, mTargetTranslationID);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onEventBufferEvent(EventBuffer.OnEventTalker talker, int tag, Bundle args) {
        if(talker instanceof UpdateLibraryDialog) {
            // TODO: 10/7/16 handle events from the dialog
            Log.d("test", tag + "");
        }
    }

    /**
     * for keeping track if dialog is being shown for orientation changes
     */
    public enum eDialogShown {
        NONE(0),
        IMPORT_VERIFICATION(2),
        OPEN_LIBRARY(3),
        IMPORT_RESULTS(4),
        MERGE_CONFLICT(5);

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
    }}
