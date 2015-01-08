package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;

/**
 * This dialog handles the final import step for projects downloaded from the server or from another device.
 *
 */
public class ImportProjectDialog extends DialogFragment {
    private Project mProject = null;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.import_project);
        View v = inflater.inflate(R.layout.dialog_import_project, container, false);

        if(mProject != null) {
            // TODO: populate the form and list.
        } else {
            dismiss();
        }

        return v;
    }

    /**
     * Specifies the project that will be imported.
     * This must be called before showing the dialog.
     * @param p The project that will be imported. This is not a fully loaded project, it just contains basic project information and available languages.
     */
    public void setProject(Project p) {
        mProject = p;
    }
}
