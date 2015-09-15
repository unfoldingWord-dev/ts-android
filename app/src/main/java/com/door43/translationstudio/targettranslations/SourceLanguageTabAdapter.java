package com.door43.translationstudio.targettranslations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.widget.ViewUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by joel on 9/15/2015.
 */
public class SourceLanguageTabAdapter extends BaseAdapter {
    public static final int TYPE_ITEM = 0;
    public static final int TYPE_SEPARATOR = 1;
    private final Context mContext;
    private List<ViewItem> mData = new ArrayList<>();
    private List<ViewItem> mSortedData = new ArrayList<>();
    private TreeSet<Integer> mSectionHeader = new TreeSet<>();

    public SourceLanguageTabAdapter(Context context) {
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
        for(ViewItem existingItem:mData) {
            if(existingItem.id.equals(item.id)) {
                return;
            }
        }
        mData.add(item);
    }

    @Override
    public ViewItem getItem(int position) {
        return mSortedData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mSectionHeader.contains(position) ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /**
     * Resorts the data
     */
    public void sort() {
        mSortedData = new ArrayList<>();
        mSectionHeader = new TreeSet<>();
        List<ViewItem> selectedItems = new ArrayList<>();
        List<ViewItem> availableItems = new ArrayList<>();

        for(ViewItem item:mData) {
            if (item.selected) {
                selectedItems.add(item);
            } else {
                availableItems.add(item);
            }
        }

        // TODO: sort lists

        // build list
        ViewItem selectedHeader = new SourceLanguageTabAdapter.ViewItem(mContext.getResources().getString(R.string.selected), null, false);
        mSortedData.add(selectedHeader);
        mSectionHeader.add(mSortedData.size() - 1);
        for(ViewItem item:selectedItems) {
            mSortedData.add(item);
        }
        ViewItem availableHeader = new SourceLanguageTabAdapter.ViewItem(mContext.getResources().getString(R.string.available), null, false);
        mSortedData.add(availableHeader);
        mSectionHeader.add(mSortedData.size() - 1);
        for(ViewItem item:availableItems) {
            mSortedData.add(item);
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
                case TYPE_ITEM:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_source_translation_list_item, null);
                    holder = new ViewHolder();
                    holder.titleView = (TextView)v.findViewById(R.id.title);
                    holder.checkboxView = (ImageView) v.findViewById(R.id.checkBoxView);
                    break;
            }
            v.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // TODO: populate data
        holder.titleView.setText(getItem(position).title);
        if(rowType == TYPE_ITEM) {
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

    public static class ViewHolder {
        public TextView titleView;
        public ImageView checkboxView;
    }

    public static class ViewItem {
        public final String title;
        public final String id;
        public Boolean selected;

        public ViewItem(String title, String id, Boolean selected) {
            this.title = title;
            this.id = id;
            this.selected = selected;
        }
    }
}
