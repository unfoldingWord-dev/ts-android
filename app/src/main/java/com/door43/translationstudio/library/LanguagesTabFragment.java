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
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 3/12/2015.
 */
public class LanguagesTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
    private LibraryLanguageAdapter mAdapter;
    private Project mProject;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_library_languages, container, false);

        if (getArguments().containsKey(ProjectLibraryDetailFragment.ARG_ITEM_INDEX)) {
            mProject = LibraryTempData.getProject(getArguments().getInt(ProjectLibraryDetailFragment.ARG_ITEM_INDEX));
        }

        mAdapter = new LibraryLanguageAdapter(AppContext.context());

        ListView list = (ListView)view.findViewById(R.id.listView);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // TODO: begin downloading (after confirmation)
            }
        });

        populateList();

        return view;
    }

    private void populateList() {
        if(mProject != null && mAdapter != null) {
            // TODO: sort and filter lanaguages
            mAdapter.changeDataSet(mProject.getSourceLanguages().toArray(new SourceLanguage[mProject.getSourceLanguages().size()]));
        }
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        populateList();
    }
}
