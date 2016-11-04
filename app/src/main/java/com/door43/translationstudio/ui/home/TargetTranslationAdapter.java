package com.door43.translationstudio.ui.home;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.tasks.CalculateTargetTranslationProgressTask;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.Project;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import com.door43.translationstudio.tasks.TranslationProgressTask;
import com.door43.widget.ViewUtil;
import com.filippudak.ProgressPieView.ProgressPieView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unfoldingword.resourcecontainer.Resource;

/**
 * Created by joel on 9/3/2015.
 */
public class TargetTranslationAdapter extends BaseAdapter implements ManagedTask.OnFinishedListener {
    private final Context mContext;
    private TargetTranslation[] mTranslations;
    private OnInfoClickListener mInfoClickListener = null;
    private Map<String, Integer> mTranslationProgress = new HashMap<>();
    private List<String> mTranslationProgressCalculated = new ArrayList<>();
    private List<ViewHolder> holders = new ArrayList<>();

    public TargetTranslationAdapter(Context context) {
        mContext = context;
        mTranslations = new TargetTranslation[0];
    }

    /**
     * Adds a listener to be called when the info button is called
     * @param listener the listener to be added
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
            holders.add(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        final TargetTranslation targetTranslation = getItem(position);
        final Door43Client library = App.getLibrary();
        holder.currentTargetTranslation = targetTranslation;
        holder.mProgressView.setVisibility(View.INVISIBLE);

        // calculate translation progress
        if(!mTranslationProgressCalculated.contains(targetTranslation.getId())) {
            String taskId = TranslationProgressTask.TASK_ID + targetTranslation.getId();
            TranslationProgressTask progressTask = (TranslationProgressTask) TaskManager.getTask(taskId);
            if(progressTask != null) {
                // attach listener
                progressTask.removeAllOnFinishedListener();
                progressTask.addOnFinishedListener(this);
            } else {
                progressTask = new TranslationProgressTask(targetTranslation);
                progressTask.addOnFinishedListener(this);
                TaskManager.addTask(progressTask, TranslationProgressTask.TASK_ID + targetTranslation.getId());
                TaskManager.groupTask(progressTask, "calc-translation-progress");
            }
        } else {
            holder.setProgress(mTranslationProgress.get(targetTranslation.getId()));
        }

        // render view
        Project project = library.index().getProject(App.getDeviceLanguageCode(), targetTranslation.getProjectId(), true);
        if(project != null) {
            if(!targetTranslation.getResourceSlug().equals(Resource.REGULAR_SLUG) && !targetTranslation.getResourceSlug().equals("obs")) {
                // display the resource type if not a regular resource e.g. this is for a gateway language
                holder.mTitleView.setText(project.name + " (" + targetTranslation.getResourceSlug() + ")");
            } else {
                holder.mTitleView.setText(project.name);
            }
        } else {
            holder.mTitleView.setText(targetTranslation.getProjectId());
        }
        holder.mLanguageView.setText(targetTranslation.getTargetLanguageName());

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
        mTranslationProgress = new HashMap<>();
        mTranslationProgressCalculated = new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public void onTaskFinished(ManagedTask task) {
        TaskManager.clearTask(task);

        if(task instanceof TranslationProgressTask) {
            // save progress
            double progressLong = ((TranslationProgressTask) task).getProgress();
            final int progress = Math.round((float)progressLong * 100);
            final TargetTranslation targetTranslation = ((TranslationProgressTask) task).targetTranslation;
            mTranslationProgress.put(targetTranslation.getId(), progress);
            mTranslationProgressCalculated.add(targetTranslation.getId());

            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    public interface OnInfoClickListener {
        void onClick(String targetTranslationId);
    }

    public class ViewHolder {
        public ImageView mIconView;
        public TextView mTitleView;
        public TextView mLanguageView;
        public ProgressPieView mProgressView;
        public ImageButton mInfoButton;
        public TargetTranslation currentTargetTranslation;

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

        public void setProgress(int progress) {
            mProgressView.setProgress(progress);
            mProgressView.setVisibility(View.VISIBLE);
        }
    }
}