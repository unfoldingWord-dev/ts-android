package com.door43.translationstudio.util;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;

import java.util.ArrayList;

/**
 * This is an adapter class that allows you to create a generic tabbed view pager with a custom set of tabs.
 * Created by joel on 8/7/2014.
 */
public class TabbedViewPagerAdapter extends FragmentPagerAdapter {
    private ArrayList<StringFragmentKeySet> tabs = new ArrayList<StringFragmentKeySet>();

    /**
     * Creates a new tabbed page adapter
     * @param fm
     * @param tabs The tabs to display
     */
    public TabbedViewPagerAdapter(FragmentManager fm, ArrayList<StringFragmentKeySet> tabs) {
        super(fm);
        this.tabs = tabs;
    }

    @Override
    public CharSequence getPageTitle(int index) {
        SpannableString s = new SpannableString(tabs.get(index).getKey());
        s.setSpan(new AbsoluteSizeSpan(20), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return tabs.get(index).getKey();
    }

    @Override
    public Fragment getItem(int index) {
        if (tabs.size() > index) {
            return tabs.get(index).getFragment();
        } else {
            return null;
        }
    }

    @Override
    public int getCount() {
        return tabs.size();
    }
}
