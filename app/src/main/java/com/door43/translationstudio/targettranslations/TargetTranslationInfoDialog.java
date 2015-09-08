package com.door43.translationstudio.targettranslations;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.door43.translationstudio.R;

/**
 * Created by joel on 9/7/2015.
 */
public class TargetTranslationInfoDialog extends DialogFragment {

    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_target_translation_info, container, false);

        // TODO: render project info

        return v;
    }
}
