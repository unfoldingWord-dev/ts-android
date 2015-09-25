package com.door43.translationstudio.library;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.core.ProjectCategory;
import com.door43.translationstudio.projects.Project;

import java.util.List;

/**
 * A list fragment representing a list of Projects. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ServerLibraryDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnClickListener}
 * interface.
 * TODO: we need to display a notice if there are no new projects available
 */
public class ServerLibraryFragment extends ListFragment {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String STATE_TASK_ID = "task_id";
    private int mTaskId = -1;
    private ServerLibraryAdapter mAdapter;
    private OnClickListener mOnClickListener = sDummyOnClickListener;
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private static OnClickListener sDummyOnClickListener = new OnClickListener() {
        @Override
        public void onProjectCategorySelected(String id) {
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new ServerLibraryAdapter(getActivity());
        setListAdapter(mAdapter);
    }

    public void filter(String query) {
        if(mAdapter != null) {
            mAdapter.getFilter().filter(query);
        }
    }

    /**
     * Returns the list of projects that are currently displayed in the list view
     * @return
     */
    public List<Project> getFilteredProjects() {
        return mAdapter.getFilteredProjects();
    }

    /**
     * Sets the data used to populate the list
     * @param availableUpdates
     * @param projectCategories
     */
    public void setData(LibraryUpdates availableUpdates, ProjectCategory[] projectCategories) {
        if(mAdapter != null) {
            mAdapter.setData(availableUpdates, projectCategories);
        } else {
            Logger.w(this.getClass().getName(), "The adapter was not initialized");
        }
    }

    public ServerLibraryFragment() {
    }

    /**
     * Reloads the view according to the latest configuration
     */
    public void reload(boolean scrollToTop) {
        if(scrollToTop) {
            getListView().smoothScrollToPosition(0);
        }
//        mAdapter.changeDataSet(getProjectList(), ServerLibraryCache.getShowNewProjects(), mActivatedPosition);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Restore the previously serialized information
        if (savedInstanceState != null) {
            if(savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
                mActivatedPosition = savedInstanceState.getInt(STATE_ACTIVATED_POSITION);
            }
            if(savedInstanceState.containsKey(STATE_TASK_ID)) {
                mTaskId = savedInstanceState.getInt(STATE_TASK_ID);
            }
        }
    }

    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof OnClickListener)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mOnClickListener = (OnClickListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mOnClickListener = sDummyOnClickListener;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        notifyItemSelected(position);
    }

    /**
     * Notify the active callbacks interface (the activity, if the
     * fragment is attached to one) that an item has been selected.
     * @param position
     */
    private void notifyItemSelected(int position) {
        mAdapter.setSelected(position);
        mActivatedPosition = position;
        mAdapter.notifyDataSetChanged();
        mOnClickListener.onProjectCategorySelected(mAdapter.getItem(position).projectId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
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

    public void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
            if(mAdapter != null) {
                mAdapter.setSelected(mActivatedPosition);
                mAdapter.notifyDataSetChanged();
            }
        } else {
            getListView().setItemChecked(position, true);
            if(mAdapter != null) {
                mAdapter.setSelected(position);
                mAdapter.notifyDataSetChanged();
            }
        }

        mActivatedPosition = position;
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public interface OnClickListener {
        void onProjectCategorySelected(String projectCategoryId);
    }
}
