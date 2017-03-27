package com.door43.translationstudio.ui.devtools;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;

import java.util.ArrayList;

/**
 * The tool adapter allows you to easily create a ListView full of tools
 */
public class ToolAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<ToolItem> mTools;

    public ToolAdapter(ArrayList<ToolItem> tools, Context context) {
        mTools = tools;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mTools.size();
    }

    @Override
    public ToolItem getItem(int i) {
        return mTools.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View recycledView, ViewGroup viewGroup) {
        LinearLayout view;

        // build or reuse layout
        if(recycledView == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = (LinearLayout)inflater.inflate(R.layout.fragment_tool_item, null);
        } else {
            view = (LinearLayout)recycledView;
        }

        // title
        TextView titleText = (TextView)view.findViewById(R.id.toolTitleText);
        titleText.setText(getItem(i).getName());

        // description
        TextView descriptionText = (TextView)view.findViewById(R.id.toolDescriptionText);
        descriptionText.setText(getItem(i).getDescription());

        if(getItem(i).getDescription().isEmpty()) {
            descriptionText.setVisibility(View.GONE);
        } else {
            descriptionText.setVisibility(View.VISIBLE);
        }

        // image
        ImageView iconImage = (ImageView)view.findViewById(R.id.toolIconImageView);
        if(getItem(i).getIcon() > 0) {
            iconImage.setVisibility(View.VISIBLE);
            iconImage.setBackgroundResource(getItem(i).getIcon());
        } else {
            iconImage.setVisibility(View.GONE);
        }

        // mark tool as disabled.
        if (!getItem(i).isEnabled()) {
            titleText.setTextColor(mContext.getResources().getColor(R.color.gray));
        } else {
            titleText.setTextColor(mContext.getResources().getColor(R.color.dark_gray));
        }

        return view;
    }
}
