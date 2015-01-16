package com.door43.translationstudio.panes.right;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 10/23/2014.
 */
public class ResourcesFragment extends TranslatorBaseFragment {
    private KeyTermFragment mTermFragment = new KeyTermFragment();
    private TranslationNotesFragment mNotesFragment = new TranslationNotesFragment();
    private Button mNotesBtn;
    private Button mTermsBtn;
    private ScrollView mScrollView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_pane_right_resources, container, false);

        mScrollView = (ScrollView)view.findViewById(R.id.scrollView);

        // hook up notes button
        mTermsBtn = (Button)view.findViewById(R.id.resourcesTermsButton);
        mNotesBtn = (Button)view.findViewById(R.id.resourcesNotesButton);
        mNotesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNotes();
            }
        });
        mTermsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTerm();
            }
        });

        // set up show callbacks
        mNotesFragment.setOnShowCallback(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                mTermFragment.hide();
                mNotesBtn.setBackgroundResource(R.drawable.button_purple_bottom_border);
                mTermsBtn.setBackgroundResource(android.R.color.transparent);
                mNotesBtn.setPadding(10, 10, 10, 10);
                mTermsBtn.setPadding(10, 10, 10, 10);
                scrollToTop();
                return false;
            }
        });

        mTermFragment.setOnShowCallback(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                mNotesFragment.hide();
                mTermsBtn.setBackgroundResource(R.drawable.button_purple_bottom_border);
                mNotesBtn.setBackgroundResource(android.R.color.transparent);
                mNotesBtn.setPadding(10, 10, 10, 10);
                mTermsBtn.setPadding(10, 10, 10, 10);
                scrollToTop();
                return false;
            }
        });

        if(MainContext.getContext().getSelectedKeyTerm() != null) {
            showTerm(MainContext.getContext().getSelectedKeyTerm());
        } else if(MainContext.getContext().getShowImportantTerms()) {
            showTerm();
        } else {
            showNotes();
        }

        // insert layouts
        getFragmentManager().beginTransaction().replace(R.id.resourcesNotesView, mNotesFragment).addToBackStack(null).commit();
        getFragmentManager().beginTransaction().replace(R.id.resourcesTermsView, mTermFragment).addToBackStack(null).commit();

        return view;
    }

    /**
     * Displays the term page without changing the term and displaying the related terms if no term is selected
     */
    public void showTerm() {
        mNotesFragment.hide();
        mTermFragment.show();
        mTermFragment.showTerm();
    }

    /**
     * Shows the term details
     * @param term
     */
    public void showTerm(Term term) {
        MainContext.getContext().setSelectedKeyTerm(term);
        mNotesFragment.hide();
        mTermFragment.show();
        mTermFragment.showTerm(term);
    }

    /**
     * Displays the translation notes for the current frame.
     * This is the default view
     */
    public void showNotes() {
        MainContext.getContext().setShowImportantTerms(false);
        MainContext.getContext().setSelectedKeyTerm(null);
        mTermFragment.hide();
        mNotesFragment.show();
        Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(p != null && p.getSelectedChapter() != null && p.getSelectedChapter().getSelectedFrame() != null) {
            mNotesFragment.showNotes(p.getSelectedChapter().getSelectedFrame().getTranslationNotes());
        }
    }

    private void scrollToTop() {
        if(mScrollView != null) {
            mScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.smoothScrollTo(0, 0);
                }
            });
        }
    }
}
