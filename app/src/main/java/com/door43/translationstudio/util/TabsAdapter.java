package com.door43.translationstudio.util;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;

/**
 * This is an adapter class that allows you to create a generic tabbed view pager with a custom set of tabs.
 */
public class TabsAdapter extends FragmentPagerAdapter {
    private final ViewPager mViewPager;
    private final Context mContext;
//    private ArrayList<StringFragmentKeySet> tabs = new ArrayList<>();
    private ArrayList<TabInfo> mTabs = new ArrayList<>();

    /**
     * Creates a new tabbed page adapter
     * @param activity
     * @param pager
     */
    public TabsAdapter(Activity activity, ViewPager pager) {
        super(activity.getFragmentManager());
        mContext = activity;
        mViewPager = pager;
        mViewPager.setAdapter(this);
    }

    @Override
    public CharSequence getPageTitle(int index) {
        TabInfo info = mTabs.get(index);
//        SpannableString s = new SpannableString(info.title);
//        s.setSpan(new AbsoluteSizeSpan(20), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return info.title;
    }

    @Override
    public Fragment getItem(int index) {
        TabInfo info = mTabs.get(index);
        return Fragment.instantiate(mContext, info.tabClass.getName(), info.args);
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    /**
     * @param containerViewId the ViewPager this adapter is being supplied to
     * @param id pass in getItemId(position) as this is whats used internally in this class
     * @return the tag used for this pages fragment
     */
    public static String makeFragmentName(int containerViewId, long id) {
        return "android:switcher:" + containerViewId + ":" + id;
    }

    /**
     * @return may return null if the fragment has not been instantiated yet for that position - this depends on if the fragment has been viewed
     * yet OR is a sibling covered by {@link android.support.v4.view.ViewPager#setOffscreenPageLimit(int)}. Can use this to call methods on
     * the current positions fragment.
     */
    public @Nullable
    Fragment getFragmentForPosition(int position)
    {
        String tag = makeFragmentName(mViewPager.getId(), getItemId(position));
        Fragment fragment = ((Activity)mContext).getFragmentManager().findFragmentByTag(tag);
        return fragment;
    }

    public void addTab(String title, Class<?> tabClass, Bundle args) {
        TabInfo info = new TabInfo(title, tabClass, args);
        mTabs.add(info);
        notifyDataSetChanged();
    }

    static final class TabInfo {
        private final Class<?> tabClass;
        private final Bundle args;
        private final String title;

        TabInfo(String _title, Class<?> _class, Bundle _args) {
            title = _title;
            tabClass = _class;
            args = _args;
        }
    }
}
