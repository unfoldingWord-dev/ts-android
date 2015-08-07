package com.door43.translationstudio.panes.right;

import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.astuetz.PagerSlidingTabStrip;
import com.door43.translationstudio.R;
import com.door43.translationstudio.panes.right.tabs.NotesTab;
import com.door43.translationstudio.panes.right.tabs.TermsTab;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.util.TabsAdapter;
import com.door43.translationstudio.util.TabsAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.door43.util.Screen;

/**
 * Created by joel on 8/26/2014.
 */
public class RightPaneFragment extends TranslatorBaseFragment {
    private static final String STATE_TERMS_SCROLL_X = "terms_scroll_x";
    private static final String STATE_TERMS_SCROLL_Y = "terms_scroll_y";
    private ViewPager mViewPager;
    private PagerSlidingTabStrip mSlidingTabLayout;
    private TabsAdapter mTabsAdapter;
    private int mDefaultPage = 0;
    private int mLayoutWidth = 0;
    private View mView;
    private static final int TAB_INDEX_NOTES = 0;
    private static final int TAB_INDEX_TERMS = 1;
    private static final String STATE_NOTES_SCROLL_X = "notes_scroll_x";
    private static final String STATE_NOTES_SCROLL_Y = "notes_scroll_y";
    private static final String STATE_SELECTED_TAB = "selected_tab_index";
    private int mSelectedTab = TAB_INDEX_NOTES;
    private ImageButton mEditButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_pane_right, container, false);
        mEditButton = (ImageButton)mView.findViewById(R.id.editButton);

        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotesTab page = (NotesTab)mTabsAdapter.getFragmentForPosition(TAB_INDEX_NOTES);
                if(page != null) {
                    int btnResource = page.translateNotes();
                    if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        mEditButton.setBackgroundDrawable(getActivity().getResources().getDrawable(btnResource));
                    } else {
                        mEditButton.setBackground(getActivity().getResources().getDrawable(btnResource));
                    }
                }
            }
        });

        // tabs
        mViewPager = (ViewPager) mView.findViewById(R.id.rightViewPager);
        mTabsAdapter = new TabsAdapter(getActivity(), mViewPager);
        mTabsAdapter.addTab(getResources().getString(R.string.label_translation_notes), NotesTab.class, null);
        mTabsAdapter.addTab(getResources().getString(R.string.translation_words), TermsTab.class, null);

        // Sliding tab layout
        mSlidingTabLayout = (PagerSlidingTabStrip) mView.findViewById(R.id.right_sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setTextColorResource(R.color.light_primary_text);
        mSlidingTabLayout.setTextSize(Screen.dpToPx(getActivity(), 20));

        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // TODO: save scroll position
            }

            @Override
            public void onPageSelected(int position) {
                updateNotesEditButton(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if(savedInstanceState != null) {
            // restore scroll position
            mSelectedTab = savedInstanceState.getInt(STATE_SELECTED_TAB, 0);
            mDefaultPage = mSelectedTab;
            NotesTab notesPage = (NotesTab)mTabsAdapter.getFragmentForPosition(TAB_INDEX_NOTES);
            if(notesPage != null) {
                notesPage.setScroll(new Pair<>(savedInstanceState.getInt(STATE_NOTES_SCROLL_X), savedInstanceState.getInt(STATE_NOTES_SCROLL_Y)));
            }
            TermsTab termsPage = (TermsTab)mTabsAdapter.getFragmentForPosition(TAB_INDEX_TERMS);
            if(termsPage != null) {
                termsPage.setScroll(new Pair<>(savedInstanceState.getInt(STATE_TERMS_SCROLL_X), savedInstanceState.getInt(STATE_TERMS_SCROLL_Y)));
            }
        }

        // open the default page
        selectTab(mDefaultPage);

        if(mLayoutWidth != 0) {
            mView.setLayoutParams(new ViewGroup.LayoutParams(mLayoutWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        return mView;
    }

    private void updateNotesEditButton(int position) {
        if(position == TAB_INDEX_NOTES) {
            mEditButton.setVisibility(View.VISIBLE);
        } else {
            mEditButton.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the selected tab index
     * @param i
     */
    public void selectTab(int i) {
        if(mViewPager != null) {
            mViewPager.setCurrentItem(i);
            TabsAdapterNotification page = (TabsAdapterNotification)mTabsAdapter.getFragmentForPosition(i);
            if(page != null) {
                page.NotifyAdapterDataSetChanged();
            }

            updateNotesEditButton(i);
        } else {
            mDefaultPage = i;
        }
        mSelectedTab = i;
    }

    /**
     * Returns the currently selected tab index
     * @return
     */
    public int getSelectedTabIndex() {
        return mViewPager.getCurrentItem();
    }

    /**
     * Notifies the notes adapter that the dataset has changed
     */
    public void reloadNotesTab() {
        if(mTabsAdapter != null) {
            TabsAdapterNotification page = (TabsAdapterNotification)mTabsAdapter.getFragmentForPosition(TAB_INDEX_NOTES);
            if(page != null) {
                page.NotifyAdapterDataSetChanged();
            }
        }
    }

    /**
     * Notifies the terms adapter that the dataset has changed
     */
    public void reloadTermsTab() {
        if(mTabsAdapter != null) {
            TabsAdapterNotification page = (TabsAdapterNotification)mTabsAdapter.getFragmentForPosition(TAB_INDEX_TERMS);
            if(page != null) {
                page.NotifyAdapterDataSetChanged();
            }
        }
    }

    /**
     * @param term
     */
    public void showTerm(Term term) {
        selectTab(TAB_INDEX_TERMS);
        TermsTab page = (TermsTab)mTabsAdapter.getFragmentForPosition(TAB_INDEX_TERMS);
        if(page != null) {
            page.NotifyAdapterDataSetChanged();
            if(term == null) {
                page.showTerms();
            } else {
                page.showTerm(term);
            }
        }
    }

    /**
     * Specifies the width of the layout
     * @param width
     */
    public void setLayoutWidth(int width) {
        if(mView != null) {
            mView.setLayoutParams(new ViewGroup.LayoutParams(mLayoutWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            mLayoutWidth = width;
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        NotesTab notesPage = (NotesTab)mTabsAdapter.getFragmentForPosition(TAB_INDEX_NOTES);
        if(notesPage != null) {
            Pair scrollPosition = notesPage.getScroll();
            outState.putInt(STATE_NOTES_SCROLL_X, (int)scrollPosition.first);
            outState.putInt(STATE_NOTES_SCROLL_Y, (int)scrollPosition.second);
        }
        TermsTab termsPage = (TermsTab)mTabsAdapter.getFragmentForPosition(TAB_INDEX_TERMS);
        if(termsPage != null) {
            Pair scrollPosition = termsPage.getScroll();
            outState.putInt(STATE_TERMS_SCROLL_X, (int)scrollPosition.first);
            outState.putInt(STATE_TERMS_SCROLL_Y, (int)scrollPosition.second);
        }
        outState.putInt(STATE_SELECTED_TAB, mSelectedTab);
    }
}
