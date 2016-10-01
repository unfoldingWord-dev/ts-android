package com.door43.translationstudio.newui.newtranslation;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.newui.library.Searchable;
import com.door43.translationstudio.newui.BaseFragment;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.SourceLanguage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by joel on 9/7/2015.
 * we were using this to choose the first tab when a user created a new translation project but now
 * we want them to go through the process of manually choosing their first tab.
 */
@Deprecated
public class SourceLanguageListFragment extends BaseFragment implements Searchable{

    public static final String ARG_PROJECT_ID = "extra_project_id";
    private Door43Client mLibrary;
    private SourceLanguageAdapter mAdapter;
    private OnItemClickListener mListener;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_language_list, container, false);

        Bundle args = getArguments();
        String projectId = args.getString(ARG_PROJECT_ID);
        mLibrary = App.getLibrary();

        ListView list = (ListView) rootView.findViewById(R.id.list);

        // TRICKY: we convert the source translations to source languages so we only get
        // languages that meet the minimum checking level
        SourceTranslation[] sourceTranslations = mLibrary.getSourceTranslations(projectId);
        Map<String, SourceLanguage> sourceLanguages = new HashMap<>();
        for(SourceTranslation sourceTranslation:sourceTranslations) {
            SourceLanguage sourceLanguage = mLibrary.index().getSourceLanguage(sourceTranslation.language.slug);
            // TRICKY: a source language could be represented several times due to multiple resources
            if(!sourceLanguages.containsKey(sourceLanguage.slug)) {
                sourceLanguages.put(sourceLanguage.slug, sourceLanguage);
            }
        }
        mAdapter = new SourceLanguageAdapter(sourceLanguages.values().toArray(new SourceLanguage[sourceLanguages.size()]));
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
