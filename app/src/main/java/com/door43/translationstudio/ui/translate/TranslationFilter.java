package com.door43.translationstudio.ui.translate;

import android.widget.Filter;

import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.ui.translate.review.SearchSubject;

import org.unfoldingword.resourcecontainer.ResourceContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs a search over list items
 */
public class TranslationFilter extends Filter {

    private final ResourceContainer sourceContainer;
    private final TargetTranslation targetTranslation;
    private final SearchSubject subject;
    private final List<ListItem> items;

    private List<ListItem> filteredItems = new ArrayList<>();
    private OnMatchListener listener = null;

    public TranslationFilter(ResourceContainer sourceContainer, TargetTranslation targetTranslation, SearchSubject subject, List<ListItem> items) {
        this.sourceContainer = sourceContainer;
        this.targetTranslation = targetTranslation;

        this.subject = subject;
        this.items = items;
    }

    /**
     * Attaches a listener to receive match events
     * @param listener
     */
    public void setListener(OnMatchListener listener) {
        this.listener = listener;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        if(constraint == null || constraint.toString().trim().isEmpty()){
            results.values = items;
            results.count = items.size();
            // reset the rendred text to clear filter highlights
            for(ListItem item:items) {
                item.renderedSourceText = null;
                item.renderedTargetText = null;
            }
        } else {
            String matcher = constraint.toString().toLowerCase().trim();
            for(ListItem item:items) {
                item.renderedSourceText = null;
                item.renderedTargetText = null;

                // load text
                item.load(sourceContainer, targetTranslation);

                // match
                boolean match = false;
                if(subject == SearchSubject.TARGET || subject == SearchSubject.BOTH) {
                    match = item.targetText.toString().toLowerCase().contains(matcher) || match;
                }
                if(subject == SearchSubject.SOURCE || subject == SearchSubject.BOTH) {
                    match = item.sourceText.toString().toLowerCase().contains(matcher) || match;
                }

                // record matches
                if(match) {
                    filteredItems.add(item);
                    if(listener != null) listener.onMatch(item);
                }
            }
            results.values = filteredItems;
            results.count = filteredItems.size();
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        if(listener != null) listener.onFinished(constraint, (List<ListItem>)results.values);
    }

    interface OnMatchListener {
        /**
         * called when a match was found
         * @param item
         */
        void onMatch(ListItem item);

        /**
         * called when the filtering is finished
         * @param constraint
         * @param results
         */
        void onFinished(CharSequence constraint, List<ListItem> results);
    }
}

