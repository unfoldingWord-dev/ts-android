package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.door43.translationstudio.R;


/**
 * Created by joel on 1/30/2015.
 */
public class NoteMarkerDialog extends DialogFragment {
    private OnClickListener mListener;
    private CharSequence mPassage;
    private CharSequence mNotes;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.title_note);
        View v = inflater.inflate(R.layout.dialog_note_marker, container, false);
        EditText passageText = (EditText)v.findViewById(R.id.passageText);
        EditText noteText = (EditText)v.findViewById(R.id.noteText);
        Button okButton = (Button)v.findViewById(R.id.okButton);
        Button deleteButton = (Button)v.findViewById(R.id.deleteButton);
        Button cancelButton = (Button)v.findViewById(R.id.cancelButton);

        // load parameters
        Bundle args = getArguments();
        if(args != null) {
            mPassage = args.getCharSequence("passage");
            mNotes = args.getCharSequence("notes");
        }

        // restore state
        if(savedInstanceState != null) {
            mPassage = savedInstanceState.getString("passage");
            mNotes = savedInstanceState.getString("notes");
        }

        passageText.setText(mPassage);
        noteText.setText(mNotes);

        // watch text change
        passageText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void afterTextChanged(Editable editable) {
                mPassage = editable.toString();
            }
        });
        noteText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void afterTextChanged(Editable editable) {
                mNotes = editable.toString();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mListener != null) {
                    mListener.onClick(mPassage, "");
                }
                dismiss();
            }
        });
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mListener != null) {
                    mListener.onClick(mPassage, mNotes);
                }
                dismiss();
            }
        });

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("passage", mPassage);
        outState.putCharSequence("notes", mNotes);
        super.onSaveInstanceState(outState);
    }


    /**
     * Sets the listener that will be triggered when the dialog is submitted.
     * @param listener
     */
    public void setOkListener(OnClickListener listener) {
        mListener = listener;
    }

    public static interface OnClickListener {
        public void onClick(CharSequence passage, CharSequence notes);
    }
}
