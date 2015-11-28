package com.door43.translationstudio.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.door43.translationstudio.R;

import org.sufficientlysecure.htmltextview.HtmlTextView;

/**
 * Created by blm on 11/28/15.
 */
public class CustomAlertDialog extends DialogFragment {

    private int mMessageID = 0;
    private int mMessageHtmlID = 0;
    private int mTitleID = 0;
    private Button mPositiveButton;
    private Button mNegativeButton;
    private Activity mContext;
    private View.OnClickListener mPositiveListener;
    private int mPositiveTextID = 0;
    private View.OnClickListener mNegativeListener;
    private int mNegativeTextID = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_custom_alert, container, false);
        if (0 != mMessageID) {
            final HtmlTextView message = (HtmlTextView) rootView.findViewById(R.id.dialog_content);
            message.setText(mMessageID);
        }
        if (0 != mMessageHtmlID) {
            final HtmlTextView message = (HtmlTextView) rootView.findViewById(R.id.dialog_content);
            String mMessageHtml = getResources().getText(mMessageHtmlID).toString();
            message.setHtmlFromString(mMessageHtml, true);
        }
        mPositiveButton = (Button) rootView.findViewById(R.id.confirmButton);
        if(0 != mPositiveTextID) {
            mPositiveButton.setText(mPositiveTextID);
        } else {
            mPositiveButton.setVisibility(View.GONE);
        }

        if(null != mPositiveListener) {
            mPositiveButton.setOnClickListener(mPositiveListener);
        }
        else {
            mPositiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

        mNegativeButton = (Button) rootView.findViewById(R.id.cancelButton);
        if(0 != mNegativeTextID) {
            mNegativeButton.setText(mNegativeTextID);
        } else {
            mNegativeButton.setVisibility(View.GONE);
        }

        if(null != mNegativeListener) {
            mNegativeButton.setOnClickListener(mNegativeListener);
        }
        else {
            mNegativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

        if (0 != mTitleID) {
            final TextView title = (TextView) rootView.findViewById(R.id.dialog_title);
            title.setText(mTitleID);
        }
        return rootView;
    }

    public CustomAlertDialog setContext(final Activity context) {
        mContext = context;
        return this;
    }

    public CustomAlertDialog setTitle(int textResId) {
        mTitleID = textResId;
        return this;
    }
//    public CustomAlertDialog setTitle(CharSequence text) {
//        mTitle = text.toString();
//        return this;
//    }

    public CustomAlertDialog setMessage(int textResId) {
        mMessageID = textResId;
        return this;
    }

    public CustomAlertDialog setMessageHtml(int textResId) {
        mMessageHtmlID = textResId;
        return this;
    }

    public void show(final String tag) {
        FragmentManager fm = mContext.getFragmentManager();
        show(fm, tag);
    }

    public CustomAlertDialog setPositiveButton(int textResId, View.OnClickListener l) {
        mPositiveListener = l;
        mPositiveTextID = textResId;
        return this;
    }

    public CustomAlertDialog setNegativeButton(int textResId, View.OnClickListener l) {
        mNegativeListener = l;
        mNegativeTextID = textResId;
        return this;
    }
}