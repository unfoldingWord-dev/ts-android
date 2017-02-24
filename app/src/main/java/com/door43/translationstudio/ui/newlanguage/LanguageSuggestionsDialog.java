package com.door43.translationstudio.ui.newlanguage;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.newtranslation.TargetLanguageAdapter;

import org.unfoldingword.door43client.models.TargetLanguage;

import java.util.List;

/**
 * Created by joel on 6/9/16.
 */
public class LanguageSuggestionsDialog extends DialogFragment {

    public static final String TAG = "language_suggestions_dialog";
    public static final String ARG_LANGUAGE_QUERY = "language_query";
    private OnClickListener listener = null;
    private TargetLanguageAdapter adapter;
    private List<TargetLanguage> targetLanguages;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_target_language_suggestions, container, false);

        Bundle args = getArguments();
        if(args != null) {
            targetLanguages = App.getLibrary().index().findTargetLanguage(args.getString(ARG_LANGUAGE_QUERY));
        } else {
            dismiss();
            return v;
        }

        v.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) {
                    listener.onDismissLanguageSuggestion();
                    dismiss();
                } else {
                    dismiss();
                }
            }
        });

        ListView listView = (ListView)v.findViewById(R.id.list_view);

        adapter = new TargetLanguageAdapter(getActivity(), targetLanguages);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(listener != null) {
                    listener.onAcceptLanguageSuggestion(adapter.getItem(position));
                    dismiss();
                }
            }
        });

        return v;
    }

    /**
     * Sets the listener to receive click events
     * @param listener
     */
    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {
        void onAcceptLanguageSuggestion(TargetLanguage language);
        void onDismissLanguageSuggestion();
    }
}
