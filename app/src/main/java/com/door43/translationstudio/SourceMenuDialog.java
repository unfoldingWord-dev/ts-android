package com.door43.translationstudio;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;

/**
 * This dialog contains contextual actions available for the currently selected source language
 */
public class SourceMenuDialog extends DialogFragment  {
    private final SourceMenuDialog me = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.source_fragment_dialog, container, false);

        Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(p == null) {
            me.dismiss();
        }

        // TODO: allow the user to switch the active source language

        // hook up buttons
        Button cancelBtn = (Button)v.findViewById(R.id.cancelSwitchSourceLanguageButton);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                me.dismiss();
            }
        });
        Button okBtn = (Button)v.findViewById(R.id.okSwitchSourceLanguageButton);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: save the selected source in the project
                me.dismiss();
            }
        });
        return v;
    }
}
