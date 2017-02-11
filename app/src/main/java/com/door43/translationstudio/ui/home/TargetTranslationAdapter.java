package com.door43.translationstudio.ui.home;


import android.content.Context;
import android.content.Intent;
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
import com.door43.translationstudio.core.BibleCodes;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unfoldingword.resourcecontainer.Resource;

/**
 * Created by joel on 9/3/2015.
 */
public class TargetTranslationAdapter extends BaseAdapter implements ManagedTask.OnFinishedListener {
    private final Context mContext;
    private List<TargetTranslation> mTranslations;
    private OnInfoClickListener mInfoClickListener = null;
    private Map<String, Integer> mTranslationProgress = new HashMap<>();
    private List<String> mTranslationProgressCalculated = new ArrayList<>();
    private List<ViewHolder> holders = new ArrayList<>();
    private SortProjectColumnType mSortProjectColumn = SortProjectColumnType.bibleOrder;
    private SortByColumnType mSortByColumn = SortByColumnType.projectThenLanguage;;

    private static List<String> bookList = Arrays.asList(BibleCodes.getBibleBooks());


    public TargetTranslationAdapter(Context context) {
        mContext = context;
        mTranslations = new ArrayList<>();
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
            return mTranslations.size();
        } else {
            return 0;
        }
    }

    public void sort() {
        sort(mSortByColumn, mSortProjectColumn);
    }

    public void sort(final SortByColumnType sortByColumn, final SortProjectColumnType sortProjectColumn) {
        mSortByColumn = sortByColumn;
        mSortProjectColumn = sortProjectColumn;
        Collections.sort(mTranslations, new Comparator<TargetTranslation>() {
            @Override
            public int compare(TargetTranslation lhs, TargetTranslation rhs) {
                int compare;
                switch (sortByColumn) {
                    case projectThenLanguage:
                        compare = compareProject(lhs, rhs, sortProjectColumn);
                        if(compare == 0) {
                            compare = lhs.getTargetLanguageName().compareToIgnoreCase(rhs.getTargetLanguageName());
                        }
                        return compare;
                    case languageThenProject:
                        compare = lhs.getTargetLanguageName().compareToIgnoreCase(rhs.getTargetLanguageName());
                        if(compare == 0) {
                            compare = compareProject(lhs, rhs, sortProjectColumn);
                        }
                        return compare;
                    case progressThenProject:
                    default:
                        compare = getProgress(rhs) - getProgress(lhs);
                        if(compare == 0) {
                            compare = compareProject(lhs, rhs, sortProjectColumn);
                        }
                        return compare;
                }
            }
        });

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    /**
     * compare projects (for use in sorting)
     * @param lhs
     * @param rhs
     * @return
     */
    private int compareProject(TargetTranslation lhs, TargetTranslation rhs, SortProjectColumnType sortProjectColumn) {
        if(sortProjectColumn == SortProjectColumnType.bibleOrder) {
            int lhsIndex = bookList.indexOf(lhs.getProjectId());
            int rhsIndex = bookList.indexOf(rhs.getProjectId());
            if((lhsIndex == rhsIndex) && (lhsIndex < 0)) { // if not bible books, then compare by name
                return getProjectName(lhs).compareToIgnoreCase(getProjectName(rhs));
            }
            return lhsIndex - rhsIndex;
        }

        // compare project names
        return getProjectName(lhs).compareToIgnoreCase(getProjectName(rhs));
    }

    @Override
    public TargetTranslation getItem(int position) {
        return mTranslations.get(position);
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
            holder.setProgress(getProgress(targetTranslation));
        }

        // render view
        holder.mTitleView.setText(getProjectName(targetTranslation));
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

    /**
     * get the project name
     * @param targetTranslation
     * @return
     */
    private String getProjectName(TargetTranslation targetTranslation) {
        String projectName = "";
        Project project = App.getLibrary().index().getProject(App.getDeviceLanguageCode(), targetTranslation.getProjectId(), true);
        if(project != null) {
            if(!targetTranslation.getResourceSlug().equals(Resource.REGULAR_SLUG) && !targetTranslation.getResourceSlug().equals("obs")) {
                // display the resource type if not a regular resource e.g. this is for a gateway language
                projectName = project.name + " (" + targetTranslation.getResourceSlug() + ")";
            } else {
                projectName = project.name;
            }
        } else {
            projectName = targetTranslation.getProjectId();
        }
        return projectName;
    }

    /**
     * get calculated project
     * @param targetTranslation
     * @return
     */
    private Integer getProgress(TargetTranslation targetTranslation) {
        if(mTranslationProgressCalculated.contains(targetTranslation.getId())) {
            Integer value =  mTranslationProgress.get(targetTranslation.getId());
            if(value != null) return value;
        }
        return -1;
    }

    public void changeData(TargetTranslation[] targetTranslations) {
        mTranslations = Arrays.asList(targetTranslations);
        mTranslationProgress = new HashMap<>();
        mTranslationProgressCalculated = new ArrayList<>();
        sort();
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
                    sort();
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


    /**
     * enum that keeps track of current state of USFM import
     */
    public enum SortByColumnType {
        projectThenLanguage(0),
        languageThenProject(1),
        progressThenProject(2);

        private int _value;

        SortByColumnType(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static SortByColumnType fromString(String value, SortByColumnType defaultValue ) {
            Integer returnValue = null;
            if(value != null) {
                try {
                    returnValue = Integer.valueOf(value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(returnValue == null) {
                return defaultValue;
            }

            return fromInt(returnValue);
        }

        public static SortByColumnType fromInt(int i) {
            for (SortByColumnType b : SortByColumnType.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }

    /**
     * enum that keeps track of current state of USFM import
     */
    public enum SortProjectColumnType {
        bibleOrder(0),
        alphabetical(1);

        private int _value;

        SortProjectColumnType(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static SortProjectColumnType fromString(String value, SortProjectColumnType defaultValue ) {
            Integer returnValue = null;
            if(value != null) {
                try {
                    returnValue = Integer.valueOf(value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(returnValue == null) {
                return defaultValue;
            }

            return fromInt(returnValue);
        }

        public static SortProjectColumnType fromInt(int i) {
            for (SortProjectColumnType b : SortProjectColumnType.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }
}