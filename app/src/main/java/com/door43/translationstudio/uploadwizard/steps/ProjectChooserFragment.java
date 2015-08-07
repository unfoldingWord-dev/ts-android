package com.door43.translationstudio.uploadwizard.steps;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.LanguageSelectorActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ModelItemAdapter;
import com.door43.translationstudio.projects.Language;
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
    private static final int TARGET_LANGUAGE_REQUEST = 0;
    private ModelItemAdapter mAdapter;
    private Project mChosenProject = null;

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

        mAdapter = new ModelItemAdapter(AppContext.context(), new Model[0], false, false, R.drawable.icon_library_dark, R.drawable.icon_library_white, "translated_project_prep_tasks");
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Project p = (Project)mAdapter.getItem(position);
                if(p.getActiveTargetLanguages().length > 1) {
                    mChosenProject = p;
                    // let the user choose which target language to upload
                    Intent languageIntent = new Intent(getActivity(), LanguageSelectorActivity.class);
                    List<String> targetIds = new ArrayList<String>();
                    for(Language l:p.getActiveTargetLanguages()) {
                        targetIds.add(l.getId());
                    }
                    languageIntent.putExtra(LanguageSelectorActivity.EXTRAS_LANGUAGES, targetIds.toArray(new String[targetIds.size()]));
                    startActivityForResult(languageIntent, TARGET_LANGUAGE_REQUEST);
                } else {
                    ((UploadWizardActivity) getActivity()).setTranslationToUpload(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource(), p.getSelectedTargetLanguage());
                    onPrevious();
                }
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
                Project notes = new Project(p.getTitle() + " " + getActivity().getResources().getString(R.string.label_translation_notes), p.getId()+"-notes", p.getDescription());
                notes.addSourceLanguage(p.getSelectedSourceLanguage());
                for(Language target:p.getActiveTargetLanguages()) {
                    if(Project.isTranslatingNotes(p.getId(), target.getId())) {
                        notes.addTargetLanguage(target);
                    }
                }
                notes.setSelectedTargetLanguage(p.getSelectedTargetLanguage().getId());
                projects.add(notes);
            }
        }
        mAdapter.changeDataSet(projects.toArray(new Model[projects.size()]));
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == TARGET_LANGUAGE_REQUEST) {
            if(resultCode == getActivity().RESULT_OK) {
                String targetId = data.getExtras().getString(LanguageSelectorActivity.EXTRAS_CHOSEN_LANGUAGE);
                ((UploadWizardActivity) getActivity()).setTranslationToUpload(mChosenProject, mChosenProject.getSelectedSourceLanguage(), mChosenProject.getSelectedSourceLanguage().getSelectedResource(), AppContext.projectManager().getLanguage(targetId));
                onPrevious();
            }
        }
    }
}
