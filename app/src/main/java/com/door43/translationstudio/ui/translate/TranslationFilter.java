package com.door43.translationstudio.ui.translate;

import android.widget.Filter;

import com.door43.translationstudio.core.TargetTranslation;

import org.unfoldingword.resourcecontainer.ResourceContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs a search over list items
 */
public class TranslationFilter extends Filter {

    private final ResourceContainer sourceContainer;
    private final TargetTranslation targetTranslation;
    private final FilterSubject subject;
    private final List<ListItem> items;

    private List<ListItem> filteredItems = new ArrayList<>();
    private OnMatchListener listener = null;

    public TranslationFilter(ResourceContainer sourceContainer, TargetTranslation targetTranslation, FilterSubject subject, List<ListItem> items) {
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
                if(subject == FilterSubject.TARGET || subject == FilterSubject.BOTH) {
                    match = item.targetText.toString().toLowerCase().contains(matcher) || match;
                }
                if(subject == FilterSubject.SOURCE || subject == FilterSubject.BOTH) {
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

    /**
     * Indicates which text should be searched
     */
    public enum FilterSubject {
        SOURCE(0),
        TARGET(1),
        BOTH(2);

        private int _value;

        FilterSubject(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static FilterSubject fromString(String value, FilterSubject defaultValue ) {
            Integer returnValue = null;
            if(value != null) {
                try {
                    returnValue = Integer.valueOf(value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(returnValue == null) {
                return defaultValue;
            }

            return fromInt(returnValue);
        }

        public static FilterSubject fromInt(int i) {
            for (FilterSubject b : FilterSubject.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
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

