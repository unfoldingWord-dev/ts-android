package com.door43.translationstudio.util;

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
 * Created by joel on 10/16/2014.
 */
public class SharingAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<SharingToolItem> mSharingTools;

    public SharingAdapter(ArrayList<SharingToolItem> tools, Context context) {
        mSharingTools = tools;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mSharingTools.size();
    }

    @Override
    public SharingToolItem getItem(int i) {
        return mSharingTools.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout sharingToolView;

        // build or reuse layout
        if(view == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            sharingToolView = (LinearLayout)inflater.inflate(R.layout.fragment_sharing_tool_item, null);
        } else {
            sharingToolView = (LinearLayout)view;
        }

        // title
        TextView nameText = (TextView)sharingToolView.findViewById(R.id.sharingToolTextView);
        nameText.setText(getItem(i).getName());

        // description
        TextView descriptionText = (TextView)sharingToolView.findViewById(R.id.sharingToolDescriptionTextView);
        descriptionText.setText(getItem(i).getDescription());

        // image
        ImageView iconImage = (ImageView)sharingToolView.findViewById(R.id.sharingToolImageView);
        iconImage.setBackgroundResource(getItem(i).getIcon());

        // mark tool as disabled.
        if (!getItem(i).isEnabled()) {
            nameText.setTextColor(mContext.getResources().getColor(R.color.gray));
        }

        return sharingToolView;
    }
}
