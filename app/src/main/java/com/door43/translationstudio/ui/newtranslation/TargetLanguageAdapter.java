package com.door43.translationstudio.ui.newtranslation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.util.ColorUtil;

import org.unfoldingword.door43client.models.TargetLanguage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by joel on 9/4/2015.
 */
public class TargetLanguageAdapter extends BaseAdapter {
    private final Context context;
    private TargetLanguage[] mTargetLanguages;
    private TargetLanguage[] mFilteredTargetLanguages;
    private TargetLanguageFilter mTargetLanguageFilter;
    private String[] disabledLanguages;

    public TargetLanguageAdapter(Context context, List<TargetLanguage> targetLanguages) {
        this.context = context;
        if(targetLanguages != null) {
            Collections.sort(targetLanguages);
            mTargetLanguages = targetLanguages.toArray(new TargetLanguage[targetLanguages.size()]);
            mFilteredTargetLanguages = mTargetLanguages;
        }
    }

    @Override
    public int getCount() {
        if(mFilteredTargetLanguages != null) {
            return mFilteredTargetLanguages.length;
        } else {
            return 0;
        }
    }

    @Override
    public TargetLanguage getItem(int position) {
        return mFilteredTargetLanguages[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    /**
     * checks if the item is disabled
     * @param position
     * @return
     */
    public boolean isItemDisabled(int position) {
        TargetLanguage l = getItem(position);
        return isLanguageDisabled(l.slug);
    }

    /**
     * Checks if the language is disabled
     * @return true if disabled
     */
    private boolean isLanguageDisabled(String id) {
        if(this.disabledLanguages != null) {
            for (String l : this.disabledLanguages) {
                if (id.equals(l)) return true;
            }
        }
        return false;
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


        TargetLanguage l = getItem(position);
        // render view
        holder.mLanguageView.setText(l.name);
        holder.mCodeView.setText(l.slug);
        holder.setDisabled(this.context, isLanguageDisabled(l.slug));

        return v;
    }

    /**
     * Returns the target language filter
     * @return
     */
    public Filter getFilter() {
        if(mTargetLanguageFilter == null) {
            mTargetLanguageFilter = new TargetLanguageFilter();
        }
        return mTargetLanguageFilter;
    }

    /**
     * Sets the language id's that will be disabled (not selectable)
     * @param disabledLanguages
     */
    public void setDisabledLanguages(String[] disabledLanguages) {
        if(disabledLanguages == null) {
            this.disabledLanguages = new String[0];
        } else {
            this.disabledLanguages = disabledLanguages;
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder {
        private final View mRootView;
        public TextView mLanguageView;
        public TextView mCodeView;

        public ViewHolder(View view) {
            mLanguageView = (TextView) view.findViewById(R.id.languageName);
            mCodeView = (TextView) view.findViewById(R.id.languageCode);
            mRootView = view;
            view.setTag(this);
        }

        public void setDisabled(Context context, boolean disabled) {
            if(disabled) {
                mRootView.setBackgroundColor(ColorUtil.getColor(context, R.color.graph_background));
                mCodeView.setTextColor(ColorUtil.getColor(context, R.color.dark_disabled_text));
                mLanguageView.setTextColor(ColorUtil.getColor(context, R.color.dark_disabled_text));
            } else {
                mRootView.setBackgroundColor(ColorUtil.getColor(context, android.R.color.transparent));
                mCodeView.setTextColor(ColorUtil.getColor(context, R.color.dark_secondary_text));
                mLanguageView.setTextColor(ColorUtil.getColor(context, R.color.dark_primary_text));
            }
        }
    }

    private class TargetLanguageFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults results = new FilterResults();
            if(charSequence == null || charSequence.length() == 0) {
                // no filter
                results.values = Arrays.asList(mTargetLanguages);
                results.count = mTargetLanguages.length;
            } else {
                // perform filter
                List<TargetLanguage> filteredCategories = new ArrayList<>();
                for(TargetLanguage language:mTargetLanguages) {
                    // match the target language id
                    boolean match = language.slug.toLowerCase().startsWith(charSequence.toString().toLowerCase());
                    if(!match) {
                        if (language.name.toLowerCase().contains(charSequence.toString().toLowerCase())) {
                            // match the target language name
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
            List<TargetLanguage> filteredLanguages = (List<TargetLanguage>)filterResults.values;
            if(charSequence != null && charSequence.length() > 0) {
                sortTargetLanguages(filteredLanguages, charSequence);
            }
            mFilteredTargetLanguages = filteredLanguages.toArray(new TargetLanguage[filteredLanguages.size()]);
            notifyDataSetChanged();
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
                String lhId = lhs.slug;
                String rhId = rhs.slug;
                // give priority to matches with the reference
                if(lhId.toLowerCase().startsWith(referenceId.toString().toLowerCase())) {
                    lhId = "!!" + lhId;
                }
                if(rhId.toLowerCase().startsWith(referenceId.toString().toLowerCase())) {
                    rhId = "!!" + rhId;
                }
                if(lhs.name.toLowerCase().startsWith(referenceId.toString().toLowerCase())) {
                    lhId = "!" + lhId;
                }
                if(rhs.name.toLowerCase().startsWith(referenceId.toString().toLowerCase())) {
                    rhId = "!" + rhId;
                }
                return lhId.compareToIgnoreCase(rhId);
            }
        });
    }
}
