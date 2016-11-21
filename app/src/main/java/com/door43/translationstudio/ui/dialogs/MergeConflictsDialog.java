package com.door43.translationstudio.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.door43.translationstudio.R;

/**
 * Displays a truncated list of merge conflicts to the user and
 * provides options for handling them.
 */
public class MergeConflictsDialog extends DialogFragment {
    public static final String TAG = "merge_conflicts_dialog";
    private OnClickListener listener = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_merge_conflict, container, false);

        v.findViewById(R.id.review).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) {
                    listener.onReview();
                }
                dismiss();
            }
        });
        v.findViewById(R.id.keep_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) {
                    listener.onKeepServer();
                }
                dismiss();
            }
        });
        v.findViewById(R.id.keep_local).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) {
                    listener.onKeepLocal();
                }
                dismiss();
            }
        });
        v.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) {
                    listener.onCancel();
                }
                dismiss();
            }
        });

        return v;
    }

    /**
     * Sets the listener that will receive events
     * @param listener
     */
    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {
        void onReview();
        void onKeepServer();
        void onKeepLocal();
        void onCancel();
    }
}
