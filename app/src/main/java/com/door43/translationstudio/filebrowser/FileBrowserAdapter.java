package com.door43.translationstudio.filebrowser;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the rendering of the file browser activity
 */
public class FileBrowserAdapter extends BaseAdapter {

    private final Context mContext;
    private List<FileItem> mFiles = new ArrayList<>();

    /**
     * Creates a new instance of the file browser mAdapter
     * @param context
     */
    public FileBrowserAdapter(Context context) {
        mContext = context;
    }

    public void loadFiles(List<FileItem> files) {
        mFiles = files;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mFiles.size();
    }

    @Override
    public FileItem getItem(int i) {
        return mFiles.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = view;
        ViewHolder holder = new ViewHolder();

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(android.R.layout.select_dialog_item, null);
            // layout
            holder.text = (TextView)v.findViewById(android.R.id.text1);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        // title
        holder.text.setText(getItem(i).toString());

        // icon
        holder.text.setCompoundDrawablesWithIntrinsicBounds(getItem(i).getIcon(), 0, 0, 0);
        int dp5 = (int) (5 * mContext.getResources().getDisplayMetrics().density + 0.5f);
        holder.text.setCompoundDrawablePadding(dp5);

        return v;
    }

    private static class ViewHolder {
        public TextView text;
    }
}
