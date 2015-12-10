package com.door43.translationstudio.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;

import org.sufficientlysecure.htmltextview.HtmlTextView;

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
        View rootView = inflater.inflate(R.layout.dialog_custom_alert, container, false);

        final TextView message = (TextView) rootView.findViewById(R.id.dialog_content);
        if (0 != mMessageID) {
            message.setText(mMessageID);
        }

        if (null != mMessage) {
            message.setText(mMessage);
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

        mPositiveButton = setupButton( rootView, R.id.positiveButton, mPositiveTextID);
        mPositiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mPositiveListener) {
                    mPositiveListener.onClick(v);
                }

                dismiss();
            }
        });

        mNegativeButton = setupButton( rootView, R.id.negativeButton, mNegativeTextID);
        mNegativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mNegativeListener) {
                    mNegativeListener.onClick(v);
                }

                dismiss();
            }
        });

        mNeutralButton = setupButton( rootView, R.id.neutralButton, mNeutralTextID);
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

    public CustomAlertDialog setNeutralButton(int textResId, View.OnClickListener l) {
        mNeutralListener = l;
        mNeutralTextID = textResId;
        return this;
    }


    public CustomAlertDialog setView(View v) {
        mContentView = v;
        return this;
    }

    public CustomAlertDialog setIcon(int textResId) {
        mIconID = textResId;
        return this;
    }

    public CustomAlertDialog setCancelableChainable(boolean cancelable) {
        super.setCancelable(cancelable);
        return this;
    }


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


    static public CustomAlertDialog Create(final Activity context) {
        CustomAlertDialog dlg = new CustomAlertDialog();
        dlg.setContext(context);
        return dlg;
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