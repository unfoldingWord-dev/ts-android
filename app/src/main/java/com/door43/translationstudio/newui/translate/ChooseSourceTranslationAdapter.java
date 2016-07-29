package com.door43.translationstudio.newui.translate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.newui.library.ServerLibraryDetailFragment;
import com.door43.translationstudio.tasks.DownloadSourceLanguageTask;
import com.door43.widget.ViewUtil;

import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Created by joel on 9/15/2015.
 */
public class ChooseSourceTranslationAdapter extends BaseAdapter  implements ManagedTask.OnFinishedListener {
    public static final int TYPE_ITEM_SELECTABLE = 0;
    public static final int TYPE_SEPARATOR = 1;
    public static final int TYPE_ITEM_NEED_DOWNLOAD = 2;
    private final Context mContext;
    private Map<String, ViewItem> mData = new HashMap<>();
    private List<String> mSelected = new ArrayList<>();
    private List<String> mAvailable = new ArrayList<>();
    private List<ViewItem> mSortedData = new ArrayList<>();
    private TreeSet<Integer> mSectionHeader = new TreeSet<>();

    public ChooseSourceTranslationAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mSortedData.size();
    }

    /**
     * Adds an item to the list
     * If the item id matches an existing item it will be skipped
     * @param item
     */
    public void addItem(final ViewItem item) {
        if(!mData.containsKey(item.id)) {
            mData.put(item.id, item);
            if(item.selected) {
                mSelected.add(item.id);
            } else {
                mAvailable.add(item.id);
            }
        }
    }

    @Override
    public ViewItem getItem(int position) {
        return mSortedData.get(position);
    }

    public void doClickOnItem(int position) {
        int type = getItemViewType( position);
        ChooseSourceTranslationAdapter.ViewItem item = getItem(position);
        switch (type) {
            case TYPE_ITEM_SELECTABLE:
                if(item.selected) {
                    deselect(position);
                } else {
                    select(position);
                }
                break;

            case TYPE_ITEM_NEED_DOWNLOAD:
                DownloadSourceLanguageTask task = new DownloadSourceLanguageTask(item.projectID, item.id);
                task.addOnFinishedListener(this);
                TaskManager.addTask(task, item.projectID + "-" + item.id);
                TaskManager.groupTask(task, ServerLibraryDetailFragment.DOWNLOAD_SOURCE_LANGUAGE_TASK_GROUP);
//                task.addOnFinishedListener(item.onFinishedListener);
//                task.addOnProgressListener(item.onProgressListener);
                break;
        }
    }

    public void onTaskFinished(ManagedTask task) {
        DownloadSourceLanguageTask downloadTask = (DownloadSourceLanguageTask) task;
        if(downloadTask.isFinished() && downloadTask.getSuccess()) {
            String sourceLang = downloadTask.getSourceLanguageId();
            String projectId = downloadTask.getProjectId();

            if((sourceLang != null) && (projectId != null)) {

                // find entry that was changed
                for (int i = 0; i < getCount(); i++) {
                    ChooseSourceTranslationAdapter.ViewItem item = getItem(i);
                    if ((item != null) && sourceLang.equals(item.id) && projectId.equals(item.projectID)) {
                        item.downloaded = true;
                        notifyDataSetChanged();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public boolean isSelectableItem(int position) {
        boolean selectable = getItemViewType(position) != ChooseSourceTranslationAdapter.TYPE_SEPARATOR;
        return selectable;
    }

    @Override
    public int getItemViewType(int position) {
        int type = mSectionHeader.contains(position) ? TYPE_SEPARATOR : TYPE_ITEM_SELECTABLE;
        if(type == TYPE_ITEM_SELECTABLE) {
            ViewItem v = getItem(position);
            if(!v.downloaded) { // check if we need to download
                type = TYPE_ITEM_NEED_DOWNLOAD;
            }
        }
        return type;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    /**
     * Resorts the data
     */
    public void sort() {
        mSortedData = new ArrayList<>();
        mSectionHeader = new TreeSet<>();

        // build list
        ViewItem selectedHeader = new ChooseSourceTranslationAdapter.ViewItem(mContext.getResources().getString(R.string.selected), null, null, false, false);
        mSortedData.add(selectedHeader);
        mSectionHeader.add(mSortedData.size() - 1);
        for(String id:mSelected) {
            mSortedData.add(mData.get(id));
        }
        ViewItem availableHeader = new ChooseSourceTranslationAdapter.ViewItem(mContext.getResources().getString(R.string.available), null, null, false, false);
        mSortedData.add(availableHeader);
        mSectionHeader.add(mSortedData.size() - 1);
        for(String id:mAvailable) {
            mSortedData.add(mData.get(id));
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder = null;
        int rowType = getItemViewType(position);

        if(convertView == null) {
            switch (rowType) {
                case TYPE_SEPARATOR:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_source_translation_list_header, null);
                    holder = new ViewHolder();
                    holder.titleView = (TextView)v;
                    break;
                case TYPE_ITEM_SELECTABLE:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_source_translation_list_item, null);
                    holder = new ViewHolder();
                    holder.titleView = (TextView)v.findViewById(R.id.title);
                    holder.checkboxView = (ImageView) v.findViewById(R.id.checkBoxView);
                    break;
                case TYPE_ITEM_NEED_DOWNLOAD:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_source_translation_list_download_item, null);
                    holder = new ViewHolder();
                    holder.titleView = (TextView)v.findViewById(R.id.title);
                    holder.checkboxView = (ImageView) v.findViewById(R.id.download_resource);
                    break;
            }
            v.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.titleView.setText(getItem(position).title);
        if(rowType == TYPE_ITEM_NEED_DOWNLOAD) {
            holder.checkboxView.setBackgroundResource(R.drawable.ic_file_download_black_24dp);
            ViewUtil.tintViewDrawable(holder.checkboxView, parent.getContext().getResources().getColor(R.color.accent));
        } else if(rowType == TYPE_ITEM_SELECTABLE) {
            if (getItem(position).selected) {
                holder.checkboxView.setBackgroundResource(R.drawable.ic_check_box_black_24dp);
                ViewUtil.tintViewDrawable(holder.checkboxView, parent.getContext().getResources().getColor(R.color.accent));
                // display checked
            } else {
                holder.checkboxView.setBackgroundResource(R.drawable.ic_check_box_outline_blank_black_24dp);
                ViewUtil.tintViewDrawable(holder.checkboxView, parent.getContext().getResources().getColor(R.color.dark_primary_text));
                // display unchecked
            }
        }

        return v;
    }

    public void select(int position) {
        ViewItem item = getItem(position);
        item.selected = true;
        mSelected.remove(item.id);
        mAvailable.remove(item.id);
        mSelected.add(item.id);
    }

    public void deselect(int position) {
        ViewItem item = getItem(position);
        item.selected = false;
        mSelected.remove(item.id);
        mAvailable.remove(item.id);
        mAvailable.add(item.id);
    }

    public static class ViewHolder {
        public TextView titleView;
        public ImageView checkboxView;
    }

    public static class ViewItem {
        public final String title;
        public final String id;
        public final String projectID;
        public boolean selected;
        public boolean downloaded;

        public ViewItem(String title, String id, String projectID, boolean selected, boolean downloaded) {
            this.title = title;
            this.id = id;
            this.selected = selected;
            this.projectID = projectID;
            this.downloaded = downloaded;
        }
    }
}
