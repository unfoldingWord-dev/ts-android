package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.events.LanguageResourceSelectedEvent;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.util.MainContext;

/**
  * Created by joel on 12/29/2014.
  */
 public class LanguageResourceDialog extends DialogFragment {
    private LanguageResourceAdapter mLanguageResourceAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.language_resources);
        View v = inflater.inflate(R.layout.dialog_language_resources, container, false);

        Bundle args = getArguments();
        String id = args.getString("projectId");
        Project p = MainContext.getContext().getSharedProjectManager().getProject(id);
        if(p != null) {
            ListView listView = (ListView)v.findViewById(R.id.listView);
            if(mLanguageResourceAdapter == null) mLanguageResourceAdapter = new LanguageResourceAdapter(MainContext.getContext(),  p.getSelectedSourceLanguage().getResources(), p.getSelectedSourceLanguage().getSelectedResource());
            listView.setAdapter(mLanguageResourceAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Resource r = mLanguageResourceAdapter.getItem(i);
                    MainContext.getEventBus().post(new LanguageResourceSelectedEvent(r));
                    dismiss();
                }
            });
        } else {
            dismiss();
        }
        return v;
    }
 }
