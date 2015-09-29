package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.door43.translationstudio.R;
import com.door43.translationstudio.AppContext;

/**
 * Created by joel on 1/29/2015.
 */
public class VerseMarkerDialog extends DialogFragment {
    private OnClickListener mListener;
    private String mVerse;
    private int mMaxVerse;
    private int mSuggestedVerse;
    private int mMinVerse;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.title_verse);
        View v = inflater.inflate(R.layout.dialog_verse_marker, container, false);
        final EditText verseText = (EditText)v.findViewById(R.id.verseNumberText);
        Button okButton = (Button)v.findViewById(R.id.okButton);
        Button cancelButton = (Button)v.findViewById(R.id.cancelButton);

        // load parameters
        Bundle args = getArguments();
        if(args != null) {
            mSuggestedVerse = args.getInt("startVerse");
            mVerse = mSuggestedVerse + "";
            mMinVerse = args.getInt("minVerse");
            mMaxVerse = args.getInt("maxVerse");
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
                // if the verse is empty it will be deleted
                int verse = -1;
                if(!mVerse.isEmpty()) {
                    verse = Integer.parseInt(mVerse);
                }
                if(verse != -1 && (verse > mMaxVerse || verse < mMinVerse)) {
                    AppContext.context().showToastMessage(String.format(AppContext.context().getResources().getString(R.string.verse_out_of_bounds), mMinVerse, mMaxVerse));
                } else {
                    if (mListener != null) {
                        mListener.onClick(verse);
                    }
                    dismiss();
                }
            }
        });

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("verse", mVerse);
        super.onSaveInstanceState(outState);
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
