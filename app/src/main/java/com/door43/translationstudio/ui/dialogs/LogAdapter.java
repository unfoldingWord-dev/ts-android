package com.door43.translationstudio.ui.dialogs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.unfoldingword.tools.logger.LogEntry;
import org.unfoldingword.tools.logger.LogLevel;
import com.door43.translationstudio.R;
import com.door43.widget.ViewUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 2/26/2015.
 */
public class LogAdapter extends BaseAdapter {

    private List<LogEntry> mLogs = new ArrayList<>();

    /**
     * Adds an item to the adapter
     * @param logs
     */
    public void setItems(List<LogEntry> logs) {
        mLogs = logs;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mLogs.size();
    }

    @Override
    public LogEntry getItem(int i) {
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

        LogEntry logEntry = getItem(i);

        if(view == null) {
            v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fragment_log_item, null);
            holder.icon = (ImageView)v.findViewById(R.id.log_icon);
            holder.title = (TextView)v.findViewById(R.id.log_title);
            holder.namespace = (TextView)v.findViewById(R.id.log_namespace);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.title.setText(logEntry.message);
        holder.namespace.setText(logEntry.classPath);
        if(logEntry.level == LogLevel.Error) {
            holder.icon.setBackgroundResource(R.drawable.ic_error_black_18dp);
            ViewUtil.tintViewDrawable(holder.icon, viewGroup.getContext().getResources().getColor(R.color.danger));
        } else if(logEntry.level == LogLevel.Warning) {
            holder.icon.setBackgroundResource(R.drawable.ic_warning_black_18dp);
            ViewUtil.tintViewDrawable(holder.icon, viewGroup.getContext().getResources().getColor(R.color.warning));
        } else if(logEntry.level == LogLevel.Info) {
            holder.icon.setBackgroundResource(R.drawable.ic_info_black_18dp);
            ViewUtil.tintViewDrawable(holder.icon, viewGroup.getContext().getResources().getColor(R.color.info));
        }

        return v;
    }

    private static class ViewHolder {
        public ImageView icon;
        public TextView title;
        public TextView namespace;
    }
}
