package com.door43.translationstudio.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;

import java.util.List;

/**
 * Created by joel on 1/12/2015.
 */
public class ProjectImportAprovalAdapter extends BaseAdapter {

    private final Context mContext;
    private List<Project.ImportRequest> mStatuses;

    public ProjectImportAprovalAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mStatuses.size();
    }

    @Override
    public Project.ImportRequest getItem(int i) {
        return mStatuses.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View v;
        final Project.ImportRequest item = getItem(i);
        boolean showSeparator = false;

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_import_approval_item, null);
        } else {
            v = convertView;
        }

        TextView langText = (TextView)v.findViewById(R.id.languageTextView);
        TextView descriptionText = (TextView)v.findViewById(R.id.approvalDescriptionText);
        TextView separator = (TextView)v.findViewById(R.id.separator);
        final ImageView approvedButton = (ImageView)v.findViewById(R.id.importApprovedButton);

        // toggle approved. Errors cannot be approved
//        if(item.getError() == null) {
//            approvedButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    item.setIsApproved(!item.isApproved());
//                    if (item.isApproved()) {
//                        approvedButton.setBackgroundResource(R.drawable.ic_success);
//                    } else {
//                        approvedButton.setBackgroundResource(R.drawable.ic_warning);
//                    }
//                }
//            });
//        }

        if(item.isApproved()) {
            approvedButton.setBackgroundResource(R.drawable.ic_success);
            descriptionText.setText(mContext.getResources().getText(R.string.label_ok));
        } else {
            if(item.getError() == null) {
                approvedButton.setBackgroundResource(R.drawable.ic_warning);
            } else {
                approvedButton.setBackgroundResource(R.drawable.ic_error);
            }
        }

        if(item.getError() != null) {
            descriptionText.setText(item.getError());
        } else if(item.getWarning() != null) {
            descriptionText.setText(item.getWarning());
        }

        langText.setText(item.languageId);

        // show separator
        if(i == 0) {
            showSeparator = true;
        } else if(!item.projectId.equals(getItem(i-1).projectId)) {
            showSeparator = true;
        } else {
            showSeparator = false;
        }

        if(showSeparator) {
            Project p = MainContext.getContext().getSharedProjectManager().getProject(item.projectId);
            separator.setText(p.getTitle());
            separator.setVisibility(View.VISIBLE);
        } else {
            separator.setVisibility(View.GONE);
        }

        return v;
    }

    public void addImportStatuses(List<Project.ImportRequest> statuses) {
        mStatuses = statuses;
    }

    public List<Project.ImportRequest> getImportStatuses() {
        return mStatuses;
    }
}
