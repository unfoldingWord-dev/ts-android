package com.door43.translationstudio.newui.library;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.tasks.DownloadAllProjectsTask;
import com.door43.translationstudio.tasks.DownloadUpdatesTask;
import com.door43.translationstudio.tasks.GetLibraryUpdatesTask;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.widget.ViewUtil;

import java.text.NumberFormat;
import java.util.Locale;

public class ServerLibraryActivity extends BaseActivity implements ServerLibraryDetailFragment.OnEventListener, ServerLibraryFragment.OnClickListener, ManagedTask.OnFinishedListener, ManagedTask.OnProgressListener, DialogInterface.OnCancelListener {

    public static final String ARG_SHOW_UPDATES = "only_show_updates";
    public static final String ARG_SHOW_NEW = "only_show_new";
    private CustomAlertDialog mConfirmDialog;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private Toolbar mToolbar;
    private ProgressDialog mProgressDialog;
    private ServerLibraryFragment mListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_library_list);

        // set up tool bar
        mToolbar = (Toolbar)findViewById(R.id.toolbar_server_library);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mListFragment = ((ServerLibraryFragment) getSupportFragmentManager().findFragmentById(R.id.project_list));
        if(mListFragment != null) {
            mListFragment.setActivatedPosition(ListView.INVALID_POSITION);
        }

        // Identify two pane mode (large screens)
        if (findViewById(R.id.detail_container) != null) {
            mTwoPane = true;
            mListFragment.setActivateOnItemClick(true);
        }

        if(savedInstanceState == null) {
            // check for available updates
//            if(ServerLibraryCache.isExpired()) {
                GetLibraryUpdatesTask getUpdatesTask = new GetLibraryUpdatesTask();
                getUpdatesTask.addOnFinishedListener(this);
                getUpdatesTask.addOnProgressListener(this);
                TaskManager.addTask(getUpdatesTask, GetLibraryUpdatesTask.TASK_ID);
//            } else {
//                // load the cached data
//                Library serverLibrary = AppContext.getLibrary();
//                Project[] projects = serverLibrary.getProjects(Locale.getDefault().getLanguage());
//                mListFragment.setData(ServerLibraryCache.getAvailableUpdates(), projects);
//            }
        } else {
            // populated cached data
            Library serverLibrary = AppContext.getLibrary();
            Project[] projects = serverLibrary.getProjects(Locale.getDefault().getLanguage());
            mListFragment.setData(ServerLibraryCache.getAvailableUpdates(), projects);

            // connect to tasks
            DownloadAllProjectsTask downloadAllTask = (DownloadAllProjectsTask)TaskManager.getTask(DownloadAllProjectsTask.TASK_ID);
            DownloadUpdatesTask downloadUpdatesTask = (DownloadUpdatesTask)TaskManager.getTask(DownloadUpdatesTask.TASK_ID);
            GetLibraryUpdatesTask getUpdatesTask = (GetLibraryUpdatesTask)TaskManager.getTask(GetLibraryUpdatesTask.TASK_ID);
            if(downloadAllTask != null) {
                downloadAllTask.addOnProgressListener(this);
                downloadAllTask.addOnFinishedListener(this);
            }
            if(downloadUpdatesTask != null) {
                downloadUpdatesTask.addOnProgressListener(this);
                downloadUpdatesTask.addOnFinishedListener(this);
            }
            if(getUpdatesTask != null) {
                getUpdatesTask.addOnProgressListener(this);
                getUpdatesTask.addOnFinishedListener(this);
            }

        }
    }

    /**
     * Callback method from {@link ServerLibraryFragment.OnClickListener}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onProjectSelected(String projectId) {
        if(projectId != null) {
            if (mTwoPane) {
                Bundle arguments = getIntent().getExtras();
                if(arguments == null) {
                    arguments = new Bundle();
                }
                arguments.putString(ServerLibraryDetailFragment.ARG_PROJECT_ID, projectId);
                // TODO: animate the card fading in and out.
                ServerLibraryDetailFragment fragment = (ServerLibraryDetailFragment)getFragmentManager().findFragmentById(R.id.detail_container);
                if(fragment == null) {
                    fragment = new ServerLibraryDetailFragment();
                    fragment.setArguments(arguments);
                    getFragmentManager().beginTransaction().replace(R.id.detail_container, fragment).commit();
                } else {
                    fragment.setProjectId(projectId);
                }
            } else {
                // In single-pane mode, simply start the detail activity
                // for the selected item ID.
                Intent detailIntent = new Intent(this, ProjectLibraryDetailActivity.class);
                Bundle arguments = getIntent().getExtras();
                if(arguments == null) {
                    arguments = new Bundle();
                }
                if (arguments != null) {
                    detailIntent.putExtras(arguments);
                }
                detailIntent.putExtra(ServerLibraryDetailFragment.ARG_PROJECT_ID, projectId);
                startActivity(detailIntent);
            }
        } else {
            reload(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_server_library, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Connects to an existing task
     */
    public void connectDownloadTasks() {
        DownloadAllProjectsTask downloadAllProjectsTask = (DownloadAllProjectsTask)TaskManager.getTask(DownloadAllProjectsTask.TASK_ID);
        DownloadUpdatesTask downloadUpdatesTask = (DownloadUpdatesTask)TaskManager.getTask(DownloadUpdatesTask.TASK_ID);
        if(downloadAllProjectsTask != null) {
            // connect to existing task
            downloadAllProjectsTask.addOnProgressListener(this);
            downloadAllProjectsTask.addOnFinishedListener(this);
        } else if(downloadUpdatesTask != null) {
            // connect to existing task
            downloadUpdatesTask.addOnProgressListener(this);
            downloadUpdatesTask.addOnProgressListener(this);
        } else {
            onFinished(null);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // hook up the search
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchViewAction = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchViewAction.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                search(s);
                return true;
            }
        });
        searchViewAction.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        // display download updates
        if(ServerLibraryCache.getAvailableUpdates() != null && ServerLibraryCache.getAvailableUpdates().numSourceTranslationUpdates() > 0) {
            menu.findItem(R.id.action_download_updates).setVisible(true);
        } else {
            menu.findItem(R.id.action_download_updates).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download_all:
                mConfirmDialog =  CustomAlertDialog.Create(this);
                mConfirmDialog.setTitle(R.string.action_download_all)
                        .setMessage(R.string.download_confirmation)
                        .setIcon(R.drawable.ic_cloud_download_black_24dp)
                        .setPositiveButton(R.string.yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DownloadAllProjectsTask task = (DownloadAllProjectsTask)TaskManager.getTask(DownloadAllProjectsTask.TASK_ID);
                                if(task == null) {
                                    // start new task
                                    task = new DownloadAllProjectsTask();
                                    task.addOnProgressListener(ServerLibraryActivity.this);
                                    task.addOnFinishedListener(ServerLibraryActivity.this);
                                    TaskManager.addTask(task, DownloadAllProjectsTask.TASK_ID);
                                } else {
                                    connectDownloadTasks();
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                    .show("DlAllConfirm");
                return true;
            case R.id.action_download_updates:
                mConfirmDialog = CustomAlertDialog.Create(this);
                mConfirmDialog.setTitle(R.string.action_download_updates)
                        .setMessage(R.string.download_confirmation)
                        .setIcon(R.drawable.ic_cloud_download_black_24dp)
                        .setPositiveButton(R.string.yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DownloadUpdatesTask task = (DownloadUpdatesTask)TaskManager.getTask(DownloadUpdatesTask.TASK_ID);
                                if(task == null) {
                                    // start new task
                                    task = new DownloadUpdatesTask(ServerLibraryCache.getAvailableUpdates());
                                    task.addOnProgressListener(ServerLibraryActivity.this);
                                    task.addOnFinishedListener(ServerLibraryActivity.this);
                                    TaskManager.addTask(task, DownloadUpdatesTask.TASK_ID);
                                } else {
                                    connectDownloadTasks();
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show("DlUpdateConfirm");
                return true;
            case R.id.action_search:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void reload(boolean scrollToTop) {
        invalidateOptionsMenu();
        if(mTwoPane) {
            // remove details
            if(getFragmentManager().findFragmentById(R.id.detail_container) != null) {
                getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentById(R.id.detail_container)).commit();
            }
        }
        // reload list
        mListFragment.reload(scrollToTop);
    }

    public void onDestroy() {
        if(mConfirmDialog != null) {
            mConfirmDialog.dismiss();
        }
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        DownloadAllProjectsTask downloadAllProjectsTask = (DownloadAllProjectsTask)TaskManager.getTask(DownloadAllProjectsTask.TASK_ID);
        if(downloadAllProjectsTask != null) {
            downloadAllProjectsTask.removeOnFinishedListener(this);
            downloadAllProjectsTask.removeOnProgressListener(this);
        }
        DownloadUpdatesTask downloadUpdatesTask = (DownloadUpdatesTask)TaskManager.getTask(DownloadUpdatesTask.TASK_ID);
        if(downloadUpdatesTask != null) {
            downloadUpdatesTask.removeOnFinishedListener(this);
            downloadUpdatesTask.removeOnProgressListener(this);
        }
        GetLibraryUpdatesTask getLibraryUpdatesTask = (GetLibraryUpdatesTask)TaskManager.getTask(GetLibraryUpdatesTask.TASK_ID);
        if(getLibraryUpdatesTask != null) {
            getLibraryUpdatesTask.removeOnFinishedListener(this);
            getLibraryUpdatesTask.removeOnProgressListener(this);
        }
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    /**
     * Handles the search
     * @param intent
     */
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            search(query);
        }
    }

    /**
     * Performs the search
     * @param query
     */
    private void search(String query) {
        mListFragment.filter(query);
    }

    @Override
    public void onFinished(final ManagedTask task) {
        TaskManager.clearTask(task);

        if(task instanceof GetLibraryUpdatesTask) {
            ServerLibraryCache.setAvailableUpdates(((GetLibraryUpdatesTask) task).getUpdates());
            if(task.isCanceled()) {
                ServerLibraryCache.setExpired();
            }
        }

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                invalidateOptionsMenu();
                Library serverLibrary = AppContext.getLibrary();
                Project[] projects = serverLibrary.getProjects(Locale.getDefault().getLanguage());
                mListFragment.setData(ServerLibraryCache.getAvailableUpdates(), projects);

                if(mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                mProgressDialog = null;

                if(task instanceof DownloadAllProjectsTask || task instanceof DownloadUpdatesTask) {
                    if (!task.isCanceled()) {
                        CustomAlertDialog.Create(ServerLibraryActivity.this)
                                .setTitle(R.string.success)
                                .setIcon(R.drawable.ic_done_black_24dp)
                                .setMessage(R.string.download_complete)
                                .setCancelableChainable(false)
                                .setPositiveButton(R.string.label_ok, null)
                                .show("DlComplete");
                    }
                }
            }
        });
    }

    @Override
    public void onProgress(final ManagedTask task, final double progress, final String message, final boolean secondary) {
        if(!task.isFinished()) {
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if (task.isFinished()) {
                        // because we start on a new thread we need to make sure the task didn't finish while this thread was starting.
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                        return;
                    }

                    if (mProgressDialog == null) {
                        mProgressDialog = new ProgressDialog(ServerLibraryActivity.this);
                        mProgressDialog.setCancelable(true);
                        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        mProgressDialog.setCanceledOnTouchOutside(false);
                        mProgressDialog.setOnCancelListener(ServerLibraryActivity.this);
                        mProgressDialog.setIcon(R.drawable.ic_cloud_download_black_24dp);
                        if(task instanceof GetLibraryUpdatesTask) {
                            mProgressDialog.setTitle(getResources().getString(R.string.checking_for_updates));
                        } else if(task instanceof DownloadAllProjectsTask) {
                            mProgressDialog.setTitle(getResources().getString(R.string.downloading));
                        } else if(task instanceof DownloadUpdatesTask) {
                            mProgressDialog.setTitle(getResources().getString(R.string.downloading));
                        }
                        mProgressDialog.setMessage("");
                    }
                    mProgressDialog.setMax(task.maxProgress());
                    if (!mProgressDialog.isShowing()) {
                        mProgressDialog.show();
                    }
                    if (progress == -1) {
                        mProgressDialog.setIndeterminate(true);
                        mProgressDialog.setProgress(mProgressDialog.getMax());
                        mProgressDialog.setProgressNumberFormat(null);
                        mProgressDialog.setProgressPercentFormat(null);
                    } else {
                        mProgressDialog.setIndeterminate(false);
                        if(secondary) {
                            mProgressDialog.setSecondaryProgress((int)progress);
                        } else {
                            mProgressDialog.setProgress((int) progress);
                        }
                        mProgressDialog.setProgressNumberFormat("%1d/%2d");
                        mProgressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                    }
                    if ((task instanceof DownloadAllProjectsTask || task instanceof DownloadUpdatesTask) && !message.isEmpty()) {
                        mProgressDialog.setMessage(String.format(getResources().getString(R.string.downloading_project), message));
                    } else {
                        mProgressDialog.setMessage("");
                    }
                }
            });
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        // the dialog was canceled
        mProgressDialog = null;
        DownloadAllProjectsTask downloadAllProjectsTask = (DownloadAllProjectsTask) TaskManager.getTask(DownloadAllProjectsTask.TASK_ID);
        if(downloadAllProjectsTask != null) {
            TaskManager.cancelTask(downloadAllProjectsTask);
        }
        DownloadUpdatesTask downloadUpdatesTask = (DownloadUpdatesTask) TaskManager.getTask(DownloadUpdatesTask.TASK_ID);
        if(downloadUpdatesTask != null) {
            TaskManager.cancelTask(downloadUpdatesTask);
        }
        GetLibraryUpdatesTask getLibraryUpdatesTask = (GetLibraryUpdatesTask) TaskManager.getTask(GetLibraryUpdatesTask.TASK_ID);
        if(getLibraryUpdatesTask != null) {
            TaskManager.cancelTask(getLibraryUpdatesTask);
            // warn user they may not see all the available updates
            Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.list_may_be_outdated, Snackbar.LENGTH_LONG);
            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
            snack.show();
        }
    }

    @Override
    public void onSourceLanguageDownloaded(String projectId, String sourceLanguageId) {
        mListFragment.reload(false);
    }

    @Override
    public void onProjectDeleted(String projectId) {
        invalidateOptionsMenu();
        mListFragment.reload(false);
        onProjectSelected(projectId);
    }
}
