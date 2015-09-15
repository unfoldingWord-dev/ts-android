package com.door43.translationstudio.targettranslations;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.util.AppContext;

import java.security.InvalidParameterException;

/**
 * Created by joel on 9/14/2015.
 */
public class FirstTabFragment extends Fragment implements TargetTranslationDetailActivityListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_first_tab, container, false);

        Bundle args = getArguments();
        String targetTranslationId = args.getString(TargetTranslationDetailActivity.EXTRA_TARGET_TRANSLATION_ID, null);
        TargetTranslation translation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);
        if(translation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        // TODO: set up view for adding a new first tab

        // TODO: ask the user to add a tab.
        // we will probably notify the actvity to swap out the fragments and display one
        // for creating a new first tab
        // then it should redirect back to the last open mode

        return rootView;
    }

    @Override
    public void onScrollProgressUpdate(int scrollProgress) {

    }
}
