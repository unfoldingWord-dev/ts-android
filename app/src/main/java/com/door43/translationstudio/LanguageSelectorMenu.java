package com.door43.translationstudio;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.door43.translationstudio.events.LanguageModalDismissedEvent;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.LanguageAdapter;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.ModalDialog;

/**
 * Created by joel on 10/7/2014.
 */
public class LanguageSelectorMenu extends ModalDialog {
    private final LanguageSelectorMenu me = this;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.target_language_fragment_dialog, container, false);
        Boolean showSourceLanguages = false;

        Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(p == null) {
            me.dismiss();
        }

        // hook up list view
        ListView list = (ListView)v.findViewById(R.id.targetLanguageListView);
        final LanguageAdapter adapter;

        Bundle bundle = getArguments();
        if(bundle != null) {
            showSourceLanguages = bundle.getBoolean("sourceLanguages", false);
        }
        final boolean willShowSourceLanguages = showSourceLanguages;

        // add items to list view
        if(willShowSourceLanguages) {
            adapter = new LanguageAdapter(p.getSourceLanguages(), getActivity());
        } else {
            adapter = new LanguageAdapter(MainContext.getContext().getSharedProjectManager().getTargetLanguages(), getActivity());
        }

        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(willShowSourceLanguages) {
                    MainContext.getContext().getSharedProjectManager().getSelectedProject().setSelectedSourceLanguage(adapter.getItem(i).getId());
                    me.onDismiss(adapter.getItem(i), false);
                } else {
                    MainContext.getContext().getSharedProjectManager().getSelectedProject().setSelectedTargetLanguage(adapter.getItem(i).getId());
                    me.onDismiss(adapter.getItem(i), false);
                }
            }
        });
        list.setTextFilterEnabled(true);
        EditText searchField = (EditText)v.findViewById(R.id.inputSearchTargetLanguage);
        if(willShowSourceLanguages) {
            // TODO: update wording to reflect languages list
        } else {
            // TODO: update wording to reflect languages list
        }

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
                me.onDismiss(null, true);
            }
        });
        return v;
    }

    private void onDismiss(Language l, boolean didCancel) {
        MainContext.getEventBus().post(new LanguageModalDismissedEvent(l, didCancel));
        this.dismiss();
    }
}
