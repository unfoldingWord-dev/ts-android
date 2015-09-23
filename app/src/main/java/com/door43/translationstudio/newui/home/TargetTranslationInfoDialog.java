package com.door43.translationstudio.newui.home;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;

/**
 * Displays detailed information about a target translation
 */
public class TargetTranslationInfoDialog extends DialogFragment {

    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";
    private TargetTranslation mTargetTranslation;
    private Translator mTranslator;
    private OnDeleteListener mListener;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_target_translation_info, container, false);

        mTranslator = AppContext.getTranslator();
        Bundle args = getArguments();
        if(args == null) {
            dismiss();
        } else {
            String targetTranslationId = args.getString(ARG_TARGET_TRANSLATION_ID, null);
            mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        }

        Button deleteButton = (Button)v.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTargetTranslation != null) {
                    mTranslator.deleteTargetTranslation(mTargetTranslation.getId());
                    AppContext.clearTargetTranslationSettings(mTargetTranslation.getId());
                }
                if(mListener != null) {
                    mListener.onDeleteTargetTranslation(mTargetTranslation.getId());
                }
                dismiss();
            }
        });
        return v;
    }

    /**
     * Assigns a listener for this dialog
     * @param listener
     */
    public void setOnDeleteListener(OnDeleteListener listener) {
        mListener = listener;
    }

    public interface OnDeleteListener {
        void onDeleteTargetTranslation(String targetTranslationId);
    }
}
