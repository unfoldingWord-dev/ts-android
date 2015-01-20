package com.door43.translationstudio.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.imports.ImportRequestInterface;

/**
 * Created by joel on 1/12/2015.
 */
public class ProjectImportAprovalAdapter extends BaseExpandableListAdapter {

    private final Context mContext;
    private ImportRequestInterface[] mRequests;

    public ProjectImportAprovalAdapter(Context context, ImportRequestInterface[] requests) {
        mContext = context;
        mRequests = requests;
    }

    /**
     * Returns the import requests with their updated information.
     * @return
     */
    public ImportRequestInterface[] getFinalImportRequests() {
        return mRequests;
    }

    @Override
    public int getGroupCount() {
        return mRequests.length;
    }

    @Override
    public int getChildrenCount(int i) {
        int size = mRequests[i].getChildImportRequests().size();
        if(size > 0) {
            // leave room for an approve all button
            return size + 1;
        } else {
            return 0;
        }
    }

    @Override
    public ImportRequestInterface getGroup(int i) {
        return mRequests[i];
    }

    @Override
    public ImportRequestInterface getChild(int groupPosition, int childPosition) {
        // TRICKY: we leave room for an approve all button so we substract one from the index
        return mRequests[groupPosition].getChildImportRequests().get(childPosition-1);
    }

    @Override
    public long getGroupId(int i) {
        return 0;
    }

    @Override
    public long getChildId(int i, int i2) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean b, View convertView, ViewGroup viewGroup) {
        View v  = convertView;
        GroupHolder holder = new GroupHolder();
        ImportRequestInterface request = getGroup(i);

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_import_approval_group, null);
            TextView headerTextView = (TextView)v.findViewById(R.id.groupListHeader);
            holder.headerTextView = headerTextView;
            v.setTag(holder);
        } else {
            holder = (GroupHolder)v.getTag();
        }

        holder.headerTextView.setText(request.getTitle());

        return v;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean b, View convertView, ViewGroup viewGroup) {
        View v = convertView;
        ChildHolder holder = new ChildHolder();

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_import_approval_item, null);
            TextView titleTextView = (TextView)v.findViewById(R.id.titleTextView);
            TextView descriptionTextView = (TextView)v.findViewById(R.id.approvalDescriptionText);
            ImageView approvalStatusImage = (ImageView)v.findViewById(R.id.approvalStatusImage);
            LinearLayout bodyLayout = (LinearLayout)v.findViewById(R.id.bodyLayout);
            LinearLayout confirmAllLayout = (LinearLayout)v.findViewById(R.id.confirmAllLayout);
            holder.titleTextView = titleTextView;
            holder.descriptionTextView = descriptionTextView;
            holder.approvalStatusImage = approvalStatusImage;
            holder.bodyLayout = bodyLayout;
            holder.confirmAllLayout = confirmAllLayout;
            v.setTag(holder);
        } else {
            holder = (ChildHolder)v.getTag();
        }

        if(childPosition == 0) {
            // render the approve all button
            holder.confirmAllLayout.setVisibility(View.VISIBLE);
            holder.bodyLayout.setVisibility(View.GONE);
        } else {
            // render child item
            holder.bodyLayout.setVisibility(View.VISIBLE);
            holder.confirmAllLayout.setVisibility(View.GONE);
            ImportRequestInterface item = getChild(groupPosition, childPosition);
            holder.bodyLayout.setBackgroundResource(Color.TRANSPARENT);

            // toggle approved. Errors cannot be approved
            if (item.isApproved()) {
                holder.approvalStatusImage.setBackgroundResource(R.drawable.ic_success);
                holder.descriptionTextView.setText(mContext.getResources().getText(R.string.label_ok));
            } else {
                if (item.getError() == null) {
                    holder.approvalStatusImage.setBackgroundResource(R.drawable.ic_warning);
                } else {
                    holder.approvalStatusImage.setBackgroundResource(R.drawable.ic_error);
                }
            }

            if (item.getError() != null) {
                holder.descriptionTextView.setText(item.getError());
            } else if (item.getWarning() != null) {
                holder.descriptionTextView.setText(item.getWarning());
            }

            holder.titleTextView.setText(item.getTitle());
        }
        return v;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }

    private static class GroupHolder {
        public TextView headerTextView;
    }

    private static class ChildHolder {
        public TextView titleTextView;
        public TextView descriptionTextView;
        public ImageView approvalStatusImage;
        public LinearLayout bodyLayout;
        public LinearLayout confirmAllLayout;
    }
}
