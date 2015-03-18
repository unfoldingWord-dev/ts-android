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
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;

/**
 * Created by joel on 3/12/2015.
 */
public class LanguagesTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification, ManagedTask.OnFinishedListener {
    private LibraryLanguageAdapter mAdapter;
    private Project mProject;
    public static final String DOWNLOAD_LANGUAGE_PREFIX = "download-language-";

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_library_languages, container, false);

        if (getArguments().containsKey(ProjectLibraryDetailFragment.ARG_ITEM_INDEX)) {
            mProject = LibraryTempData.getProject(getArguments().getInt(ProjectLibraryDetailFragment.ARG_ITEM_INDEX));
        }

        mAdapter = new LibraryLanguageAdapter(AppContext.context(), mProject.getId());

        ListView list = (ListView)view.findViewById(R.id.listView);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                SourceLanguage lang = mAdapter.getItem(i);
                connectDownloadTask(lang);
            }
        });

        populateList();

        return view;
    }

    /**
     * Begins or connects to an existing download
     * @param language
     */
    private void connectDownloadTask(SourceLanguage language) {
        String taskId = DOWNLOAD_LANGUAGE_PREFIX +mProject.getId()+"-"+language.getId();
        DownloadLanguageTask task = (DownloadLanguageTask) TaskManager.getTask(taskId);
        if(task == null) {
            // start new download
            task = new DownloadLanguageTask(mProject, language);
//            task.setOnFinishedListener(LanguagesTabFragment.this);
            TaskManager.addTask(task, taskId);
            mAdapter.notifyDataSetChanged();
        } else {
            // attach to existing task
//            task.setOnFinishedListener(LanguagesTabFragment.this);
        }
    }

    private void populateList() {
        if(mProject != null && mAdapter != null) {
            mAdapter.changeDataSet(mProject.getSourceLanguages());
        }
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        populateList();
    }

    @Override
    public void onFinished(ManagedTask task) {
        // TODO: the source has finished downloading
    }
}
