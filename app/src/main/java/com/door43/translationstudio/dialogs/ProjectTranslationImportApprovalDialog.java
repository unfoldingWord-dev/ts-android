package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.imports.ImportRequestInterface;
import com.door43.translationstudio.projects.imports.ProjectImport;
import com.door43.translationstudio.projects.imports.TranslationImport;

/**
 * Created by joel on 1/12/2015.
 */
public class ProjectTranslationImportApprovalDialog extends DialogFragment {
    private ProjectImport[] mRequests = new ProjectImport[]{};
    private OnClickListener mListener;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.import_project);
        View v = inflater.inflate(R.layout.dialog_project_translation_import_approval, container, false);

        if(mRequests.length > 0) {
            // load the adapter
            final ProjectImportAprovalAdapter adapter = new ProjectImportAprovalAdapter(this.getActivity(), mRequests);

            ExpandableListView list = (ExpandableListView)v.findViewById(R.id.importListView);
            Button cancelButton = (Button)v.findViewById(R.id.buttonCancel);
            final Button okButton = (Button)v.findViewById(R.id.buttonOk);
            list.setAdapter(adapter);
            for(int i=0; i<adapter.getGroupCount(); i ++) {
                ImportRequestInterface r = adapter.getGroup(i);
                if(!r.isApproved()) {
                    list.expandGroup(i);
                }
            }
            list.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long l) {
                    ImportRequestInterface item = adapter.getChild(groupPosition, childPosition);
                    if(item.getError() == null && item.getChildImportRequests().size() > 0) {
                        // display dialog to handle children
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ft.addToBackStack(null);
                        ChapterFrameImportApprovalDialog newFragment = new ChapterFrameImportApprovalDialog();
                        newFragment.setImportRequests((TranslationImport)item);
                        newFragment.show(ft, "dialog");
                        return true;
                    } else {
                        // imports with errors can never be approved
                        return false;
                    }
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mListener != null) {
                        mListener.onCancel(mRequests);
                    }
                    dismiss();
                }
            });

            // TODO: we need to disable the ok button until at least one language is approved for import.
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mListener != null) {
                        mListener.onOk(mRequests);
                    }
                    dismiss();
                }
            });
        } else {
            if(mListener != null) {
                mListener.onCancel(mRequests);
            }
            dismiss();
        }

        return v;
    }

    /**
     * Specifies the model list to use in the dialog.
     * This must be called before showing the dialog.
     */
    public void setImportRequests(ProjectImport[] requests) {
        mRequests = requests;
    }

    /**
     * Sets the listener to be trigered when the form is submitted
     * @param listener
     */
    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    public static interface OnClickListener {
        public void onOk(ProjectImport[] requests);
        public void onCancel(ProjectImport[] requests);
    }
}
