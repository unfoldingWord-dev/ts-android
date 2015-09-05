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
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.util.AppContext;

/**
 * Created by joel on 9/4/2015.
 */
public class TargetLanguageListFragment extends Fragment {
    private OnItemClickListener mListener;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_target_language_list, container, false);

        ListView list = (ListView) rootView.findViewById(R.id.list);
        final TargetLanguageAdapter adapter = new TargetLanguageAdapter(AppContext.getLibrary().getTargetLanguages());
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.onItemClick(adapter.getItem(position));
            }
        });

        // TODO: set up search

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

    public interface OnItemClickListener {
        void onItemClick(TargetLanguage targetLanguage);
    }
}
