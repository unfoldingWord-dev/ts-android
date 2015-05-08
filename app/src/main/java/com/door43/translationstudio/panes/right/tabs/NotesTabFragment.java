package com.door43.translationstudio.panes.right.tabs;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Navigator;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.TranslationNote;
import com.door43.translationstudio.rendering.LinkRenderer;
import com.door43.translationstudio.spannables.PassageLinkSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
  * Created by joel on 2/12/2015.
  */
 public class NotesTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
//    private LinearLayout mNotesView;
    private Boolean mIsLoaded = false;
//    private ScrollView mNotesInfoScroll;
    private TextView mNotesMessageText;
    private Integer mScrollX = 0;
    private Integer mScrollY = 0;
//    private Button mTranslateBtn;
    private ListView mNotesListView;
    private NotesAdapter mAdapter;
    private FrameLayout mNotesDisplay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_right_resources_notes, container, false);
//        mNotesView = (LinearLayout)view.findViewById(R.id.notesView);
        /**
         * @deprecated
         */
//        mNotesInfoScroll = (ScrollView)view.findViewById(R.id.notesInfoScroll);
        mNotesDisplay = (FrameLayout)view.findViewById(R.id.notesDisplay);
        mNotesListView = (ListView)view.findViewById(R.id.notesListView);
        mAdapter = new NotesAdapter(getActivity());
        mNotesListView.setAdapter(mAdapter);
//        mNotesListView.setClickable(false);
//        mNotesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                // TODO: display interface for translating notes
//            }
//        });

        mNotesMessageText = (TextView)view.findViewById(R.id.notesMessageText);
        Button translateBtn = (Button)view.findViewById(R.id.translateNotesBtn);
        translateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: display ui for translating the notes
                AppContext.context().showToastMessage("Would you like to translate the notes?");
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

//                    mNotesView.removeAllViews();
//                    mNotesInfoScroll.scrollTo(mScrollX, mScrollY);

                    // notes
//                    if(note != null && note.getNotes().size() > 0) {
//                        mNotesMessageText.setVisibility(View.GONE);
//                        mNotesInfoScroll.setVisibility(View.VISIBLE);
//                        mTranslateBtn.setVisibility(View.VISIBLE);

//                        for (final TranslationNote.Note noteItem : note.getNotes()) {
////                            if(getActivity() != null) {
//                                LinearLayout noteItemView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_pane_right_resources_note_item, null);
//
//                                // title
//                                TextView titleText = (TextView) noteItemView.findViewById(R.id.translationNoteReferenceText);
//                                titleText.setText(noteItem.getRef());
//
//                                // passage
//                                TextView passageText = (TextView) noteItemView.findViewById(R.id.translationNoteText);

                                // passage links
//                                LinkRenderer renderingEngine = new LinkRenderer(new LinkRenderer.OnPreprocessLink() {
//                                    @Override
//                                    public boolean onPreprocess(PassageLinkSpan span) {
//                                        return getLinkEndpoint(span) != null;
//                                    }
//                                }, new Span.OnClickListener() {
//                                    @Override
//                                    public void onClick(View view, Span span, int start, int end) {
//                                        PassageLinkSpan link = (PassageLinkSpan) span;
//                                        final Frame f = getLinkEndpoint(link);
//                                        if (f != null) {
//                                            final ProgressDialog dialog = new ProgressDialog(getActivity());
//                                            dialog.setMessage(getResources().getString(R.string.loading_project_chapters));
//                                            dialog.show();
//                                            AppContext.navigator().open(f.getChapter().getProject(), new Navigator.OnSuccessListener() {
//                                                @Override
//                                                public void onSuccess() {
//                                                    AppContext.navigator().open(f.getChapter());
//                                                    AppContext.navigator().open(f);
//                                                    dialog.dismiss();
//                                                }
//
//                                                @Override
//                                                public void onFailed() {
//                                                    dialog.dismiss();
//                                                }
//                                            });
//                                            ((MainActivity) getActivity()).closeDrawers();
//                                        }
//                                    }
//                                });
//                                CharSequence text = renderingEngine.render(Html.fromHtml(noteItem.getText()));
//                                passageText.setText(text);

                                // make passage links clickable
//                                MovementMethod mmNotes = passageText.getMovementMethod();
//                                if ((mmNotes == null) || !(mmNotes instanceof LinkMovementMethod)) {
//                                    if (passageText.getLinksClickable()) {
//                                        passageText.setMovementMethod(LinkMovementMethod.getInstance());
//                                    }
//                                }

//                                mNotesView.addView(noteItemView);
//                            } else {
//                                Logger.e(this.getClass().getName(), "showNotes the activity is null");
//                            }
//                        }
//                    } else {
//                        mNotesMessageText.setVisibility(View.VISIBLE);
//                        mNotesInfoScroll.setVisibility(View.GONE);
//                        mNotesDisplay.setVisibility(View.GONE);
//                        mTranslateBtn.setVisibility(View.GONE);
//                    }
//                    return;
                }
            }
        }

        // no notes are available
        if(mIsLoaded) {
            mNotesMessageText.setVisibility(View.VISIBLE);
//            mNotesInfoScroll.setVisibility(View.GONE);
            mNotesDisplay.setVisibility(View.GONE);
//            mTranslateBtn.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the link endpoint
     * @param link
     * @return
     */
//    private Frame getLinkEndpoint(PassageLinkSpan link) {
//        Project p = AppContext.projectManager().getProject(link.getProjectId());
//        if(p != null) {
//            Chapter c = p.getChapter(link.getChapterId());
//            if(c != null) {
//                return c.getFrame(link.getFrameId());
//            }
//        }
//        return null;
//    }

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
//        if(mNotesInfoScroll != null) {
//            return new Pair<>(mNotesInfoScroll.getScrollX(), mNotesInfoScroll.getScrollY());
//        } else {
//            return new Pair<>(0, 0);
//        }
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
