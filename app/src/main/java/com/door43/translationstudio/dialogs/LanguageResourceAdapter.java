package com.door43.translationstudio.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Resource;

import org.w3c.dom.Text;

/**
 * Created by joel on 12/30/2014.
 */
public class LanguageResourceAdapter extends BaseAdapter {

    private final Context mContext;
    private final Resource[] mResources;
    private final Resource mSelectedResource;

    public LanguageResourceAdapter(Context context, Resource[] resources, Resource selectedResource)  {
        mContext = context;
        mResources = resources;
        mSelectedResource = selectedResource;
    }

    @Override
    public int getCount() {
        return mResources.length;
    }

    @Override
    public Resource getItem(int i) {
        return mResources[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Resource r = getItem(i);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.fragment_text_list_item, null);
        TextView textView =(TextView)v.findViewById(R.id.textView);

        textView.setText(r.getName());

        // highlight selected resource
        if(mSelectedResource.getId().equals(r.getId())) {
            v.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
            textView.setTextColor(Color.WHITE);
        } else {
            v.setBackgroundColor(Color.TRANSPARENT);
            textView.setTextColor(mContext.getResources().getColor(R.color.gray));
        }

        return v;
    }
}
