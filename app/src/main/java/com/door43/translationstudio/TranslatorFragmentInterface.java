package com.door43.translationstudio;

import android.view.MenuItem;

/**
 * Created by joel on 4/17/2015.
 */
public interface TranslatorFragmentInterface {
    void reload();
    void save();
    boolean onContextualMenuItemClick(MenuItem item);
}
