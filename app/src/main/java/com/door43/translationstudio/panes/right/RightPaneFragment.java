package com.door43.translationstudio.panes.right;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import com.door43.translationstudio.R;
import com.door43.translationstudio.panes.right.tabs.NotesTab;
import com.door43.translationstudio.panes.right.tabs.TermsTab;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.util.TabsAdapter;
import com.door43.translationstudio.util.TabsAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 8/26/2014.
 */
public class RightPaneFragment extends TranslatorBaseFragment {
    private ViewPager mViewPager;
    private PagerSlidingTabStrip mSlidingTabLayout;
    private TabsAdapter mTabsAdapter;
//    private ArrayList<StringFragmentKeySet> tabs = new ArrayList<StringFragmentKeySet>();
    private int mDefaultPage = 0;
    private int mSelectedTabColor = 0;
//    private NotesTab mNotesTab = new NotesTab();
//    private TermsTab mTermsTab = new TermsTab();
    private int mLayoutWidth = 0;
    private View mView;
    private static final int TAB_INDEX_NOTES = 0;
    private static final int TAB_INDEX_TERMS = 1;
    private static final String STATE_NOTES_SCROLL_X = "notes_scroll_x";
    private static final String STATE_NOTES_SCROLL_Y = "notes_scroll_y";
    private static final String STATE_SELECTED_TAB = "selected_tab_index";
    private int mSelectedTab = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_pane_right, container, false);

        if(savedInstanceState != null) {
            // restore scroll position
            mSelectedTab = savedInstanceState.getInt(STATE_SELECTED_TAB, 0);
            mDefaultPage = mSelectedTab;
            // TODO: give scroll to notes tab
//            mNotesTab.setScroll(new Pair<>(savedInstanceState.getInt(STATE_NOTES_SCROLL_X), savedInstanceState.getInt(STATE_NOTES_SCROLL_Y)));
        }

        // tabs
        mViewPager = (ViewPager) mView.findViewById(R.id.rightViewPager);
        mTabsAdapter = new TabsAdapter(getActivity(), mViewPager);
        mTabsAdapter.addTab(getResources().getString(R.string.label_translation_notes), NotesTab.class, null);
        mTabsAdapter.addTab(getResources().getString(R.string.translation_words), TermsTab.class, null);

        // Sliding tab layout
        mSlidingTabLayout = (PagerSlidingTabStrip) mView.findViewById(R.id.right_sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        if(mSelectedTabColor == 0) mSelectedTabColor = getResources().getColor(R.color.purple);
        mSlidingTabLayout.setIndicatorColor(mSelectedTabColor);
        mSlidingTabLayout.setDividerColor(Color.TRANSPARENT);

        // open the default page
        selectTab(mDefaultPage);

        if(mLayoutWidth != 0) {
            mView.setLayoutParams(new ViewGroup.LayoutParams(mLayoutWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        return mView;
    }

    /**
     * Sets the selected tab index
     * @param i
     */
    public void selectTab(int i) {
        if(mViewPager != null) {
//            if(tabs.size() > i && i >= 0) {
                // select the tab
            mViewPager.setCurrentItem(i);
            TabsAdapterNotification page = (TabsAdapterNotification)mTabsAdapter.getFragmentForPosition(i);
            if(page != null) {
                page.NotifyAdapterDataSetChanged();
            }
                // notify the tab list adapter that it should reload
//                ((TabsAdapterNotification)tabs.get(i).getFragment()).NotifyAdapterDataSetChanged();
//            }
        } else {
            mDefaultPage = i;
        }
        mSelectedTab = i;
    }

    /**
     * Returns the currently selected tab index
     * @return
     */
    public int getSelectedTabIndex() {
        return mViewPager.getCurrentItem();
    }

//    /**
//     * Changes the color of the tabs selector
//     * @param color a hexidecimal color value
//     */
//    public void setSelectedTabColor(int color) {
//        if(mSlidingTabLayout != null) {
//            mSlidingTabLayout.setIndicatorColor(color);
//        } else {
//            mSelectedTabColor = color;
//        }
//    }

    /**
     * Notifies the notes adapter that the dataset has changed
     */
    public void reloadNotesTab() {
        if(mTabsAdapter != null) {
            TabsAdapterNotification page = (TabsAdapterNotification)mTabsAdapter.getFragmentForPosition(TAB_INDEX_NOTES);
            if(page != null) {
                page.NotifyAdapterDataSetChanged();
            }
        }
    }

    /**
     * Notifies the terms adapter that the dataset has changed
     */
    public void reloadTermsTab() {
        if(mTabsAdapter != null) {
            TabsAdapterNotification page = (TabsAdapterNotification)mTabsAdapter.getFragmentForPosition(TAB_INDEX_TERMS);
            if(page != null) {
                page.NotifyAdapterDataSetChanged();
            }
        }
    }

    /**
     * @param term
     */
    public void showTerm(Term term) {
        selectTab(1);
        // TODO: display terms
//        if(term == null) {
//            mTermsTab.showTerms();
//        } else {
//            mTermsTab.showTerm(term);
//        }
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

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO: save scroll for notes tab
//        Pair scrollPosition = mNotesTab.getScroll();
//        outState.putInt(STATE_NOTES_SCROLL_X, (int)scrollPosition.first);
//        outState.putInt(STATE_NOTES_SCROLL_Y, (int)scrollPosition.second);
        outState.putInt(STATE_SELECTED_TAB, mSelectedTab);
    }
}
