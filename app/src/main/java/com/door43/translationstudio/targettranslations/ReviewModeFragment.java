package com.door43.translationstudio.targettranslations;

import android.app.Fragment;
import android.content.Context;

/**
 * Created by joel on 9/8/2015.
 */
public class ReviewModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter getAdapter(Context context, String targetTranslationId, String sourceTranslationId) {
        return new ReviewModeAdapter(context, targetTranslationId, sourceTranslationId);
    }
}
