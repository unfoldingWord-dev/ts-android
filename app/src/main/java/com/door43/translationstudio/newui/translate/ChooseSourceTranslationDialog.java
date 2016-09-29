package com.door43.translationstudio.newui.translate;

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

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.tools.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import org.unfoldingword.resourcecontainer.Resource;

/**
 * Created by joel on 9/15/2015.
 */
public class ChooseSourceTranslationDialog extends DialogFragment {
    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";
    public static final String TAG = ChooseSourceTranslationDialog.class.getSimpleName();
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private OnClickListener mListener;
    private ChooseSourceTranslationAdapter mAdapter;
    private Door43Client mLibrary;
    public static final boolean ENABLE_DRAFTS = true;
    private static final int MIN_CHECKING_LEVEL = 1; // the minimum level to be considered a source translation

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_choose_source_translation, container, false);

        mTranslator = App.getTranslator();
        mLibrary = App.getLibrary();

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
        searchView.setEnabled(false);
        ImageButton searchBackButton = (ImageButton) v.findViewById(R.id.search_back_button);
        searchBackButton.setVisibility(View.GONE);
        ImageView searchIcon = (ImageView) v.findViewById(R.id.search_mag_icon);
        // TODO: set up search

        mAdapter = new ChooseSourceTranslationAdapter(getActivity());
        // add selected
        String[] sourceTranslationIds = App.getOpenSourceTranslationIds(mTargetTranslation.getId());
        for(String id:sourceTranslationIds) {
            SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(id);
            if(sourceTranslation != null) {
                addSourceTranslation(sourceTranslation, true);
            }
        }

        int min_checking = App.MIN_CHECKING_LEVEL;
        if(ENABLE_DRAFTS) {
            min_checking = this.MIN_CHECKING_LEVEL;
        }

        // add available (duplicates are filtered by the adapter)
        SourceTranslation[] availableSourceTranslations = mLibrary.getSourceTranslations(mTargetTranslation.getProjectId(), min_checking);
        for(SourceTranslation sourceTranslation:availableSourceTranslations) {
            addSourceTranslation(sourceTranslation, false);
        }

        if(ENABLE_DRAFTS) {
            SourceTranslation[] draftSourceTranslations = mLibrary.getDraftTranslations(mTargetTranslation.getProjectId());
            for(SourceTranslation sourceTranslation:draftSourceTranslations) {
                if(sourceTranslation != null && sourceTranslation.getCheckingLevel() >= min_checking) {
                    addSourceTranslation(sourceTranslation, false);
                }
            }
        }
        mAdapter.sort();

        ListView listView = (ListView) v.findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mAdapter.isSelectableItem(position)) {
                    mAdapter.doClickOnItem(position);
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
                    if(mAdapter.isSelectableItem(i)) {
                        ChooseSourceTranslationAdapter.ViewItem item = mAdapter.getItem(i);
                        if(item.selected) {
                            sourceTranslationIds.add(item.id);
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
     * adds this source translation to the adapter
     * @param sourceTranslation
     * @param selected
     */
    private void addSourceTranslation(SourceTranslation sourceTranslation, boolean selected) {
        String title = sourceTranslation.getSourceLanguageTitle() + " - " + sourceTranslation.getResourceTitle();

        if(sourceTranslation.getCheckingLevel() < App.MIN_CHECKING_LEVEL) { // see if draft
            String format = getActivity().getResources().getString(R.string.draft_translation);
            String newTitle = String.format(format, sourceTranslation.getCheckingLevel(), title);
            title = newTitle;
        }

        Resource resource = mLibrary.getResource( sourceTranslation);
        if(resource != null) {
            boolean downloaded = resource.isDownloaded();
            mAdapter.addItem(new ChooseSourceTranslationAdapter.ViewItem(title, sourceTranslation, selected, downloaded));
        } else {
            Logger.e(TAG, "Failed to get resource for " + sourceTranslation.getId());
        }
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
