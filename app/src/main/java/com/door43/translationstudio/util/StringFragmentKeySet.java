package com.door43.translationstudio.util;

import android.app.Fragment;

/**
 * Created by joel on 8/7/2014.
 */
public class StringFragmentKeySet {
    private String key;
    private Fragment fragment;

    /**
     * Acts as a keyset so we can easily pass fragments around with a label.
     * @param k the key or label of the fragment
     * @param f the fragment
     */
    public StringFragmentKeySet(String k, Fragment f) {
        key = k;
        fragment = f;
    }

    public String getKey() {
        return key;
    }

    public Fragment getFragment() {
        return fragment;
    }
}
