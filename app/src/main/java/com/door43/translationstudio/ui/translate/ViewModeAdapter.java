package com.door43.translationstudio.ui.translate;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.SectionIndexer;

import com.door43.translationstudio.core.TranslationViewMode;

import org.unfoldingword.resourcecontainer.ResourceContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 9/18/2015.
 */
public abstract class ViewModeAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH>  implements SectionIndexer {
    private List<VH> mViewHolders = new ArrayList<>();
    private OnEventListener mListener;
    private int mStartPosition = 0;

    /**
     * Returns the viewholder generated by the child class so we can keep track of it
     * @param parent
     * @param viewType
     * @return
     */
    abstract VH onCreateManagedViewHolder(ViewGroup parent, int viewType);

    @Override
    public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
        VH holder = onCreateManagedViewHolder(parent, viewType);
        mViewHolders.add(holder);
        return holder;
    }

    /**
     * Returns the start position where the list should start when first built
     * @return
     */
    protected int getListStartPosition() {
        return mStartPosition;
    }

    /**
     * Sets the position where the list should start when first built
     * @param startPosition
     */
    protected void setListStartPosition(int startPosition) {
        mStartPosition = startPosition;
    }

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
     * Updates the source translation to be displayed.
     * This should call notifyDataSetChanged()
     * @param sourceContainer
     */
    abstract void setSourceContainer(ResourceContainer sourceContainer);

    /**
     * Called when coordinating operations need to be applied to all the view holders
     * @param holder
     */
    abstract void onCoordinate(VH holder);

    /**
     * get the chapter slug for the position
     * @param position
     */
    public String getChapterSlug(int position) {
        int section = getSectionForPosition( position);
        Object[] sections = getSections();
        if(section >= 0 && section < sections.length) {
            return (String) sections[section];
        }
        return "";
    }

    /**
     * Requests the layout manager to coordinate all visible children in the list
     */
    protected void coordinateViewHolders() {
        for(VH holder:mViewHolders) {
            onCoordinate(holder);
        }
    }

    /**
     * calls notify dataset changed and triggers some other actions
     */
    protected void triggerNotifyDataSetChanged() {
        notifyDataSetChanged();
        if(mListener != null) {
            mListener.onDataSetChanged(getItemCount());
        }
    }

    /**
     * Filters the adapter by the constraint
     * @param constraint the search query
     * @param subject the text that will be searched
     */
    public void filter(CharSequence constraint, TranslationFilter.FilterSubject subject) {
        // Override this in your adapter to enable searching
    }

    /**
     * Filters the adapter by merge conflicts
     */
    public void toggleMergeConflictFilter() {
        // Override this in your adapter to enable merge conflict filtering
    }

    /**
     * Checks if filtering is enabled for this adapter.
     * Override this to customize filtering.
     * @return
     */
    public boolean hasFilter() {
        return false;
    }

    /**
     * returns the frame at the given position
     * @param position
     * @return
     */
    public abstract String getFocusedChunkSlug(int position);

    /**
     * returns the frame at the given position
     * @param position
     * @return
     */
    public abstract String getFocusedChapterSlug(int position);

    /**
     * Returns the position of an item in the adapter.
     * @param chapterId
     * @param frameId
     * @return -1 if no item is found
     */
    public abstract int getItemPosition(String chapterId, String frameId);

    /**
     * Restarts the auto commit timer
     */
    public void restartAutoCommitTimer() {
        mListener.restartAutoCommitTimer();
    }

    public interface OnEventListener {
        void onSourceTranslationTabClick(String sourceTranslationId);
        void onNewSourceTranslationTabClick();
        void closeKeyboard();
        void openTranslationMode(TranslationViewMode mode, Bundle extras);
        void onTranslationWordClick(String resourceContainerSlug, String chapterSlug, int width);
        void onTranslationArticleClick(String volume, String manual, String slug, int width);
        void onTranslationNoteClick(String chapterId, String frameId, String translationNoteId, int width);
        void onCheckingQuestionClick(String chapterId, String frameId, String checkingQuestionId, int width);
        void scrollToFrame(String chapterSlug, String frameSlug);
        void restartAutoCommitTimer();
        void onSearching(boolean enable);
        void onDataSetChanged(int count);
        void onEnableMergeConflict(boolean showConflicted, boolean active);
    }
}
