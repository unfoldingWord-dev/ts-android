package com.door43.translationstudio.newui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NativeSpeaker;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.widget.ViewUtil;

import java.security.InvalidParameterException;

/**
 * Created by joel on 2/19/2016.
 */
public class NativeSpeakerDialog extends DialogFragment {
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

        if(mNativeSpeaker != null) {
            mNameView.setText(mNativeSpeaker.name);
            mTitleView.setText(R.string.edit_contributor);
            mDeleteButton.setVisibility(View.VISIBLE);
            mAgreementCheck.setEnabled(false);
            mAgreementCheck.setChecked(true);
        } else {
            mTitleView.setText(R.string.add_contributor);
            mDeleteButton.setVisibility(View.GONE);
            mAgreementCheck.setEnabled(true);
            mAgreementCheck.setChecked(false);
        }

        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomAlertDialog.Create(getActivity())
                    .setTitle(R.string.delete_translator_title)
                    .setMessageHtml(R.string.confirm_delete_translator)
                    .setPositiveButton(R.string.confirm, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mTargetTranslation.removeContributor(mNativeSpeaker);
                                    if(mListener != null) {
                                        mListener.onClick(v);
                                    }
                                    dismiss();
                                }
                            }
                    )
                        .setNegativeButton(R.string.title_cancel, null)
                    .show("confirm-delete");
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
