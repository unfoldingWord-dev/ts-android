package com.door43.translationstudio.newui.newtranslation;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.ProjectCategory;
import com.door43.translationstudio.newui.library.Searchable;
import com.door43.translationstudio.newui.BaseFragment;
import com.door43.translationstudio.util.AppContext;

import java.util.Locale;

/**
 * Created by joel on 9/4/2015.
 */
public class ProjectListFragment extends BaseFragment implements Searchable {
    private OnItemClickListener mListener;
    private Library mLibrary;
    private ProjectCategoryAdapter mAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_list, container, false);

        mLibrary = AppContext.getLibrary();

        ListView list = (ListView) rootView.findViewById(R.id.list);
        mAdapter = new ProjectCategoryAdapter(mLibrary.getProjectCategories(Locale.getDefault().getLanguage()));
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ProjectCategory category = mAdapter.getItem(position);
                if(category.isProject()) {
                    mListener.onItemClick(category.projectId);
                } else {
                    // TODO: we need to display another back arrow to back up a level in the categories
                    mAdapter.changeData(mLibrary.getProjectCategories(category));
                }
            }
        });

        // TODO: set up update button

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
        void onItemClick(String projectId);
    }
}
