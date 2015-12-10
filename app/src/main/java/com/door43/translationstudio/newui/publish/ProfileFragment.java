package com.door43.translationstudio.newui.publish;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.library.ServerLibraryActivity;
import com.door43.translationstudio.user.Profile;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.widget.ViewUtil;

//import java.security.InvalidParameterException;

/**
 * Created by joel on 9/20/2015.
 */
public class ProfileFragment extends PublishStepFragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_publish_profile, container, false);

        final EditText nameText = (EditText)rootView.findViewById(R.id.name_edittext);
        final EditText emailText = (EditText)rootView.findViewById(R.id.email_edittext);
        final EditText phoneText = (EditText)rootView.findViewById(R.id.phone_edittext);

        // buttons
        ImageButton nameInfoButton = (ImageButton)rootView.findViewById(R.id.name_info_button);
        ViewUtil.tintViewDrawable(nameInfoButton, getResources().getColor(R.color.dark_secondary_text));
        ImageButton emailInfoButton = (ImageButton)rootView.findViewById(R.id.email_info_button);
        ViewUtil.tintViewDrawable(emailInfoButton, getResources().getColor(R.color.dark_secondary_text));
        ImageButton phoneInfoButton = (ImageButton)rootView.findViewById(R.id.phone_info_button);
        ViewUtil.tintViewDrawable(phoneInfoButton, getResources().getColor(R.color.dark_secondary_text));

        nameInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrivacyNotice(rootView, true);
            }
        });
        emailInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrivacyNotice(rootView, true);
            }
        });
        phoneInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrivacyNotice(rootView, true);
            }
        });

        Button nextButton = (Button)rootView.findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nameText.getText().toString().isEmpty() || emailText.getText().toString().isEmpty()) {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.complete_required_fields, Snackbar.LENGTH_SHORT);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                } else {
                    showPrivacyNotice(rootView, false);
                }
            }
        });

        // pre-populate fields
        Profile profile = ProfileManager.getProfile();
        if(profile != null) {
            nameText.setText(profile.getName());
            emailText.setText(profile.getEmail());
            phoneText.setText(profile.getPhone());
        } else {
            nameText.setText("");
            emailText.setText("");
            phoneText.setText("");
        }

        return rootView;
    }

    /***
     * display the privacy notice
     * @param rootView
     * @param infoOnly
     */

    public void showPrivacyNotice(final View rootView, boolean infoOnly) {
        CustomAlertDialog privacy;

        final EditText nameText = (EditText)rootView.findViewById(R.id.name_edittext);
        final EditText emailText = (EditText)rootView.findViewById(R.id.email_edittext);
        final EditText phoneText = (EditText)rootView.findViewById(R.id.phone_edittext);

        privacy = CustomAlertDialog.Create(getActivity())
                            .setTitle("Privacy Notice")
                            .setIcon(R.drawable.ic_security_black_24dp)
                            .setMessage(R.string.publishing_privacy_notice);

        if(infoOnly) {
            privacy.setPositiveButton(R.string.label_ok,null);
        } else {
            privacy.setPositiveButton(R.string.label_ok, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ProfileManager.setProfile(new Profile(nameText.getText().toString(), emailText.getText().toString(), phoneText.getText().toString()));
                    getListener().nextStep();
                }
            })
            .setNegativeButton(R.string.title_cancel, null);
        }

        privacy.show("PrivacyWarn");
    }
}
