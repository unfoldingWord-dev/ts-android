package com.door43.translationstudio.newui.home;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ThreadableUI;
import com.door43.widget.ViewUtil;
import com.filippudak.ProgressPieView.ProgressPieView;

import java.util.Locale;

/**
 * Created by joel on 9/3/2015.
 */
public class TargetTranslationAdapter extends BaseAdapter {

    private final Context mContext;
    private TargetTranslation[] mTranslations;
    private OnInfoClickListener mInfoClickListener = null;
    private int[] mTranslationProgress;
    private boolean[] mTranslationProgressCalculated;

    public TargetTranslationAdapter(Context context) {
        mContext = context;
        mTranslations = new TargetTranslation[0];
        mTranslationProgress = new int[0];
        mTranslationProgressCalculated = new boolean[0];
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
        final ViewHolder holder;

        if(convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_target_translation_list_item, null);
            holder = new ViewHolder(v, parent.getContext());
        } else {
            holder = (ViewHolder)v.getTag();
        }

        // render view
        final TargetTranslation targetTranslation = getItem(position);
        final Library library = AppContext.getLibrary();
        Project project = library.getProject(targetTranslation.getProjectId(), Locale.getDefault().getLanguage());
        if(project != null) {
            holder.mTitleView.setText(project.name);
        } else {
            holder.mTitleView.setText(targetTranslation.getProjectId());
        }
        holder.mLanguageView.setText(targetTranslation.getTargetLanguageName());

        // calculate translation progress
        if(!mTranslationProgressCalculated[position] && holder.mCalculatingProgressForPosition != position) {
            holder.mCalculatingProgressForPosition = position;
            final ViewHolder staticHolder = holder;
            if(holder.mProgressTask != null) {
                holder.mProgressTask.stop();
            }
            holder.mProgressTask = new ThreadableUI(mContext) {
                private int progress = 0;

                @Override
                public void onStop() {
                    staticHolder.mCalculatingProgressForPosition = -1;
                }

                @Override
                public void run() {
                    // TODO: this method should respond correctly to thread interruptions
                    progress = Math.round(library.getTranslationProgress(targetTranslation) * 100);
                }

                @Override
                public void onPostExecute() {
                    if (!isInterrupted()) {
                        mTranslationProgress[position] = progress;
                        mTranslationProgressCalculated[position] = true;
                        staticHolder.mProgressView.setProgress(progress);
                        staticHolder.mProgressView.setVisibility(View.VISIBLE);
                        // TODO: animate in progress view (pin)
                    }
                    staticHolder.mCalculatingProgressForPosition = -1;
                }
            };
            holder.mProgressTask.start();
            holder.mProgressView.setVisibility(View.INVISIBLE);
        } else {
            holder.mProgressView.setProgress(mTranslationProgress[position]);
            holder.mProgressView.setVisibility(View.VISIBLE);
        }

        // TODO: finish rendering project icon
        holder.mInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mInfoClickListener != null) {
                    mInfoClickListener.onClick(getItem(position).getId());
                }
            }
        });

        // check for draft language
        boolean enableSettings = false;
        if(library != null) {
            String targetProjectID = targetTranslation.getProjectId();
            String targetLanguageID = targetTranslation.getTargetLanguageId();
            TargetTranslation[] targetTranslations = AppContext.getTranslator().getTargetTranslations();
            for(TargetTranslation t:targetTranslations) {
                Logger.i(this.getClass().toString(), "TargetTranslation:" + t.getId());
                String projectID = t.getProjectId();
                String languageID = t.getTargetLanguageId();
                if(targetProjectID.equals(projectID) && targetLanguageID.equals(languageID)) {
                    SourceTranslation[] sourceTranslations = library.getDraftTranslations(t.getProjectId());
                    for (SourceTranslation s : sourceTranslations) {
                        String draftLanguageID = s.sourceLanguageSlug;
                        if(targetLanguageID.equals(draftLanguageID)) {
                            Logger.i(this.getClass().toString(), ".....SourceTranslation:" + s.getId());
                            enableSettings = true;
                            holder.mDraftTranslation = s;
                        }
                    }
                }
            }
        }

        holder.mSettingsButton.setVisibility(enableSettings ? View.VISIBLE : View.GONE);
        holder.mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.i(this.getClass().toString(),"settings button click, draft translation: " + holder.mDraftTranslation.getId());
            }
        });

        return v;
    }

    public void changeData(TargetTranslation[] targetTranslations) {
        mTranslations = targetTranslations;
        mTranslationProgress = new int[targetTranslations.length];
        mTranslationProgressCalculated = new boolean[targetTranslations.length];
        // TODO: scheduele calcualtions
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
        public ImageButton mSettingsButton;
        public ThreadableUI mProgressTask;
        public int mCalculatingProgressForPosition = -1;
        public SourceTranslation mDraftTranslation;

        public ViewHolder(View view, Context context) {
            mIconView = (ImageView) view.findViewById(R.id.projectIcon);
            mTitleView = (TextView) view.findViewById(R.id.projectTitle);
            mLanguageView = (TextView) view.findViewById(R.id.targetLanguage);
            mProgressView = (ProgressPieView) view.findViewById(R.id.translationProgress);
            mProgressView.setMax(100);
            mInfoButton = (ImageButton) view.findViewById(R.id.infoButton);
            mSettingsButton = (ImageButton) view.findViewById(R.id.settings);
            ViewUtil.tintViewDrawable(mInfoButton, context.getResources().getColor(R.color.dark_disabled_text));
            view.setTag(this);
        }
    }
}