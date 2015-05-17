package com.door43.translationstudio.uploadwizard.steps.choose;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ModelItemAdapter;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.wizard.WizardFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/14/2015.
 */
public class ProjectChooserFragment extends WizardFragment {
    private ModelItemAdapter mAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_project_chooser, container, false);
        ListView list = (ListView)v.findViewById(R.id.uploadProjectListView);
        Button backBtn = (Button)v.findViewById(R.id.backButton);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrevious();
            }
        });

        mAdapter = new ModelItemAdapter(AppContext.context(), new Model[0], "translated_project_prep_tasks");
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: allow the user to choose the target language if there are multiple being translated.
                Project p = (Project)mAdapter.getItem(position);
                ((UploadWizardActivity)getActivity()).setTranslationToUpload(p, p.getSelectedSourceLanguage(), p.getSelectedTargetLanguage());
                onPrevious();
            }
        });

        // load the data
        // TODO: we might want to place this into a task
        List<Project> projects = new ArrayList<>();
        for(Project p:AppContext.projectManager().getProjects()) {
            // add project being translated
            if(p.isTranslating() || p.isTranslatingGlobal()) {
                projects.add(p);
            }
            // include translation notes
            if(p.isTranslatingNotes() || p.isTranslatingNotesGlobal()) {
                // TODO: create a new project to hold the translation notes and add it to the list
            }
        }
        mAdapter.changeDataSet(projects.toArray(new Model[projects.size()]));
        return v;
    }
}
