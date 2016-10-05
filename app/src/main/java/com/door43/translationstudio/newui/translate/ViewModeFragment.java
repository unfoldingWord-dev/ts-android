package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.BaseFragment;

import org.json.JSONException;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.util.List;

/**
 * Created by joel on 9/18/2015.
 */
public abstract class ViewModeFragment extends BaseFragment implements ViewModeAdapter.OnEventListener, ChooseSourceTranslationDialog.OnClickListener, ManagedTask.OnFinishedListener {

    public static final String TAG = ViewModeFragment.class.getSimpleName();
    private static final String TASK_ID_SET_SOURCE = "set-source";
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ViewModeAdapter mAdapter;
    private boolean mFingerScroll = false;
    private OnEventListener mListener;
    private TargetTranslation mTargetTranslation;
    private Translator mTranslator;
    private Door43Client mLibrary;
    private String mSourceTranslationSlug;
    private GestureDetector mGesture;
    private boolean mRememberLastPosition = true;

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

        // check if we have draft source
        String draftTranslationId = args.getString(App.EXTRA_SOURCE_DRAFT_TRANSLATION_ID, null);
        if(null != draftTranslationId) {
            try {
                ResourceContainer rc = mLibrary.open(draftTranslationId);
                mSourceTranslationSlug = rc.slug;
            } catch (Exception e) {
                e.printStackTrace();
            }
//
//            SourceTranslation sourceTranslation = mLibrary.getDraftTranslation(draftTranslationId);
//            mSourceTranslationId = sourceTranslation.getId();
        } else {
            // open selected tab
            mSourceTranslationSlug = App.getSelectedSourceTranslationId(targetTranslationSlug);
        }

        if(null == mSourceTranslationSlug) {
            mListener.onNoSourceTranslations(targetTranslationSlug);
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
                        int position = mLayoutManager.findFirstVisibleItemPosition();
                        mListener.onScrollProgress(position);
                    }
                }
            });

            onDataSetChanged(mAdapter.getItemCount());

            mAdapter.setOnClickListener(this);

            if(savedInstanceState == null) {
                mLayoutManager.scrollToPosition(mAdapter.getListStartPosition());
                mListener.onScrollProgress(mAdapter.getListStartPosition());
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
        try {
            return mLibrary.open(mSourceTranslationSlug);
        } catch (Exception e) {
            // We perform checks when loading so this should never happen
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Scrolls to the given frame
     * @param chapterId
     * @param frameId
     */
    public void scrollToFrame(String chapterId, String frameId) {
        closeKeyboard();
        int position = mAdapter.getItemPosition(chapterId, frameId);
        if(position != -1) {
            mLayoutManager.scrollToPosition(position);
            mListener.onScrollProgress(position);
        }
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
            int position = mLayoutManager.findFirstVisibleItemPosition();
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
        if(mListener != null) {
            mListener.onDataSetChanged(count);
        }
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
        ManagedTask loadingTask = new ManagedTask() {
            @Override
            public void start() {
                if(mAdapter != null) {
                    mAdapter.setSourceTranslation(mSourceTranslationSlug, false);
                }
            }
        };
        loadingTask.addOnFinishedListener(this);
        TaskManager.addTask(loadingTask, TASK_ID_SET_SOURCE);
    }

    /**
     * Filters the adapter by the constraint
     * @param constraint the search will be cleared if null or an empty string
     * @param subject the text to be searched
     */
    public final void filter(CharSequence constraint, TranslationFilter.FilterSubject subject) {
        if(getAdapter() != null) {
            getAdapter().filter(constraint, subject);
            getAdapter().triggerNotifyDataSetChanged();
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
    public void onTranslationWordClick(String translationWordId, int width) {

    }

    @Override
    public void onTranslationArticleClick(String volume, String manual, String slug, int width) {

    }

    @Override
    public void onTranslationNoteClick(String chapterId, String frameId, String translationNoteId, int width) {

    }

    @Override
    public void onCheckingQuestionClick(String chapterId, String frameId, String checkingQuestionId, int width) {

    }

    /**
     * Require correct interface
     * @param activity
     */
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnEventListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ViewModeFragment.OnEventListener");
        }
    }

    /**
     * Called when the scroll progress manually changes
     * @param scrollProgress
     * @param percent - percentage to scroll within card
     */
    public void onScrollProgressUpdate(int scrollProgress, int percent) {
        mFingerScroll = false;
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
        mSourceTranslationSlug = sourceTranslationId;
        mAdapter.setSourceTranslation(sourceTranslationId);
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
        String[] oldSourceTranslationIds = App.getSelectedSourceTranslations(targetTranslationId);
        for(String id:oldSourceTranslationIds) {
            App.removeOpenSourceTranslation(targetTranslationId, id);
        }

        if(sourceTranslationIds.size() > 0) {
            // save open source language tabs
            for(String slug:sourceTranslationIds) {
                ResourceContainer rc = null;
                try {
                    rc = mLibrary.open(slug);
                    App.addOpenSourceTranslation(targetTranslationId, slug);
                    TargetTranslation targetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
                    if (targetTranslation != null) {
                        try {
                            targetTranslation.addSourceTranslation(rc);
                        } catch (JSONException e) {
                            Logger.e(this.getClass().getName(), "Failed to record source translation (" + slug + ") usage in the target translation " + targetTranslation.getId(), e);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String selectedSourceTranslationId = App.getSelectedSourceTranslationId(targetTranslationId);
            mAdapter.setSourceTranslation(selectedSourceTranslationId);
        } else {
            mListener.onNoSourceTranslations(targetTranslationId);
        }
    }

    @Override
    public void onDestroy() {
        // save position state
        if(mRememberLastPosition && (mLayoutManager != null)) {
            int lastItemPosition = mLayoutManager.findFirstVisibleItemPosition();
            String chapterId = mAdapter.getFocusedChapterSlug(lastItemPosition);
            String frameId = mAdapter.getFocusedChunkSlug(lastItemPosition);
            App.setLastFocus(mTargetTranslation.getId(), chapterId, frameId);
        }
        super.onDestroy();
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
        mListener.openTranslationMode(mode, extras);
    }

    /**
     * Restarts the auto commit timer
     */
    public void restartAutoCommitTimer() {
        mListener.restartAutoCommitTimer();
    }

    public void setRememberLastPosition(boolean rememberLastPosition) {
        this.mRememberLastPosition = rememberLastPosition;
    }

    /**
     * enable/disable the busy indiciator
     * @param isSearching
     */
    public void onSearching(boolean isSearching) {
        mListener.onSearching(isSearching);
    }

    @Override
    public void onTaskFinished(ManagedTask task) {
        TaskManager.clearTask(task);
        if(task.getTaskId().equals(TASK_ID_SET_SOURCE)) {
            Handler hand  = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if(mAdapter != null) {
                        mAdapter.triggerNotifyDataSetChanged();
                    }
                }
            });
        }
    }

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
         * enable/disable busy spinner
         * @param enable
         */
        void onSearching(boolean enable);
    }
}
