package com.door43.translationstudio.ui.translate;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.SectionIndexer;

import com.door43.translationstudio.core.SlugSorter;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.tasks.CheckForMergeConflictsTask;

import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by joel on 9/18/2015.
 */
public abstract class ViewModeAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH>  implements SectionIndexer, ManagedTask.OnFinishedListener {
    private List<VH> mViewHolders = new ArrayList<>();
    private OnEventListener mListener;
    private int mStartPosition = 0;
    protected String startingChapterSlug;
    protected String startingChunkSlug;
    private HashSet<Integer> visiblePositions = new HashSet<>();

    /**
     * Returns the ViewHolder generated by the child class so we can keep track of it
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
     * Binds the ViewHolder to the current position
     * @param holder
     * @param position
     */
    abstract void onBindManagedViewHolder(VH holder, int position);

    @Override
    public final void onBindViewHolder(VH holder, int position) {
        onBindManagedViewHolder(holder, position);
        if(mListener != null) {
            int[] range = mListener.getVisiblePositions();
            HashSet<Integer> visible = new HashSet<>();
            // record visible positions;
            for(int i=range[0]; i<range[1]; i++) {
                visible.add(i);
            }
            // notify not-visible
            this.visiblePositions.removeAll(visible);
            for (Integer i:this.visiblePositions) {
                onPositionNotVisible(i);
            }

            this.visiblePositions = visible;
        }
    }

    /**
     * Called when the position is no longer visible
     * @param position the position that left the screen
     */
    protected void onPositionNotVisible(int position) {
        // stub
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
     * Registers the click listener
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
    public void filter(CharSequence constraint, TranslationFilter.FilterSubject subject, int initialPosition) {
        // Override this in your adapter to enable searching
    }

    /**
     * move to next/previous search item
     * @param next if true then find next, otherwise will find previous
     */
    public void onMoveSearch(boolean next) {
        // Override this in your adapter to enable next/previous
    }

    protected void initializeListItems(List<ListItem> items, List<String> chapters, ResourceContainer sourceContainer) {
        // TODO: there is also a map form of the toc.
        setListStartPosition(0);
        items.clear();
        chapters.clear();
        boolean foundStartPosition = false;
        if(sourceContainer != null) {
            SlugSorter sorter = new SlugSorter();
            List<String> chapterSlugs = sorter.sort(sourceContainer.chapters());

            for (String chapterSlug : chapterSlugs) {
                chapters.add(chapterSlug);
                List<String> chunkSlugs = sorter.sort(sourceContainer.chunks(chapterSlug));
                for (String chunkSlug : chunkSlugs) {
                    if (!foundStartPosition && chapterSlug.equals(startingChapterSlug) && (chunkSlug.equals(startingChunkSlug) || startingChunkSlug == null)) {
                        setListStartPosition(items.size());
                        foundStartPosition = true;
                    }
                    items.add(createListItem(chapterSlug, chunkSlug));
                }
            }
        }
    }

    /**
     * need to override
     * @param chapterSlug
     * @param chunkSlug
     * @return
     */
    public abstract ListItem createListItem(String chapterSlug, String chunkSlug);

    /**
     * check all cards for merge conflicts to see if we should show warning.  Runs as background task.
     */
    protected void doCheckForMergeConflictTask(List<ListItem> items, ResourceContainer sourceContainer, TargetTranslation targetTranslation) {
        if((items != null) && (items.size() > 0) ) {  // make sure initialized
            CheckForMergeConflictsTask task = new CheckForMergeConflictsTask(items, sourceContainer, targetTranslation);
            task.addOnFinishedListener(this);
            TaskManager.addTask(task, CheckForMergeConflictsTask.TASK_ID);
        }
    }

    @Override
    public abstract void onTaskFinished(ManagedTask task);

    /**
     * enable/disable merge conflict filter in adapter
     * @param enableFilter
     */
    public void setMergeConflictFilter(boolean enableFilter) {
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
     * @param chapterSlug
     * @param chunkSlug
     * @return -1 if no item is found
     */
    public abstract int getItemPosition(String chapterSlug, String chunkSlug);

    /**
     * Restarts the auto commit timer
     */
    public void restartAutoCommitTimer() {
        mListener.restartAutoCommitTimer();
    }

    /**
     * called to set new selected position
     * @param position
     * @param offset - if greater than or equal to 0, then set specific offset
     */
    protected void onSetSelectedPosition(int position, int offset) {
        if(mListener != null) {
            mListener.onSetSelectedPosition(position, offset);
        }
    }

    public interface OnEventListener {
        void onSourceTranslationTabClick(String sourceTranslationId);
        void onNewSourceTranslationTabClick();
        void closeKeyboard();
        void openTranslationMode(TranslationViewMode mode, Bundle extras);
        void onTranslationWordClick(String resourceContainerSlug, String chapterSlug, int width);
        void onTranslationArticleClick(String volume, String manual, String slug, int width);
        void onTranslationNoteClick(TranslationHelp note, int width);
        void onCheckingQuestionClick(TranslationHelp question, int width);
        void scrollToChunk(String chapterSlug, String frameSlug);
        void restartAutoCommitTimer();
        void onSearching(boolean enable, int foundCount, boolean atEnd, boolean atStart);
        void onDataSetChanged(int count);
        void onEnableMergeConflict(boolean showConflicted, boolean active);
        void onSetSelectedPosition(int position, int offset);
        RecyclerView.ViewHolder getVisibleViewHolder(int position);

        /**
         * Returns the range of visible positions.
         * @return a 2-value array with '0' min and '1' max positions.
         */
        int[] getVisiblePositions();
    }
}
