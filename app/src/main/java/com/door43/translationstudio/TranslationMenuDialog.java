package com.door43.translationstudio;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.ModalDialog;

/**
 * This dialog contains contextual actions available for the currently selected translation content
 */
public class TranslationMenuDialog extends ModalDialog {
    private final TranslationMenuDialog me = this;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.translation_fragment_dialog, container, false);

        final Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(p == null || p.getSelectedChapter() == null) {
            me.dismiss();
        }

        // set values
        final EditText chapterTitleEditText = (EditText)v.findViewById(R.id.editChapterTitleEditText);
        chapterTitleEditText.setText(p.getSelectedChapter().getTitleTranslation().getText());
        final EditText chapterReferenceEditText = (EditText)v.findViewById(R.id.editChapterReferenceEditText);
        chapterReferenceEditText.setText(p.getSelectedChapter().getReferenceTranslation().getText());
        TextView languageText = (TextView)v.findViewById(R.id.languageNameText);
        languageText.setText(p.getSelectedTargetLanguage().getName());

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
        Button languageBtn = (Button)v.findViewById(R.id.switchTargetLanguageButton);
        languageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).showTargetLanguageMenu();
            }
        });
        return v;
    }
}
