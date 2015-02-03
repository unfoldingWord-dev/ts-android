package com.door43.translationstudio.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.imports.ImportRequestInterface;
import com.door43.translationstudio.util.BetterSwitchCompat;
import com.door43.translationstudio.util.MainContext;

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
        return mRequests[i].getChildImportRequests().size();
    }

    @Override
    public ImportRequestInterface getGroup(int i) {
        return mRequests[i];
    }

    @Override
    public ImportRequestInterface getChild(int groupPosition, int childPosition) {
        return mRequests[groupPosition].getChildImportRequests().get(childPosition);
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
        final ImportRequestInterface request = getGroup(i);

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_import_approval_group, null);
            TextView headerTextView = (TextView)v.findViewById(R.id.groupListHeader);
            ImageView statusImage = (ImageView)v.findViewById(R.id.statusImage);
            RelativeLayout bodyLayout = (RelativeLayout)v.findViewById(R.id.bodyLayout);
            holder.headerTextView = headerTextView;
            holder.statusImage = statusImage;
            holder.bodyLayout = bodyLayout;
            v.setTag(holder);
        } else {
            holder = (GroupHolder)v.getTag();
        }

        holder.headerTextView.setText(request.getTitle());
        if(request.isApproved()) {
            if(request.getWarning() != null) {
                holder.statusImage.setBackgroundResource(R.drawable.ic_custom_selection);
            } else {
                holder.statusImage.setBackgroundResource(R.drawable.ic_success_light);
            }
        } else {
            if(request.getError() != null) {
                holder.statusImage.setBackgroundResource(R.drawable.ic_error);
            } else {
                holder.statusImage.setBackgroundResource(R.drawable.ic_warning_light);
            }
        }

        return v;
    }

    @Override
    public View getChildView(final int groupPosition, int childPosition, boolean b, View convertView, ViewGroup viewGroup) {
        View v = convertView;
        ChildHolder holder = new ChildHolder();

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_import_approval_item, null);
            TextView titleTextView = (TextView)v.findViewById(R.id.approvalItemTitleText);
            TextView descriptionTextView = (TextView)v.findViewById(R.id.approvalDescriptionText);
            ImageView statusImage = (ImageView)v.findViewById(R.id.approvalStatusImage);
            LinearLayout statusButton = (LinearLayout)v.findViewById(R.id.approvalStatusButton);
            RelativeLayout bodyLayout = (RelativeLayout)v.findViewById(R.id.bodyLayout);
            ImageView sublistImageView = (ImageView)v.findViewById(R.id.sublistImageView);
            BetterSwitchCompat confirmationSwitch = (BetterSwitchCompat)v.findViewById(R.id.confirmationSwitch);
            BetterSwitchCompat confirmAllSwitch = (BetterSwitchCompat)v.findViewById(R.id.confirmAllSwitch);
            LinearLayout confirmAllLayout = (LinearLayout)v.findViewById(R.id.confirmAllLayout);
            confirmAllSwitch.setFocusable(false);
            confirmationSwitch.setFocusable(false);
            statusButton.setFocusable(false);
            holder.titleTextView = titleTextView;
            holder.descriptionTextView = descriptionTextView;
            holder.statusImage = statusImage;
            holder.statusButton = statusButton;
            holder.bodyLayout = bodyLayout;
            holder.sublistImageView = sublistImageView;
            holder.confirmationSwitch = confirmationSwitch;
            holder.confirmAllSwitch = confirmAllSwitch;
            holder.confirmAllLayout = confirmAllLayout;
            v.setTag(holder);
        } else {
            holder = (ChildHolder)v.getTag();
        }

        final ImportRequestInterface request = getChild(groupPosition, childPosition);

        // indicate that this item is approved
        holder.confirmationSwitch.silentlySetChecked(request.isApproved());

        // set text
        holder.titleTextView.setText(request.getTitle());
        if(request.isApproved()) {
            holder.descriptionTextView.setText(mContext.getResources().getText(R.string.label_ok));
        }
        if (request.getError() != null) {
            holder.descriptionTextView.setText(request.getError());
        } else if (request.getWarning() != null) {
            holder.descriptionTextView.setText(request.getWarning());
        }

        // handle child confirmation switch
        holder.confirmationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                request.setIsApproved(b);
                notifyDataSetChanged();
            }
        });

        // indicate the user may view children of this item
        if(request.getError() == null && request.getChildImportRequests().size() > 0) {
            holder.sublistImageView.setVisibility(View.VISIBLE);
        } else {
            holder.sublistImageView.setVisibility(View.INVISIBLE);
        }

        // display group confirm all layout
        if(childPosition == 0) {
            final ImportRequestInterface group = getGroup(groupPosition);
            holder.confirmAllLayout.setVisibility(View.VISIBLE);
            holder.confirmAllSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    group.setIsApproved(b);
                    notifyDataSetChanged();
                }
            });
            // indicate group is approved
            holder.confirmAllSwitch.silentlySetChecked(group.isApproved());
        } else {
            holder.confirmAllLayout.setVisibility(View.GONE);
            holder.confirmAllSwitch.setOnCheckedChangeListener(null);
        }

        return v;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }

    private static class GroupHolder {
        public TextView headerTextView;
        public ImageView statusImage;
        public RelativeLayout bodyLayout;
    }

    private static class ChildHolder {
        public TextView titleTextView;
        public TextView descriptionTextView;
        public ImageView statusImage;
        public RelativeLayout bodyLayout;
        public LinearLayout statusButton;
        public ImageView sublistImageView;
        public BetterSwitchCompat confirmationSwitch;
        public BetterSwitchCompat confirmAllSwitch;
        public LinearLayout confirmAllLayout;
    }
}
