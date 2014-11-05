package com.door43.translationstudio.panes.right;



import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.projects.TranslationNote;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class TranslationNotesFragment extends TranslatorBaseFragment {
    private TextView mImportantTerms;
    private TextView mImportantTermsTitle;
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
        mImportantTermsTitle = (TextView)view.findViewById(R.id.importantTermsTitleText);
        mImportantTermsTitle.setText(R.string.translation_notes_important_terms_title);
        mImportantTerms = (TextView)view.findViewById(R.id.importantTermsText);

        mainView = view.findViewById(R.id.resourcesNotesView);

        // make links clickable
        MovementMethod m = mImportantTerms.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (mImportantTerms.getLinksClickable()) {
                mImportantTerms.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        mIsLoaded = true;
        if(mTranslationNote != null) {
            showNotes(mTranslationNote);
        }
        return view;
    }


    public void showNotes(TranslationNote note) {
        if(note == null) {
            app().showToastMessage(getResources().getString(R.string.error_note_missing));
            ((MainActivity)getActivity()).closeDrawers();
            return;
        }

        // load later if not loaded
        mTranslationNote = note;
        if(!mIsLoaded) return;

        onShow();

        // load the notes
        final Project p = app().getSharedProjectManager().getSelectedProject();
        mImportantTerms.setText("");
        mNotesView.removeAllViews();

        // important terms
        int numImportantTerms = 0;
//        for(String term:note.getImportantTerms()) {
//            final Term importantTerm = p.getTerm(term);
//            if(importantTerm != null) {
//                final String termName = term;
//                SpannableString link = new SpannableString(importantTerm.getName());
//                ClickableSpan cs = new ClickableSpan() {
//                    @Override
//                    public void onClick(View widget) {
//                        ((MainActivity)getActivity()).showTermDetails(termName);
//                    }
//                };
//                link.setSpan(cs, 0, importantTerm.getName().length(), 0);
//                mImportantTerms.append(link);
//
//            } else {
//                mImportantTerms.append(term);
//            }
//            numImportantTerms++;
//            if(numImportantTerms < note.getImportantTerms().size()) {
//                mImportantTerms.append(", ");
//            }
//        }
        if(numImportantTerms == 0) {
            mImportantTermsTitle.setVisibility(View.GONE);
            mImportantTerms.setVisibility(View.GONE);
        } else {
            mImportantTermsTitle.setVisibility(View.VISIBLE);
            mImportantTerms.setVisibility(View.VISIBLE);
        }

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
            mNotesTitleText.setVisibility(View.GONE);
            mNotesView.setVisibility(View.GONE);
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