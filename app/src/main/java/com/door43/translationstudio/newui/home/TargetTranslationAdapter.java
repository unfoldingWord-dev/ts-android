package com.door43.translationstudio.newui.home;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.util.AppContext;
import com.door43.widget.ViewUtil;
import com.filippudak.ProgressPieView.ProgressPieView;

import java.util.Locale;

/**
 * Created by joel on 9/3/2015.
 */
public class TargetTranslationAdapter extends BaseAdapter {

    private TargetTranslation[] mTranslations;
    private OnInfoClickListener mInfoClickListener = null;

    public TargetTranslationAdapter(TargetTranslation[] translations) {
        mTranslations = translations;
    }

    /**
     * Adds a listener to be called when the info button is called
     * @param listener
     */
    public void setOnInfoClickListener(OnInfoClickListener listener) {
        mInfoClickListener = listener;
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
    public View getView(final int position, View convertView, final ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        if(convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_target_translation_list_item, null);
            holder = new ViewHolder(v, parent.getContext());
        } else {
            holder = (ViewHolder)v.getTag();
        }

        // render view
        TargetTranslation targetTranslation = getItem(position);
        Library library = AppContext.getLibrary();
        Project project = library.getProject(targetTranslation.getProjectId(), Locale.getDefault().getLanguage());
        if(project != null) {
            holder.mTitleView.setText(project.name);
        } else {
            holder.mTitleView.setText(targetTranslation.getProjectId());
        }
        holder.mLanguageView.setText(targetTranslation.getTargetLanguageName());
        // TODO: read actual progress from project
        holder.mProgressView.setProgress(Math.round(library.getTranslationProgress(targetTranslation) * 100));
        // TODO: finish rendering project icon
        holder.mInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mInfoClickListener != null) {
                    mInfoClickListener.onClick(getItem(position).getId());
                }
            }
        });


        return v;
    }

    public void changeData(TargetTranslation[] targetTranslations) {
        mTranslations = targetTranslations;
        notifyDataSetChanged();
    }

    public interface OnInfoClickListener {
        void onClick(String targetTranslationId);
    }

    public static class ViewHolder {
        public ImageView mIconView;
        public TextView mTitleView;
        public TextView mLanguageView;
        public ProgressPieView mProgressView;
        public ImageButton mInfoButton;

        public ViewHolder(View view, Context context) {
            mIconView = (ImageView) view.findViewById(R.id.projectIcon);
            mTitleView = (TextView) view.findViewById(R.id.projectTitle);
            mLanguageView = (TextView) view.findViewById(R.id.targetLanguage);
            mProgressView = (ProgressPieView) view.findViewById(R.id.translationProgress);
            mProgressView.setMax(100);
            mInfoButton = (ImageButton) view.findViewById(R.id.infoButton);
            ViewUtil.tintViewDrawable(mInfoButton, context.getResources().getColor(R.color.dark_disabled_text));
            view.setTag(this);
        }
    }
}