package com.door43.translationstudio.library;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.library.temp.LibraryTempData;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.tasks.DownloadAvailableProjectsTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;

import java.util.ArrayList;

/**
 * A list fragment representing a list of Projects. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ProjectLibraryDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 * TODO: we need to display a notice if there are no new projects available
 */
public class ProjectLibraryListFragment extends ListFragment implements ManagedTask.OnFinishedListener, ManagedTask.OnProgressListener, DialogInterface.OnCancelListener {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String STATE_TASK_ID = "task_id";
    private int mTaskId = -1;
    private LibraryProjectAdapter mAdapter;
    private ProgressDialog mDialog;
    private boolean mActivityPaused = false;

    /**
     * Called when the task has finished fetching the available projects
     * @param task
     */
    @Override
    public void onFinished(ManagedTask task) {
        updateList();
        TaskManager.clearTask(mTaskId);
        mTaskId = -1;
        if(mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    /**
     * Pulls the latest results from the task and populates the list
     */
    private void updateList() {
        if(TaskManager.getTask(mTaskId) != null) {
            final DownloadAvailableProjectsTask task = ((DownloadAvailableProjectsTask) TaskManager.getTask(mTaskId));
            LibraryTempData.setAvailableProjects(task.getProjects());

            Handler handle = new Handler(Looper.getMainLooper());
            handle.post(new Runnable() {
                @Override
                public void run() {
                    if(LibraryTempData.getShowNewProjects() && !LibraryTempData.getShowProjectUpdates()) {
                        // new languages/projects
                        mAdapter.changeDataSet(LibraryTempData.getNewProjects());
                    } else if(LibraryTempData.getShowProjectUpdates() && !LibraryTempData.getShowNewProjects()) {
                        // updates
                        mAdapter.changeDataSet(LibraryTempData.getUpdatedProjects());
                    } else {
                        // just show everything
                        mAdapter.changeDataSet(LibraryTempData.getProjects());
                    }
                }
            });
        }
    }

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    @Override
    public void onProgress(ManagedTask task, final double progress, final String message) {
        // don't display anything if we've already loading the results
        if(LibraryTempData.getProjects().length == 0) {
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if (mDialog == null) {
                        if (getActivity() != null) {
                            mDialog = new ProgressDialog(getActivity());
                            mDialog.setCancelable(true);
                            mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            mDialog.setCanceledOnTouchOutside(false);
                            mDialog.setOnCancelListener(ProjectLibraryListFragment.this);
                            mDialog.setMax(100);
                            mDialog.setTitle(R.string.loading);
                            mDialog.setMessage("");
                        }
                    }
                    if (getActivity() != null) {
                        if (!mDialog.isShowing()) {
                            mDialog.show();
                        }
                        if (progress == -1) {
                            mDialog.setIndeterminate(true);
                            mDialog.setProgress(mDialog.getMax());
                        } else {
                            mDialog.setIndeterminate(false);
                            mDialog.setProgress((int) Math.ceil(progress * 100));
                        }
                        mDialog.setMessage(message);
                    }
                }
            });
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        if(TaskManager.getTask(mTaskId) != null) {
            DownloadAvailableProjectsTask task = (DownloadAvailableProjectsTask) TaskManager.getTask(mTaskId);
            task.setOnFinishedListener(null);
            task.setOnProgressListener(null);
            TaskManager.cancelTask(task);
            mTaskId = -1;
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(int index);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(int index) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProjectLibraryListFragment() {
    }

    /**
     * Reloads the view according to the latest configuration
     */
    public void reload() {
        getActivity().getIntent().putExtra(ProjectLibraryListActivity.ARG_ONLY_SHOW_NEW, LibraryTempData.getShowNewProjects());
        getActivity().getIntent().putExtra(ProjectLibraryListActivity.ARG_ONLY_SHOW_UPDATES, LibraryTempData.getShowProjectUpdates());

        getListView().smoothScrollToPosition(0);
        mAdapter.changeDataSet(getProjectList(), LibraryTempData.getShowNewProjects());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null && savedInstanceState == null) {
            // we only set the display arguments when first loading
            LibraryTempData.setShowNewProjects(getActivity().getIntent().getBooleanExtra(ProjectLibraryListActivity.ARG_ONLY_SHOW_NEW, false));
            LibraryTempData.setShowProjectUpdates(getActivity().getIntent().getBooleanExtra(ProjectLibraryListActivity.ARG_ONLY_SHOW_UPDATES, false));
        }

        // Restore the previously serialized information
        if (savedInstanceState != null) {
            if(savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
                setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
            }
            if(savedInstanceState.containsKey(STATE_TASK_ID)) {
                mTaskId = savedInstanceState.getInt(STATE_TASK_ID);
            }
        }
        mActivityPaused = false;

        mAdapter = new LibraryProjectAdapter(AppContext.context(), getProjectList(), LibraryTempData.getShowNewProjects());

        setListAdapter(mAdapter);

        preparProjectList();
    }

    private Project[] getProjectList() {
        Project[] projects;
        if(LibraryTempData.getShowNewProjects() && !LibraryTempData.getShowProjectUpdates()) {
            // new languages/projects
            projects = LibraryTempData.getNewProjects();
            getActivity().setTitle(getResources().getString(R.string.title_activity_project_library));
        } else if(LibraryTempData.getShowProjectUpdates() && !LibraryTempData.getShowNewProjects()) {
            // updates
            projects = LibraryTempData.getUpdatedProjects();
            getActivity().setTitle(getResources().getString(R.string.title_activity_project_updates));
        } else {
            // just show everything
            projects = LibraryTempData.getProjects();
            getActivity().setTitle(getResources().getString(R.string.title_activity_project_library));
        }
        return projects;
    }

    /**
     * Fetches a list of available projects
     * TODO: we should first load all the projects then load each set of languages one at a time so we can update the ui as we receive more projects.
     * TODO: then we should just display a loading icon at the bottom of the screen.
     */
    private void preparProjectList() {
        if(TaskManager.getTask(mTaskId) != null) {
            // connect to existing task
            DownloadAvailableProjectsTask task = (DownloadAvailableProjectsTask) TaskManager.getTask(mTaskId);
//            onProgress(task, -1, "");
            task.setOnFinishedListener(this);
            task.setOnProgressListener(this);
        } else if(LibraryTempData.getProjects().length == 0) {
//            onProgress(-1, "");
            // start process
            DownloadAvailableProjectsTask task = new DownloadAvailableProjectsTask();
            task.setOnFinishedListener(this);
            task.setOnProgressListener(this);
            mTaskId = TaskManager.addTask(task);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mAdapter.setSelected(position);
        mAdapter.notifyDataSetChanged();
        mCallbacks.onItemSelected(position);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
        if(mTaskId != -1) {
            outState.putInt(STATE_TASK_ID, mTaskId);
            // disconnect listeners
            TaskManager.getTask(mTaskId).setOnFinishedListener(null);
            ((DownloadAvailableProjectsTask) TaskManager.getTask(mTaskId)).setOnProgressListener(null);
        }
        mActivityPaused = true;
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    public void onDestroy() {
        if(!mActivityPaused) {
            // the activity is being destroyed
            LibraryTempData.setAvailableProjects(new ArrayList<Project>());

        }
        if(mDialog != null) {
            mDialog.setOnCancelListener(null);
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
        }
        super.onDestroy();
    }
}
