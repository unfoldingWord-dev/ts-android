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
import com.door43.translationstudio.core.Profile;
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
    private View.OnClickListener mOnNativeSpeakerDialogClick;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_publish_translators, container, false);

        Bundle args = getArguments();
        String targetTranslationId = args.getString(PublishActivity.EXTRA_TARGET_TRANSLATION_ID);
        Translator translator = AppContext.getTranslator();
        mTargetTranslation = translator.getTargetTranslation(targetTranslationId);

        // auto add profile
        Profile profile = AppContext.getProfile();
        if(profile != null) {
            mTargetTranslation.addContributor(profile.getNativeSpeaker());
        }

        mRecylerView = (RecyclerView)view.findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecylerView.setLayoutManager(linearLayoutManager);
        mRecylerView.setItemAnimator(new DefaultItemAnimator());
        mNativeSpeakerAdapter = new NativeSpeakerAdapter();
        mNativeSpeakerAdapter.setContributors(mTargetTranslation.getContributors());
        mNativeSpeakerAdapter.setOnClickListener(this);
        mRecylerView.setAdapter(mNativeSpeakerAdapter);

        mOnNativeSpeakerDialogClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNativeSpeakerAdapter.setContributors(mTargetTranslation.getContributors());
            }
        };

        // re-attach to dialogs
        Fragment prevEditDialog = getFragmentManager().findFragmentByTag("edit-native-speaker");
        if(prevEditDialog != null) {
            ((NativeSpeakerDialog)prevEditDialog).setOnClickListener(mOnNativeSpeakerDialogClick);
        }
        Fragment prevAddDialog = getFragmentManager().findFragmentByTag("add-native-speaker");
        if(prevAddDialog != null) {
            ((NativeSpeakerDialog)prevAddDialog).setOnClickListener(mOnNativeSpeakerDialogClick);
        }
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
        dialog.setOnClickListener(mOnNativeSpeakerDialogClick);
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
        dialog.setOnClickListener(mOnNativeSpeakerDialogClick);
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
