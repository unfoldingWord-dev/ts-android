package com.door43.translationstudio.panes.left;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 8/26/2014.
 */
public class LeftPaneFragment extends TranslatorBaseFragment {
    private LeftPaneSlidingTabsFragment mTabsPager = new LeftPaneSlidingTabsFragment();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_pane_left, container, false);

        // set up tabs
        getFragmentManager().beginTransaction().replace(R.id.left_pane_tabs, mTabsPager).addToBackStack(null).commit();
        mTabsPager.selectTab(0);

        return rootView;
    }

    /**
     * Changes the selected tab
     * @param index
     */
    public void selectTab(int index) {
        mTabsPager.selectTab(index);
    }

    /**
     * Triggered whenever the pane is opened
     */
    public void onOpen() {
        // always start with the first tab
        selectTab(0);
    }
}
