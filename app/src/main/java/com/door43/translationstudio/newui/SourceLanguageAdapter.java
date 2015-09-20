package com.door43.translationstudio.newui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.SourceLanguage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by joel on 9/7/2015.
 */
public class SourceLanguageAdapter extends BaseAdapter {

    private SourceLanguage[] mLanguages;
    private SourceLanguage[] mFilteredLanguages;
    private SourceLanguageFilter mSourceLanguageFilter;


    public SourceLanguageAdapter(SourceLanguage[] languages) {
        List<SourceLanguage> languagesList = Arrays.asList(languages);
        // TODO: sort
        mLanguages = languagesList.toArray(new SourceLanguage[languagesList.size()]);
        mFilteredLanguages = mLanguages;
    }

    @Override
    public int getCount() {
        if(mFilteredLanguages != null) {
            return mFilteredLanguages.length;
        } else {
            return 0;
        }
    }

    @Override
    public SourceLanguage getItem(int position) {
        return mFilteredLanguages[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        if(convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_language_list_item, null);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        // render view
        holder.mLanguageView.setText(getItem(position).name);
        holder.mCodeView.setText(getItem(position).code);

        return v;
    }

    /**
     * Returns the source language filter
     * @return
     */
    public Filter getFilter() {
        if(mSourceLanguageFilter == null) {
            mSourceLanguageFilter = new SourceLanguageFilter();
        }
        return mSourceLanguageFilter;
    }

    public static class ViewHolder {
        public TextView mLanguageView;
        public TextView mCodeView;

        public ViewHolder(View view) {
            mLanguageView = (TextView) view.findViewById(R.id.languageName);
            mCodeView = (TextView) view.findViewById(R.id.languageCode);
            view.setTag(this);
        }
    }

    /**
     * A filter for projects
     */
    private class SourceLanguageFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults results = new FilterResults();
            if(charSequence == null || charSequence.length() == 0) {
                // no filter
                results.values = Arrays.asList(mLanguages);
                results.count = mLanguages.length;
            } else {
                // perform filter
                List<SourceLanguage> filteredCategories = new ArrayList<>();
                for(SourceLanguage language:mLanguages) {
                    // match the source language id
                    boolean match = language.getId().toLowerCase().startsWith(charSequence.toString().toLowerCase());
                    if(!match) {
                        if (language.name.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                            // match the source language name
                            match = true;
                        }
                    }
                    if(match) {
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
            List<SourceLanguage> filteredLanguages = ((List<SourceLanguage>) filterResults.values);
            if(charSequence != null && charSequence.length() > 0) {
                sortSourceLanguages(filteredLanguages, charSequence);
            }
            mFilteredLanguages = filteredLanguages.toArray(new SourceLanguage[filterResults.count]);
            notifyDataSetChanged();
        }
    }

    /**
     * Sorts target languages by id
     * @param languages
     * @param referenceId languages are sorted according to the reference id
     */
    private static void sortSourceLanguages(List<SourceLanguage> languages, final CharSequence referenceId) {
        Collections.sort(languages, new Comparator<SourceLanguage>() {
            @Override
            public int compare(SourceLanguage lhs, SourceLanguage rhs) {
                String lhId = lhs.getId();
                String rhId = rhs.getId();
                // give priority to matches with the reference
                if (lhId.startsWith(referenceId.toString().toLowerCase())) {
                    lhId = "!" + lhId;
                }
                if (rhId.startsWith(referenceId.toString().toLowerCase())) {
                    rhId = "!" + rhId;
                }
                return lhId.compareToIgnoreCase(rhId);
            }
        });
    }
}
