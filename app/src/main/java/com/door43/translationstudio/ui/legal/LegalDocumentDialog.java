package com.door43.translationstudio.ui.legal;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.door43.translationstudio.R;

/**
 * Created by joel on 10/23/2015.
 */
public class LegalDocumentDialog extends DialogFragment {
    private ScrollView mScrollView;
    private DialogInterface.OnDismissListener mDismissListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        int resourceId = 0;
        if(args != null) {
            resourceId = args.getInt(LegalDocumentActivity.ARG_RESOURCE, 0);
        }
        if(resourceId == 0) {
            dismiss();
        }

        // inflate the views
        View mView = inflater.inflate(R.layout.dialog_license, container, false);
        TextView mLicenseText = (TextView) mView.findViewById(R.id.license_text);
        mScrollView = (ScrollView)mView.findViewById(R.id.scrollView);
        Button mDismissButton = (Button)mView.findViewById(R.id.dismiss_license_btn);

        // validate the arguments
        if(resourceId == 0) {
            dismiss();
        } else {
            // load the string
            String licenseString = getResources().getString(resourceId);
            mLicenseText.setText(Html.fromHtml(licenseString));
            mLicenseText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        // enable button
        mDismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LegalDocumentDialog.this.dismiss();
            }
        });
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // safety check
        if (getDialog() == null) {
            return;
        }
        getDialog().getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        // scroll to the top
        mScrollView.smoothScrollTo(0, 0);
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        if(mDismissListener != null) {
            mDismissListener.onDismiss(dialogInterface);
        }
        super.onDismiss(dialogInterface);
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mDismissListener = listener;
    }
}
