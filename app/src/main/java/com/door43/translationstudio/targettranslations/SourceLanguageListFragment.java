package com.door43.translationstudio.targettranslations;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.library.Searchable;
import com.door43.translationstudio.util.AppContext;

/**
 * Created by joel on 9/7/2015.
 */
public class SourceLanguageListFragment extends Fragment implements Searchable{

    public static final String ARG_PROJECT_ID = "extra_project_id";
    private Library mLibrary;
    private SourceLanguageAdapter mAdapter;
    private OnItemClickListener mListener;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_source_language_list, container, false);

        Bundle args = getArguments();
        String projectId = args.getString(ARG_PROJECT_ID);
        mLibrary = AppContext.getLibrary();

        ListView list = (ListView) rootView.findViewById(R.id.list);
        mAdapter = new SourceLanguageAdapter(mLibrary.getSourceLanguages(projectId));
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.onItemClick(mAdapter.getItem(position));
            }
        });

        return rootView;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnItemClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnItemClickListener");
        }
    }

    @Override
    public void onSearchQuery(String query) {
        if(mAdapter != null) {
            mAdapter.getFilter().filter(query);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(SourceLanguage sourceLanguageId);
    }
}
