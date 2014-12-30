package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;

/**
  * Created by joel on 12/29/2014.
  */
 public class LanguageResourceDialog extends DialogFragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.projects);
        View v = inflater.inflate(R.layout.dialog_language_resources, container, false);

        Bundle args = getArguments();
        String id = args.getString("projectId");
        Project p = MainContext.getContext().getSharedProjectManager().getProject(id);
        if(p != null) {
            ListView listView = (ListView)v.findViewById(R.id.listView);

            // TODO: set up adapter and display the resources
        } else {
            dismiss();
        }
        return v;
    }
 }
