package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.MetaProject;
import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 12/23/2014.
 */
public class MetaProjectDialog extends DialogFragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.projects);
        View v = inflater.inflate(R.layout.dialog_meta_project, container, false);

        Bundle args = getArguments();
        final String id = args.getString("metaId");
        MetaProject p = MainContext.getContext().getSharedProjectManager().getMetaProject(id);

        // TODO: build a list interface where users can continue to make selections.

        return v;
    }
}
