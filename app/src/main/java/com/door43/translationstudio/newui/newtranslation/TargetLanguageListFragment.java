package com.door43.translationstudio.newui.newtranslation;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.newui.library.Searchable;
import com.door43.translationstudio.newui.BaseFragment;
import com.door43.translationstudio.App;

/**
 * Created by joel on 9/4/2015.
 */
public class TargetLanguageListFragment extends BaseFragment implements Searchable {
    private OnItemClickListener mListener;
    private TargetLanguageAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_language_list, container, false);

        ListView list = (ListView) rootView.findViewById(R.id.list);
        mAdapter = new TargetLanguageAdapter(App.getLibrary().getTargetLanguages());
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.onItemClick(mAdapter.getItem(position));
            }
        });

        EditText searchView = (EditText) rootView.findViewById(R.id.search_text);
        searchView.setHint(R.string.choose_target_language);
        searchView.setEnabled(false);
        ImageButton searchBackButton = (ImageButton) rootView.findViewById(R.id.search_back_button);
        searchBackButton.setVisibility(View.GONE);
        ImageView searchIcon = (ImageView) rootView.findViewById(R.id.search_mag_icon);
        searchIcon.setVisibility(View.GONE);

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
        void onItemClick(TargetLanguage targetLanguage);
    }
}
