package com.door43.translationstudio.newui.home;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.door43.translationstudio.R;

/**
 * Created by joel on 10/6/16.
 */

public class UpdateLibraryDialog extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_update_library, container, false);

        // TODO: 10/6/16 hook up buttons

        return v;
    }
}
