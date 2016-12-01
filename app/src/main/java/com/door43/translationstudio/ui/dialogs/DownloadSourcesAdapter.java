package com.door43.translationstudio.ui.dialogs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
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
import com.door43.translationstudio.tasks.GetAvailableSourcesTask;
import com.door43.widget.ViewUtil;

import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.tools.logger.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by blm on 12/1/16.
 */

public class DownloadSourcesAdapter  extends BaseAdapter {

    public static final String TAG = DownloadSourcesAdapter.class.getSimpleName();
    private final Context mContext;
    private Map<String, ViewItem> mData = new HashMap<>();
    private List<String> mSelected = new ArrayList<>();
    private List<ViewItem> mSortedData = new ArrayList<>();
    private List<Translation> availableSources;
    private Map<String,Set<Integer>> byLanguage;
    private Map<String,Set<Integer>> otBooks;
    private Map<String,Set<Integer>> ntBooks;
    private Map<String,Set<Integer>> other;

    private FilterType filterOn = FilterType.byLanguage;

    public DownloadSourcesAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mSortedData.size();
    }

    /**
     * Adds an item to the list
     * If the item id matches an existing item it will be skipped
a     * @param item
     */
    public void setData(GetAvailableSourcesTask task) {
        if(task != null) {
            availableSources = task.getSources();
            Logger.i(TAG, "Found " + availableSources.size() + " sources");

            byLanguage = task.getByLanguage();
            otBooks = task.getOtBooks();
            ntBooks = task.getNtBooks();
            other = task.getOther();

            sort();
        }
    }

    public void setFilterOn(FilterType filterOn) {
        this.filterOn = filterOn;
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

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    /**
     * Resorts the data
     */
    public void sort() {
        switch (filterOn) {
            case byLanguage:
            default:
                mData = new HashMap<>();
                Map<String,Set<Integer>> sortSet = byLanguage;
                for (String key : sortSet.keySet()) {
                    Set<Integer> items = sortSet.get(key);
                    if((items != null)  && (items.size() > 0)) {
                        int index = items.iterator().next();
                        Translation sourceTranslation = availableSources.get(index);
                        String title = sourceTranslation.language.name + "  (" + sourceTranslation.language.slug + ")";
                        ViewItem newItem = new ViewItem(title, sourceTranslation, false, false);
                        mSortedData.add(newItem);
                    }
                }
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
        final ViewItem item = getItem(position);

        if(convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_source_item, null);
            holder = new ViewHolder();
            holder.titleView = (TextView)v.findViewById(R.id.title);
            holder.checkboxView = (ImageView) v.findViewById(R.id.checkBoxView);

            v.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.titleView.setText(item.title);

        if (item.selected) {
            holder.checkboxView.setBackgroundResource(R.drawable.ic_check_box_black_24dp);
            ViewUtil.tintViewDrawable(holder.checkboxView, parent.getContext().getResources().getColor(R.color.accent));
            // display checked
        } else {
            holder.checkboxView.setBackgroundResource(R.drawable.ic_check_box_outline_blank_black_24dp);
            ViewUtil.tintViewDrawable(holder.checkboxView, parent.getContext().getResources().getColor(R.color.dark_primary_text));
            // display unchecked
        }

        return v;
    }

    public void select(int position) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.selected = true;
            mSelected.add(item.containerSlug);
        }
    }

    public void deselect(int position) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.selected = false;
            mSelected.remove(item.containerSlug);
        }
    }

    /**
     * marks an item as downloaded
     * @param position
     */
    public void markItemDownloaded(int position) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.downloaded = true;
            item.error = false;
        }
    }

    /**
     * marks an item as error
     * @param position
     */
    public void markItemError(int position) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.error = true;
        }
    }

    public static class ViewHolder {
        public TextView titleView;
        public ImageView checkboxView;
        public Object currentTaskId;
        public int currentPosition;
    }

    public static class ViewItem {
        public final CharSequence title;
        public final String containerSlug;
        public final Translation sourceTranslation;
        public boolean selected;
        public boolean downloaded;
        public boolean error;

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
            this.error = false;
        }
    }

    /**
     * enum that keeps track of current state of USFM import
     */
    public enum FilterType {
        byLanguage(0),
        oldTestament(1),
        newTestament(2),
        other(3);

        private int _value;

        FilterType(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static FilterType fromInt(int i) {
            for (FilterType b : FilterType.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }
}
