package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.user.Profile;

/**
 * Created by joel on 2/5/2015.
 */
@Deprecated
public class ContactFormDialog extends DialogFragment {
    private OnOkListener mListener;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.dialog_contact_form);
        View v = inflater.inflate(R.layout.dialog_contact_form, container, false);

        final TextView warning = (TextView)v.findViewById(R.id.warningNotice);
        final TextView nameTitle = (TextView)v.findViewById(R.id.nameTitle);
        final EditText name = (EditText)v.findViewById(R.id.nameText);
        final TextView emailTitle = (TextView)v.findViewById(R.id.emailTitle);
        final EditText email = (EditText)v.findViewById(R.id.emailText);
        final EditText phone = (EditText)v.findViewById(R.id.phoneText);

        warning.setVisibility(View.GONE);

        Button okBtn = (Button)v.findViewById(R.id.okButton);
        Button cancelBtn = (Button)v.findViewById(R.id.cancelButton);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mListener != null) {
                    Profile profile = new Profile(name.getText().toString(), email.getText().toString());
                    profile.setPhone(phone.getText().toString());

                    if(!profile.getName().isEmpty() && !profile.getEmail().isEmpty()) {
                        mListener.onOk(profile);
                        dismiss();
                    } else {
                        warning.setVisibility(View.VISIBLE);
                        // invalid input
                        if(profile.getName().isEmpty()) {
                            nameTitle.setTextColor(Color.RED);
                        }
                        if(profile.getEmail().isEmpty()) {
                            emailTitle.setTextColor(Color.RED);
                        }
                    }
                }

            }
        });
        // TODO: hook up forms and buttons

        return v;
    }

    /**
     * Sets the listener that will be triggered when the user clicks ok
     * @param listener
     */
    public void setOkListener(OnOkListener listener) {
        mListener = listener;
    }

    public static interface OnOkListener {
        public void onOk(Profile profile);
    }
}
