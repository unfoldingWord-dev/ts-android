package com.door43.translationstudio.targettranslations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetLanguage;

/**
 * Created by joel on 9/4/2015.
 */
public class TargetLanguageAdapter extends BaseAdapter {
    private TargetLanguage[] mTargetLanguages;

    public TargetLanguageAdapter(TargetLanguage[] targetLanguages) {
        mTargetLanguages = targetLanguages;
    }

    @Override
    public int getCount() {
        if(mTargetLanguages != null) {
            return mTargetLanguages.length;
        } else {
            return 0;
        }
    }

    @Override
    public TargetLanguage getItem(int position) {
        return mTargetLanguages[position];
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
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_target_language_list_item, null);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        // render view
        holder.mLanguageView.setText(getItem(position).name);
        holder.mCodeView.setText(getItem(position).code);

        return v;
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
