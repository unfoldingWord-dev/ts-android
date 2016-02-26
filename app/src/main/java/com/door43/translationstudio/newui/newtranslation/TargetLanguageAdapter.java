package com.door43.translationstudio.newui.newtranslation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetLanguage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by joel on 9/4/2015.
 */
public class TargetLanguageAdapter extends BaseAdapter {

    private TargetLanguageFilter mTargetLanguageFilter;

    public TargetLanguageAdapter(TargetLanguage[] targetLanguages) {
        List<TargetLanguage> targetLanguagesList = Arrays.asList(targetLanguages);

        mTargetLanguageFilter = new TargetLanguageFilter(targetLanguagesList);
        mTargetLanguageFilter.setResultsListener(new TargetLanguageFilter.OnPublishResultsListener() {
            @Override
            public void onFinish(TargetLanguage[] filteredTargetLanguages) {
                notifyDataSetChanged();
            }
        } );
    }

    @Override
    public int getCount() {
        TargetLanguage[] filteredTargetLanguages = mTargetLanguageFilter.getFilteredTargetLanguages();
        if(filteredTargetLanguages != null) {
            return filteredTargetLanguages.length;
        } else {
            return 0;
        }
    }

    @Override
    public TargetLanguage getItem(int position) {
        TargetLanguage[] filteredTargetLanguages = mTargetLanguageFilter.getFilteredTargetLanguages();
        return filteredTargetLanguages[position];
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
     * Returns the target language filter
     * @return
     */
    public Filter getFilter() {
        return mTargetLanguageFilter;
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
 }
