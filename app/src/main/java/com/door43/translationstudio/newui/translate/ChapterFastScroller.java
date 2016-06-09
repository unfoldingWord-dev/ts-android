package com.door43.translationstudio.newui.translate;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.door43.translationstudio.R;

import xyz.danoz.recyclerviewfastscroller.AbsRecyclerViewFastScroller;
import xyz.danoz.recyclerviewfastscroller.RecyclerViewScroller;
import xyz.danoz.recyclerviewfastscroller.calculation.VerticalScrollBoundsProvider;
import xyz.danoz.recyclerviewfastscroller.calculation.position.VerticalScreenPositionCalculator;
import xyz.danoz.recyclerviewfastscroller.calculation.progress.TouchableScrollProgressCalculator;
import xyz.danoz.recyclerviewfastscroller.calculation.progress.VerticalLinearLayoutManagerScrollProgressCalculator;
import xyz.danoz.recyclerviewfastscroller.calculation.progress.VerticalScrollProgressCalculator;

/**
 * Replacement for VerticalRecyclerViewFastScroller.
 */
public class ChapterFastScroller  extends AbsRecyclerViewFastScroller implements RecyclerViewScroller {

    public static final String TAG = ChapterFastScroller.class.getSimpleName();
    @Nullable private VerticalScrollProgressCalculator mScrollProgressCalculator;
    @Nullable private VerticalScreenPositionCalculator mScreenPositionCalculator;
    private int mPosition;
    private float mFraction;
    private boolean mFromTouch = false;
    private boolean mOnLayout = false;

    public ChapterFastScroller(Context context) {
        this(context, null);
    }

    public ChapterFastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChapterFastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.vertical_recycler_fast_scroller_layout;
    }

    @Override
    @Nullable
    protected TouchableScrollProgressCalculator getScrollProgressCalculator() {
        return mScrollProgressCalculator;
    }

    /**
     * calculates position for mRecyclerView which needs position in range of 0 to n-1 and adds on the fractional amount for scroll
     * @param scrollProgress
     * @return
     */
    private void calculatePositionFromScrollProgress(float scrollProgress) {
        int itemCount = mRecyclerView.getAdapter().getItemCount();
        float itemPos = itemCount * scrollProgress;
        int position = (int) itemPos;
        float fraction = itemPos - position;
        if(position >= itemCount) { // limit in case scrollProgress is exactly 1
            position = itemCount - 1;
            fraction = 0.99999f;
        }
        mPosition = position;
        mFraction = fraction;
    }

    @Override
    public void scrollTo(float scrollProgress, boolean fromTouch) {
        calculatePositionFromScrollProgress(scrollProgress);
        Log.d(TAG, "scrollTo fromTouch=" + fromTouch);
        Log.d(TAG, "scrollTo scrollProgress=" + scrollProgress);
        Log.d(TAG, "scrollTo position: " + mPosition);
        Log.d(TAG, "scrollTo fraction: " + mFraction);

        mFromTouch = fromTouch;

        fineScrollToPosition(mPosition, mFraction);

        updateSectionIndicator(mPosition, scrollProgress);

        Log.d(TAG, "scrollTo done");

//        mRecyclerView.scrollToPosition(position);
//
//        updateSectionIndicator(position, scrollProgress);
    }

    /**
     * makes sure view is visible, plus it scrolls down proportionally in view
     * @param position
     * @param fraction
     * @return
     */
    private void fineScrollToPosition(int position, float fraction) {

        View visibleChild = mRecyclerView.getChildAt(0);
        if (visibleChild == null) {
            mRecyclerView.scrollToPosition(mPosition); // do coarse adjustment
            return;
        }

        RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(visibleChild);
        if(holder == null) {
            mRecyclerView.scrollToPosition(mPosition); // do coarse adjustment
            return;
        }

        int itemHeight = holder.itemView.getHeight();  // looks like there is an assumption that view items are same height

        int offset = (int) (fraction * itemHeight);
        Log.d(TAG, "fineScrollToPosition itemHeight: " + itemHeight);
        Log.d(TAG, "fineScrollToPosition offset: " + offset);

//        mRecyclerView.scrollBy(0, offset);
        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.scrollToPositionWithOffset(position, -offset);
        return;
    }

    @Override
    public void moveHandleToPosition(float scrollProgress) {

        if(mOnLayout && mFromTouch) {
            Log.d(TAG, "moveHandleToPosition: ignore onLayout update after touch");
            mFromTouch = false;
            return;
        }

        if (mScreenPositionCalculator == null) {
            Log.d(TAG, "moveHandleToPosition: NULL calculator.  scrollProgress=" + scrollProgress);
            return;
        }
        Log.d(TAG, "moveHandleToPosition: scrollProgress=" + scrollProgress);
        float yPositionFromScrollProgress = mScreenPositionCalculator.getYPositionFromScrollProgress(scrollProgress);
        Log.d(TAG, "moveHandleToPosition: yPositionFromScrollProgress=" + yPositionFromScrollProgress);
        mHandle.setY(yPositionFromScrollProgress);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mOnLayout = true;
        super.onLayout(changed, left, top, right, bottom);
        mOnLayout = false;
    }


    protected void onCreateScrollProgressCalculator() {
        VerticalScrollBoundsProvider boundsProvider =
                new VerticalScrollBoundsProvider(mBar.getY(), mBar.getY() + mBar.getHeight() - mHandle.getHeight());
        mScrollProgressCalculator = new VerticalLinearLayoutManagerScrollProgressCalculator(boundsProvider);
        mScreenPositionCalculator = new VerticalScreenPositionCalculator(boundsProvider);
    }
}
