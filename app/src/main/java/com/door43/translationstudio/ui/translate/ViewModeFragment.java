package com.door43.translationstudio.ui.translate;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ContainerCache;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.BaseFragment;
import com.door43.translationstudio.ui.translate.review.SearchSubject;

import org.json.JSONException;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.util.List;

/**
 * Created by joel on 9/18/2015.
 */
public abstract class ViewModeFragment extends BaseFragment implements ViewModeAdapter.OnEventListener, ChooseSourceTranslationDialog.OnClickListener, ManagedTask.OnFinishedListener {

    public static final String TAG = ViewModeFragment.class.getSimpleName();
    private static final String TASK_ID_OPEN_SELECTED_SOURCE = "open-selected-source";
    private static final String TASK_ID_OPEN_SOURCE = "open-source";
    private static ResourceContainer mSourceContainer = null;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ViewModeAdapter mAdapter;
    private boolean mFingerScroll = false;
    private OnEventListener mListener;
    private TargetTranslation mTargetTranslation;
    private Translator mTranslator;
    private Door43Client mLibrary;
    private GestureDetector mGesture;
    private Translation mSourceTranslation = null;
    private ProgressDialog mProgressDialog = null;
    private int mSavedPosition = 0;

    /**
     * Returns an instance of the adapter
     * @param activity
     * @param targetTranslationSlug
     * @param startingChapterSlug
     * @param startingChunkSlug
     * @param extras
     * @return
     */
    abstract ViewModeAdapter generateAdapter(Activity activity, String targetTranslationSlug, String startingChapterSlug, String startingChunkSlug, Bundle extras);

    /**
     * Resets the static variables
     */
    public static void reset() {
        ContainerCache.empty();
        mSourceContainer = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stacked_card_list, container, false);

        mLibrary = App.getLibrary();
        mTranslator = App.getTranslator();

        Bundle args = getArguments();
        String targetTranslationSlug = args.getString(App.EXTRA_TARGET_TRANSLATION_ID, null);
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationSlug);
        if(mTargetTranslation == null) {
            Logger.e(getClass().getName() ,"A valid target translation id is required. Received '" + targetTranslationSlug + "' but the translation could not be found");
            getActivity().finish();
        }

        String chapterSlug = args.getString(App.EXTRA_CHAPTER_ID, App.getLastFocusChapterId(targetTranslationSlug));
        String chunkSlug = args.getString(App.EXTRA_FRAME_ID, App.getLastFocusFrameId(targetTranslationSlug));

        try {
            String sourceTranslationSlug = App.getSelectedSourceTranslationId(targetTranslationSlug);
            mSourceTranslation = mLibrary.index().getTranslation(sourceTranslationSlug);
            if(mSourceTranslation == null) App.removeOpenSourceTranslation(targetTranslationSlug, sourceTranslationSlug);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // open selected tab
        if(null == mSourceTranslation) {
            if(mListener != null) mListener.onNoSourceTranslations(targetTranslationSlug);
        } else {
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
            mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mAdapter = generateAdapter(this.getActivity(), targetTranslationSlug, chapterSlug, chunkSlug, args);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    mFingerScroll = true;
                    super.onScrollStateChanged(recyclerView, newState);
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (mFingerScroll) {
                        int position = getCurrentPosition();
                        if(mListener != null) mListener.onScrollProgress(position);
                    }
                }
            });

            // notify activity contents changed
            onDataSetChanged(mAdapter.getItemCount());

            mAdapter.setOnClickListener(this);

            if(savedInstanceState == null) {
                doScrollToPosition(mAdapter.getListStartPosition(), 0);
            }
        }

        mGesture = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            public MotionEvent mLastOnDownEvent;
            private final float SWIPE_THRESHOLD_VELOCITY = 20f;
            private final float SWIPE_MIN_DISTANCE = 50f;
            private final float SWIPE_MAX_ANGLE_DEG = 30;
            @Override
            public boolean onDown(MotionEvent e) {
                mLastOnDownEvent = e;
                return super.onDown(e);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if(e1 == null) {
                    e1 = mLastOnDownEvent;
                }
                try {
                    float distanceX = e2.getX() - e1.getX();
                    float distanceY = e2.getY() - e1.getY();
                    // don't handle vertical swipes (division error)
                    if (distanceX == 0) return false;

                    double flingAngle = Math.toDegrees(Math.asin(Math.abs(distanceY / distanceX)));
                    if (flingAngle <= SWIPE_MAX_ANGLE_DEG && Math.abs(distanceX) >= SWIPE_MIN_DISTANCE && Math.abs(velocityX) >= SWIPE_THRESHOLD_VELOCITY) {
                        if (distanceX > 0) {
                            onRightSwipe(e1, e2);
                        } else {
                            onLeftSwipe(e1, e2);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        // let child classes modify the view
        onPrepareView(rootView);

        return rootView;
    }

    /**
     * scroll panes to go to specific position with vertical offset
     * @param position
     * @param offset
     */
    public void doScrollToPosition(int position, int offset) {
        if(mLayoutManager != null) {
            mLayoutManager.scrollToPositionWithOffset(position, offset);
            Logger.i(TAG, "doScrollToPosition: position=" + position + ", offset=" + offset);
        }
        if(mListener != null) mListener.onScrollProgress(position);
    }

    /**
     * get the chapter slug for the position
     * @param position
     */
    public String getChapterSlug(int position) {
        if(mAdapter != null) return mAdapter.getChapterSlug(position);
        return Integer.toString(position + 1);
    }

    /**
     * Called when the user performs a swipe towards the right
     * @param e1
     * @param e2
     */
    protected void onRightSwipe(MotionEvent e1, MotionEvent e2) {

    }

    /**
     * Called when the user performs a swipe towards the left
     * @param e1
     * @param e2
     */
    protected void onLeftSwipe(MotionEvent e1, MotionEvent e2) {

    }

    /**
     * Returns the currently selected resource container
     * @return
     */
    protected ResourceContainer getSelectedResourceContainer() {
        return ContainerCache.cache(mLibrary, mSourceTranslation.resourceContainerSlug);
    }

    /**
     * Scrolls to the given frame
     * // TODO: 11/2/16 this does not scroll to the correct chunk. see obs 1:15. it seems to always be 2 off.
     * @param chapterSlug
     * @param chunkSlug
     */
    public void scrollToChunk(String chapterSlug, String chunkSlug) {
        closeKeyboard();
        int position = mAdapter.getItemPosition(chapterSlug, chunkSlug);
        if(position != -1) {
            doScrollToPosition(position, 0);
        }
    }

    /**
     * Similar to scrollToChunk except it will automatically guess what chunk to scroll to.
     * @param chapterSlug
     * @param verseSlug
     */
    public void scrollToVerse(String chapterSlug, String verseSlug) {
        String chunkSlug = mAdapter.getVerseChunk(chapterSlug, verseSlug);
        scrollToChunk(chapterSlug, chunkSlug);
    }

    /**
     * Returns the adapter position of a view holder under the coordinates
     * @param x
     * @param y
     * @return
     */
    protected int findViewHolderAdapterPosition(float x, float y) {
        if(mRecyclerView != null) {
            View view = mRecyclerView.findChildViewUnder(x, y);
            return mRecyclerView.getChildAdapterPosition(view);
        } else {
            return 0;
        }
    }

    /**
     * Returns a viewholder item for the adapter position
     * @param position
     * @return
     */
    protected RecyclerView.ViewHolder getViewHolderForAdapterPosition(int position) {
        if(mRecyclerView != null) {
            return mRecyclerView.findViewHolderForAdapterPosition(position);
        } else {
            return null;
        }
    }

    /**
     * Returns a sample viewholder so we can check on the state of the ui
     * @return
     */
    protected RecyclerView.ViewHolder getViewHolderSample() {
        if(mLayoutManager != null && mRecyclerView != null) {
            int position = getCurrentPosition();
            return mRecyclerView.findViewHolderForLayoutPosition(position);
        } else {
            return null;
        }
    }

    protected void onPrepareView(View rootView) {
        // place holder so child classes can modify the view
    }

    protected ViewModeAdapter getAdapter() {
        return mAdapter;
    }

    public void onDataSetChanged(int count) {
        if(mListener != null) mListener.onDataSetChanged(count);
    }

    public void onEnableMergeConflict(boolean showConflicted, boolean active) {
        if(mListener != null) {
            mListener.onEnableMergeConflict(showConflicted, active);
        }
    }

    @Override
    public RecyclerView.ViewHolder getVisibleViewHolder(int position) {
        if(mLayoutManager != null && mRecyclerView != null) {
            return mRecyclerView.findViewHolderForAdapterPosition(position);//.findViewHolderForLayoutPosition(position);
        }
        return null;
    }

    /**
     * gets item count of adapter
     * @return
     */
    public int getItemCount() {
        if(mAdapter != null) {
            return mAdapter.getItemCount();
        }
        return 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        showProgressDialog();
        if(mSourceContainer == null) {
            // load the container
            if(mAdapter != null) mAdapter.setSourceContainer(null);
            onSourceContainerLoaded(null);
            ManagedTask task = new ManagedTask() {
                @Override
                public void start() {
                    if (mSourceTranslation != null) {
                        mSourceContainer = ContainerCache.cache(mLibrary, mSourceTranslation.resourceContainerSlug);
                    }
                }
            };
            task.addOnFinishedListener(this);
            TaskManager.addTask(task, TASK_ID_OPEN_SELECTED_SOURCE);
        } else if(mAdapter != null) {
            mAdapter.setSourceContainer(mSourceContainer);
            doScrollToPosition(mAdapter.getListStartPosition(), 0);
            onSourceContainerLoaded(mSourceContainer);
            stopProgressDialog();
        }
    }

    /**
     * Initiates a task to open a resource container.
     * This is used for switching the source translation.
     * @param slug
     */
    private void openResourceContainer(final String slug) {
        showProgressDialog();
        mSavedPosition = getCurrentPosition();
        if(mAdapter != null) mAdapter.setSourceContainer(null);
        onSourceContainerLoaded(null);
        ManagedTask task = new ManagedTask() {
            @Override
            public void start() {
                setResult(ContainerCache.cache(mLibrary, slug));
            }
        };
        Bundle args = new Bundle();
        args.putString("slug", slug);
        task.setArgs(args);
        task.addOnFinishedListener(this);
        TaskManager.addTask(task, TASK_ID_OPEN_SOURCE);
    }

    /**
     * removes progress dialog if shown
     */
    private void stopProgressDialog() {
        if(mProgressDialog != null) {
            try {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * create a general progress dialog
     * @param titleId
     * @return
     */
    private void showProgressDialog(int titleId) {
        if(mProgressDialog != null) {
            mProgressDialog.dismiss(); // remove previous
        }
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setTitle(titleId);
        mProgressDialog.setMessage("");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();
    }

    private void showProgressDialog() {
        showProgressDialog(R.string.loading_sources);
    }

    /**
     * enable/disable merge conflict filter in adapter
     * @param enableFilter
     */
    public final void setMergeConflictFilter(boolean enableFilter) {
        if(getAdapter() != null) {
            getAdapter().setMergeConflictFilter(enableFilter);
            getAdapter().triggerNotifyDataSetChanged();
        }
    }

    /**
     * Filters the adapter by the constraint
     * @param constraint the search will be cleared if null or an empty string
     * @param subject the text to be searched
     */
    public final void filter(CharSequence constraint, SearchSubject subject) {
        if(getAdapter() != null) {
            getAdapter().filter(constraint, subject, getCurrentPosition());
            getAdapter().triggerNotifyDataSetChanged();
        }
    }

    /**
     * move to next/previous search item
     * @param next if true then find next, otherwise will find previous
     */
    public void onMoveSearch(boolean next) {
        if(getAdapter() != null) {
            getAdapter().onMoveSearch(next);
        }
    }

    /**
     * Checks if filtering is enabled.
     */
    public final boolean hasFilter() {
        if(getAdapter() != null) return getAdapter().hasFilter();
        return false;
    }

    /**
     * Forces the software keyboard to close
     */
    public void closeKeyboard() {
        App.closeKeyboard(getActivity());
    }

    @Override
    public void onTranslationWordClick(String resourceContainerSlug, String chapterSlug, int width) {

    }

    @Override
    public void onTranslationArticleClick(String volume, String manual, String slug, int width) {

    }

    @Override
    public void onTranslationNoteClick(TranslationHelp note, int width) {

    }

    @Override
    public void onTranslationQuestionClick(TranslationHelp question, int width) {

    }

    /**
     * Require correct interface
     * @param activity
     */
    @Override
    @TargetApi(23)
    public void onAttach(Context activity) {
        super.onAttach(activity);
        onAttachToContext(activity);
    }

    /**
     * Deprecated on API 23
     * Require correct interface
     * @param activity
     */
    @Override
    @SuppressWarnings("deprecation")
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(Build.VERSION.SDK_INT < 23) {
            onAttachToContext(activity);
        }
    }

    /**
     * This method will be called when the fragment attaches to the context/activity
     * @param context
     */
    protected void onAttachToContext(Context context) {
        try {
            this.mListener = (OnEventListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ViewModeFragment.OnEventListener");
        }
    }

    /**
     * Called when the scroll progress manually changes
     * @param scrollProgress
     * @param percent - percentage to scroll within card
     */
    public void onScrollProgressUpdate(int scrollProgress, int percent) {
        mFingerScroll = false;
        Log.d(TAG, "onScrollProgressUpdate: scrollProgress=" + scrollProgress + ", percent=" + percent);
        if(percent == 0) {
            mRecyclerView.scrollToPosition(scrollProgress);
        } else {
            fineScrollToPosition(scrollProgress, percent);
        }
    }

    /**
     * makes sure view is visible, plus it scrolls down proportionally in view
     * @param position
     * @param percent - percentage to scroll within card
     * @return
     */
    private void fineScrollToPosition(int position, int percent) {

        mRecyclerView.scrollToPosition(position); // do coarse adjustment

        View visibleChild = mRecyclerView.getChildAt(0);
        if (visibleChild == null) {
            return;
        }

        RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(visibleChild);
        if(holder == null) {
            return;
        }

        int itemHeight = holder.itemView.getHeight();
        int offset = (int) (percent * itemHeight / 100);

        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.scrollToPositionWithOffset(position, -offset);
        return;
    }

    public void setScrollProgress(int position) {
        // TODO: 6/28/16 update scrollbar
    }

    @Override
    public void onSourceTranslationTabClick(String sourceTranslationId) {
        App.setSelectedSourceTranslation(mTargetTranslation.getId(), sourceTranslationId);
        openResourceContainer(sourceTranslationId);
    }

    @Override
    public void onNewSourceTranslationTabClick() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("tabsDialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        ChooseSourceTranslationDialog dialog = new ChooseSourceTranslationDialog();
        Bundle args = new Bundle();
        args.putString(ChooseSourceTranslationDialog.ARG_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
        dialog.setOnClickListener(this);
        dialog.setArguments(args);
        dialog.show(ft, "tabsDialog");
    }

    @Override
    public void onCancelTabsDialog(String targetTranslationId) {

    }

    @Override
    public void onConfirmTabsDialog(String targetTranslationId, List<String> sourceTranslationIds) {
        String[] oldSourceTranslationIds = App.getOpenSourceTranslations(targetTranslationId);
        for(String id:oldSourceTranslationIds) {
            App.removeOpenSourceTranslation(targetTranslationId, id);
        }

        if(sourceTranslationIds.size() > 0) {
            // save open source language tabs
            for(String slug:sourceTranslationIds) {
                Translation t = mLibrary.index().getTranslation(slug);
                int modifiedAt = mLibrary.getResourceContainerLastModified(t.language.slug, t.project.slug, t.resource.slug);
                try {
                    App.addOpenSourceTranslation(targetTranslationId, slug);
                    TargetTranslation targetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
                    if (targetTranslation != null) {
                        try {
                            targetTranslation.addSourceTranslation(t, modifiedAt);
                        } catch (JSONException e) {
                            Logger.e(this.getClass().getName(), "Failed to record source translation (" + slug + ") usage in the target translation " + targetTranslation.getId(), e);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String selectedSourceTranslationId = App.getSelectedSourceTranslationId(targetTranslationId);
            openResourceContainer(selectedSourceTranslationId);
        } else {
            if(mListener != null) mListener.onNoSourceTranslations(targetTranslationId);
        }
    }

    @Override
    public void onDestroy() {
        // save position state
        if(mLayoutManager != null) {
            int lastItemPosition = getCurrentPosition();
            String chapterId = mAdapter.getFocusedChapterSlug(lastItemPosition);
            String frameId = mAdapter.getFocusedChunkSlug(lastItemPosition);
            App.setLastFocus(mTargetTranslation.getId(), chapterId, frameId);
        }
        if(mAdapter != null) mAdapter.setOnClickListener(null);
        super.onDestroy();
    }

    /**
     * gets the currently viewed position
     * @return
     */
    public int getCurrentPosition() {
        if(mLayoutManager != null) {
            return mLayoutManager.findFirstVisibleItemPosition();
        }
        return 0;
    }

    /**
     * Receives touch events directly from the activity
     * Some times click events can get consumed by a list view or some other object.
     * Receiving events directly from the activity avoids these issues
     *
     * @param event
     * @return
     */
    public boolean onTouchEvent(MotionEvent event) {
        if(mGesture != null) {
            return mGesture.onTouchEvent(event);
        } else {
            Logger.w(this.getClass().getName(), "The gesture detector was not initialized so the touch was not handled");
            return false;
        }
    }

    /**
     * Opens a translation mode
     * @param mode
     */
    public void openTranslationMode(TranslationViewMode mode, Bundle extras) {
        if(mListener != null) mListener.openTranslationMode(mode, extras);
    }

    /**
     * Restarts the auto commit timer
     */
    public void restartAutoCommitTimer() {
        if(mListener != null) mListener.restartAutoCommitTimer();
    }

    /**
     * notify listener of search state changes
     * @param doingSearch - search is currently processing
     * @param numberOfChunkMatches - number of chunks that have the search string
     * @param atEnd - we are at last search item highlighted
     * @param atStart - we are at first search item highlighted
     */
    public void onSearching(boolean doingSearch, int numberOfChunkMatches, boolean atEnd, boolean atStart) {
        if(mListener != null) mListener.onSearching(doingSearch, numberOfChunkMatches, atEnd, atStart);
    }

    /**
     * user has selected to update sources
     */
    public void onUpdateSources() {
        if(mListener != null) mListener.onUpdateSources();
    }

    @Override
    public void onTaskFinished(final ManagedTask task) {
        TaskManager.clearTask(task);
        if(task.getTaskId().equals(TASK_ID_OPEN_SELECTED_SOURCE)) {
            Handler hand  = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if(mSourceContainer == null) {
                        if(mListener != null) mListener.onNoSourceTranslations(mTargetTranslation.getId());
                    } else if(mAdapter != null) {
                        mAdapter.setSourceContainer(mSourceContainer);
                        doScrollToPosition(mAdapter.getListStartPosition(), 0);
                        onSourceContainerLoaded(mSourceContainer);
                    }
                    stopProgressDialog();
                }
            });
        } else if(task.getTaskId().equals(TASK_ID_OPEN_SOURCE)) {
            Handler hand  = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if(task.getResult() == null) {
                        // failed to load the container
                        String slug = task.getArgs().getString("slug");
                        App.removeOpenSourceTranslation(mTargetTranslation.getId(), slug);
                        mAdapter.triggerNotifyDataSetChanged();

                        // TODO: 10/5/16 notify user we failed to select the source
                    } else if(mAdapter != null) {
                        mSourceContainer = (ResourceContainer) task.getResult();
                        mAdapter.setSourceContainer(mSourceContainer);
                        doScrollToPosition(mSavedPosition, 0);
                        onSourceContainerLoaded(mSourceContainer);
                    }
                    stopProgressDialog();
                }
            });
        }
    }

    /**
     * called to set new selected position
     * @param position
     * @param offset - if greater than or equal to 0, then set specific offset
     */
    public void onSetSelectedPosition(int position, int offset) {
        Log.d(TAG, "onSetSelectedPosition: position=" + position + ", offset=" + offset);
        doScrollToPosition(position, offset);
    }

    /**
     * Allows child classes to perform operations that dependon the source container
     * @param sourceContainer
     */
    protected abstract void onSourceContainerLoaded(ResourceContainer sourceContainer);

    public interface OnEventListener {

        /**
         * Called when the user scrolls with their finger
         * @param progress
         */
        void onScrollProgress(int progress);

        /**
         * Called when the dataset in the adapter changed
         * @param count the number of items in the adapter
         */
        void onDataSetChanged(int count);

        /**
         * No source translation has been chosen for this target translation
         * @param targetTranslationId
         */
        void onNoSourceTranslations(String targetTranslationId);

        /**
         * Opens a particular translation mode
         * @param mode
         */
        void openTranslationMode(TranslationViewMode mode, Bundle extras);

        /**
         * Restarts the timer to auto commit changes
         */
        void restartAutoCommitTimer();

        /**
         * notify listener of search state changes
         * @param doingSearch - search is currently processing
         * @param numberOfChunkMatches - number of chunks that have the search string
         * @param atEnd - we are at last search item highlighted
         * @param atStart - we are at first search item highlighted
         */
        void onSearching(boolean doingSearch, int numberOfChunkMatches, boolean atEnd, boolean atStart);

        /**
         * enable/disable merge conflict indicator
         * @param showConflicted
         */
        void onEnableMergeConflict(boolean showConflicted, boolean active);

        /**
         * user has selected to update sources
         */
        void onUpdateSources();
    }
}
