package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.events.SelectedProjectFromMetaEvent;
import com.door43.translationstudio.panes.left.tabs.ModelItemAdapter;
import com.door43.translationstudio.projects.MetaProject;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 12/23/2014.
 */
public class MetaProjectDialog extends DialogFragment {
    private ModelItemAdapter mModelItemAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.projects);
        View v = inflater.inflate(R.layout.dialog_meta_project, container, false);

        Bundle args = getArguments();
        final String id = args.getString("metaId");
        MetaProject p = MainContext.getContext().getSharedProjectManager().getMetaProject(id);
        if(p != null) {
            ListView listView = (ListView)v.findViewById(R.id.listView);
            // create adapter
            if(mModelItemAdapter == null) mModelItemAdapter = new ModelItemAdapter(MainContext.getContext(), p.getChildren());

            // connect adapter
            listView.setAdapter(mModelItemAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Model m = mModelItemAdapter.getItem(i);
                    if(m.getClass().equals(MetaProject.class)) {
                        // re-load list
                        mModelItemAdapter.changeDataSet(((MetaProject)m).getChildren());
                    } else {
                        // return the selected project.
                        Project p = (Project)m;
                        MainContext.getEventBus().post(new SelectedProjectFromMetaEvent(p));
                        dismiss();
                    }
                }
            });
        } else {
            dismiss();
        }

        return v;
    }
}
