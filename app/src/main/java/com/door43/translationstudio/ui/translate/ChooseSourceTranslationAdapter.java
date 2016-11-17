package com.door43.translationstudio.ui.translate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.widget.ViewUtil;

import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
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
public class ChooseSourceTranslationAdapter extends BaseAdapter {
    public static final int TYPE_ITEM_SELECTABLE = 0;
    public static final int TYPE_SEPARATOR = 1;
    public static final int TYPE_ITEM_NEED_DOWNLOAD = 2;
    public static final int TYPE_ITEM_SELECTABLE_UPDATABLE = 3;
    public static final String TAG = ChooseSourceTranslationAdapter.class.getSimpleName();
    private final Context mContext;
    private Map<String, ViewItem> mData = new HashMap<>();
    private List<String> mSelected = new ArrayList<>();
    private List<String> mAvailable = new ArrayList<>();
    private List<String> mDownloadable = new ArrayList<>();
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
        if(!mData.containsKey(item.containerSlug)) {
            mData.put(item.containerSlug, item);
            if(item.selected  && item.downloaded) {
                mSelected.add(item.containerSlug);
            } else if(!item.downloaded) {
                mDownloadable.add(item.containerSlug);
            } else {
                mAvailable.add(item.containerSlug);
            }
        }
    }

    @Override
    public ViewItem getItem(int position) {
        return mSortedData.get(position);
    }

    /**
     * toggle selection state for item
     * @param position
     */
    public void toggleSelection(int position) {
        if(getItem(position).selected) {
            deselect(position);
        } else {
            select(position);
        }
        sort();
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
            } else if(v.hasUpdates) {
                type = TYPE_ITEM_SELECTABLE_UPDATABLE;
            }
        }
        return type;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    /**
     * Resorts the data
     */
    public void sort() {
        mSortedData = new ArrayList<>();
        mSectionHeader = new TreeSet<>();

        // build list
        ViewItem selectedHeader = new ChooseSourceTranslationAdapter.ViewItem(getSelectedText(), null, false, false);
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
        ViewItem downloadableHeader = new ChooseSourceTranslationAdapter.ViewItem(getDownloadableText(), null, false, false);
        mSortedData.add(downloadableHeader);
        mSectionHeader.add(mSortedData.size() - 1);
        for(String id:mDownloadable) {
            mSortedData.add(mData.get(id));
        }
        notifyDataSetChanged();
    }

    /**
     * create text for selected separator
     * @return
     */
    private CharSequence getSelectedText() {
        CharSequence text = mContext.getResources().getString(R.string.selected);
        SpannableStringBuilder refresh = createImageSpannable(R.drawable.ic_refresh_black_24dp);
        CharSequence warning = mContext.getResources().getString(R.string.requires_internet);
        SpannableStringBuilder wifi = createImageSpannable(R.drawable.ic_wifi_black_18dp);
        return TextUtils.concat(text, "    ", refresh, " ", warning, " ", wifi); // combine all on one line
    }

    /**
     * create text for selected separator
     * @return
     */
    private CharSequence getDownloadableText() {
        CharSequence text = mContext.getResources().getString(R.string.available_online);
        CharSequence warning = mContext.getResources().getString(R.string.requires_internet);
        SpannableStringBuilder wifi = createImageSpannable(R.drawable.ic_wifi_black_18dp);
        return TextUtils.concat(text, "    ", warning, " ", wifi); // combine all on one line
    }

    /**
     * create an image spannable
     * @param resource
     * @return
     */
    private SpannableStringBuilder createImageSpannable(int resource) {
        SpannableStringBuilder refresh = new SpannableStringBuilder(" ");
        Bitmap refreshImage = BitmapFactory.decodeResource(App.context().getResources(), resource);
        BitmapDrawable refreshBackground = new BitmapDrawable(App.context().getResources(), refreshImage);
        refreshBackground.setBounds(0, 0, refreshBackground.getMinimumWidth(), refreshBackground.getMinimumHeight());
        refresh.setSpan(new ImageSpan(refreshBackground), 0, refresh.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return refresh;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder = null;
        int rowType = getItemViewType(position);
        final ViewItem item = getItem(position);

        if(convertView == null) {
            switch (rowType) {
                case TYPE_SEPARATOR:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_source_translation_list_header, null);
                    holder = new ViewHolder();
                    holder.titleView = (TextView)v;
                    holder.titleView.setTransformationMethod(null);
                    break;
                case TYPE_ITEM_SELECTABLE:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_source_translation_list_item, null);
                    holder = new ViewHolder();
                    holder.titleView = (TextView)v.findViewById(R.id.title);
                    holder.checkboxView = (ImageView) v.findViewById(R.id.checkBoxView);
                    break;
                case TYPE_ITEM_SELECTABLE_UPDATABLE:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_source_translation_list_updatable_item, null);
                    holder = new ViewHolder();
                    holder.titleView = (TextView)v.findViewById(R.id.title);
                    holder.checkboxView = (ImageView) v.findViewById(R.id.checkBoxView);
                    holder.downloadView = (ImageView) v.findViewById(R.id.download_resource);
                    break;
                case TYPE_ITEM_NEED_DOWNLOAD:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_source_translation_list_download_item, null);
                    holder = new ViewHolder();
                    holder.titleView = (TextView)v.findViewById(R.id.title);
                    holder.downloadView = (ImageView) v.findViewById(R.id.download_resource);
                    break;
            }
            v.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // load update status
        final ViewHolder staticHolder = holder;
        ManagedTask oldTask = TaskManager.getTask(holder.currentTaskId);
        TaskManager.cancelTask(oldTask);
        TaskManager.clearTask(oldTask);
        if(!item.checkedUpdates && !item.downloaded) {
            ManagedTask task = new ManagedTask() {
                @Override
                public void start() {
                    try {
                        if(interrupted()) return;
                        ResourceContainer container = App.getLibrary().open(item.containerSlug);
                        int lastModified = App.getLibrary().getResourceContainerLastModified(container.language.slug, container.project.slug, container.resource.slug);
                        setResult(lastModified > container.modifiedAt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            task.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
                @Override
                public void onTaskFinished(ManagedTask task) {
                    TaskManager.clearTask(task);
                    boolean hasUpdates = false;
                    if(task.getResult() != null) hasUpdates = (boolean)task.getResult();
                    item.hasUpdates = hasUpdates;
                    if(!task.isCanceled() && position == staticHolder.currentPosition) {
                        Handler hand = new Handler(Looper.getMainLooper());
                        hand.post(new Runnable() {
                            @Override
                            public void run() {
                                notifyDataSetChanged();
                            }
                        });
                    }
                }
            });
        }

        holder.titleView.setText(item.title);
        if( (rowType == TYPE_ITEM_NEED_DOWNLOAD) || (rowType == TYPE_ITEM_SELECTABLE_UPDATABLE)) {
            if(rowType == TYPE_ITEM_NEED_DOWNLOAD) {
                holder.downloadView.setBackgroundResource(R.drawable.ic_file_download_black_24dp);
            } else {
                holder.downloadView.setBackgroundResource(R.drawable.ic_refresh_black_24dp);
            }
            ViewUtil.tintViewDrawable(holder.downloadView, parent.getContext().getResources().getColor(R.color.accent));
        }

        if((rowType == TYPE_ITEM_SELECTABLE) || (rowType == TYPE_ITEM_SELECTABLE_UPDATABLE)){
            if (item.selected) {
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
        mSelected.remove(item.containerSlug);
        mAvailable.remove(item.containerSlug);
        mDownloadable.remove(item.containerSlug);
        mSelected.add(item.containerSlug);
    }

    public void deselect(int position) {
        ViewItem item = getItem(position);
        item.selected = false;
        mSelected.remove(item.containerSlug);
        mAvailable.remove(item.containerSlug);
        mDownloadable.remove(item.containerSlug);
        mAvailable.add(item.containerSlug);
    }

    /**
     * Marks an item as deleted
     * @param position
     */
    public void markItemDeleted(int position) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.hasUpdates = false;
            item.downloaded = false;
            item.selected = false;
            mSelected.remove(item.containerSlug);
            mAvailable.remove(item.containerSlug);
            if(!mDownloadable.contains(item.containerSlug)) mDownloadable.add(item.containerSlug);
        }
        sort();
    }

    /**
     * marks an item as downloaded
     * @param position
     */
    public void markItemDownloaded(int position) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.hasUpdates = false;
            item.downloaded = true;
            if(item.selected) {
                select(position);
            } else {
                deselect(position);
            }
        }
        sort();
    }

    public static class ViewHolder {
        public TextView titleView;
        public ImageView checkboxView;
        public ImageView downloadView;
        public Object currentTaskId;
        public int currentPosition;
    }

    public static class ViewItem {
        public final CharSequence title;
        public final String containerSlug;
        public final Translation sourceTranslation;
        public boolean selected;
        public boolean downloaded;
        public boolean hasUpdates;
        public boolean checkedUpdates = false;

        public ViewItem(CharSequence title, Translation sourceTranslation, boolean selected, boolean downloaded) {
            this.title = title;
            this.selected = selected;
            this.sourceTranslation = sourceTranslation;
            if(sourceTranslation != null) {
                this.containerSlug = sourceTranslation.resourceContainerSlug;
            } else {
                this.containerSlug = null;
            }
            this.downloaded = downloaded;
        }
    }
}
