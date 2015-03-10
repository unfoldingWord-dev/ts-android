package com.door43.translationstudio.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.events.ChoseProjectEvent;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.util.AppContext;

/**
 * This dialog allows a user to browse a project dialog and make selections.
 */
public class ProjectLibraryDialog extends DialogFragment {
    private Model[] mProjectList;
    private ModelItemAdapter mAdapter;
    private String mSelectedProjectId;
    private Boolean mDismissed = false;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.project_library);
        View v = inflater.inflate(R.layout.dialog_choose_project, container, false);

        ListView listView = (ListView)v.findViewById(R.id.listView);



        if(mAdapter == null) mAdapter = new ModelItemAdapter(AppContext.context(), new Model[0]);


//        else {
//            Bundle args = getArguments();
//            String id = args.getString("metaId");
//            PseudoProject p = AppContext.projectManager().getPseudoProject(id);
//            if(p != null) {
//                if (mAdapter == null) mAdapter = new ModelItemAdapter(AppContext.context(), p.getChildren());
//            }
//        }
        if(savedInstanceState != null) {
            mProjectList = (Model[]) savedInstanceState.getSerializable("projects");
        }

        // connect adapter
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Model m = mAdapter.getItem(i);
                if(m.getClass().equals(PseudoProject.class)) {
                    // re-load list
                    mAdapter.changeDataSet(((PseudoProject)m).getChildren());
                } else {
                    // return the selected project.
                    mSelectedProjectId = m.getId();
                    if(getActivity() != null) {
                        ((ProjectLibraryListener)getActivity()).onProjectLibrarySelected(ProjectLibraryDialog.this, mSelectedProjectId);
                    }
                }
            }
        });

        init();

        return v;
    }

    /**
     * Initializes the adapter data
     */
    public void init() {
        if(mAdapter != null && mProjectList != null) {
            mAdapter.changeDataSet(mProjectList);
        }
    }

    /**
     * Ensure the activity is configured properly
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(!(activity instanceof ProjectLibraryListener)) {
            throw new ClassCastException(activity.toString() + " must implement ProjectLibraryListener");
        } else {
            if(mDismissed) {
                ((ProjectLibraryListener)getActivity()).onProjectLibraryDismissed(this);
            } else if(mSelectedProjectId != null) {
                ((ProjectLibraryListener)getActivity()).onProjectLibrarySelected(this, mSelectedProjectId);
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mDismissed = true;
        if(getActivity() != null) {
            ((ProjectLibraryListener)getActivity()).onProjectLibraryDismissed(this);
        }
        super.onCancel(dialog);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDismissed = true;
        if(getActivity() != null) {
            ((ProjectLibraryListener)getActivity()).onProjectLibraryDismissed(this);
        }
        super.onDismiss(dialog);
    }

    /**
     * Sets an array of projects and Pseudo projects to display in the list
     * @param projects
     */
    public void setProjects(Model[] projects) {
        mProjectList = projects;
    }

    /**
     * Sets the callback to be notified when submitting or canceling the dialog.
     * IMPORTANT! You should always reset the listener on screen rotation
     * @param listener
     */
//    public void setOnClickListener(OnClickListener listener) {
//        mListener = listener;
//        if(mDismissed) {
//            listener.onClick(this, null);
//        } else if(mSelectedProject != null) {
//            listener.onClick(this, mSelectedProject);
//        }
//    }

    /**
     * interface to handle submitting the dialog
     */
//    public static interface OnClickListener {
//        /**
//         * Called when the user dismisses the dialog or selects a project
//         * @param dialog the dialog instance
//         * @param p the project that was selected. This will be null if the dialog was dismissed.
//         */
//        public void onClick(ProjectLibraryDialog dialog, Project p);
//    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("projects", mProjectList);
        outState.putSerializable("selected_project_id", mSelectedProjectId);
    }

    public static interface ProjectLibraryListener {
        public void onProjectLibrarySelected(ProjectLibraryDialog dialog, String projectId);
        public void onProjectLibraryDismissed(ProjectLibraryDialog dialog);
    }
}
