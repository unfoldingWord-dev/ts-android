package com.door43.translationstudio.panes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 8/26/2014.
 */
public class TopPaneFragment extends TranslatorBaseFragment {
    Button mButtonSync;
    ImageButton mButtonResources;
    ImageButton mButtonSettings;
    ImageButton mButtonShare;
    ImageButton mButtonUser;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pane_top, container, false);

        // set up buttons clicks
        mButtonSync = (Button)rootView.findViewById(R.id.buttonSync);
        mButtonSettings = (ImageButton)rootView.findViewById(R.id.buttonSettings);
        mButtonShare = (ImageButton)rootView.findViewById(R.id.buttonShare);

        mButtonSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app().getSharedTranslationManager().syncSelectedProject();
            }
        });
        mButtonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            }
        });
        mButtonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app().showToastMessage("You clicked share!");
            }
        });

        return rootView;
    }

    /**
     * Triggered whenever the pane is opened
     */
    public void onOpen() {

    }
}
