package com.door43.translationstudio.panes.right;


import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.TranslationNote;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class TranslationNotesFragment extends TranslatorBaseFragment {

    private TextView mNotesTitleText;
    private LinearLayout mNotesView;
    private View mainView;
    private Boolean mIsLoaded = false;
    private TranslationNote mTranslationNote;
    private Handler.Callback mOnShowCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_right_resources_notes, container, false);

        mNotesTitleText = (TextView)view.findViewById(R.id.translationNotesTitleText);
        mNotesTitleText.setText(R.string.translation_notes_title);
        mNotesView = (LinearLayout)view.findViewById(R.id.translationNotesView);


        mainView = view.findViewById(R.id.resourcesNotesView);

        mIsLoaded = true;
        if(mTranslationNote != null) {
            showNotes(mTranslationNote);
        }
        return view;
    }


    public void showNotes(TranslationNote note) {
        if(note == null) {
            if(getActivity() != null) {
                ((MainActivity) getActivity()).closeDrawers();
            }
            return;
        }

        // load later if not loaded
        mTranslationNote = note;
        if(!mIsLoaded) return;

        onShow();

        // load the notes
        mNotesView.removeAllViews();

        // notes
        if(note.getNotes().size() > 0) {
            mNotesTitleText.setVisibility(View.VISIBLE);
            for (final TranslationNote.Note noteItem : note.getNotes()) {
                LinearLayout noteItemView = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.fragment_pane_right_resources_note_item, null);

                // link
                TextView linkText = (TextView)noteItemView.findViewById(R.id.translationNoteReferenceText);
                linkText.setText(noteItem.getRef() + "-");

                // passage
                TextView passageText = (TextView)noteItemView.findViewById(R.id.translationNoteText);
                passageText.setText(Html.fromHtml(noteItem.getText()));

                mNotesView.addView(noteItemView);
            }
            mNotesView.setVisibility(View.VISIBLE);
        } else {
            TextView placeholder = new TextView(getActivity());
            placeholder.setText(R.string.no_translation_notes);
            placeholder.setTextColor(getResources().getColor(R.color.gray));
            mNotesView.addView(placeholder);
        }
    }

    public void show() {
        if(mainView != null) {
            mainView.setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        // TODO: fade out
        if(mainView != null) {
            mainView.setVisibility(View.GONE);
        }
    }

    public void onShow() {
        if(mOnShowCallback != null) {
            mOnShowCallback.handleMessage(null);
        }
    }

    public void setOnShowCallback(Handler.Callback callback) {
        mOnShowCallback = callback;
    }
}