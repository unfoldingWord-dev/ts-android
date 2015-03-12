package com.door43.translationstudio.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 3/12/2015.
 */
public class LanguagesTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
    private LibraryLanguageAdapter mAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_library_languages, container, false);

        // TODO: prepare list view

        return view;
    }

    @Override
    public void NotifyAdapterDataSetChanged() {

    }
}
