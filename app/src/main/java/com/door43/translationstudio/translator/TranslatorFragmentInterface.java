package com.door43.translationstudio.translator;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;

/**
 * Created by joel on 4/17/2015.
 */
public interface TranslatorFragmentInterface {
    void reload();
    boolean onContextualMenuItemClick(MenuItem item);
    void onPrepareContextualMenu(Menu menu);
}
