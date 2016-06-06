package xyz.danoz.recyclerviewfastscroller.calculation.progress;

import xyz.danoz.recyclerviewfastscroller.calculation.VerticalScrollBoundsProvider;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.View;

/**
 * Calculates scroll progress for a {@link RecyclerView} with a {@link LinearLayoutManager}
 */
public class VerticalLinearLayoutManagerScrollProgressCalculator extends VerticalScrollProgressCalculator {

    public static final String TAG = VerticalLinearLayoutManagerScrollProgressCalculator.class.getSimpleName();

    public VerticalLinearLayoutManagerScrollProgressCalculator(VerticalScrollBoundsProvider scrollBoundsProvider) {
        super(scrollBoundsProvider);
    }

    /**
     * @param recyclerView recycler that experiences a scroll event
     * @return the progress through the recycler view list content
     */
    @Override
    public float calculateScrollProgress(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        int lastFullyVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
        Log.d(TAG, "calculateScrollProgress: lastFullyVisiblePosition=" + lastFullyVisiblePosition);

//        int count = recyclerView.getChildCount();
//        for (int i = 0; i < count; i++) {
//            Log.d(TAG, "calculateScrollProgress: child i=" + i);
//            View visibleChild = recyclerView.getChildAt(i);
//            if (visibleChild == null) {
//                Log.d(TAG, "calculateScrollProgress: child null");
//            }
//            ViewHolder holder = recyclerView.getChildViewHolder(visibleChild);
//            if(holder == null) {
//                return 0;
//            }
//            int itemHeight = holder.itemView.getHeight();  // looks like there is an assumption that view items are same height
//            Log.d(TAG, "calculateScrollProgress: itemHeight=" + itemHeight);
//            float y = -holder.itemView.getY();
//            Log.d(TAG, "calculateScrollProgress: y=" + y);
//        }

        View visibleChild = recyclerView.getChildAt(0);
        if (visibleChild == null) {
            return 0;
        }

        Log.d(TAG, "calculateScrollProgress: lastFullyVisiblePosition=" + lastFullyVisiblePosition);

        ViewHolder holder = recyclerView.getChildViewHolder(visibleChild);
        if(holder == null) {
            return 0;
        }
        int itemHeight = holder.itemView.getHeight();  // looks like there is an assumption that view items are same height

        if(lastFullyVisiblePosition >= 0) {

            int recyclerHeight = recyclerView.getHeight();
            int itemsInWindow = recyclerHeight / itemHeight;

            int numItemsInList = recyclerView.getAdapter().getItemCount();
            int numScrollableSectionsInList = numItemsInList - itemsInWindow;
            int indexOfLastFullyVisibleItemInFirstSection = numItemsInList - numScrollableSectionsInList - 1;

            int currentSection = lastFullyVisiblePosition - indexOfLastFullyVisibleItemInFirstSection;

            return (float) currentSection / numScrollableSectionsInList;

        } else { // in case the child views are too big to fit in window

            int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
            Log.d(TAG, "calculateScrollProgress: firstVisibleItemPosition=" + firstVisibleItemPosition);
            int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
            Log.d(TAG, "calculateScrollProgress: lastVisibleItemPosition=" + lastVisibleItemPosition);
            int numItemsInList = recyclerView.getAdapter().getItemCount();
            float progress;
            if(numItemsInList <= 1) { // sanity check
                progress = 1.0f;
            } else {
                float stepSize = 1.0f / (numItemsInList - 1);
                progress = firstVisibleItemPosition * stepSize ;

                float y = -holder.itemView.getY();
                float offset = y/itemHeight;
                float progressOffset = offset * stepSize;
                Log.d(TAG, "calculateScrollProgress: progress=" + progress);
                progress += progressOffset;
                Log.d(TAG, "calculateScrollProgress: progressOffset=" + progressOffset);
            }
            return progress;
        }
    }
}
