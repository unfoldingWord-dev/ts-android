package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

/**
 * Created by joel on 9/8/2015.
 */
public class ReadModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId) {
        return new ReadModeAdapter(activity, targetTranslationId, sourceTranslationId, chapterId, frameId);
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
                ((ReadModeAdapter) getAdapter()).closeTargetTranslation((ReadModeAdapter.ViewHolder)holder, position);
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
                ((ReadModeAdapter) getAdapter()).openTargetTranslation((ReadModeAdapter.ViewHolder)holder, position);
            }
        }
    }
}
