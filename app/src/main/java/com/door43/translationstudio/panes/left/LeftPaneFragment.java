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
    private int mLayoutWidth = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_pane_left, container, false);

        // set up tabs
        getFragmentManager().beginTransaction().replace(R.id.left_pane_tabs, mTabsPager).addToBackStack(null).commit();
        mTabsPager.selectTab(0);

        if(mLayoutWidth != 0) {
            rootView.setLayoutParams(new ViewGroup.LayoutParams(mLayoutWidth, ViewGroup.LayoutParams.FILL_PARENT));
        }

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
     * Returns the currently selected tab index
     * @return
     */
    public int getSelectedTabIndex() {
        return mTabsPager.getSelectedTabIndex();
    }

    /**
     * Reloads the projects tab
     */
    public void reloadProjectsTab() {
        mTabsPager.reloadProjectsTab();
    }

    /**
     * Reloads the chapters tab
     */
    public void reloadChaptersTab() {
        mTabsPager.reloadChaptersTab();
    }

    /**
     * Reloads the frames tab
     */
    public void reloadFramesTab() {
        mTabsPager.reloadFramesTab();
    }

    /**
     * Specifies the width of the layout
     * @param width
     */
    public void setLayoutWidth(int width) {
        mLayoutWidth = width;
    }
}
