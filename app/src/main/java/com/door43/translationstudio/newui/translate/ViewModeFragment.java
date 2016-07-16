package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.BaseFragment;

import org.json.JSONException;

/**
 * Created by joel on 9/18/2015.
 */
public abstract class ViewModeFragment extends BaseFragment implements ViewModeAdapter.OnEventListener, ChooseSourceTranslationDialog.OnClickListener {

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ViewModeAdapter mAdapter;
    private boolean mFingerScroll = false;
    private OnEventListener mListener;
    private TargetTranslation mTargetTranslation;
    private Translator mTranslator;
    private Library mLibrary;
    private String mSourceTranslationId;
    private GestureDetector mGesture;
    private boolean mRememberLastPosition = true;

    /**
     * Returns an instance of the adapter
     * @param activity
     * @param targetTranslationId
     * @param sourceTranslationId
     * @return
     */
    abstract ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId, Bundle extras);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stacked_card_list, container, false);

        mLibrary = App.getLibrary();
        mTranslator = App.getTranslator();

        Bundle args = getArguments();
        String targetTranslationId = args.getString(App.EXTRA_TARGET_TRANSLATION_ID, null);
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        if(mTargetTranslation == null) {
            Logger.e(getClass().getName() ,"A valid target translation id is required. Received '" + targetTranslationId + "' but the translation could not be found");
            getActivity().finish();
        }

        String chapterId = args.getString(App.EXTRA_CHAPTER_ID, App.getLastFocusChapterId(targetTranslationId));
        String frameId = args.getString(App.EXTRA_FRAME_ID, App.getLastFocusFrameId(targetTranslationId));

        // check if we have draft source
        String draftTranslationId = args.getString(App.EXTRA_SOURCE_DRAFT_TRANSLATION_ID, null);
        if(null != draftTranslationId) {
            SourceTranslation sourceTranslation = mLibrary.getDraftTranslation(draftTranslationId);
            mSourceTranslationId = sourceTranslation.getId();
        } else {
            // open selected tab
            mSourceTranslationId = App.getSelectedSourceTranslationId(targetTranslationId);
        }

        if(null == mSourceTranslationId) {
            mListener.onNoSourceTranslations(targetTranslationId);
        } else {
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
            mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mAdapter = generateAdapter(this.getActivity(), targetTranslationId, mSourceTranslationId, chapterId, frameId, args);
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

            mListener.onItemCountChanged(mAdapter.getItemCount(), 0);

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
     * Returns the currently selected source translation
     * @return
     */
    protected SourceTranslation getSourceTranslation() {
        return mLibrary.getSourceTranslation(mSourceTranslationId);
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

    @Override
    public void onResume() {
        super.onResume();
        if(mAdapter != null) {
            mAdapter.rebuild();
        }
    }

    /**
     * start search (filter)
     * @param searchString
     * @param searchTarget - if true then search target text, else search source text
     */
    public void kickOffSearchFilter(String searchString, boolean searchTarget) {
        // default is do nothing
    }

    /**
     * method to see if searching is supported
     */
    public boolean isSearchSupported() {
        return false; // default is not supported
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
     */
    public void onScrollProgressUpdate(int scrollProgress) {
        mFingerScroll = false;
        mRecyclerView.scrollToPosition(scrollProgress);
    }

    @Override
    public void onSourceTranslationTabClick(String sourceTranslationId) {
        App.setSelectedSourceTranslation(mTargetTranslation.getId(), sourceTranslationId);
        mSourceTranslationId = sourceTranslationId;
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
    public void onConfirmTabsDialog(String targetTranslationId, String[] sourceTranslationIds) {
        String[] oldSourceTranslationIds = App.getOpenSourceTranslationIds(targetTranslationId);
        for(String id:oldSourceTranslationIds) {
            App.removeOpenSourceTranslation(targetTranslationId, id);
        }

        if(sourceTranslationIds.length > 0) {
            // save open source language tabs
            for(String id:sourceTranslationIds) {
                SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(id);
                if(sourceTranslation != null) {
                    App.addOpenSourceTranslation(targetTranslationId, sourceTranslation.getId());
                    TargetTranslation targetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
                    if (targetTranslation != null) {
                        try {
                            targetTranslation.addSourceTranslation(sourceTranslation);
                        } catch (JSONException e) {
                            Logger.e(this.getClass().getName(), "Failed to record source translation (" + sourceTranslation.getId() + ") usage in the target translation " + targetTranslation.getId(), e);
                        }
                    }
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
            String chapterId = mAdapter.getFocusedChapterId(lastItemPosition);
            String frameId = mAdapter.getFocusedFrameId(lastItemPosition);
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

    public interface OnEventListener {

        /**
         * Called when the user scrolls with their finger
         * @param progress
         */
        void onScrollProgress(int progress);

        /**
         * Called when the number of items in the fragment adapter changes
         * @param itemCount
         * @param progress
         */
        void onItemCountChanged(int itemCount, int progress);

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
    }
}
