package com.door43.translationstudio.panes.right.tabs;

import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.TranslationNote;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TabsAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
  * Created by joel on 2/12/2015.
  */
 public class NotesTab extends TranslatorBaseFragment implements TabsAdapterNotification {
    private Boolean mIsLoaded = false;
    private TextView mNotesMessageText;
    private Integer mScrollX = 0;
    private Integer mScrollY = 0;
    private ListView mNotesListView;
    private NotesAdapter mAdapter;
    private LinearLayout mNotesDisplay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_right_resources_notes, container, false);
        mNotesDisplay = (LinearLayout)view.findViewById(R.id.notesDisplay);
        mNotesListView = (ListView)view.findViewById(R.id.notesListView);
        mAdapter = new NotesAdapter(getActivity());
        mNotesListView.setAdapter(mAdapter);
        mNotesMessageText = (TextView)view.findViewById(R.id.notesMessageText);
        final Button translateBtn = (Button)view.findViewById(R.id.translateNotesBtn);
        translateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAdapter.getRenderTranslations()) {
                    // save changes and change button image
                    if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        translateBtn.setBackgroundDrawable(getActivity().getResources().getDrawable(R.drawable.ic_new_pencil_small));
                    } else {
                        translateBtn.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_new_pencil_small));
                    }
                } else {
                    // change button image
                    if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        translateBtn.setBackgroundDrawable(getActivity().getResources().getDrawable(R.drawable.ic_check_small));
                    } else {
                        translateBtn.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_check_small));
                    }
                }
                mAdapter.setRenderTranslations(!mAdapter.getRenderTranslations());
            }
        });

        mIsLoaded = true;
        showNotes();
        return view;
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        showNotes();
    }

    public void showNotes() {
        Project p = AppContext.projectManager().getSelectedProject();
        if(p != null) {
            Chapter c = p.getSelectedChapter();
            if(c != null) {
                Frame f = c.getSelectedFrame();
                if(f != null) {
                    if(!mIsLoaded) return;
                    // load the notes
                    TranslationNote note = f.getTranslationNotes();
                    mAdapter.changeDataset(note);

                    if(note != null && note.getNotes().size() > 0) {
                        mNotesMessageText.setVisibility(View.GONE);
                        mNotesDisplay.setVisibility(View.VISIBLE);
                    }
                    return;
                }
            }
        }

        // no notes are available
        if(mIsLoaded) {
            mNotesMessageText.setVisibility(View.VISIBLE);
            mNotesDisplay.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the scroll x and y position of the notes
     * @return
     */
    public Pair<Integer, Integer> getScroll() {
        if(mNotesListView != null) {
            return new Pair<>(mNotesListView.getScrollX(), mNotesListView.getScrollY());
        } else {
            return new Pair<>(0, 0);
        }
    }

    /**
     * Sets the scroll position of the notes
     * @param scrollPair
     */
    public void setScroll(Pair<Integer, Integer> scrollPair) {
        mScrollX = scrollPair.first;
        mScrollY = scrollPair.second;
        if(mNotesListView != null) {
            mNotesListView.scrollTo(mScrollX, mScrollY);
        }
    }
}
