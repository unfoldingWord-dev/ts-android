package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

/**
 * Created by blm on 1/10/2016.
 */
public class PreviewModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId, Bundle extras) {
        return new PreviewModeAdapter(activity, sourceTranslationId, chapterId, frameId);
    }

}

