package com.door43.translationstudio.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This dialog allows users to browse project source langauges and make selections
 */
@Deprecated
public class SourceLanguageLibraryDialog extends DialogFragment {
    private SourceLanguage[] mLanguages;
    private LanguageAdapter mAdapter;
    private boolean mDismissed;

    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.source_language_library);
        View v = inflater.inflate(R.layout.dialog_import_project, container, false);

        ListView list = (ListView)v.findViewById(R.id.languageListView);

        if(savedInstanceState != null) {
            mLanguages = (SourceLanguage[]) savedInstanceState.getSerializable("languages");
        }

        if(mAdapter == null) mAdapter = new LanguageAdapter(new ArrayList<Language>(Arrays.asList(mLanguages)), AppContext.context(), false);

        Button cancelButton = (Button)v.findViewById(R.id.buttonCancel);
        final Button okButton = (Button)v.findViewById(R.id.buttonOk);
        okButton.setBackgroundColor(getResources().getColor(R.color.gray));

        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mAdapter.toggleSelected(i);
                if(mAdapter.getSelectedItems().length > 0) {
                    okButton.setBackgroundColor(getResources().getColor(R.color.blue));
                } else {
                    okButton.setBackgroundColor(getResources().getColor(R.color.gray));
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDismissed = true;
                if(getActivity() != null) {
                    ((SourceLanguageLibraryListener)getActivity()).onSourceLanguageLibraryDismissed(SourceLanguageLibraryDialog.this);
                }
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Language[] selectedItems = mAdapter.getSelectedItems();
                if(getActivity() != null) {
                    ((SourceLanguageLibraryListener)getActivity()).onSourceLanguageLibrarySelected(SourceLanguageLibraryDialog.this, selectedItems);
                }
            }
        });
        return v;
    }

    /**
     * Sets ana rray of source languages that will be displayed in the list
     * @param languages
     */
    public void setLanguages(SourceLanguage[] languages) {
        mLanguages = languages;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mDismissed = true;
        if(getActivity() != null) {
            ((SourceLanguageLibraryListener)getActivity()).onSourceLanguageLibraryDismissed(this);
        }
        super.onCancel(dialog);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDismissed = true;
        if(getActivity() != null) {
            ((SourceLanguageLibraryListener)getActivity()).onSourceLanguageLibraryDismissed(this);
        }
        super.onDismiss(dialog);
    }
    /**
     * Ensure the activity is configured properly
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(!(activity instanceof SourceLanguageLibraryListener)) {
            throw new ClassCastException(activity.toString() + " must implement SourceLanguageLibraryListener");
        } else {
            if(mDismissed) {
                ((SourceLanguageLibraryListener)getActivity()).onSourceLanguageLibraryDismissed(this);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("languages", mLanguages);
//        outState.putSerializable("selected_languages", mSelectedProjectId);
    }

    public static interface SourceLanguageLibraryListener {
        public void onSourceLanguageLibrarySelected(SourceLanguageLibraryDialog dialog, Language[] languages);
        public void onSourceLanguageLibraryDismissed(SourceLanguageLibraryDialog dialog);
    }
}
