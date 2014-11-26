package com.door43.translationstudio.panes.left;

import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.panes.left.tabs.ChaptersTabFragment;
import com.door43.translationstudio.panes.left.tabs.ProjectsTabFragment;
import com.door43.translationstudio.panes.left.tabs.FramesTabFragment;
import com.door43.translationstudio.util.StringFragmentKeySet;
import com.door43.translationstudio.util.TabbedViewPagerAdapter;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.slidingtabs.SlidingTabLayout;

import java.util.ArrayList;


/**
 * This class handles the tabbing for projects, chapters, and frames.
 */
public class LeftPaneSlidingTabsFragment extends Fragment {
    private ViewPager mViewPager;
    private SlidingTabLayout mSlidingTabLayout;
    private TabbedViewPagerAdapter tabbedViewPagerAdapter;
    private ArrayList<StringFragmentKeySet> tabs = new ArrayList<StringFragmentKeySet>();
    private int mDefaultPage = 0;
    private int mSelectedTabColor = 0;
    private ProjectsTabFragment mProjectsTab = new ProjectsTabFragment();
    private ChaptersTabFragment mChaptersTab = new ChaptersTabFragment();
    private FramesTabFragment mFramesTab = new FramesTabFragment();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tabbed_pager, container, false);

        if(tabs.size() == 0) {
            // Tabs
            tabs.add(new StringFragmentKeySet(getResources().getString(R.string.projects), mProjectsTab));
            tabs.add(new StringFragmentKeySet(getResources().getString(R.string.chapters), mChaptersTab));
            tabs.add(new StringFragmentKeySet(getResources().getString(R.string.frames), mFramesTab));
        }

        // ViewPager
        mViewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
        tabbedViewPagerAdapter = new TabbedViewPagerAdapter(getFragmentManager(), tabs);
        mViewPager.setAdapter(tabbedViewPagerAdapter);

        // Sliding tab layout
        mSlidingTabLayout = (SlidingTabLayout) rootView.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        if(mSelectedTabColor == 0) mSelectedTabColor = getResources().getColor(R.color.blue);
        mSlidingTabLayout.setSelectedIndicatorColors(mSelectedTabColor);
        mSlidingTabLayout.setDividerColors(Color.TRANSPARENT);

        // open the default page
        mViewPager.setCurrentItem(mDefaultPage);

        return rootView;
    }

    /**
     * Sets the selected tab index
     * @param i
     */
    public void selectTab(int i) {
        if(mViewPager != null) {
            if(tabs.size() > i && i >= 0) {
                // select the tab
                mViewPager.setCurrentItem(i);
                // notify the tab list adapter that it should reload
                ((TabsFragmentAdapterNotification)tabs.get(i).getFragment()).NotifyAdapterDataSetChanged();
            }
        } else {
            mDefaultPage = i;
        }
    }

    /**
     * Returns the currently selected tab index
     * @return
     */
    public int getSelectedTabIndex() {
        return mViewPager.getCurrentItem();
    }

    /**
     * Changes the color of the tabs selector
     * @param color a hexidecimal color value
     */
    public void setSelectedTabColor(int color) {
        if(mSlidingTabLayout != null) {
            mSlidingTabLayout.setSelectedIndicatorColors(color);
        } else {
            mSelectedTabColor = color;
        }
    }

    /**
     * Notifies the projects adapter that the dataset has changed
     */
    public void reloadProjectsTab() {
        mProjectsTab.NotifyAdapterDataSetChanged();
    }

    /**
     * Notifies the chapters adapter that the dataset has changed
     */
    public void reloadChaptersTab() {
        mChaptersTab.NotifyAdapterDataSetChanged();
    }

    /**
     * Notifies the frames adapter that the dataset has changed
     */
    public void reloadFramesTab() {
        mFramesTab.NotifyAdapterDataSetChanged();
    }
}
