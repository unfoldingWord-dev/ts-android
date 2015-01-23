package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.events.ChoseProjectToImportEvent;
import com.door43.translationstudio.events.ProjectImportApprovalEvent;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.imports.ImportRequestInterface;
import com.door43.translationstudio.projects.imports.ProjectImport;
import com.door43.translationstudio.projects.imports.TranslationImport;
import com.door43.translationstudio.util.MainContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.logging.Handler;

/**
 * This dialog allows a user to browse a project dialog and make selections.
 */
public class ProjectLibraryDialog extends DialogFragment {
    // TODO: we need to finish implimenting this and replace ChooseProjectToImportDialog with it.

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.project_library);
        View v = inflater.inflate(R.layout.dialog_choose_project, container, false);

        // don't destroy this dialog on device rotations
        setRetainInstance(true);

        ListView listView = (ListView)v.findViewById(R.id.listView);
//        if(mModelList != null) {
//            if(mModelItemAdapter == null) mModelItemAdapter = new ModelItemAdapter(MainContext.getContext(), mModelList, false);
//            // connect adapter
//            listView.setAdapter(mModelItemAdapter);
//            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                    Model m = mModelItemAdapter.getItem(i);
//                    if (m.getClass().equals(PseudoProject.class)) {
//                        // re-load list
//                        mModelItemAdapter.changeDataSet(((PseudoProject) m).getChildren());
//                    } else {
//                        // return the selected project.
//                        Project p = (Project) m;
//                        MainContext.getEventBus().post(new ChoseProjectToImportEvent(mPeer, p, ChooseProjectToImportDialog.this));
//                        // NOTE: the caller should close this dialog
//                    }
//                }
//            });
//        } else {
//            listView.setAdapter(new ModelItemAdapter(MainContext.getContext(), new Model[]{},false));
//            dismiss();
//        }

        return v;
    }

    public Model[] parseLibrary(String library) {
        ArrayList<Model> projects = new ArrayList<Model>();
        android.os.Handler handle = new android.os.Handler(getActivity().getMainLooper());
        JSONArray json;
        try {
            json = new JSONArray(library);
        } catch (final JSONException e) {
            handle.post(new Runnable() {
                @Override
                public void run() {
                    MainContext.getContext().showException(e);
                }
            });
            return new Model[0];
        }

        // load the data
        for(int i=0; i<json.length(); i++) {
            try {
                JSONObject projectJson = json.getJSONObject(i);
                if (projectJson.has("id") && projectJson.has("project") && projectJson.has("language") && projectJson.has("target_languages")) {
                    Project p = new Project(projectJson.getString("id"));

                    // source language (just for project info)
                    JSONObject sourceLangJson = projectJson.getJSONObject("language");
                    String sourceLangDirection = sourceLangJson.getString("direction");
                    Language.Direction langDirection;
                    if(sourceLangDirection.toLowerCase().equals("ltr")) {
                        langDirection = Language.Direction.LeftToRight;
                    } else {
                        langDirection = Language.Direction.RightToLeft;
                    }
                    SourceLanguage sourceLanguage = new SourceLanguage(sourceLangJson.getString("slug"), sourceLangJson.getString("name"), langDirection, 0);
                    p.addSourceLanguage(sourceLanguage);
                    p.setSelectedSourceLanguage(sourceLanguage.getId());

                    // project info
                    JSONObject projectInfoJson = projectJson.getJSONObject("project");
                    p.setDefaultTitle(projectInfoJson.getString("name"));
                    if(projectInfoJson.has("description")) {
                        p.setDefaultDescription(projectInfoJson.getString("description"));
                    }

                    // meta (sudo projects)
                    // TRICKY: we are actually getting the meta names instead of the id's since we only receive one translation of the project info
                    if (projectInfoJson.has("meta")) {
                        JSONArray metaJson = projectInfoJson.getJSONArray("meta");
                        PseudoProject currentPseudoProject = null;
                        for(int j=0; j<metaJson.length(); j++) {
                            // create sudo project out of the meta name
                            PseudoProject sp  = new PseudoProject(metaJson.getString(j));
                            // link to parent sudo project
                            if(currentPseudoProject != null) {
                                currentPseudoProject.addChild(sp);
                            }
                            // add to project
                            p.addSudoProject(sp);
                            currentPseudoProject = sp;
                        }
                    }

                    // available translation languages
                    JSONArray languagesJson = projectJson.getJSONArray("target_languages");
                    for(int j=0; j<languagesJson.length(); j++) {
                        JSONObject langJson = languagesJson.getJSONObject(j);
                        String languageId = langJson.getString("slug");
                        String languageName = langJson.getString("name");
                        String direction  = langJson.getString("direction");
                        Language.Direction langDir;
                        if(direction.toLowerCase().equals("ltr")) {
                            langDir = Language.Direction.LeftToRight;
                        } else {
                            langDir = Language.Direction.RightToLeft;
                        }
                        Language l = new Language(languageId, languageName, langDir);
                        p.addTargetLanguage(l);
                    }
                    // finish linking the sudo projects together with the project so the menu can be rendered correctly
                    if(p.numSudoProjects() > 0) {
                        p.getSudoProject(p.numSudoProjects() - 1).addChild(p);
                        projects.add(p.getSudoProject(0));
                    } else {
                        projects.add(p);
                    }
                } else {
                    // TODO: invalid json
                }
            } catch(final JSONException e) {
                handle.post(new Runnable() {
                    @Override
                    public void run() {
                        MainContext.getContext().showException(e);
                    }
                });
            }
        }
        return projects.toArray(new Model[projects.size()]);
    }
}
