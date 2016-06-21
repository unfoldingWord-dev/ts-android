package com.door43.translationstudio.newui;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NativeSpeaker;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.newui.legal.LegalDocumentActivity;
import com.door43.widget.ViewUtil;

import java.security.InvalidParameterException;

/**
 * Created by joel on 2/19/2016.
 */
public class ContributorDialog extends DialogFragment {
    public static final java.lang.String ARG_NATIVE_SPEAKER = "native_speaker_name";
    public static final java.lang.String ARG_TARGET_TRANSLATION = "target_translation_id";
    private EditText mNameView;
    private Button mCancelButton;
    private Button mSaveButton;
    private TargetTranslation mTargetTranslation = null;
    private NativeSpeaker mNativeSpeaker = null;
    private TextView mTitleView;
    private View.OnClickListener mListener;
    private Button mDeleteButton;
    private CheckBox mAgreementCheck;
    private View mLicenseGroup;
    private Button mLicenseAgreementButton;
    private Button mStatementOfFaithButton;
    private Button mTranslationGuidelinesButton;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_native_speaker, container, false);

        // load target
        Bundle args = getArguments();
        if(args != null) {
            String nativeSpeakerName = args.getString(ARG_NATIVE_SPEAKER, null);
            String targetTranslationId = args.getString(ARG_TARGET_TRANSLATION, null);
            mTargetTranslation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);
            if(nativeSpeakerName != null && mTargetTranslation != null) {
                mNativeSpeaker = mTargetTranslation.getContributor(nativeSpeakerName);
            }
        }
        if(mTargetTranslation == null) {
            throw new InvalidParameterException("Missing the target translation parameter");
        }

        mTitleView = (TextView)v.findViewById(R.id.title);
        mNameView = (EditText)v.findViewById(R.id.name);
        mCancelButton = (Button)v.findViewById(R.id.cancel_button);
        mSaveButton = (Button)v.findViewById(R.id.save_button);
        mDeleteButton = (Button)v.findViewById(R.id.delete_button);
        mAgreementCheck = (CheckBox)v.findViewById(R.id.agreement_check);
        mLicenseGroup = v.findViewById(R.id.license_group);
        mLicenseAgreementButton = (Button)v.findViewById(R.id.license_agreement_btn);
        mStatementOfFaithButton = (Button)v.findViewById(R.id.statement_of_faith_btn);
        mTranslationGuidelinesButton = (Button)v.findViewById(R.id.translation_guidelines_btn);

        if(mNativeSpeaker != null) {
            mNameView.setText(mNativeSpeaker.name);
            mTitleView.setText(R.string.edit_contributor);
            mDeleteButton.setVisibility(View.VISIBLE);
            mAgreementCheck.setEnabled(false);
            mAgreementCheck.setChecked(true);
            mLicenseGroup.setVisibility(View.GONE);
        } else {
            mTitleView.setText(R.string.add_contributor);
            mDeleteButton.setVisibility(View.GONE);
            mAgreementCheck.setEnabled(true);
            mAgreementCheck.setChecked(false);
            mLicenseGroup.setVisibility(View.VISIBLE);
        }

        mLicenseAgreementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), LegalDocumentActivity.class);
                intent.putExtra(LegalDocumentActivity.ARG_RESOURCE, R.string.license);
                startActivity(intent);
            }
        });
        mStatementOfFaithButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), LegalDocumentActivity.class);
                intent.putExtra(LegalDocumentActivity.ARG_RESOURCE, R.string.statement_of_faith);
                startActivity(intent);
            }
        });
        mTranslationGuidelinesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), LegalDocumentActivity.class);
                intent.putExtra(LegalDocumentActivity.ARG_RESOURCE, R.string.translation_guidlines);
                startActivity(intent);
            }
        });
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                    .setTitle(R.string.delete_translator_title)
                    .setMessage(Html.fromHtml(getString(R.string.confirm_delete_translator)))
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mTargetTranslation.removeContributor(mNativeSpeaker);
                            if(mListener != null) {
                                mListener.onClick(v);
                            }
                            dismiss();
                        }
                    }
                    )
                    .setNegativeButton(R.string.title_cancel, null)
                    .show();
            }
        });
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mNameView.getText().toString();
                if(mAgreementCheck.isChecked() && !name.isEmpty()) {
                    NativeSpeaker duplicate = mTargetTranslation.getContributor(name);
                    if (duplicate != null) {
                        if (mNativeSpeaker != null && mNativeSpeaker.equals(duplicate)) {
                            // no change
                            dismiss();
                        } else {
                            Snackbar snack = Snackbar.make(v, R.string.duplicate_native_speaker, Snackbar.LENGTH_SHORT);
                            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.white));
                            snack.show();
                        }
                    } else {
                        mTargetTranslation.removeContributor(mNativeSpeaker); // remove old name
                        mTargetTranslation.addContributor(new NativeSpeaker(name));
                        if (mListener != null) {
                            mListener.onClick(v);
                        }
                        dismiss();
                    }
                } else {
                    Snackbar snack = Snackbar.make(v, R.string.complete_required_fields, Snackbar.LENGTH_SHORT);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.white));
                    snack.show();
                }
            }
        });

        return v;
    }

    /**
     * Sets the listener to be called when the dialog is submitted
     * @param listener
     */
    public void setOnClickListener(View.OnClickListener listener) {
        mListener = listener;
    }
}
