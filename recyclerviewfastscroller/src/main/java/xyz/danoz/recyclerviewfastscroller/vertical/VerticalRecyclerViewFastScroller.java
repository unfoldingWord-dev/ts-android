package xyz.danoz.recyclerviewfastscroller.vertical;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;

import xyz.danoz.recyclerviewfastscroller.AbsRecyclerViewFastScroller;
import xyz.danoz.recyclerviewfastscroller.R;
import xyz.danoz.recyclerviewfastscroller.RecyclerViewScroller;
import xyz.danoz.recyclerviewfastscroller.calculation.VerticalScrollBoundsProvider;
import xyz.danoz.recyclerviewfastscroller.calculation.position.VerticalScreenPositionCalculator;
import xyz.danoz.recyclerviewfastscroller.calculation.progress.TouchableScrollProgressCalculator;
import xyz.danoz.recyclerviewfastscroller.calculation.progress.VerticalLinearLayoutManagerScrollProgressCalculator;
import xyz.danoz.recyclerviewfastscroller.calculation.progress.VerticalScrollProgressCalculator;

/**
 * Widget used to fast-scroll a vertical {@link RecyclerView}.
 * Currently assumes the use of a {@link LinearLayoutManager}
 */
public class VerticalRecyclerViewFastScroller extends AbsRecyclerViewFastScroller implements RecyclerViewScroller {

    public static final String TAG = VerticalRecyclerViewFastScroller.class.getSimpleName();
    @Nullable private VerticalScrollProgressCalculator mScrollProgressCalculator;
    @Nullable private VerticalScreenPositionCalculator mScreenPositionCalculator;

    public VerticalRecyclerViewFastScroller(Context context) {
        this(context, null);
    }

    public VerticalRecyclerViewFastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalRecyclerViewFastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
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

    @Override
    public void moveHandleToPosition(float scrollProgress) {
        if (mScreenPositionCalculator == null) {
            Log.d(TAG, "moveHandleToPosition: NULL calculator.  scrollProgress=" + scrollProgress);
            return;
        }
        Log.d(TAG, "moveHandleToPosition: scrollProgress=" + scrollProgress);
        float yPositionFromScrollProgress = mScreenPositionCalculator.getYPositionFromScrollProgress(scrollProgress);
        Log.d(TAG, "moveHandleToPosition: yPositionFromScrollProgress=" + yPositionFromScrollProgress);
        int handleHeight = mHandle.getHeight();
        Log.d(TAG, "moveHandleToPosition: handleHeight=" + handleHeight);
        yPositionFromScrollProgress += handleHeight / 2;
        Log.d(TAG, "moveHandleToPosition: new yPositionFromScrollProgress=" + yPositionFromScrollProgress);
        mHandle.setY(yPositionFromScrollProgress);
    }

    protected void onCreateScrollProgressCalculator() {
        VerticalScrollBoundsProvider boundsProvider =
                new VerticalScrollBoundsProvider(mBar.getY(), mBar.getY() + mBar.getHeight() - mHandle.getHeight());
        mScrollProgressCalculator = new VerticalLinearLayoutManagerScrollProgressCalculator(boundsProvider);
        mScreenPositionCalculator = new VerticalScreenPositionCalculator(boundsProvider);
    }
}
