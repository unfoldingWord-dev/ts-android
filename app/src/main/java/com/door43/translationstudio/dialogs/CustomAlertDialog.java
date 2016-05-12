package com.door43.translationstudio.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;

import org.sufficientlysecure.htmltextview.HtmlTextView;

/**
 * Created by blm on 11/28/15.
 * The intent of this is to create an AlertDialog replacement that has a modern UI appearance even
 *     on older devices.  Many of the calls are chainable to simplify code.
 *
 * Limitations:
 *      not all features of AlertDialog supported
 *      Dialog buttons are modal and will dismiss dialog.  No support for modeless buttons.
 */
public class CustomAlertDialog extends DialogFragment {

    static int testCntr = 0;
    static String TAG = CustomAlertDialog.class.getSimpleName();

    private int mMessageID = 0;
    private int mMessageHtmlID = 0;
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
    private String mMessageHtml = "";

    private EditText mEditText = null;

    private View mContentView = null;

    private Activity mContext;

    private boolean mAutoDismiss = true;
    private boolean mCancellable = true;
    private boolean mDialogDismissed = false;
    private OnDismissListener mOnDismissListener = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE, theme = 0;
        setStyle(style, theme);
        if(savedInstanceState != null) {
            dismiss();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_custom_alert, container, false);

        final TextView message = (TextView) rootView.findViewById(R.id.dialog_content);

        if(mMessageHtmlID != 0) {
            mMessageHtml = mContext.getResources().getString(mMessageHtmlID);
        }

        if(!mMessageHtml.isEmpty()) {
            mContentView = inflater.inflate(R.layout.dialog_html_alert, null);
            HtmlTextView text = (HtmlTextView) mContentView.findViewById(R.id.text);
            text.setHtmlFromString(mMessageHtml, true);
        } else {
            if (0 != mMessageID) {
                message.setText(mMessageID);
            }
            else if (null != mMessage) {
                message.setText(mMessage);
            }
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

        if (null != mEditText) {
            final LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.content_layout);
            LinearLayout.LayoutParams linLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            mEditText.setLayoutParams(linLayoutParams);
            layout.addView(mEditText);
        }

        mPositiveButton = setupButton( rootView, R.id.positiveButton, mPositiveTextID);
        mPositiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mPositiveListener) {
                    mPositiveListener.onClick(v);
                }

                if(mAutoDismiss) {
                    dismiss();
                }
            }
        });

        mNegativeButton = setupButton( rootView, R.id.negativeButton, mNegativeTextID);
        mNegativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mNegativeListener) {
                    mNegativeListener.onClick(v);
                }

                if(mAutoDismiss) {
                    dismiss();
                }
            }
        });

        mNeutralButton = setupButton( rootView, R.id.neutralButton, mNeutralTextID);
        mNeutralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mNeutralListener) {
                    mNeutralListener.onClick(v);
                }

                if(mAutoDismiss) {
                    dismiss();
                }
            }
        });

        final TextView title = (TextView) rootView.findViewById(R.id.dialog_title);
        if (0 != mTitleID) {
            title.setText(mTitleID);
        } else
        if(mTitle != null) {
            title.setText(mTitle);
        } else { // if not set then hide
            title.setVisibility(View.GONE);
        }

        return rootView;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if(mOnDismissListener != null) {
            mOnDismissListener.onDismiss();
        }
    }

    public CustomAlertDialog setOnDismissListener(OnDismissListener listener) {
        mOnDismissListener = listener;
        return this;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if(mCancellable && !mDialogDismissed) {
            mNegativeButton.callOnClick();
        }
    }

    @Override
    public void dismiss() {

        // clear windows and listeners
        mDialogDismissed = true;
        mNeutralListener = mPositiveListener = mNegativeListener = null;
        mEditText = null;
        mContentView = null;
        mContext = null;
        super.dismiss();
    }

    /**
     * Chainable - set context to use for resources
     * @param context
     * @return
     */
    public CustomAlertDialog setContext(final Activity context) {
        mContext = context;
        return this;
    }

    /**
     * Chainable - set resource to display in title
     * @param textResId
     * @return
     */
    public CustomAlertDialog setTitle(int textResId) {
        mTitleID = textResId;
        mTitle = null;
        return this;
    }

    /**
     * Chainable - set text to display in title
     * @param text
     * @return
     */
    public CustomAlertDialog setTitle(CharSequence text) {
        mTitle = text;
        mTitleID = 0;
        return this;
    }

    /**
     * Chainable - set resource to display in message
     * @param textResId
     * @return
     */
    public CustomAlertDialog setMessage(int textResId) {
        mMessageID = textResId;
        mMessage = null;
        return this;
    }

    /**
     * Chainable - set text to display in message
     * @param text
     * @return
     */
    public CustomAlertDialog setMessage(CharSequence text) {
        mMessage = text;
        mMessageID = 0;
        return this;
    }

    /**
     * Chainable - set HTML string to display in message
     * @param textHtml
     * @return
     */
    public CustomAlertDialog setMessageHtml(String textHtml) {
        mMessageHtml = textHtml;
        mMessageHtmlID = 0;
        return this;
    }

    /**
     * Chainable - set HTML resource to display in message
     * @param textResId
     * @return
     */
    public CustomAlertDialog setMessageHtml(int textResId) {
        mMessageHtmlID = textResId;
        mMessageHtml = "";
        return this;
    }

    /**
     * display the dialog
     * @param tag - string identifier to associate with dialog
     */
    public void show(final String tag) {
        FragmentManager fm = mContext.getFragmentManager();
        show(fm, tag);
    }

    /**
     * Chainable - enables display of a positive button (right most) on button bar
     * @param textResId
     * @param l - optional click listener.  Set null if not needed.
     * @return
     */
    public CustomAlertDialog setPositiveButton(int textResId, View.OnClickListener l) {
        mPositiveListener = l;
        mPositiveTextID = textResId;
        return this;
    }

    /**
     * Chainable - enables display of a negative button (left most) on button bar
     * @param textResId
     * @param l - optional click listener.  Set null if not needed.
     * @return
     */
    public CustomAlertDialog setNegativeButton(int textResId, View.OnClickListener l) {
        mNegativeListener = l;
        mNegativeTextID = textResId;
        return this;
    }

    /**
     * Chainable - enables display of an extra button on button bar between the negative and positive buttons
     * @param textResId
     * @param l - optional click listener.  Set null if not needed.
     * @return
     */
    public CustomAlertDialog setNeutralButton(int textResId, View.OnClickListener l) {
        mNeutralListener = l;
        mNeutralTextID = textResId;
        return this;
    }

    /**
     * Chainable - replaces message text with custom view
     * @param v
     * @return
     */
    public CustomAlertDialog setView(View v) {
        mContentView = v;
        return this;
    }

    /**
     * Chainable - displays an icon in title
     * @param textResId
     * @return
     */
    public CustomAlertDialog setIcon(int textResId) {
        mIconID = textResId;
        return this;
    }

    /**
     * Chainable - adds a text input field if enableInput is true
     * @param enableInput
     * @return
     */
    public CustomAlertDialog addInputPrompt(boolean enableInput) {
        if(enableInput) {
            if(null == mEditText) {
                mEditText = new EditText(mContext);
            }
        } else {
            mEditText = null;
        }
        return this;
    }

    /**
     * if text input was selected, then this returns the text enterred by the user
     * @return
     */
    @Nullable
    public CharSequence getEnteredText() {
        if(mEditText != null) {
            return mEditText.getText();
        }
        return null;
    }

    /**
     * Chainable - enable/disable auto dismiss of dialog. If autoDismiss is set false, then dismiss()
     *      must be explicitly called on dialog to remove it.
     * @param autoDismiss
     * @return
     */
    public CustomAlertDialog setAutoDismiss(boolean autoDismiss) {
        this.mAutoDismiss = autoDismiss;
        return this;
    }

    /**
     * applies cancelable setting in a Chainable fashion
     * @param cancelable
     * @return
     */
    public CustomAlertDialog setCancelableChainable(boolean cancelable) {
        super.setCancelable(cancelable);
        mCancellable = cancelable;
        return this;
    }

    /**
     * shared button setup code
     * @param rootView
     * @param buttonResID
     * @param buttonTextID
     * @return
     */
    private Button setupButton(View rootView, int buttonResID, int buttonTextID) {
        Button button = (Button) rootView.findViewById(buttonResID);
        if(null != button) {
            String label = "";
            if(0 != buttonTextID) {
                label = getResources().getText(buttonTextID).toString().toUpperCase();
            }
            if (!label.isEmpty()) {
                // convert string to upper case manually, because on older devices uppercase attribute
                // in UI
                button.setText(label);
            } else {
                button.setVisibility(View.GONE);
            }
        }

        return button;
    }

    /**
     * creates an instance of a custom alert dialog (not displayed until show() is called)
     * @param context
     * @return
     */
    static public CustomAlertDialog Create(final Activity context) {
        CustomAlertDialog dlg = new CustomAlertDialog();
        dlg.setContext(context);
        return dlg;
    }

    public interface OnDismissListener {
        void onDismiss();
    }


//    static public void test(final Activity context) {
//
//        Log.d(TAG, "Test pass: " + testCntr);
//
//        switch (testCntr++) {
//
//            default:
//                testCntr = 1;
//
//            case 0:
//                LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                View layout = inflater.inflate(R.layout.dialog_html_alert, null);
//                HtmlTextView text = (HtmlTextView)layout.findViewById(R.id.text);
//                text.setHtmlFromString(context.getResources().getString(R.string.chunk_checklist_body), true);
//
//                CustomAlertDialog.Create(context)
//                        .setTitle(R.string.chunk_checklist_title)
//                        .setView(layout)
//                        .setPositiveButton(R.string.confirm, new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View v) {
//                                        Log.d(TAG, "Positive");
//                                    }
//                                }
//                        )
//                        .setNegativeButton(R.string.title_cancel, null)
//                        .show("Chunk2");
//                break;
//
//            case 5:
//                CustomAlertDialog.Create(context)
//                        .setTitle(R.string.success)
//                        .setIcon(R.drawable.ic_done_black_24dp)
//                        .setMessage(R.string.download_complete)
//                        .setCancelableChainable(true)
//                        .setPositiveButton(R.string.label_ok, null)
//                        .show("Success");
//
//                break;
//
//            case 4:
//                CustomAlertDialog.Create(context)
//                        .setTitle("Chunk Checklist")
//                        .setMessage("Are you sure you are done with this chunk?\n" +
//                                        "  * I have placed the verses correctly\n" +
//                                        "  * I have reviewed the words and meaning\n" +
//                                        "  * I have reviewed the translation questions"
//                        )
//                        .setPositiveButton(R.string.confirm, new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View v) {
//                                        Log.d(TAG, "Positive");
//                                    }
//                                    }
//                        )
//                        .setNegativeButton(R.string.title_cancel, null)
//                        .show("Chunk2");
//            break;
//
//            case 3:
//                CustomAlertDialog.Create(context)
//                        .setTitle(R.string.apk_update_available)
//                        .setMessage(R.string.upload_report_or_download_latest_apk)
//                        .setNegativeButton(R.string.title_cancel, new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                Log.d(TAG, "Negative");
//                                }
//                        })
//                        .setNeutralButton(R.string.download_update, new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                Log.d(TAG, "Neutral");
//                                }
//                        })
//                        .setPositiveButton(R.string.label_continue, new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                Log.d(TAG, "Positive");
//                                }
//                        })
//                        .show("ReleaseNotify");
//                break;
//
//            case 1:
//                CustomAlertDialog.Create(context)
//                        .setTitle(R.string.update_projects)
//                        .setIcon(R.drawable.ic_local_library_black_24dp)
//                        .setMessage(R.string.use_internet_confirmation)
//                        .setPositiveButton(R.string.yes, new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                            Log.d(TAG, "Positive");
//                            }
//                        })
//                        .setNegativeButton(R.string.no, null)
//                        .show("Update");
//                break;
//
//            case 2:
//                    CustomAlertDialog.Create(context)
//                        .setTitle(R.string.publish)
//                            .setMessage(R.string.upload_failed)
//                        .setPositiveButton(R.string.dismiss, null)
//                            .setNeutralButton(R.string.menu_bug, new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    Log.d(TAG, "Neutral");
//                                }
//                        }).show("PublishFail");
//                break;
//
//        }
//    }
}