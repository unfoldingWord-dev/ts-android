package com.door43.translationstudio.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.library.temp.LibraryTempData;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.tasks.DownloadLanguageTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.door43.util.threads.TaskManager;
import com.door43.util.threads.ThreadableUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 3/12/2015.
 */
public class LanguagesTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
    private LibraryLanguageAdapter mAdapter;
    private Project mProject;
    public static final String DOWNLOAD_LANGUAGE_PREFIX = "download-language-";

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_library_languages, container, false);

        if (getArguments().containsKey(ProjectLibraryDetailFragment.ARG_ITEM_ID)) {
            String id = getArguments().getString(ProjectLibraryDetailFragment.ARG_ITEM_ID);
            mProject = LibraryTempData.getProject(id);
        }

        mAdapter = new LibraryLanguageAdapter(AppContext.context(), mProject.getId(), DOWNLOAD_LANGUAGE_PREFIX, false);

        ListView list = (ListView)view.findViewById(R.id.listView);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(LibraryTempData.getEnableEditing()) {
                    // TODO: place all of this in a task
                    SourceLanguage lang = mAdapter.getItem(i);
                    AppContext.projectManager().deleteSourceLanguage(mProject.getId(), lang.getId());
                    LibraryTempData.organizeProjects();
                    mAdapter.notifyDataSetChanged();
                    if(!AppContext.projectManager().isProjectDownloaded(mProject.getId()) && getActivity() != null && getActivity() instanceof LibraryCallbacks) {
                        ((LibraryCallbacks)getActivity()).refreshUI();
                    }
                } else {
                    SourceLanguage lang = mAdapter.getItem(i);
                    connectDownloadTask(lang);
                }
            }
        });

        populateList();

        return view;
    }

    /**
     * Begins a new download if one is not already in progress
     * @param language
     */
    private void connectDownloadTask(SourceLanguage language) {
        String taskId = DOWNLOAD_LANGUAGE_PREFIX +mProject.getId()+"-"+language.getId();
        DownloadLanguageTask task = (DownloadLanguageTask) TaskManager.getTask(taskId);
        if(task == null) {
            // start new download
            task = new DownloadLanguageTask(mProject, language);
            TaskManager.addTask(task, taskId);
            // NOTE: the LibraryLanguageAdapter handles the onProgress and onFinish events
            mAdapter.notifyDataSetChanged();
        }
    }

    private void populateList() {
        // filter languages
        // TODO: it would be safer to put this in the task manager
        new ThreadableUI(getActivity()) {
            List<SourceLanguage> languages = new ArrayList<>();
            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                if(mProject != null) {
                    for(SourceLanguage l: mProject.getSourceLanguages()) {
                        if(l.checkingLevel() >= AppContext.context().getResources().getInteger(R.integer.min_source_lang_checking_level)) {
                            if(LibraryTempData.getShowNewProjects() && !LibraryTempData.getShowProjectUpdates()) {
                                if(!AppContext.projectManager().isSourceLanguageDownloaded(mProject.getId(), l.getId())) {
                                    languages.add(l);
                                }
                            } else if(LibraryTempData.getShowProjectUpdates() && !LibraryTempData.getShowNewProjects()) {
                                if(AppContext.projectManager().isSourceLanguageDownloaded(mProject.getId(), l.getId())) {
                                    languages.add(l);
                                }
                            } else {
                                languages.add(l);
                            }
                        }
                    }
                }
            }

            @Override
            public void onPostExecute() {
                if(mAdapter != null) {
                    mAdapter.changeDataSet(languages);
                }
            }
        }.start();
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        if(mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }
}
