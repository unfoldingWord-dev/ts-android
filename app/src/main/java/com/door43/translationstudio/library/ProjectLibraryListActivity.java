package com.door43.translationstudio.library;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.door43.translationstudio.R;
import com.door43.translationstudio.library.temp.LibraryTempData;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.tasks.DownloadProjectsTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;

import java.util.List;

/**
 * An activity representing a list of Projects. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ProjectLibraryDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ProjectLibraryListFragment} and the item details
 * (if present) is a {@link ProjectLibraryDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link ProjectLibraryListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ProjectLibraryListActivity extends TranslatorBaseActivity implements ProjectLibraryListFragment.Callbacks, TranslationDraftsTabFragment.Callbacks, LibraryCallbacks, ManagedTask.OnFinishedListener, ManagedTask.OnProgressListener, DialogInterface.OnCancelListener {

    public static final String ARG_SHOW_UPDATES = "only_show_updates";
    public static final String ARG_SHOW_NEW = "only_show_new";
    private AlertDialog mConfirmDialog;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private Toolbar mToolbar;
    private String TASK_DOWNLOAD_ALL_PROJECTS = "library_download_all_projects";
    private ProgressDialog mDownloadProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_library_list);

        // set up tool bar
        mToolbar = (Toolbar)findViewById(R.id.toolbar_server_library);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (findViewById(R.id.project_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ProjectLibraryListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.project_list))
                    .setActivateOnItemClick(true);

        }

        handleIntent(getIntent());

        // TODO: If exposing deep links into your app, handle intents here.
        connectDownloaAllTask();
    }

    /**
     * Callback method from {@link ProjectLibraryListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if(id != null) {
            if (mTwoPane) {
                // In two-pane mode, show the detail view in this activity by
                // adding or replacing the detail fragment using a
                // fragment transaction.

                Bundle arguments = getIntent().getExtras();
                if(arguments == null) {
                    arguments = new Bundle();
                }
                arguments.putString(ProjectLibraryDetailFragment.ARG_ITEM_ID, id);
                ProjectLibraryDetailFragment fragment = new ProjectLibraryDetailFragment();
                if (arguments != null) {
                    fragment.setArguments(arguments);
                }
                getFragmentManager().beginTransaction().replace(R.id.project_detail_container, fragment).commit();

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
                detailIntent.putExtra(ProjectLibraryDetailFragment.ARG_ITEM_ID, id);
                startActivity(detailIntent);
            }
        } else {
            reload(false);
        }
    }

    @Override
    public void onEmptyDraftsList() {
        ProjectLibraryDetailFragment fragment = (ProjectLibraryDetailFragment)getFragmentManager().findFragmentById(R.id.project_detail_container);
        if(fragment != null) {
            fragment.hideDraftsTab();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_project_library, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Connects to an existing task
     */
    public void connectDownloaAllTask() {
        DownloadProjectsTask task = (DownloadProjectsTask)TaskManager.getTask(TASK_DOWNLOAD_ALL_PROJECTS);
        if(task != null) {
            // connect to existing task
            task.setOnProgressListener(this);
            task.setOnFinishedListener(this);
        } else {
            onFinished(null);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(LibraryTempData.getUpdatedProjects().size() > 0) {
            if(LibraryTempData.getEnableEditing()) {
                menu.findItem(R.id.action_edit_projects).setVisible(false);
                menu.findItem(R.id.action_cancel_edit_projects).setVisible(true);
            } else {
                menu.findItem(R.id.action_edit_projects).setVisible(true);
                menu.findItem(R.id.action_cancel_edit_projects).setVisible(false);
            }
        } else {
            menu.findItem(R.id.action_edit_projects).setVisible(false);
            menu.findItem(R.id.action_cancel_edit_projects).setVisible(false);
        }

        // TODO: editing is disabled for now. Finish implementing editing.
        menu.findItem(R.id.action_edit_projects).setVisible(false);
        menu.findItem(R.id.action_cancel_edit_projects).setVisible(false);

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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_filter:
//                // TODO: display a dialog that will provide filter options. We might want these to be user settings.
//                // e.g. only show projects from a certain language, new projects, updates, etc.
//                return true;
            case R.id.action_download_all:
                mConfirmDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.action_download_all)
                        .setMessage(R.string.download_all_confirmation)
                        .setIcon(R.drawable.ic_download_small)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                DownloadProjectsTask task = (DownloadProjectsTask)TaskManager.getTask(TASK_DOWNLOAD_ALL_PROJECTS);
                                if(task == null) {
                                    // connect to existing task
                                    ProjectLibraryListFragment listFragment = ((ProjectLibraryListFragment) getSupportFragmentManager().findFragmentById(R.id.project_list));
                                    List<Project> projects = listFragment.getFilteredProjects();
                                    task = new DownloadProjectsTask(projects);
                                    task.setOnProgressListener(ProjectLibraryListActivity.this);
                                    task.setOnFinishedListener(ProjectLibraryListActivity.this);
                                    TaskManager.addTask(task, TASK_DOWNLOAD_ALL_PROJECTS);
                                } else {
                                    connectDownloaAllTask();
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            case R.id.action_edit_projects:
                LibraryTempData.setEnableEditing(true);
                reload(false);
                return true;
            case R.id.action_cancel_edit_projects:
                LibraryTempData.setEnableEditing(false);
                reload(false);
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
            if(getFragmentManager().findFragmentById(R.id.project_detail_container) != null) {
                getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentById(R.id.project_detail_container)).commit();
            }
        }
        // reload list
        ProjectLibraryListFragment listFragment = ((ProjectLibraryListFragment) getSupportFragmentManager().findFragmentById(R.id.project_list));
        listFragment.reload(scrollToTop);
    }

    public void onDestroy() {
        if(mConfirmDialog != null) {
            mConfirmDialog.dismiss();
        }
        if(mDownloadProgressDialog != null) {
            mDownloadProgressDialog.dismiss();
        }
        DownloadProjectsTask task = (DownloadProjectsTask)TaskManager.getTask(TASK_DOWNLOAD_ALL_PROJECTS);
        if(task != null) {
            task.setOnFinishedListener(null);
            task.setOnProgressListener(null);
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
        ProjectLibraryListFragment listFragment = ((ProjectLibraryListFragment) getSupportFragmentManager().findFragmentById(R.id.project_list));
        listFragment.filter(query);
    }

    @Override
    public void refreshUI() {
        reload(false);
    }

    @Override
    public void onFinished(final ManagedTask task) {
        TaskManager.clearTask(TASK_DOWNLOAD_ALL_PROJECTS);

        // reload list
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                ProjectLibraryListFragment listFragment = ((ProjectLibraryListFragment) getSupportFragmentManager().findFragmentById(R.id.project_list));
                listFragment.refresh();

                if(mDownloadProgressDialog != null && mDownloadProgressDialog.isShowing()) {
                    mDownloadProgressDialog.dismiss();
                }

                if(task != null && !task.isCanceled()) {
                     new AlertDialog.Builder(ProjectLibraryListActivity.this)
                             .setTitle(R.string.success)
                             .setIcon(R.drawable.ic_check_small)
                             .setMessage(R.string.download_complete)
                             .setCancelable(false)
                             .setPositiveButton(R.string.label_ok, null)
                             .show();
                }
            }
        });
    }

    @Override
    public void onProgress(final ManagedTask task, final double progress, final String message) {
        if(!task.isFinished()) {
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if (task.isFinished()) {
                        // because we start on a new thread we need to make sure the task didn't finish while this thread was starting.
                        if (mDownloadProgressDialog != null) {
                            mDownloadProgressDialog.dismiss();
                        }
                        return;
                    }
                    if (mDownloadProgressDialog == null) {
                        mDownloadProgressDialog = new ProgressDialog(ProjectLibraryListActivity.this);
                        mDownloadProgressDialog.setCancelable(true);
                        mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        mDownloadProgressDialog.setCanceledOnTouchOutside(false);
                        mDownloadProgressDialog.setOnCancelListener(ProjectLibraryListActivity.this);
                        mDownloadProgressDialog.setMax(100);
                        mDownloadProgressDialog.setIcon(R.drawable.ic_download_small);
                        mDownloadProgressDialog.setTitle(getResources().getString(R.string.downloading));
                        mDownloadProgressDialog.setMessage("");
                    }
                    if (!mDownloadProgressDialog.isShowing()) {
                        mDownloadProgressDialog.show();
                    }
                    if (progress == -1) {
                        mDownloadProgressDialog.setIndeterminate(true);
                        mDownloadProgressDialog.setProgress(mDownloadProgressDialog.getMax());
                    } else {
                        mDownloadProgressDialog.setIndeterminate(false);
                        mDownloadProgressDialog.setProgress((int) Math.ceil(progress * 100));
                    }
                    if (!message.isEmpty()) {
                        mDownloadProgressDialog.setMessage(String.format(getResources().getString(R.string.downloading_project), message));
                    } else {
                        mDownloadProgressDialog.setMessage("");
                    }
                }
            });
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        // the download dialog was canceled
        mDownloadProgressDialog = null;
        DownloadProjectsTask task = (DownloadProjectsTask) TaskManager.getTask(TASK_DOWNLOAD_ALL_PROJECTS);
        if(task != null) {
            TaskManager.cancelTask(task);
        }
    }
}
