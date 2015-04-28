package com.door43.translationstudio.translator;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;

/**
 * Created by joel on 4/17/2015.
 */
public interface TranslatorFragmentInterface {
    void reload();

    /**
     * @deprecated we are using the method from TranslationManager instead
     */
    void save();
    boolean onContextualMenuItemClick(MenuItem item);
    void onPrepareContextualMenu(Menu menu);
}
