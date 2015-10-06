package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

/**
 * Displays translations in chunks
 */
public class ChunkModeFragment extends ViewModeFragment {

    public static final String EXTRA_TARGET_OPEN = "extra_target_start_open";

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId, Bundle extras) {
        boolean openTarget = false;
        if(extras != null && extras.containsKey(EXTRA_TARGET_OPEN)) {
            openTarget = extras.getBoolean(EXTRA_TARGET_OPEN, false);
        }
        return new ChunkModeAdapter(activity, targetTranslationId, sourceTranslationId, chapterId, frameId, openTarget);
    }

    @Override
    protected void onRightSwipe(MotionEvent e1, MotionEvent e2) {
        if(getAdapter() != null) {
            int position = findViewHolderAdapterPosition(e1.getX(), e1.getY());
            if(position == -1) {
                position = findViewHolderAdapterPosition(e2.getX(), e2.getY());
            }
            if(position != -1) {
                RecyclerView.ViewHolder holder = getViewHolderForAdapterPosition(position);
                ((ChunkModeAdapter) getAdapter()).closeTargetTranslationCard((ChunkModeAdapter.ViewHolder)holder, position);
            }
        }
    }

    @Override
    protected void onLeftSwipe(MotionEvent e1, MotionEvent e2) {
        if(getAdapter() != null) {
            int position = findViewHolderAdapterPosition(e1.getX(), e1.getY());
            if(position == -1) {
                position = findViewHolderAdapterPosition(e2.getX(), e2.getY());
            }
            if(position != -1) {
                RecyclerView.ViewHolder holder = getViewHolderForAdapterPosition(position);
                ((ChunkModeAdapter) getAdapter()).openTargetTranslationCard((ChunkModeAdapter.ViewHolder)holder, position);
            }
        }
    }
}
