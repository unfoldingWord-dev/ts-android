package com.door43.translationstudio.newui.home;


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
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.tasks.CalculateTargetTranslationProgressTask;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;
import com.door43.widget.ViewUtil;
import com.filippudak.ProgressPieView.ProgressPieView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            holders.add(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        final TargetTranslation targetTranslation = getItem(position);
        final Library library = App.getLibrary();
        holder.currentTargetTranslation = targetTranslation;
        holder.mProgressView.setVisibility(View.INVISIBLE);

        // calculate translation progress
        if(!mTranslationProgressCalculated.contains(targetTranslation.getId())) {
            String taskId = CalculateTargetTranslationProgressTask.TASK_ID + targetTranslation.getId();
            CalculateTargetTranslationProgressTask calcTask = (CalculateTargetTranslationProgressTask) TaskManager.getTask(taskId);
            if(calcTask != null) {
                // attach listener
                calcTask.removeAllOnFinishedListener();
                calcTask.addOnFinishedListener(this);
            } else {
                calcTask = new CalculateTargetTranslationProgressTask(library, targetTranslation);
                calcTask.addOnFinishedListener(this);
                TaskManager.addTask(calcTask, CalculateTargetTranslationProgressTask.TASK_ID + targetTranslation.getId());
                TaskManager.groupTask(calcTask, "calc-translation-progress");
            }
        } else {
            holder.setProgress(mTranslationProgress.get(targetTranslation.getId()));
        }

        // render view
        Project project = library.getProject(targetTranslation.getProjectId(), Locale.getDefault().getLanguage());
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
        // save progress
        final int progress = ((CalculateTargetTranslationProgressTask)task).translationProgress;
        final TargetTranslation targetTranslation = ((CalculateTargetTranslationProgressTask)task).targetTranslation;
        mTranslationProgress.put(targetTranslation.getId(), progress);
        mTranslationProgressCalculated.add(targetTranslation.getId());

        // update view holders
        for(final ViewHolder holder:holders) {
            if(holder.currentTargetTranslation.getId().endsWith(targetTranslation.getId())) {
                Handler hand = new Handler(Looper.getMainLooper());
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        holder.setProgress(progress);
                    }
                });
                break;
            }
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