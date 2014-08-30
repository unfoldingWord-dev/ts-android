package com.door43.translationstudio.panes;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 8/26/2014.
 */
public class RightPaneFragment extends TranslatorBaseFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pane_right, container, false);
        return rootView;
    }
}
