package com.door43.translationstudio.newui.home;

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
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.ProfileActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;

import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.library.ServerLibraryActivity;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.newtranslation.NewTargetTranslationActivity;
import com.door43.translationstudio.newui.FeedbackDialog;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.translationstudio.tasks.ExamineImportsForCollisionsTask;
import com.door43.translationstudio.tasks.ImportProjectsTask;
import org.unfoldingword.tools.taskmanager.SimpleTaskWatcher;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import com.door43.util.FileUtilities;
import com.door43.widget.ViewUtil;


import java.io.File;
import java.util.Locale;

public class HomeActivity extends BaseActivity implements SimpleTaskWatcher.OnFinishedListener, WelcomeFragment.OnCreateNewTargetTranslation, TargetTranslationListFragment.OnItemClickListener {
    private static final int REQUEST_CODE_STORAGE_ACCESS = 42;
    private static final int NEW_TARGET_TRANSLATION_REQUEST = 1;
    public static final String TAG = HomeActivity.class.getSimpleName();
    private Library mLibrary;
    private Translator mTranslator;
    private Fragment mFragment;
    private boolean mUsfmImport = false;
    private SimpleTaskWatcher taskWatcher;
    private ExamineImportsForCollisionsTask mExamineTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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
                                openLibrary();
                                return true;
                            case R.id.action_import:
                                FragmentTransaction backupFt = getFragmentManager().beginTransaction();
                                Fragment backupPrev = getFragmentManager().findFragmentByTag(ImportDialog.TAG);
                                if (backupPrev != null) {
                                    backupFt.remove(backupPrev);
                                }
                                backupFt.addToBackStack(null);

                                ImportDialog importDialog = new ImportDialog();
                                importDialog.show(backupFt, ImportDialog.TAG);
                                return true;
                            case R.id.action_feedback:
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                Fragment prev = getFragmentManager().findFragmentByTag("bugDialog");
                                if (prev != null) {
                                    ft.remove(prev);
                                }
                                ft.addToBackStack(null);

                                FeedbackDialog dialog = new FeedbackDialog();
                                dialog.show(ft, "bugDialog");

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
        }
    }

    @Override
    public void onFinished(ManagedTask task) {
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
                    String[] importedSlugs = importTask.getImportedSlugs();
                    boolean success = (importedSlugs != null) && (importedSlugs.length > 0);
                    showImportResults(mExamineTask.mContentUri.toString(), mExamineTask.mProjectsFound, success);
                }
            });
            mExamineTask.cleanup();
        }
    }

    /**
     * display the final import Results.
     */
    private void showImportResults(String projectPath, String projectNames, boolean success) {
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
        if(null == taskWatcher) {
            taskWatcher = new SimpleTaskWatcher(this, R.string.import_project);
            taskWatcher.setOnFinishedListener(this);
        }

        mExamineTask = new ExamineImportsForCollisionsTask(resolver, contentUri);
        taskWatcher.watch(mExamineTask);
        TaskManager.addTask(mExamineTask, ExamineImportsForCollisionsTask.TASK_ID);
    }

    /**
     * show dialog to verify that we want to import, restore or cancel.
     */
    private void displayImportVerification() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this,R.style.AppTheme_Dialog);
            dlg.setTitle(R.string.label_import)
                .setMessage(String.format(getResources().getString(R.string.confirm_import_target_translation), mExamineTask.mProjectsFound))
                .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mExamineTask.cleanup();
                        HomeActivity.this.finish();
                    }
                })
                .setPositiveButton(R.string.label_restore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doArchiveImport(true);
                    }
                });

        if(mExamineTask.mAlreadyPresent) { // add merge option
            dlg.setNeutralButton(R.string.label_import, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
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
     * Triggers the process of opening the server library
     */
    private void openLibrary() {
        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setTitle(R.string.update_library)
                .setIcon(R.drawable.ic_local_library_black_24dp)
                .setMessage(R.string.use_internet_confirmation)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(HomeActivity.this, ServerLibraryActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
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
                    Project project = mLibrary.getProject(existingTranslation.getProjectId(), App.getDeviceLanguageCode());
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

        Project project = App.getLibrary().getProject(targetTranslation.getProjectId(), "en");
        TargetLanguage language = App.getLibrary().getTargetLanguage(targetTranslation);
        if(project == null || !App.getLibrary().projectHasSource(project.getId())) {
            // validate project source exists
            Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.missing_project, Snackbar.LENGTH_LONG);
            snack.setAction(R.string.download, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openLibrary();
                }
            });
            snack.setActionTextColor(getResources().getColor(R.color.light_primary_text));
            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
            snack.show();
        } else if(language == null) {
            // validate target language exists
            Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.missing_language, Snackbar.LENGTH_LONG);
            snack.setAction(R.string.download, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openLibrary();
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
}
