package com.door43.translationstudio.newui;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Profile;

import java.util.List;

/**
 * Created by jshuma on 1/27/2015.
 */
public class ProfileDialog extends DialogFragment {

    private OnDismissListener listener = null;

    public static final String TAG = "profile-dialog";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_profile, container, false);

        final EditText name = (EditText)v.findViewById(R.id.name_edittext);
        final EditText email = (EditText)v.findViewById(R.id.email_edittext);
        final EditText phone = (EditText)v.findViewById(R.id.phone_edittext);

        // TODO: Support multiple profiles.
        List<Profile> profiles = AppContext.getProfiles();
        if (profiles.size() > 0) {
            Profile profile = profiles.get(0);
            name.setText(profile.name);
            email.setText(profile.email);
            phone.setText(profile.phone);
        } else {
            name.setText("");
            email.setText("");
            phone.setText("");
        }

        Button cancelButton = (Button)v.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        Button confirmButton = (Button)v.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Profile profile = new Profile(
                        name.getText().toString(),
                        email.getText().toString(),
                        phone.getText().toString());

                // If there is no profile set, save it.
                // If there are more than one, only set 0 for now.
                // TODO: Support multiple profiles.
                List<Profile> profiles = AppContext.getProfiles();
                if (profiles.isEmpty()) {
                    profiles.add(profile);
                } else {
                    profiles.set(0, profile);
                }

                AppContext.setProfiles(profiles);
                dismiss();
            }
        });

        return v;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        if(listener != null) {
            listener.onDismiss();
        }
        super.onDismiss(dialogInterface);
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.listener = listener;
    }

    public interface OnDismissListener {
        void onDismiss();
    }

    public void onDestroy() {
        listener = null;
        super.onDestroy();
    }
}
