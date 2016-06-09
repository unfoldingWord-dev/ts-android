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

        View visibleChild = recyclerView.getChildAt(0);
        if (visibleChild == null) {
            return 0;
        }

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
            int numItemsInList = recyclerView.getAdapter().getItemCount();
            float progress;
            if(numItemsInList < 1) { // sanity check
                numItemsInList = 1;
            }

            float stepSize = 1.0f / numItemsInList;
            progress = firstVisibleItemPosition * stepSize ;

            float y = -holder.itemView.getY();
            float percentScrollOfView = y / itemHeight;

            int viewHeight = recyclerView.getHeight();
            if( (firstVisibleItemPosition == numItemsInList - 1)  // if last chapter, need to tighten range otherwise scroll handle will not reach bottom of screen
                && (viewHeight < itemHeight) ) {

                int scrollRange = itemHeight - viewHeight;
                percentScrollOfView = y / scrollRange;
            }

            float progressOffset = percentScrollOfView * stepSize;
            progress += progressOffset;
            return progress;
        }
    }
}
