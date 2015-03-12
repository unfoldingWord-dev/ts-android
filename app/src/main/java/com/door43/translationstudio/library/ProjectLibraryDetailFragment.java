package com.door43.translationstudio.library;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import com.door43.translationstudio.R;

import com.door43.translationstudio.library.temp.LibraryTempData;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.StringFragmentKeySet;
import com.door43.translationstudio.util.TabbedViewPagerAdapter;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

import java.util.ArrayList;

/**
 * A fragment representing a single Project detail screen.
 * This fragment is either contained in a {@link ProjectLibraryListActivity}
 * in two-pane mode (on tablets) or a {@link ProjectLibraryDetailActivity}
 * on handsets.
 */
public class ProjectLibraryDetailFragment extends TranslatorBaseFragment {
    public static final String ARG_ITEM_INDEX = "item_id";
    private Project mItem;
    private ArrayList<StringFragmentKeySet> mTabs = new ArrayList<>();
    private LanguagesTabFragment mLanguagesTab = new LanguagesTabFragment();
    private ViewPager mViewPager;
    private int mDefaultPage = 0;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProjectLibraryDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_INDEX)) {
            mItem = LibraryTempData.getProject(getArguments().getInt(ARG_ITEM_INDEX));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_library_detail, container, false);

        // Tabs
        mTabs.add(new StringFragmentKeySet(getResources().getString(R.string.languages), mLanguagesTab));

        // view pager
        mViewPager = (ViewPager) rootView.findViewById(R.id.projectBrowserViewPager);
        TabbedViewPagerAdapter tabbedViewPagerAdapter = new TabbedViewPagerAdapter(getFragmentManager(), mTabs);
        mViewPager.setAdapter(tabbedViewPagerAdapter);

        // sliding tabs layout
        PagerSlidingTabStrip slidingTabLayout = (PagerSlidingTabStrip) rootView.findViewById(R.id.projectBrowserTabs);
        slidingTabLayout.setViewPager(mViewPager);
        slidingTabLayout.setIndicatorColor(getResources().getColor(R.color.blue));
        slidingTabLayout.setDividerColor(Color.TRANSPARENT);

        selectTab(mDefaultPage);


        return rootView;
    }

    /**
     * Changes the selected tab
     * @param i
     */
    public void selectTab(int i) {
        if(mViewPager != null) {
            if(mTabs.size() > i && i >= 0) {
                // select the tab
                mViewPager.setCurrentItem(i);
                // notify the tab list adapter that it should reload
                ((TabsFragmentAdapterNotification) mTabs.get(i).getFragment()).NotifyAdapterDataSetChanged();
            }
        } else {
            mDefaultPage = i;
        }
    }
}
