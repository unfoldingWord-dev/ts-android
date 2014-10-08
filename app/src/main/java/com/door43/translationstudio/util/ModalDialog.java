package com.door43.translationstudio.util;

import android.app.DialogFragment;
import android.os.Bundle;

import com.door43.translationstudio.events.LanguageModalDismissedEvent;

/**
 * Created by joel on 10/7/2014.
 */
public class ModalDialog extends DialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // use theme = 1 for full screen and 0 for small window
        int style = DialogFragment.STYLE_NO_TITLE, theme = 1;
        setStyle(style, theme);
    }
}
