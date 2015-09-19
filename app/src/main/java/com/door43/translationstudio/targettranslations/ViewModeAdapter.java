package com.door43.translationstudio.targettranslations;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by joel on 9/18/2015.
 */
public abstract class ViewModeAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private OnEventListener mListener;

    /**
     * Returns the registered click listener
     * @return
     */
    protected OnEventListener getListener() {
        return mListener;
    }

    /**
     * Registeres the click listener
     * @param listener
     */
    public void setOnClickListener(OnEventListener listener) {
        mListener = listener;
    }

    /**
     * Notifies the adapter that it should rebuild it's view holders
     */
    abstract void rebuild();

    /**
     * Updates the source translation to be displayed
     * @param sourceTranslationId
     */
    abstract void setSourceTranslation(String sourceTranslationId);

    abstract void coordinateChild(Context context, View view);

    public interface OnEventListener {
        void onTabClick(String sourceTranslationId);
        void onNewTabClick();

        /**
         * Performs a custom action on all visible children
         */
        void onCoordinateVisible();
    }
}
