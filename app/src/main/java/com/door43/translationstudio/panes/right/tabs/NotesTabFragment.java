package com.door43.translationstudio.panes.right.tabs;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.TranslationNote;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
  * Created by joel on 2/12/2015.
  */
 public class NotesTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
    private LinearLayout mNotesView;
    private Boolean mIsLoaded = false;
    private ScrollView mNotesInfoScroll;
    private TextView mNotesMessageText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_right_resources_notes, container, false);
        mNotesView = (LinearLayout)view.findViewById(R.id.notesView);
        mNotesInfoScroll = (ScrollView)view.findViewById(R.id.notesInfoScroll);
        mNotesMessageText = (TextView)view.findViewById(R.id.notesMessageText);

        mIsLoaded = true;
        showNotes();
        return view;
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        showNotes();
    }

    public void showNotes() {
        Project p = app().getSharedProjectManager().getSelectedProject();
        if(p != null) {
            Chapter c = p.getSelectedChapter();
            if(c != null) {
                Frame f = c.getSelectedFrame();
                if(f != null) {
                    if(!mIsLoaded) return;
                    // load the notes
                    TranslationNote note = f.getTranslationNotes();
                    mNotesView.removeAllViews();
                    mNotesInfoScroll.scrollTo(0, 0);

                    // notes
                    if(note != null && note.getNotes().size() > 0) {
                        mNotesMessageText.setVisibility(View.GONE);
                        mNotesInfoScroll.setVisibility(View.VISIBLE);

                        for (final TranslationNote.Note noteItem : note.getNotes()) {
                            LinearLayout noteItemView = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.fragment_pane_right_resources_note_item, null);

                            // title
                            TextView titleText = (TextView)noteItemView.findViewById(R.id.translationNoteReferenceText);
                            titleText.setText(noteItem.getRef());

                            // passage
                            TextView passageText = (TextView)noteItemView.findViewById(R.id.translationNoteText);
                            passageText.setText(Html.fromHtml(noteItem.getText()));

                            mNotesView.addView(noteItemView);
                        }
                    } else {
                        mNotesMessageText.setVisibility(View.VISIBLE);
                        mNotesInfoScroll.setVisibility(View.GONE);
                    }
                    return;
                }
            }
        }

        // no notes are available
        if(mIsLoaded) {
            mNotesMessageText.setVisibility(View.VISIBLE);
            mNotesInfoScroll.setVisibility(View.GONE);
        }
    }
}
