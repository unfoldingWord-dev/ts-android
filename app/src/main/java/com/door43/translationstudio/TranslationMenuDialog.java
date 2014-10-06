package com.door43.translationstudio;

import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContextLink;

/**
 * This dialog contains contextual actions available for the currently selected translation content
 */
public class TranslationMenuDialog extends DialogFragment {
    private final TranslationMenuDialog me = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.translation_fragment_dialog, container, false);

        final Project p = MainContextLink.getContext().getSharedProjectManager().getSelectedProject();
        if(p == null || p.getSelectedChapter() == null) {
            me.dismiss();
        }

        // set values
        final EditText chapterTitleEditText = (EditText)v.findViewById(R.id.editChapterTitleEditText);
        chapterTitleEditText.setText(p.getSelectedChapter().getTitleTranslation().getText());
        final EditText chapterReferenceEditText = (EditText)v.findViewById(R.id.editChapterReferenceEditText);
        chapterReferenceEditText.setText(p.getSelectedChapter().getReferenceTranslation().getText());

        // hook up buttons
        Button cancelBtn = (Button)v.findViewById(R.id.cancelEditChapterTitleButton);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                me.dismiss();
            }
        });
        Button saveBtn = (Button)v.findViewById(R.id.saveChapterTitleButton);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                p.getSelectedChapter().setTitleTranslation(chapterTitleEditText.getText().toString());
                p.getSelectedChapter().setReferenceTranslation(chapterReferenceEditText.getText().toString());
                p.getSelectedChapter().save();
                ((MainActivity) getActivity()).reloadCenterPane();
                me.dismiss();
            }
        });
        return v;
    }
}
