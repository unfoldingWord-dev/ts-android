package com.door43.translationstudio.newui.translate;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.newui.library.ServerLibraryDetailFragment;
import com.door43.translationstudio.tasks.DownloadSourceLanguageTask;
import com.door43.widget.ViewUtil;

import org.unfoldingword.tools.logger.Logger;
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
    public static final String TAG = ChooseSourceTranslationAdapter.class.getSimpleName();
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
                promptToDownloadSourceLangauge(item);
                break;
        }
    }

    /**
     * make sure the user is aware that download will use the internet
     * @param item
     */
    private void promptToDownloadSourceLangauge(final ViewItem item) {
        String format = mContext.getResources().getString(R.string.download_source_language);
        String message = String.format(format, item.title);
        new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                .setTitle(R.string.title_download_source_language)
                .setMessage(message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadSourceLanguage(item);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    /**
     * initiate download
     * @param item
     */
    private void downloadSourceLanguage(ViewItem item) {
        DownloadSourceLanguageTask task = new DownloadSourceLanguageTask(item.sourceTranslation.projectSlug, item.sourceTranslation.sourceLanguageSlug);
        task.addOnFinishedListener(this);
        TaskManager.addTask(task, item.sourceTranslation.projectSlug + "-" + item.id);
        TaskManager.groupTask(task, ServerLibraryDetailFragment.DOWNLOAD_SOURCE_LANGUAGE_TASK_GROUP);
    }

    /**
     * called when download is finished
     * @param task
     */
    public void onTaskFinished(ManagedTask task) {
        DownloadSourceLanguageTask downloadTask = (DownloadSourceLanguageTask) task;
        Library library = App.getLibrary();
        if(downloadTask.isFinished() && downloadTask.getSuccess()) {
            String sourceLang = downloadTask.getSourceLanguageId();
            String projectId = downloadTask.getProjectId();
            if((sourceLang != null) && (projectId != null)) {
                // find entry that was changed
                for (int i = 0; i < getCount(); i++) {
                    ChooseSourceTranslationAdapter.ViewItem item = getItem(i);
                    if (item != null) {
                        if((item.sourceTranslation != null) && sourceLang.equals(item.sourceTranslation.sourceLanguageSlug) && projectId.equals(item.sourceTranslation.projectSlug)) {
                            Resource resource = library.getResource(item.sourceTranslation);
                            if (resource != null) {
                                item.downloaded = resource.isDownloaded();
                            } else {
                                Logger.e(TAG, "Failed to get resource for " + item.sourceTranslation.getId());
                            }
                            notifyDataSetChanged();
                            break;
                        }
                    } else {
                        Logger.e(TAG, "Failed to get SourceTranslation for " + item.id);
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
        ViewItem selectedHeader = new ChooseSourceTranslationAdapter.ViewItem(mContext.getResources().getString(R.string.selected), null, false, false);
        mSortedData.add(selectedHeader);
        mSectionHeader.add(mSortedData.size() - 1);
        for(String id:mSelected) {
            mSortedData.add(mData.get(id));
        }
        ViewItem availableHeader = new ChooseSourceTranslationAdapter.ViewItem(mContext.getResources().getString(R.string.available), null, false, false);
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
        public final SourceTranslation sourceTranslation;
        public boolean selected;
        public boolean downloaded;

        public ViewItem(String title, SourceTranslation sourceTranslation, boolean selected, boolean downloaded) {
            this.title = title;
            this.selected = selected;
            this.sourceTranslation = sourceTranslation;
            if(sourceTranslation != null) {
                this.id = sourceTranslation.getId();
            } else {
                this.id = null;
            }
            this.downloaded = downloaded;
        }
    }
}
