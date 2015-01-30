package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.door43.translationstudio.R;

/**
 * Created by joel on 1/29/2015.
 */
public class VerseMarkerDialog extends DialogFragment {
    private OnClickListener mListener;
    private String mVerse;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.verse_marker);
        View v = inflater.inflate(R.layout.dialog_verse_marker, container, false);
        EditText verseText = (EditText)v.findViewById(R.id.verseNumberText);
        Button okButton = (Button)v.findViewById(R.id.okButton);
        Button cancelButton = (Button)v.findViewById(R.id.cancelButton);

        // load parameters
        Bundle args = getArguments();
        if(args != null) {
            mVerse = args.getInt("verse") + "";
            verseText.setText(mVerse);
        }

        // restore state
        if(savedInstanceState != null) {
            mVerse = savedInstanceState.getString("verse");
            verseText.setText(mVerse+"");
        }

        verseText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // watch text change
        verseText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void afterTextChanged(Editable editable) {
                mVerse = editable.toString();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mListener != null) {
                    try {
                        mListener.onClick(Integer.parseInt(mVerse));
                    } catch (Exception e) {
                        // Delete the verse
                        mListener.onClick(-1);
                    }
                }
                dismiss();
            }
        });

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("verse", mVerse);
    }

    /**
     * Sets up the listener to be triggered when the user clicks the ok button
     * @param listener
     */
    public void setOkListener(OnClickListener listener) {
        mListener = listener;
    }

    public static interface OnClickListener {
        public void onClick(int verse);
    }
}
