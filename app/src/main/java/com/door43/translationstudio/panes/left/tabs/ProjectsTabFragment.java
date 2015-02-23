package com.door43.translationstudio.panes.left.tabs;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ChooseProjectDialog;
import com.door43.translationstudio.dialogs.ModelItemAdapter;
import com.door43.translationstudio.events.ChoseProjectEvent;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.squareup.otto.Subscribe;

/**
 * Created by joel on 8/29/2014.
 */
public class ProjectsTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
    private ModelItemAdapter mModelItemAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_left_projects, container, false);
        ListView listView = (ListView)view.findViewById(R.id.projects_list_view);

        // create adapter
        if(mModelItemAdapter == null) mModelItemAdapter = new ModelItemAdapter(app(), AppContext.projectManager().getListableProjects());

        // connectAsync adapter
        listView.setAdapter(mModelItemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // TRICKY: the project list may contain meta projects as well as normal projects.

                if(getActivity() != null) {
                    // save changes to the current frame first
                    ((MainActivity) getActivity()).save();
                    Model m = mModelItemAdapter.getItem(i);
                    boolean isProject = m.getClass().equals(Project.class);

                    if (isProject) {
                        // this is a normal project
                        handleProjectSelection((Project) m);
                    } else {
                        // this is a meta project
                        handleMetaSelection((PseudoProject) m);
                    }
                } else {
                    Logger.e(this.getClass().getName(), "onItemClickListener the activity is null");
                }
            }
        });

        return view;
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        if(mModelItemAdapter != null && app() != null && AppContext.projectManager() != null) {
            mModelItemAdapter.notifyDataSetChanged();
            mModelItemAdapter.changeDataSet(AppContext.projectManager().getListableProjects());
        }
    }

    /**
     * This handles the selection of a project.
     * @param p
     */
    private void handleProjectSelection(Project p) {
        Activity activity = getActivity();
        if(activity != null) {
            // this is a normal project
            if (AppContext.projectManager().getSelectedProject() == null || !AppContext.projectManager().getSelectedProject().getId().equals(p.getId())) {
                // reload the center pane so we don't accidently overwrite a frame
                ((MainActivity) activity).reloadCenterPane();

                AppContext.projectManager().setSelectedProject(p.getId());
                // load the project source
                new LoadProjectTask().execute();
            } else {
                // select the project
                AppContext.projectManager().setSelectedProject(p.getId());
                // reload the center pane so we don't accidently overwrite a frame
                ((MainActivity) activity).reloadCenterPane();
                // open up the chapters tab
                ((MainActivity) activity).getLeftPane().selectTab(1);
                // let the adapter redraw itself so the selected project is corectly highlighted
                NotifyAdapterDataSetChanged();
            }
        } else {
            Logger.e(this.getClass().getName(), "handleProjectSelection the activity is null");
        }
    }

    /**
     * Handles the selection of a meta project
     * @param p
     */
    private void handleMetaSelection(PseudoProject p) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        app().closeToastMessage();
        // Create and show the dialog.
        ChooseProjectDialog newFragment = new ChooseProjectDialog();
        Bundle args = new Bundle();
        args.putString("metaId", p.getId());
        newFragment.setArguments(args);
        newFragment.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Activity activity = getActivity();
                if(activity != null) {
                    activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                } else {
                    Logger.e(this.getClass().getName(), "handleMetaSelection the activity is null");
                }
            }
        });
        newFragment.show(ft, "dialog");
    }

    private class LoadProjectTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            AppContext.projectManager().fetchProjectSource(AppContext.projectManager().getSelectedProject());
            return null;
        }

        protected void onPostExecute(Void result) {
            Activity activity = getActivity();
            if(activity != null) {
                // populate the center pane
                ((MainActivity) activity).reloadCenterPane();
                // open up the chapters tab
                ((MainActivity) activity).getLeftPane().selectTab(1);
                // reload the frames tab so we don't see frames from the previous project
                ((MainActivity) activity).getLeftPane().reloadFramesTab();
                // let the adapter redraw itself so the selected project is corectly highlighted
            } else {
                Logger.e(this.getClass().getName(), "onPostExecute the activity is null");
            }
            NotifyAdapterDataSetChanged();
        }
    }

    @Subscribe
    public void onSelectedProjectFromMeta(ChoseProjectEvent event) {
        handleProjectSelection(event.getProject());
        event.getDialog().dismiss();
        if(getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } else {
            Logger.e(this.getClass().getName(), "onSelectedProjectFromMeta the activity is null");
        }
    }
}
