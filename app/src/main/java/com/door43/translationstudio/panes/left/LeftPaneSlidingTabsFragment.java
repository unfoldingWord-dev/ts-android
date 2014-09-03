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
    private int defaultPage = 0;
    private int selectedTabColor = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tabbed_pager, container, false);

        if(tabs.size() == 0) {
            // Tabs
            tabs.add(new StringFragmentKeySet(getResources().getString(R.string.projects), new ProjectsTabFragment()));
            tabs.add(new StringFragmentKeySet(getResources().getString(R.string.chapters), new ChaptersTabFragment()));
            tabs.add(new StringFragmentKeySet(getResources().getString(R.string.frames), new FramesTabFragment()));
        }

        // ViewPager
        mViewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
        tabbedViewPagerAdapter = new TabbedViewPagerAdapter(getFragmentManager(), tabs);
        mViewPager.setAdapter(tabbedViewPagerAdapter);

        // Sliding tab layout
        mSlidingTabLayout = (SlidingTabLayout) rootView.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        if(selectedTabColor == 0) selectedTabColor = getResources().getColor(R.color.tab_color);
        mSlidingTabLayout.setSelectedIndicatorColors(selectedTabColor);
        mSlidingTabLayout.setDividerColors(Color.TRANSPARENT);

        // open the default page
        mViewPager.setCurrentItem(defaultPage);

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
            defaultPage = i;
        }
    }

    /**
     * Changes the color of the tabs selector
     * @param color a hexidecimal color value
     */
    public void setSelectedTabColor(int color) {
        if(mSlidingTabLayout != null) {
            mSlidingTabLayout.setSelectedIndicatorColors(color);
        } else {
            selectedTabColor = color;
        }
    }
}
