package com.door43.translationstudio.ui.dialogs;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.door43.translationstudio.ui.LoginDoor43Activity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.RegisterDoor43Activity;

/**
 * This dialog provides options for the user to login into or create a Door43 account
 * and connect it to their profile.
 * This should be used anywhere a Door43 account is required but does not exist.
 */
public class Door43LoginDialog extends DialogFragment {
    public static final String TAG = "door43_login_options_dialog";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_door43_login, container, false);

        v.findViewById(R.id.register_door43).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), RegisterDoor43Activity.class);
                startActivity(intent);
                dismiss();
            }
        });
        v.findViewById(R.id.login_door43).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), LoginDoor43Activity.class);
                startActivity(intent);
                dismiss();
            }
        });
        v.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return v;
    }
}
