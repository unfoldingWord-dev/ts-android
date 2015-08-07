package com.door43.translationstudio.panes.right.tabs;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.TermSpan;
import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TabsAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.door43.util.Screen;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;

/**
  * Created by joel on 2/12/2015.
  */
 public class TermsTab extends TranslatorBaseFragment implements TabsAdapterNotification {
//    private TextView mTermName;
    private HtmlTextView mTermDescriptionView;
    private TextView mRelatedTerms;
    private TextView mRelatedTermsTitle;
    private TextView mExamplePassagesTitle;
    private LinearLayout mExamplePassagesView;
    private View mTermInfoLayout;
    private ListView mTermsListLayout;
    private Button mImportantTermsButton;
    private boolean mIsLoaded = false;
    private ScrollView mTermInfoScroll;
    private TextView mTermsMessageText;
    private View mTermsLayout;
    private ImportantTermsAdapter mTermsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_right_resources_term, container, false);

//        mTermName = (TextView)view.findViewById(R.id.termName);
        mRelatedTermsTitle = (TextView)view.findViewById(R.id.relatedTermsTitleText);
        mExamplePassagesView = (LinearLayout)view.findViewById(R.id.examplePassagesView);
        mExamplePassagesTitle = (TextView)view.findViewById(R.id.examplePassagesTitleText);
        mTermDescriptionView = (HtmlTextView)view.findViewById(R.id.termDescriptionText);
        mRelatedTerms = (TextView)view.findViewById(R.id.relatedTermsText);
        mImportantTermsButton = (Button)view.findViewById(R.id.importantTermsButton);
        mTermInfoScroll = (ScrollView)view.findViewById(R.id.termInfoScroll);
        mTermsMessageText = (TextView)view.findViewById(R.id.termsMessageText);
        mTermsLayout = view.findViewById(R.id.termsLayout);

        mTermInfoLayout = view.findViewById(R.id.termInfoLayout);
        mTermsListLayout = (ListView)view.findViewById(R.id.termsListLayout);

        // hook up adapter for key terms

        mTermsAdapter = new ImportantTermsAdapter(this.getActivity(), new ArrayList<String>(){});
        mTermsListLayout.setAdapter(mTermsAdapter);

        mTermsListLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Project p = AppContext.projectManager().getSelectedProject();
                if (p != null) {
                    Term t = p.getTerm(mTermsAdapter.getItem(i));
                    showTerm(t);
                }
            }
        });

        // return to the important terms view
        mImportantTermsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppContext.context().setSelectedKeyTerm(null); // needed for device rotations.
                showTerms();
            }
        });

        // make related links clickable
        MovementMethod movementMethodRelatedTerms = mRelatedTerms.getMovementMethod();
        if ((movementMethodRelatedTerms == null) || !(movementMethodRelatedTerms instanceof LinkMovementMethod)) {
            if (mRelatedTerms.getLinksClickable()) {
                mRelatedTerms.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        mIsLoaded = true;

        showTerms();

        return view;
    }

    /**
     * Shows the term details.
     * Will show the related terms of the current term if one has already been selected.
     */
    public void showTerms() {
        Project p = AppContext.projectManager().getSelectedProject();
        if(p != null) {
            Chapter c = p.getSelectedChapter();
            if(c != null) {
                Frame f = c.getSelectedFrame();
                if(f != null) {
                    if(!mIsLoaded) return;

                    // load the terms
                    toggleTermsList(true);
                    mTermsAdapter.setTermsList(f.getImportantTerms());

                    if(f.getImportantTerms().size() > 0) {
                        return;
                    }
                }
            }
        }

        // no terms are available
        if(mIsLoaded) {
            toggleMissingNotice(true);
        }
    }

    /**
     * Shows the term details
     * @param term the term to display
     */
    public void showTerm(Term term) {
        // TODO: this should load asynchronously with a loading indicator
        final Project p = AppContext.projectManager().getSelectedProject();
        if(term != null && mIsLoaded && p != null) {
            AppContext.context().setShowImportantTerms(false);

            toggleTermDetails(true);

            mRelatedTerms.setText("");
            mExamplePassagesView.removeAllViews();
            mImportantTermsButton.setText(term.getName());
            mTermDescriptionView.setHtmlFromString(term.getDefinition(), true);

            // related terms
            int numRelatedTerms = 0;
            for(final String related:term.getRelatedTerms()) {
                final Term relatedTerm = p.getTerm(related);
                if(relatedTerm != null) {
                    if(mRelatedTerms.length() > 0) {
                        mRelatedTerms.append(", ");
                    }

                    TermSpan span = new TermSpan(relatedTerm.getName(), relatedTerm.getName());
                    span.setOnClickListener(new Span.OnClickListener() {
                        @Override
                        public void onClick(View view, Span span, int start, int end) {
                            showTerm(relatedTerm);
                        }
                    });
                    mRelatedTerms.append(span.toCharSequence());
                    numRelatedTerms++;
                } else {
                    Logger.w(this.getClass().getName(), "unknown term "+related);
                }
            }
            if(numRelatedTerms == 0) {
                mRelatedTermsTitle.setVisibility(View.GONE);
            } else {
                mRelatedTermsTitle.setVisibility(View.VISIBLE);
            }

            // example passages
            if(term.numExamples() > 0) {
                mExamplePassagesTitle.setVisibility(View.VISIBLE);
                for (final Term.Example example : term.getExamples()) {
                    if(getActivity() != null) {
                        LinearLayout exampleView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_pane_right_resources_example_item, null);

                        // link
                        TextView linkText = (TextView) exampleView.findViewById(R.id.examplePassageLinkText);
                        final String linkName = "[" + example.getChapterId() + "-" + example.getFrameId() + "]";
                        TermSpan span = new TermSpan(linkName, linkName);
                        span.setOnClickListener(new Span.OnClickListener() {
                            @Override
                            public void onClick(View view, Span span, int start, int end) {
                                // TODO: this should be done in the navigator
                                p.setSelectedChapter(example.getChapterId());
                                p.getSelectedChapter().setSelectedFrame(example.getFrameId());
                                // TODO: we need to load the chapters and frames first.
                                ((MainActivity) getActivity()).reload();
                                ((MainActivity) getActivity()).closeDrawers();
                                Chapter c = p.getChapter(example.getChapterId());
                                if (c != null) {
                                    app().showToastMessage(String.format(getResources().getString(R.string.now_viewing_frame), example.getFrameId(), c.getTitle()));
                                } else {
                                    app().showToastMessage(R.string.missing_chapter);
                                }
                            }
                        });
                        linkText.setText(span.toCharSequence());

                        // make links clickable
                        MovementMethod m = linkText.getMovementMethod();
                        if ((m == null) || !(m instanceof LinkMovementMethod)) {
                            if (linkText.getLinksClickable()) {
                                linkText.setMovementMethod(LinkMovementMethod.getInstance());
                            }
                        }

                        // passage
                        HtmlTextView passageText = (HtmlTextView) exampleView.findViewById(R.id.examplePassageText);
                        passageText.setHtmlFromString(example.getText(), true);

                        mExamplePassagesView.addView(exampleView);
                    } else {
                        Logger.e(this.getClass().getName(), "showTerm the activity is null");
                    }
                }
                mExamplePassagesView.setVisibility(View.VISIBLE);
            } else {
                mExamplePassagesTitle.setVisibility(View.GONE);
                mExamplePassagesView.setVisibility(View.GONE);
            }
        } else if(mIsLoaded) {
            showTerms();
        }
    }

    /**
     * Displays the terms list
     * @param show
     */
    private void toggleTermsList(boolean show) {
        if(show) {
            toggleMissingNotice(false);
            toggleTermDetails(false);
            mTermsListLayout.setVisibility(View.VISIBLE);
            mTermsListLayout.scrollTo(0, 0);
        } else {
            mTermsListLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Displays the term details page
     * @param show
     */
    private void toggleTermDetails(boolean show) {
        if(show) {
            toggleMissingNotice(false);
            toggleTermsList(false);
            mTermInfoLayout.setVisibility(View.VISIBLE);
            mTermInfoScroll.scrollTo(0, 0);
        } else {
            mTermInfoLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Displays a notice that there are no terms
     */
    private void toggleMissingNotice(boolean show) {
        if(show) {
            mTermsMessageText.setVisibility(View.VISIBLE);
            mTermsLayout.setVisibility(View.GONE);
        } else {
            mTermsMessageText.setVisibility(View.GONE);
            mTermsLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        showTerms();
    }
}
