package com.door43.translationstudio.newui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 9/15/2015.
 */
public class ChooseSourceTranslationDialog extends DialogFragment {
    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private OnClickListener mListener;
    private SourceLanguageTabAdapter mAdapter;
    private Library mLibrary;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_choose_source_translation, container, false);

        mTranslator = AppContext.getTranslator();
        mLibrary = AppContext.getLibrary();

        Bundle args = getArguments();
        if(args == null) {
            dismiss();
        } else {
            String targetTranslationId = args.getString(ARG_TARGET_TRANSLATION_ID, null);
            mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
            if(mTargetTranslation == null) {
                // missing target translation
                dismiss();
            }
        }

        EditText searchView = (EditText) v.findViewById(R.id.search_text);
        searchView.setHint(R.string.choose_source_translations);
        ImageButton searchBackButton = (ImageButton) v.findViewById(R.id.search_back_button);
        searchBackButton.setVisibility(View.GONE);
        ImageView searchIcon = (ImageView) v.findViewById(R.id.search_mag_icon);
        // TODO: set up search

        mAdapter = new SourceLanguageTabAdapter(getActivity());
        // add selected
        String[] sourceTranslationIds = mTranslator.getSourceTranslations(mTargetTranslation.getId());
        for(String id:sourceTranslationIds) {
            SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(id);
            if(sourceTranslation != null) {
                String title = sourceTranslation.getSourceLanguageTitle() + " - " + sourceTranslation.getResourceTitle();
                mAdapter.addItem(new SourceLanguageTabAdapter.ViewItem(title, sourceTranslation.getId(), true));
            }
        }
        // add available (duplicates are filtered by the adapter)
        SourceTranslation[] availableSourceTranslations = mLibrary.getSourceTranslations(mTargetTranslation.getProjectId());
        for(SourceTranslation sourceTranslation:availableSourceTranslations) {
            String title = sourceTranslation.getSourceLanguageTitle() + " - " + sourceTranslation.getResourceTitle();
            mAdapter.addItem(new SourceLanguageTabAdapter.ViewItem(title, sourceTranslation.getId(), false));
        }
        mAdapter.sort();

        ListView listView = (ListView) v.findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mAdapter.getItemViewType(position) == SourceLanguageTabAdapter.TYPE_ITEM) {
                    mAdapter.getItem(position).selected = !mAdapter.getItem(position).selected;
                    mAdapter.sort();
                }
            }
        });

        Button cancelButton = (Button) v.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null) {
                    mListener.onCancelTabsDialog(mTargetTranslation.getId());
                }
                dismiss();
            }
        });
        Button confirmButton = (Button) v.findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // collect selected source translations
                int count = mAdapter.getCount();
                List<String> sourceTranslationIds = new ArrayList<>();
                for(int i = 0; i < count; i ++) {
                    if(mAdapter.getItemViewType(i) == SourceLanguageTabAdapter.TYPE_ITEM) {
                        SourceLanguageTabAdapter.ViewItem item = mAdapter.getItem(i);
                        if(item.selected) {
                            sourceTranslationIds.add(item.id);
//                            mTranslator.addSourceTranslation(mTargetTranslation.getId(), mLibrary.getSourceTranslations(item.id));
//                        } else {
//                            mTranslator.removeSourceTranslation(mTargetTranslation.getId(), item.id);
                        }
                    }
                }
                if(mListener != null) {
                    mListener.onConfirmTabsDialog(mTargetTranslation.getId(), sourceTranslationIds.toArray(new String[sourceTranslationIds.size()]));
                }
                dismiss();
            }
        });

        return v;
    }

    /**
     * Assigns a listener for this dialog
     * @param listener
     */
    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    public interface OnClickListener {
        void onCancelTabsDialog(String targetTranslationId);
        void onConfirmTabsDialog(String targetTranslationId, String[] sourceTranslationIds);
    }
}
