package com.door43.translationstudio.panes.left;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import com.door43.translationstudio.R;
import com.door43.translationstudio.panes.left.tabs.ChaptersTab;
import com.door43.translationstudio.panes.left.tabs.FramesTab;
import com.door43.translationstudio.panes.left.tabs.ProjectsTab;
import com.door43.translationstudio.util.TabsAdapter;
import com.door43.translationstudio.util.TabsAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.door43.widget.ScreenUtil;

/**
 * Created by joel on 8/26/2014.
 */
public class LeftPaneFragment extends TranslatorBaseFragment {
    private ViewPager mViewPager;
    private PagerSlidingTabStrip mSlidingTabLayout;
    private TabsAdapter mTabsAdapter;
//    private ArrayList<StringFragmentKeySet> tabs = new ArrayList<StringFragmentKeySet>();
    private int mDefaultPage = 0;
//    private int mSelectedTabColor = 0;
//    private ProjectsTab mProjectsTab = new ProjectsTab();
//    private ChaptersTab mChaptersTab = new ChaptersTab();
//    private FramesTab mFramesTab = new FramesTab();
    public static final int TAB_INDEX_PROJECTS = 0;
    public static final int TAB_INDEX_CHAPTERS = 1;
    public static final int TAB_INDEX_FRAMES = 2;
    private int mLayoutWidth = 0;
    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_pane_left, container, false);

        // tabs
        mViewPager = (ViewPager) mView.findViewById(R.id.leftViewPager);
        mTabsAdapter = new TabsAdapter(getActivity(), mViewPager);
        mTabsAdapter.addTab(getResources().getString(R.string.title_projects), ProjectsTab.class, null);
        mTabsAdapter.addTab(getResources().getString(R.string.title_chapters), ChaptersTab.class, null);
        mTabsAdapter.addTab(getResources().getString(R.string.title_frames), FramesTab.class, null);

        // Sliding tab layout
        mSlidingTabLayout = (PagerSlidingTabStrip) mView.findViewById(R.id.left_sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setTextColorResource(R.color.light_primary_text);
        mSlidingTabLayout.setTextSize(ScreenUtil.dpToPx(getActivity(), 20));

        selectTab(mDefaultPage);

        if(mLayoutWidth != 0) {
            mView.setLayoutParams(new ViewGroup.LayoutParams(mLayoutWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        return mView;
    }

    /**
     * Changes the selected tab
     * @param i
     */
    public void selectTab(int i) {
        if(mViewPager != null) {
//            if(tabs.size() > i && i >= 0) {
                // select the tab
            mViewPager.setCurrentItem(i);
            mTabsAdapter.notifyDataSetChanged();
                // notify the tab list adapter that it should reload
            TabsAdapterNotification page = (TabsAdapterNotification)mTabsAdapter.getFragmentForPosition(i);
            if(page != null) {
                page.NotifyAdapterDataSetChanged();
            }
//                ((TabsAdapterNotification)tabs.get(i).getFragment()).NotifyAdapterDataSetChanged();
//            }
        } else {
            mDefaultPage = i;
        }
    }

    /**
     * Returns the currently selected tab index
     * @return
     */
    public int getSelectedTabIndex() {
        if(mViewPager != null) {
            return mViewPager.getCurrentItem();
        } else {
            return 0;
        }
    }

    /**
     * Notifies the projects adapter that the dataset has changed
     */
    public void reloadProjectsTab() {
        if(mTabsAdapter != null) {
            TabsAdapterNotification page = (TabsAdapterNotification)mTabsAdapter.getFragmentForPosition(TAB_INDEX_PROJECTS);
            if(page != null) {
                page.NotifyAdapterDataSetChanged();
            }
        }
    }

    /**
     * Notifies the chapters adapter that the dataset has changed
     */
    public void reloadChaptersTab() {
        if(mTabsAdapter != null) {
            TabsAdapterNotification page = (TabsAdapterNotification)mTabsAdapter.getFragmentForPosition(TAB_INDEX_CHAPTERS);
            if(page != null) {
                page.NotifyAdapterDataSetChanged();
            }
        }
    }

    /**
     * Notifies the frames adapter that the dataset has changed
     */
    public void reloadFramesTab() {
        if(mTabsAdapter != null) {
            TabsAdapterNotification page = (TabsAdapterNotification)mTabsAdapter.getFragmentForPosition(TAB_INDEX_FRAMES);
            if(page != null) {
                page.NotifyAdapterDataSetChanged();
            }
        }
    }

    /**
     * Specifies the width of the layout
     * @param width
     */
    public void setLayoutWidth(int width) {
        if(mView != null) {
            mView.setLayoutParams(new ViewGroup.LayoutParams(mLayoutWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            mLayoutWidth = width;
        }
    }
}
