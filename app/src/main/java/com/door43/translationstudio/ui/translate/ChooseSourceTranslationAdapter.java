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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ContainerCache;
import com.door43.widget.ViewUtil;

import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Handles the list of source translations that can be chosen for viewing along side
 * a target translation.
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
    private String mSearchText;

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
     * @param selectable - true if this is to be in the selectable section
     */
    public void addItem(final ViewItem item, boolean selectable) {
        if(!mData.containsKey(item.containerSlug)) {
            mData.put(item.containerSlug, item);
            if(item.selected && item.downloaded) {
                mSelected.add(item.containerSlug);
            } else if(!item.downloaded) {
                mDownloadable.add(item.containerSlug);
            } else {
                mAvailable.add(item.containerSlug);
            }

            if (selectable) { // see if there are updates available to download
                Log.i(TAG, "Checking for updates on " + item.containerSlug);
                boolean hasUpdates = false;
                try {
                    ResourceContainer container = ContainerCache.cache(App.getLibrary(), item.containerSlug);
                    int lastModified = App.getLibrary().getResourceContainerLastModified(container.language.slug, container.project.slug, container.resource.slug);
                    hasUpdates = (lastModified > container.modifiedAt);
                    Log.i(TAG, "Checking for updates on " + item.containerSlug + " finished, needs updates: " +hasUpdates);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                item.hasUpdates = hasUpdates;
                item.checkedUpdates = true;
            }
        }
    }

    /**
     * will check for updates for language if needed
     * @param item
     */
    public void checkForItemUpdates(final ViewItem item) {
        if(!item.checkedUpdates && item.downloaded)
        {
            ManagedTask task = new ManagedTask() {
                @Override
                public void start() {
                    Log.i(TAG, "Checking for updates on " + item.containerSlug);
                    try {
                        if (interrupted()) return;
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
                public void onTaskFinished(final ManagedTask task) {
                    TaskManager.clearTask(task);
                    boolean hasUpdates = false;
                    item.currentTaskId = null;
                    if(task.isCanceled()) {
                        Log.i(TAG, "Checking for updates on " + item.containerSlug + " cancelled");
                        return;
                    }
                    if (task.getResult() != null) hasUpdates = (boolean) task.getResult();
                    item.hasUpdates = hasUpdates;
                    Log.i(TAG, "Checking for updates on " + item.containerSlug + " finished, needs updates: " + hasUpdates);
                    item.checkedUpdates = true;

                    Handler hand = new Handler(Looper.getMainLooper());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                }
            });
            item.currentTaskId = TaskManager.addTask(task);
        }
    }

    @Override
    public ViewItem getItem(int position) {
        if(position >= 0 && position < mSortedData.size()) {
            return mSortedData.get(position);
        } else {
            return null;
        }
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
     * applies search string and resorts list
     */
    public void applySearch(String search) {
        mSearchText = search;
        sort();
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

        List<ViewItem> section = getViewItems(mSelected, null); // do not restrict selections by search string
        mSortedData.addAll(section);

        ViewItem availableHeader = new ChooseSourceTranslationAdapter.ViewItem(mContext.getResources().getString(R.string.available), null, false, false);
        mSortedData.add(availableHeader);
        mSectionHeader.add(mSortedData.size() - 1);

        section = getViewItems(mAvailable, mSearchText);
        mSortedData.addAll(section);

        ViewItem downloadableHeader = new ChooseSourceTranslationAdapter.ViewItem(getDownloadableText(), null, false, false);
        mSortedData.add(downloadableHeader);
        mSectionHeader.add(mSortedData.size() - 1);

        section = getViewItems(mDownloadable, mSearchText);
        mSortedData.addAll(section);

        notifyDataSetChanged();
    }

    /**
     * get ViewItems from data list and apply any search filters
     * @param data
     * @return
     */
    private List<ViewItem> getViewItems(List<String> data, String searchText) {
        List<ViewItem> section = new ArrayList<>();
        for(String id:data) {
            section.add(mData.get(id));
        }

        // sort by language code
        Collections.sort(section, new Comparator<ViewItem>() { // do numeric sort
            @Override
            public int compare(ViewItem lhs, ViewItem rhs) {
                return lhs.sourceTranslation.language.slug.compareTo(rhs.sourceTranslation.language.slug);
            }
        });

        if((searchText != null) && (!searchText.isEmpty())) {
            List<ViewItem> filtered = new ArrayList<>();

            // filter by language code
            for (ViewItem item : section) {
                String code = item.sourceTranslation.language.slug;
                if(code.length() >= searchText.length()) {
                    if (code.substring(0, searchText.length()).equalsIgnoreCase(searchText)) {
                        filtered.add(item);
                    }
                }
            }

            // filter by language name
            for (ViewItem item : section) {
                String name = item.sourceTranslation.language.name;
                if(name.length() >= searchText.length()) {
                    if (name.substring(0, searchText.length()).equalsIgnoreCase(searchText)) {
                        if (!filtered.contains(item)) { // prevent duplicates
                            filtered.add(item);
                        }
                    }
                }
            }

            // filter by resource name
            for (ViewItem item : section) {
                String[] parts = item.sourceTranslation.resource.name.split("-");
                for (String part : parts) { // handle sections separately
                    String name = part.trim();
                    if(name.length() >= searchText.length()) {
                        if (name.substring(0, searchText.length()).equalsIgnoreCase(searchText)) {
                            if (!filtered.contains(item)) { // prevent duplicates
                                filtered.add(item);
                            }
                        }
                    }
                }
            }
            section = filtered;
        }
        return section;
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
        holder.currentPosition = position;

        holder.titleView.setText(item.title);
        if( (rowType == TYPE_ITEM_NEED_DOWNLOAD) || (rowType == TYPE_ITEM_SELECTABLE_UPDATABLE)) {
            if(holder.downloadView != null) {
                if (rowType == TYPE_ITEM_NEED_DOWNLOAD) {
                    holder.downloadView.setBackgroundResource(R.drawable.ic_file_download_black_24dp);
                } else {
                    holder.downloadView.setBackgroundResource(R.drawable.ic_refresh_black_24dp);
                }
                ViewUtil.tintViewDrawable(holder.downloadView, parent.getContext().getResources().getColor(R.color.accent));
            }
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
            select(position); // auto select download item
        }
        sort();
    }

    public static class ViewHolder {
        public TextView titleView;
        public ImageView checkboxView;
        public ImageView downloadView;
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
        public Object currentTaskId = null;

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
