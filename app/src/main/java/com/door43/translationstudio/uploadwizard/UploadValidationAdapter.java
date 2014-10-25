package com.door43.translationstudio.uploadwizard;

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
 * Created by joel on 10/24/2014.
 */
public class UploadValidationAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<UploadValidationItem> mValidationItems;

    public UploadValidationAdapter(ArrayList<UploadValidationItem> items, Context context) {
        mValidationItems = items;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mValidationItems.size();
    }

    @Override
    public UploadValidationItem getItem(int i) {
        return mValidationItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout itemView;

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = (LinearLayout)inflater.inflate(R.layout.fragment_upload_validation_item, null);
        } else {
            itemView = (LinearLayout)view;
        }

        // name
        TextView nameText = (TextView)itemView.findViewById(R.id.validationName);
        nameText.setText(getItem(i).getTitle());

        // description
        TextView descriptionText = (TextView)itemView.findViewById(R.id.validationDescription);
        if(!getItem(i).getDescription().isEmpty()) {
            descriptionText.setText(R.string.click_for_details);
        } else {
            descriptionText.setText("");
        }

        // icon
        ImageView imageView = (ImageView)itemView.findViewById(R.id.validationSuccessImage);
        if(getItem(i).getStatus() == UploadValidationItem.Status.ERROR) {
            imageView.setBackgroundResource(R.drawable.ic_error);
        } else if(getItem(i).getStatus() == UploadValidationItem.Status.WARNING) {
            imageView.setBackgroundResource(R.drawable.ic_warning);
        } else {
            imageView.setBackgroundResource(R.drawable.ic_success);
        }
        return itemView;
    }
}
