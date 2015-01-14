package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.events.ProjectImportApprovalEvent;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 1/12/2015.
 */
public class ProjectImportApprovalDialog extends DialogFragment {
    private List<Project.ImportRequest> mRequests = new ArrayList<Project.ImportRequest>();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.import_project);
        View v = inflater.inflate(R.layout.dialog_import_approval, container, false);

        if(mRequests.size() > 0) {
            // load the adapter
            final ProjectImportAprovalAdapter adapter = new ProjectImportAprovalAdapter(this.getActivity());
            adapter.addImportStatuses(mRequests);

            ListView list = (ListView)v.findViewById(R.id.importListView);
            Button cancelButton = (Button)v.findViewById(R.id.buttonCancel);
            final Button okButton = (Button)v.findViewById(R.id.buttonOk);
            list.setAdapter(adapter);

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Project.ImportRequest item = adapter.getItem(i);
                    item.setIsApproved(!item.isApproved());
                    adapter.notifyDataSetChanged();
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    close();
                }
            });

            // TODO: we need to disable the ok button until at least one language is approved for import.
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainContext.getEventBus().post(new ProjectImportApprovalEvent(adapter.getImportStatuses()));
                    close();
                }
            });
        } else {
            close();
        }

        return v;
    }

    /**
     * Specifies the model list to use in the dialog.
     * This must be called before showing the dialog.
     */
    public void setImportRequests(List<Project.ImportRequest> requests) {
        mRequests = requests;
    }

    public void close() {
        // the import has been canceled so we need to clean up after ourselves
        for(Project.ImportRequest r:mRequests) {
            Project.cleanImport(r);
        }
        dismiss();
    }
}
