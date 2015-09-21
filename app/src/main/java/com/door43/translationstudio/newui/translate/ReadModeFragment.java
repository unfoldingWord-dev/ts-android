package com.door43.translationstudio.newui.translate;

import android.app.Activity;

/**
 * Created by joel on 9/8/2015.
 */
public class ReadModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId) {
        return new ReadModeAdapter(activity, targetTranslationId, sourceTranslationId);
    }
}
