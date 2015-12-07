package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

/**
 * Created by joel on 9/8/2015.
 */
public class ReadModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId, Bundle extras) {
        return new ReadModeAdapter(activity, targetTranslationId, sourceTranslationId, chapterId, frameId);
    }

    /***
     * doTranslationCardToggle
     * @param e1
     * @param e2
     * @param swipeLeft
     */
    protected void doTranslationCardToggle(final MotionEvent e1, final MotionEvent e2, final boolean swipeLeft) {
        if(getAdapter() != null) {
            int position = findViewHolderAdapterPosition(e1.getX(), e1.getY());
            if(position == -1) {
                position = findViewHolderAdapterPosition(e2.getX(), e2.getY());
            }
            if(position != -1) {
                RecyclerView.ViewHolder holder = getViewHolderForAdapterPosition(position);
                ((ReadModeAdapter) getAdapter()).toggleTargetTranslationCard((ReadModeAdapter.ViewHolder) holder, position, swipeLeft);
            }
        }
    }

    @Override
    protected void onRightSwipe(MotionEvent e1, MotionEvent e2) {
        doTranslationCardToggle(e1, e2, false);
    }

    @Override
    protected void onLeftSwipe(MotionEvent e1, MotionEvent e2) {
        doTranslationCardToggle(e1, e2, true);
    }
}
