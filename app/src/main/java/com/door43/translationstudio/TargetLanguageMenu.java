package com.door43.translationstudio;

import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.LanguageAdapter;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.ModalDialog;

/**
 * Created by joel on 10/7/2014.
 */
public class TargetLanguageMenu extends ModalDialog {
    private final TargetLanguageMenu me = this;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.target_language_fragment_dialog, container, false);

        Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(p == null) {
            me.dismiss();
        }

        // hook up list view
        ListView list = (ListView)v.findViewById(R.id.targetLanguageListView);
        final LanguageAdapter adapter;

        // add items to list view
        adapter = new LanguageAdapter(MainContext.getContext().getSharedProjectManager().getTargetLanguages(), getActivity());
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                MainContext.getContext().getSharedProjectManager().getSelectedProject().setSelectedTargetLanguage(adapter.getItem(i).getId());
                me.onDismiss("target_language", false);
            }
        });
        list.setTextFilterEnabled(true);
        EditText searchField = (EditText)v.findViewById(R.id.inputSearchTargetLanguage);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if(count < before) {
                    adapter.resetData();
                }
                adapter.getFilter().filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // hook up buttons
        Button cancelBtn = (Button)v.findViewById(R.id.cancelSwitchTargetLanguageButton);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                me.onDismiss("target_language", true);
            }
        });
        return v;
    }
}
