package com.door43.translationstudio.ui.home;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.BaseFragment;

/**
 * Displays a welcome message with instructions about creating target translations
 */
public class WelcomeFragment extends BaseFragment {

    private OnCreateNewTargetTranslation mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_target_translation_welcome, container, false);

        // new project Button
        Button createTargetTranslationButton = (Button) rootView.findViewById(R.id.extraAddTargetTranslationButton);
        if(createTargetTranslationButton != null) {
            createTargetTranslationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onCreateNewTargetTranslation();
                }
            });
        }

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnCreateNewTargetTranslation) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCreateNewTargetTranslation");
        }
    }
    public interface OnCreateNewTargetTranslation {
        void onCreateNewTargetTranslation();
    }
}
