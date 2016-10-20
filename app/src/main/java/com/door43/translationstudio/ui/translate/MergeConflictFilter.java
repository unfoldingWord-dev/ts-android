package com.door43.translationstudio.ui.translate;

import android.widget.Filter;

import com.door43.translationstudio.core.TargetTranslation;

import org.unfoldingword.resourcecontainer.ResourceContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by blm on 10/20/16.
 */

public class MergeConflictFilter extends Filter {

        private final ResourceContainer sourceContainer;
        private final TargetTranslation targetTranslation;
        private final List<ListItem> items;

        private List<ListItem> filteredItems = new ArrayList<>();
        private com.door43.translationstudio.ui.translate.MergeConflictFilter.OnMatchListener listener = null;

        public MergeConflictFilter(ResourceContainer sourceContainer, TargetTranslation targetTranslation, List<ListItem> items) {
            this.sourceContainer = sourceContainer;
            this.targetTranslation = targetTranslation;

            this.items = items;
        }

        /**
         * Attaches a listener to receive match events
         * @param listener
         */
        public void setListener(com.door43.translationstudio.ui.translate.MergeConflictFilter.OnMatchListener listener) {
            this.listener = listener;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            FilterResults results = new FilterResults();
            boolean bEmpty = constraint == null || constraint.toString().trim().isEmpty();
            if(bEmpty){
                results.values = items;
                results.count = items.size();
                // reset the rendered text to clear filter highlights
                for(ListItem item:items) {
                    item.renderedSourceText = null;
                    item.renderedTargetText = null;
                }
            } else {
                for(ListItem item:items) {
                    item.renderedSourceText = null;
                    item.renderedTargetText = null;

                    // load text
                    item.load(sourceContainer, targetTranslation);

                    // match
                    boolean match = item.hasMergeConflicts;

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
            SOURCE,
            TARGET,
            BOTH
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


