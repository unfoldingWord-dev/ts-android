package com.door43.translationstudio.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 2/26/2015.
 */
public class ErrorLogAdapter extends BaseAdapter {

    private final Context mContext;
    private List<Logger.Entry> mLogs = new ArrayList<>();

    /**
     * Adds an item to the adapter
     * @param logs
     */
    public void setItems(List<Logger.Entry> logs) {
        mLogs = logs;
        notifyDataSetChanged();
    }

    public ErrorLogAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mLogs.size();
    }

    @Override
    public Logger.Entry getItem(int i) {
        return mLogs.get(i);
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
            v = inflater.inflate(R.layout.fragment_log_item, null);
            holder.levelText = (TextView)v.findViewById(R.id.logLevelTextView);
            holder.titleText = (TextView)v.findViewById(R.id.logTitleTextView);
            holder.classText = (TextView)v.findViewById(R.id.logClassTextView);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.titleText.setText(getItem(i).message);
        holder.classText.setText(getItem(i).classPath);
        holder.levelText.setText(getItem(i).level.getLabel());
        switch (getItem(i).level) {
            case Error:
                holder.levelText.setBackgroundColor(mContext.getResources().getColor(R.color.red));
                holder.levelText.setTextColor(mContext.getResources().getColor(R.color.white));
                break;
            case Warning:
                holder.levelText.setBackgroundColor(mContext.getResources().getColor(R.color.yellow));
                holder.levelText.setTextColor(mContext.getResources().getColor(R.color.dark_gray));
                break;
            case Info:
            default:
                holder.levelText.setBackgroundColor(mContext.getResources().getColor(R.color.lighter_blue));
                holder.levelText.setTextColor(mContext.getResources().getColor(R.color.accent));
        }

        return v;
    }

    private static class ViewHolder {
        public TextView levelText;
        public TextView titleText;
        public TextView classText;
    }
}
