package com.door43.translationstudio.targettranslations;

import android.support.v7.widget.RecyclerView;

/**
 * Created by joel on 9/18/2015.
 */
public abstract class ViewModeAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private OnClickListener mListener;

    /**
     * Returns the registered click listener
     * @return
     */
    protected OnClickListener getListener() {
        return mListener;
    }

    /**
     * Registeres the click listener
     * @param listener
     */
    public void setOnClickListener(OnClickListener listener) {
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

    public interface OnClickListener {
        void onTabClick(String sourceTranslationId);
        void onNewTabClick();
    }
}
