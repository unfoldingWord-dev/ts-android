package com.door43.translationstudio.targettranslations;

import android.app.Activity;
import android.content.Context;

/**
 * Created by joel on 9/8/2015.
 */
public class ReadModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId) {
        return new ReadModeAdapter(activity, targetTranslationId, sourceTranslationId);
    }
}
