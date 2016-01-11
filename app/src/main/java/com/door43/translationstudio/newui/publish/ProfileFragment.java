package com.door43.translationstudio.newui.publish;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NativeSpeaker;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.widget.ViewUtil;

import java.util.ArrayList;

//import java.security.InvalidParameterException;

/**
 * Created by joel on 9/20/2015.
 */
public class ProfileFragment extends PublishStepFragment {

    private TargetTranslation mTargetTranslation;
    private int mCurrentTranslator = 0;
    private EditText mNameText;
    private EditText mEmailText;
    private EditText mPhoneText;
    private View mContributor;
    private TextView mContributorToggle;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_publish_profile, container, false);

        Bundle args = getArguments();
        String targetTranslationId = args.getString(PublishActivity.EXTRA_TARGET_TRANSLATION_ID);
        Translator translator = AppContext.getTranslator();
        mTargetTranslation = translator.getTargetTranslation(targetTranslationId);

        mNameText = (EditText)rootView.findViewById(R.id.name_edittext);
        mEmailText = (EditText)rootView.findViewById(R.id.email_edittext);
        mPhoneText = (EditText)rootView.findViewById(R.id.phone_edittext);

        // buttons
        ImageButton nameInfoButton = (ImageButton)rootView.findViewById(R.id.name_info_button);
        ViewUtil.tintViewDrawable(nameInfoButton, getResources().getColor(R.color.dark_secondary_text));
        ImageButton emailInfoButton = (ImageButton)rootView.findViewById(R.id.email_info_button);
        ViewUtil.tintViewDrawable(emailInfoButton, getResources().getColor(R.color.dark_secondary_text));
        ImageButton phoneInfoButton = (ImageButton)rootView.findViewById(R.id.phone_info_button);
        ViewUtil.tintViewDrawable(phoneInfoButton, getResources().getColor(R.color.dark_secondary_text));

        mContributor = (View) rootView.findViewById(R.id.contributor_button);
        mContributorToggle = (TextView) rootView.findViewById(R.id.toggle_contributor);

        mContributorToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNextTranslator();
            }
        });

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

        Button addContributorButton = (Button)rootView.findViewById(R.id.add_contributor_button);
        addContributorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NativeSpeaker translator = saveCurrentTranslator();
                showTranslator(translator.name);
            }
        });

        addContributorButton.setVisibility(View.GONE); //TODO remove to re-enable support for multiple contributors

        ImageButton deleteContributorButton = (ImageButton)rootView.findViewById(R.id.delete_contributor_button);
        deleteContributorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final CustomAlertDialog dlg = CustomAlertDialog.Create(getActivity());
                dlg.setTitle(R.string.delete_translator_title)
                        .setMessageHtml(R.string.confirm_delete_translator)
                        .setPositiveButton(R.string.confirm, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        String name = mContributorToggle.getText().toString();
                                        mTargetTranslation.removeTranslator(name);
                                        updateTranslator();

                                        dlg.dismiss();
                                    }
                                }
                        )
                        .setNegativeButton(R.string.title_cancel, null)
                        .show("DeleteTrans");
            }
        });

        Button nextButton = (Button)rootView.findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mNameText.getText().toString().isEmpty() || mEmailText.getText().toString().isEmpty()) {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.complete_required_fields, Snackbar.LENGTH_SHORT);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                } else {
                    showPrivacyNotice(rootView, false);
                }
            }
        });

        mTargetTranslation.applyDefaultTranslatorsIfNoneSpecified();
        updateTranslator();

        return rootView;
    }

    /***
     * show specific translator
     * @param name
     */
    private void showTranslator(String name) {

        int pos = mTargetTranslation.getTranslatorByName(name);

        if(pos < 0) {
            pos = 0;
        }

        mCurrentTranslator = pos;
        updateTranslator();
    }

    /***
     * move to next translator in list
     */
    private void getNextTranslator() {

        mCurrentTranslator++;
        updateTranslator();
    }

    /***
     * put info on screen for current translator
     */
    private void updateTranslator() {
        ArrayList<NativeSpeaker> translators = mTargetTranslation.getTranslators();

        if(mCurrentTranslator >= translators.size()) {
            mCurrentTranslator = 0;
        }

        boolean moreThanOne = (translators.size() > 1);
        mContributor.setVisibility(moreThanOne ? View.VISIBLE : View.GONE);

        if(translators.size() > 0) {
            NativeSpeaker trans = translators.get(mCurrentTranslator);
            mNameText.setText(trans.name);
            mEmailText.setText(trans.email);
            mPhoneText.setText(trans.phone);

            if(moreThanOne) {
                mContributorToggle.setText(trans.name);
            }
        } else { // if empty
            mNameText.setText("");
            mEmailText.setText("");
            mPhoneText.setText("");
        }
    }

    /***
     * get the current translator data in
     * @return
     */
    private NativeSpeaker getNewTranslatorData() {
        return new NativeSpeaker(mNameText.getText().toString(), mEmailText.getText().toString(), mPhoneText.getText().toString());
    }

    /***
     * get info about current translator and save it
     * @return
     */
    private NativeSpeaker saveCurrentTranslator() {
        NativeSpeaker translator = getNewTranslatorData();
        mTargetTranslation.addTranslator(translator);
        return translator;
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
                    saveCurrentTranslator();
                    getListener().nextStep();
                }
            })
            .setNegativeButton(R.string.title_cancel, null);
        }

        privacy.show("PrivacyWarn");
    }
}
