package com.door43.translationstudio.newui.publish;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NativeSpeaker;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.NativeSpeakerDialog;
import com.door43.widget.ViewUtil;

/**
 * Created by joel on 9/20/2015.
 */
public class TranslatorsFragment extends PublishStepFragment implements NativeSpeakerAdapter.OnClickListener {

    private TargetTranslation mTargetTranslation;
    private RecyclerView mRecylerView;
    private NativeSpeakerAdapter mNativeSpeakerAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_publish_translators, container, false);

        Bundle args = getArguments();
        String targetTranslationId = args.getString(PublishActivity.EXTRA_TARGET_TRANSLATION_ID);
        Translator translator = AppContext.getTranslator();
        mTargetTranslation = translator.getTargetTranslation(targetTranslationId);


        mRecylerView = (RecyclerView)view.findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecylerView.setLayoutManager(linearLayoutManager);
        mRecylerView.setItemAnimator(new DefaultItemAnimator());
        mNativeSpeakerAdapter = new NativeSpeakerAdapter();
        mNativeSpeakerAdapter.setTranslators(mTargetTranslation.getContributors());
        mNativeSpeakerAdapter.setOnClickListener(this);
        mRecylerView.setAdapter(mNativeSpeakerAdapter);
//        mNameText = (EditText)rootView.findViewById(R.id.name_edittext);
//        mEmailText = (EditText)rootView.findViewById(R.id.email_edittext);
//        mPhoneText = (EditText)rootView.findViewById(R.id.phone_edittext);

        // buttons
//        ImageButton nameInfoButton = (ImageButton)rootView.findViewById(R.id.name_info_button);
//        ViewUtil.tintViewDrawable(nameInfoButton, getResources().getColor(R.color.dark_secondary_text));
//        ImageButton emailInfoButton = (ImageButton)rootView.findViewById(R.id.email_info_button);
//        ViewUtil.tintViewDrawable(emailInfoButton, getResources().getColor(R.color.dark_secondary_text));
//        ImageButton phoneInfoButton = (ImageButton)rootView.findViewById(R.id.phone_info_button);
//        ViewUtil.tintViewDrawable(phoneInfoButton, getResources().getColor(R.color.dark_secondary_text));

//        mContributor = (View) rootView.findViewById(R.id.contributor_button);
//        mContributorToggle = (TextView) rootView.findViewById(R.id.toggle_contributor);

//        mContributorToggle.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                getNextTranslator();
//            }
//        });

//        nameInfoButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showPrivacyNotice(rootView, true);
//            }
//        });
//        emailInfoButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showPrivacyNotice(rootView, true);
//            }
//        });
//        phoneInfoButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showPrivacyNotice(rootView, true);
//            }
//        });

//        Button addContributorButton = (Button)rootView.findViewById(R.id.add_contributor_button);
//        addContributorButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                NativeSpeaker translator = saveCurrentTranslator();
//                showTranslator(translator.name);
//            }
//        });

//        addContributorButton.setVisibility(View.GONE); //TODO remove to re-enable support for multiple contributors

//        ImageButton deleteContributorButton = (ImageButton)rootView.findViewById(R.id.delete_contributor_button);
//        deleteContributorButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                final CustomAlertDialog dlg = CustomAlertDialog.Create(getActivity());
//                dlg.setTitle(R.string.delete_translator_title)
//                        .setMessageHtml(R.string.confirm_delete_translator)
//                        .setPositiveButton(R.string.confirm, new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View v) {
//
//                                        String name = mContributorToggle.getText().toString();
//                                        List<NativeSpeaker> speakers = mTargetTranslation.getContributor(name);
//                                        if(speakers.size() > 0) {
//                                            mTargetTranslation.removeTranslator(speakers.get(0));
//                                        }
//                                        updateTranslator();
//
//                                        dlg.dismiss();
//                                    }
//                                }
//                        )
//                        .setNegativeButton(R.string.title_cancel, null)
//                        .show("DeleteTrans");
//            }
//        });

//        Button nextButton = (Button)rootView.findViewById(R.id.next_button);
//        nextButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(mNameText.getText().toString().isEmpty() || mEmailText.getText().toString().isEmpty()) {
//                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.complete_required_fields, Snackbar.LENGTH_SHORT);
//                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
//                    snack.show();
//                } else {
//                    showPrivacyNotice(rootView, false);
//                }
//            }
//        });

//        mTargetTranslation.setDefaultContributor(AppContext.getProfile().getNativeSpeaker());
//        updateTranslator();

        return view;
    }

    @Override
    public void onEditNativeSpeaker(NativeSpeaker speaker) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("edit-native-speaker");
        if(prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        NativeSpeakerDialog dialog = new NativeSpeakerDialog();
        Bundle args = new Bundle();
        args.putString(NativeSpeakerDialog.ARG_TARGET_TRANSLATION, mTargetTranslation.getId());
        args.putString(NativeSpeakerDialog.ARG_NATIVE_SPEAKER, speaker.name);
        dialog.setArguments(args);
        // TODO: 2/19/2016 add listener to reload
        dialog.show(ft, "edit-native-speaker");
    }

    @Override
    public void onClickAddNativeSpeaker() {
        showAddNativeSpeakerDialog();
    }

    @Override
    public void onClickNext() {
        if(mTargetTranslation.getContributors().size() > 0) {
            getListener().nextStep();
        } else {
            Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.need_translator_notice, Snackbar.LENGTH_LONG);
            snack.setAction(R.string.add_contributor, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddNativeSpeakerDialog();
                }
            });
            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.white));
            snack.show();
        }
    }

    @Override
    public void onClickPrivacyNotice() {
        showPrivacyNotice(null);
    }

    public void showAddNativeSpeakerDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("add-native-speaker");
        if(prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        NativeSpeakerDialog dialog = new NativeSpeakerDialog();
        Bundle args = new Bundle();
        args.putString(NativeSpeakerDialog.ARG_TARGET_TRANSLATION, mTargetTranslation.getId());
        dialog.setArguments(args);
        // TODO: 2/19/2016 add listener to reload
        dialog.show(ft, "add-native-speaker");
    }

    /**
     * Displays the privacy notice
     * @param listener if set the dialog will become a confirmation dialog
     */
    public void showPrivacyNotice(View.OnClickListener listener) {
        CustomAlertDialog privacy = CustomAlertDialog.Create(getActivity())
            .setTitle(R.string.privacy_notice)
            .setIcon(R.drawable.ic_info_black_24dp)
            .setMessage(R.string.publishing_privacy_notice);

        if(listener != null) {
            privacy.setPositiveButton(R.string.label_continue, listener);
            privacy.setNegativeButton(R.string.title_cancel, null);
        } else {
            privacy.setPositiveButton(R.string.dismiss, null);
        }
        privacy.show("privacy-notice");
    }
}
