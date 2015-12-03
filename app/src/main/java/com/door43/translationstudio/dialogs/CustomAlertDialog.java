package com.door43.translationstudio.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;

/**
 * Created by blm on 11/28/15.
 * The intent of this is to create an AlertDialog replacement that has a modern UI appearance even
 *     on older devices.
 *
 * Limitations:
 *      not all features of AlertDialog supported
 *      Dialog buttons are modal and will dismiss dialog.  No support for modeless buttons.
 */
public class CustomAlertDialog extends DialogFragment {

    static int testCntr = 0;
    static String TAG = CustomAlertDialog.class.getSimpleName();

    private int mMessageID = 0;
    private int mTitleID = 0;
    private int mIconID = 0;

    private Button mPositiveButton;
    private View.OnClickListener mPositiveListener;
    private int mPositiveTextID = 0;

    private Button mNegativeButton;
    private View.OnClickListener mNegativeListener;
    private int mNegativeTextID = 0;

    private Button mNeutralButton;
    private View.OnClickListener mNeutralListener;
    private int mNeutralTextID = 0;

    private CharSequence mTitle = null;
    private CharSequence mMessage = null;

    private View mContentView = null;

    private Activity mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = null;
        try {
            rootView = inflater.inflate(R.layout.dialog_custom_alert, container, false);
        } catch (Exception e) {
            Log.d(TAG,"Exception: " + e);
        }

        if (0 != mMessageID) {
            final TextView message = (TextView) rootView.findViewById(R.id.dialog_content);
            message.setText(mMessageID);
        }

        final ImageView icon = (ImageView) rootView.findViewById(R.id.dialog_icon);
        if (0 != mIconID) {
            icon.setImageResource(mIconID);
        } else {
            icon.setVisibility(View.GONE);
        }

        if (null != mContentView) {
            final TextView content = (TextView) rootView.findViewById(R.id.dialog_content);

            ViewGroup parent = (ViewGroup) content.getParent();
            int index = parent.indexOfChild(content);
            parent.removeView(content);
            parent.addView(mContentView, index);
        }

        mPositiveButton = (Button) rootView.findViewById(R.id.positiveButton);
        if(0 != mPositiveTextID) {
            // convert string to upper case manually, because on older devices uppercase attribute
            // in UI
            String label = getResources().getText(mPositiveTextID).toString().toUpperCase();
            mPositiveButton.setText(label);
        } else {
            mPositiveButton.setVisibility(View.GONE);
        }

        mPositiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mPositiveListener) {
                    mPositiveListener.onClick(v);
                }

                dismiss();
            }
        });

        mNegativeButton = (Button) rootView.findViewById(R.id.negativeButton);
        if(0 != mNegativeTextID) {
            // convert string to upper case manually, because on older devices uppercase attribute
            // in UI
            String label = getResources().getText(mNegativeTextID).toString().toUpperCase();
            mNegativeButton.setText(label);
        } else {
            mNegativeButton.setVisibility(View.GONE);
        }

        mNegativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mNegativeListener) {
                    mNegativeListener.onClick(v);
                }

                dismiss();
            }
        });

        mNeutralButton = (Button) rootView.findViewById(R.id.neutralButton);
        if(0 != mNeutralTextID) {
            // convert string to upper case manually, because on older devices uppercase attribute
            // in UI
            String label = getResources().getText(mNeutralTextID).toString().toUpperCase();
            mNeutralButton.setText(label);
        } else {
            mNeutralButton.setVisibility(View.GONE);
        }

        mNeutralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mNeutralListener) {
                    mNeutralListener.onClick(v);
                }

                dismiss();
            }
        });

        if (0 != mTitleID) {
            final TextView title = (TextView) rootView.findViewById(R.id.dialog_title);
            title.setText(mTitleID);
        }
        if(mTitle != null) {
            TextView title = (TextView) rootView.findViewById(R.id.dialog_title);
            title.setText(mTitle);
        }

        return rootView;
    }

    public CustomAlertDialog setContext(final Activity context) {
        mContext = context;
        return this;
    }

    public CustomAlertDialog setTitle(int textResId) {
        mTitleID = textResId;
        mTitle = null;
        return this;
    }

    public CustomAlertDialog setTitle(CharSequence text) {
        mTitle = text;
        mTitleID = 0;
        return this;
    }

    public CustomAlertDialog setMessage(int textResId) {
        mMessageID = textResId;
        mMessage = null;
        return this;
    }

    public CustomAlertDialog setMessage(CharSequence text) {
        mMessage = text;
        mMessageID = 0;
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

    public CustomAlertDialog setView(View v) {
        mContentView = v;
        return this;
    }

    static public CustomAlertDialog Create(final Activity context) {
        CustomAlertDialog dlg = new CustomAlertDialog();
        dlg.setContext(context);
        return dlg;
    }
}