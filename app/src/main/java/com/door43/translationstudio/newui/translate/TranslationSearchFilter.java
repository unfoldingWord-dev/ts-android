package com.door43.translationstudio.newui.translate;

import android.widget.Filter;

/**
 * Created by blm on 7/16/16.
 */
abstract class TranslationSearchFilter extends Filter {


    public TranslationSearchFilter setTargetSearch(boolean searchTarget) { // chainable

        return this;
    }

}

