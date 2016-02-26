package com.door43.translationstudio.newui.newtranslation;

import android.widget.Filter;

import com.door43.translationstudio.core.TargetLanguage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by blm on 2/26/16.
 */
public class TargetLanguageFilter  extends Filter {

    private TargetLanguage[] mTargetLanguages;
    private TargetLanguage[] mFilteredTargetLanguages;

    OnPublishResultsListener mResultsListener;

    TargetLanguageFilter(List<TargetLanguage> targetLanguagesList) {
        Collections.sort(targetLanguagesList);
        mTargetLanguages = targetLanguagesList.toArray(new TargetLanguage[targetLanguagesList.size()]);
        mFilteredTargetLanguages = mTargetLanguages;
    }

    @Override
    protected FilterResults performFiltering(CharSequence charSequence) {
        FilterResults results = new FilterResults();
        if (charSequence == null || charSequence.length() == 0) {
            // no filter
            results.values = Arrays.asList(mTargetLanguages);
            results.count = mTargetLanguages.length;
        } else {
            // perform filter
            List<TargetLanguage> filteredCategories = new ArrayList<>();
            for (TargetLanguage language : mTargetLanguages) {
                // match the target language id
                boolean match = language.getId().toLowerCase().startsWith(charSequence.toString().toLowerCase());
                if (!match) {
                    if (language.name.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                        // match the target language name
                        match = true;
                    }
                }
                if (match) {
                    filteredCategories.add(language);
                }
            }
            results.values = filteredCategories;
            results.count = filteredCategories.size();
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
        List<TargetLanguage> filteredLanguages = (List<TargetLanguage>) filterResults.values;
        if (charSequence != null && charSequence.length() > 0) {
            sortTargetLanguages(filteredLanguages, charSequence);
        }
        mFilteredTargetLanguages = filteredLanguages.toArray(new TargetLanguage[filteredLanguages.size()]);
        if(null != this.mResultsListener) {
            this.mResultsListener.onFinish(mFilteredTargetLanguages);
        }
    }

    /**
     * Sorts target languages by id
     * @param languages
     * @param referenceId languages are sorted according to the reference id
     */
    private static void sortTargetLanguages(List<TargetLanguage> languages, final CharSequence referenceId) {
        Collections.sort(languages, new Comparator<TargetLanguage>() {
            @Override
            public int compare(TargetLanguage lhs, TargetLanguage rhs) {
                String lhId = lhs.getId();
                String rhId = rhs.getId();
                // give priority to matches with the reference
                if(lhId.startsWith(referenceId.toString().toLowerCase())) {
                    lhId = "!" + lhId;
                }
                if(rhId.startsWith(referenceId.toString().toLowerCase())) {
                    rhId = "!" + rhId;
                }
                return lhId.compareToIgnoreCase(rhId);
            }
        });
    }

    public TargetLanguage[] getTargetLanguages() {
        return mTargetLanguages;
    }

    public TargetLanguage[] getFilteredTargetLanguages() {
        return mFilteredTargetLanguages;
    }

    public void setResultsListener(OnPublishResultsListener listener) {
        this.mResultsListener = listener;
    }

    public interface OnPublishResultsListener {

        void onFinish(TargetLanguage[] filteredTargetLanguages);
    }

}
