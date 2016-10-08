package com.door43.translationstudio.newui.home;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.door43.translationstudio.EventBuffer;
import com.door43.translationstudio.R;

/**
 * Created by joel on 10/6/16.
 */

public class UpdateLibraryDialog extends DialogFragment implements EventBuffer.OnEventTalker {

    public static final String TAG = "update-library-dialog";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_update_library, container, false);

        v.findViewById(R.id.update_languages).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBuffer.write(UpdateLibraryDialog.this, 1, null);
            }
        });
        v.findViewById(R.id.update_source).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBuffer.write(UpdateLibraryDialog.this, 2, null);
            }
        });
        v.findViewById(R.id.update_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBuffer.write(UpdateLibraryDialog.this, 3, null);
            }
        });
        v.findViewById(R.id.dismiss_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return v;
    }

    @Override
    public void onDestroy() {
        eventBuffer.removeAllListeners();
        super.onDestroy();
    }
}
