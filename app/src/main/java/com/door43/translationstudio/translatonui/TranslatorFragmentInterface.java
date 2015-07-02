package com.door43.translationstudio.translatonui;

import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by joel on 4/17/2015.
 */
public interface TranslatorFragmentInterface {
    void reload();
    boolean onContextualMenuItemClick(MenuItem item);
    void onPrepareContextualMenu(Menu menu);
}
