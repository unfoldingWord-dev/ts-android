package com.door43.translationstudio.panes.left.tabs;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AsyncTaskResultEvent;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 8/29/2014.
 */
public class ProjectsTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
    private ProjectsTabFragment me = this;
    private ProjectItemAdapter mProjectItemAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_left_projects, container, false);
        ListView listView = (ListView)view.findViewById(R.id.projects_list_view);

        // create adapter
        if(mProjectItemAdapter == null) mProjectItemAdapter = new ProjectItemAdapter(app());

        // connectAsync adapter
        listView.setAdapter(mProjectItemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // save changes to the current frame first
                ((MainActivity)me.getActivity()).save();
                if(app().getSharedProjectManager().getSelectedProject() == null || !app().getSharedProjectManager().getSelectedProject().getId().equals(((Project)mProjectItemAdapter.getItem(i)).getId())) {
                    // select the project
                    app().getSharedProjectManager().setSelectedProject(i);
                    // load the project source
                    new LoadProjectTask().execute();
                } else {
                    // select the project
                    app().getSharedProjectManager().setSelectedProject(i);
                    // reload the center pane so we don't accidently overwrite a frame
                    ((MainActivity)me.getActivity()).reloadCenterPane();
                    // open up the chapters tab
                    ((MainActivity)me.getActivity()).getLeftPane().selectTab(1);
                    // let the adapter redraw itself so the selected project is corectly highlighted
                    NotifyAdapterDataSetChanged();
                }
            }
        });

        return view;
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        if(mProjectItemAdapter != null) {
            mProjectItemAdapter.notifyDataSetChanged();
        }
    }

    private class LoadProjectTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            app().getSharedProjectManager().fetchProjectSource(app().getSharedProjectManager().getSelectedProject());
            return null;
        }

        protected void onPostExecute(Void result) {
            // reload the center pane so we don't accidently overwrite a frame
            ((MainActivity) me.getActivity()).reloadCenterPane();
            // open up the chapters tab
            ((MainActivity)me.getActivity()).getLeftPane().selectTab(1);
            // let the adapter redraw itself so the selected project is corectly highlighted
            NotifyAdapterDataSetChanged();
        }
    }
}
