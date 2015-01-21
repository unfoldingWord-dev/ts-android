package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.events.ProjectImportApprovalEvent;
import com.door43.translationstudio.projects.imports.ChapterImport;
import com.door43.translationstudio.projects.imports.ImportRequestInterface;
import com.door43.translationstudio.projects.imports.ProjectImport;
import com.door43.translationstudio.projects.imports.TranslationImport;
import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 1/20/2015.
 */
public class ChapterFrameImportApprovalDialog extends DialogFragment {
    private TranslationImport mRequest;


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.import_project);
        View v = inflater.inflate(R.layout.dialog_chapter_frame_import_approval, container, false);

        if(mRequest != null) {
            // load the adapter
            final ProjectImportAprovalAdapter adapter = new ProjectImportAprovalAdapter(this.getActivity(), mRequest.getChildImportRequests().getAll().toArray(new ImportRequestInterface[mRequest.getChildImportRequests().size()]));

            ExpandableListView list = (ExpandableListView)v.findViewById(R.id.importListView);
            Button backButton = (Button)v.findViewById(R.id.buttonBack);
            list.setAdapter(adapter);
            list.expandGroup(0);

            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });
        } else {
            dismiss();
        }

        return v;
    }

    /**
     * Specifies the model list to use in the dialog.
     * This must be called before showing the dialog.
     */
    public void setImportRequests(TranslationImport request) {
        mRequest = request;
    }
}
