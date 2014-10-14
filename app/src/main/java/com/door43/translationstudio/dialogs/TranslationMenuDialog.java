package com.door43.translationstudio.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
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
        View v = inflater.inflate(R.layout.dialog_translation, container, false);

        final Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(p == null || p.getSelectedChapter() == null) {
            me.dismiss();
        }

        // set values
        TextView sourceLanguageChapterTitleEditText = (TextView)v.findViewById(R.id.sourceLanguageChapterTitle);
        sourceLanguageChapterTitleEditText.setText(p.getSelectedChapter().getTitle());
        TextView sourceLanguageChapterReferenceEditText = (TextView)v.findViewById(R.id.sourceLanguageChapterReference);
        sourceLanguageChapterReferenceEditText.setText(p.getSelectedChapter().getReference());
        TextView sourceLanguageText = (TextView)v.findViewById(R.id.sourceLanguageNameText);
        sourceLanguageText.setText(p.getSelectedSourceLanguage().getName());

        final EditText targetLanguageChapterTitleEditText = (EditText)v.findViewById(R.id.targetLanguageChapterTitleEditText);
        targetLanguageChapterTitleEditText.setText(p.getSelectedChapter().getTitleTranslation().getText());
        final EditText targetLanguageChapterReferenceEditText = (EditText)v.findViewById(R.id.targetLanguageChapterReferenceEditText);
        targetLanguageChapterReferenceEditText.setText(p.getSelectedChapter().getReferenceTranslation().getText());
        TextView targetLanguageText = (TextView)v.findViewById(R.id.targetLanguageNameText);
        targetLanguageText.setText(p.getSelectedTargetLanguage().getName());

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
                p.getSelectedChapter().setTitleTranslation(targetLanguageChapterTitleEditText.getText().toString());
                p.getSelectedChapter().setReferenceTranslation(targetLanguageChapterReferenceEditText.getText().toString());
                p.getSelectedChapter().save();
                ((MainActivity) getActivity()).reloadCenterPane();
                me.dismiss();
            }
        });
        Button targetLanguageBtn = (Button)v.findViewById(R.id.switchTargetLanguageButton);
        targetLanguageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).showTargetLanguageMenu();
            }
        });
        Button sourceLanguageBtn = (Button)v.findViewById(R.id.switchSourceLanguageButton);
        sourceLanguageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).showSourceLanguageMenu();
            }
        });
        return v;
    }
}
