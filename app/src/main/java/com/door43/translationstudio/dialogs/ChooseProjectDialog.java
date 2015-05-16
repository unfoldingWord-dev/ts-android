package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.TaskManager;
import com.door43.util.tasks.ThreadableUI;

/**
 * This dialog displays a list view that allows the user to dig down through projects and meta projects.
 * When a real project is selected an event containing the project will be fired and the dialog dismissed.
 * If metaId is provided as a bundled argument that meta project's children will be displayed in the list
 * otherwise all listable projects will be shown.
 * setModelList takes presedence over the metaId.
 */
public class ChooseProjectDialog extends DialogFragment {
    private ModelItemAdapter mModelItemAdapter;
    private DialogInterface.OnDismissListener mDismissListener;
    public static final String ARG_META_ID = "metaId";
    private String mMetaId;
    private static final String GROUP_TASK_ID = "select_project_list_group";
    private OnSuccessListener mSuccessListener;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.title_projects);
        View v = inflater.inflate(R.layout.dialog_choose_project, container, false);

        ListView listView = (ListView)v.findViewById(R.id.listView);

        Bundle args = getArguments();
        mMetaId = args.getString(ARG_META_ID);

        mModelItemAdapter = new ModelItemAdapter(AppContext.context(), new Model[]{}, GROUP_TASK_ID);

        if(mMetaId != null) {
            // connect adapter
            listView.setAdapter(mModelItemAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Model m = mModelItemAdapter.getItem(i);
                    if(m.getClass().equals(PseudoProject.class)) {
                        // re-load list
                        mModelItemAdapter.changeDataSet(((PseudoProject)m).getChildren());
                    } else {
                        // return the selected project.
                        Project p = (Project)m;
                        if(mSuccessListener != null) {
                            try {
                                mSuccessListener.onSuccess(ChooseProjectDialog.this.getDialog(), p);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        dismiss();
                    }
                }
            });
        } else {
            dismiss();
        }

        populateList();
        return v;
    }

    /**
     * Loads the list async
     */
    private void populateList() {
        new ThreadableUI(getActivity()) {
            private PseudoProject p;
            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                p = AppContext.projectManager().getPseudoProject(mMetaId);
                if(p!=null) {
                    p.sortChildren();
                }
            }

            @Override
            public void onPostExecute() {
                if(p != null) {
                    mModelItemAdapter.changeDataSet(p.getChildren());
                }
            }
        }.start();
    }

    public void onDismiss(DialogInterface dialog) {
        TaskManager.killGroup(GROUP_TASK_ID);
        if(mDismissListener != null) {
            mDismissListener.onDismiss(dialog);
        }
        super.onDismiss(dialog);
    }

    /**
     * Sets the listener to be called when the dialog is dismissed
     * @param listener
     */
    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mDismissListener = listener;
    }

    /**
     * Sets the listener to be called when a project is selected
     * @param listener
     */
    public void setOnSuccessListener(OnSuccessListener listener) {
        mSuccessListener = listener;
    }

    public interface OnSuccessListener {
        void onSuccess(DialogInterface dialog, Project project);
    }
}
