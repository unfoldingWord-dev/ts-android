package com.door43.translationstudio.targettranslations;


import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.filippudak.ProgressPieView.ProgressPieView;

/**
 * Created by joel on 9/3/2015.
 */
public class TargetTranslationAdapter extends BaseAdapter {

    private final TargetTranslation[] mTranslations;

    public TargetTranslationAdapter(TargetTranslation[] translations) {
        mTranslations = translations;
    }

    @Override
    public int getCount() {
        if(mTranslations != null) {
            return mTranslations.length;
        } else {
            return 0;
        }
    }

    @Override
    public TargetTranslation getItem(int position) {
        return mTranslations[position];
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
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_target_project_list_item, null);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        // render view


        return v;
    }

    public static class ViewHolder {
        public ImageView mIconView;
        public TextView mTitleView;
        public TextView mLanguageView;
        public ProgressPieView mProgressView;
        public FloatingActionButton mInfoButton;

        public ViewHolder(View view) {
            mIconView = (ImageView) view.findViewById(R.id.projectIcon);
            mTitleView = (TextView) view.findViewById(R.id.projectTitle);
            mLanguageView = (TextView) view.findViewById(R.id.targetLanguage);
            mProgressView = (ProgressPieView) view.findViewById(R.id.translationProgress);
            mInfoButton = (FloatingActionButton) view.findViewById(R.id.infoButton);
            view.setTag(this);
        }
    }
}