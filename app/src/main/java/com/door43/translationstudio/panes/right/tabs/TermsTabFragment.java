package com.door43.translationstudio.panes.right.tabs;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
  * Created by joel on 2/12/2015.
  */
 public class TermsTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
    private TextView mTermName;
    private WebView mTermDescriptionWebView;
    private TextView mRelatedTerms;
    private TextView mRelatedTermsTitle;
    private TextView mExamplePassagesTitle;
    private LinearLayout mExamplePassagesView;
    private View mTermLayout;
    private ListView mImportantTermsList;
    private Button mImportantTermsButton;
    private boolean mIsLoaded = false;
    private ScrollView mTermInfoScroll;
    private TextView mTermsMessageText;
    private View mTermsLayout;
    private ImportantTermsAdapter mTermsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_right_resources_term, container, false);

        mTermName = (TextView)view.findViewById(R.id.termName);
        mRelatedTermsTitle = (TextView)view.findViewById(R.id.relatedTermsTitleText);
        mExamplePassagesView = (LinearLayout)view.findViewById(R.id.examplePassagesView);
        mExamplePassagesTitle = (TextView)view.findViewById(R.id.examplePassagesTitleText);
        mTermDescriptionWebView = (WebView)view.findViewById(R.id.termDescriptionText);
        mRelatedTerms = (TextView)view.findViewById(R.id.relatedTermsText);
        mImportantTermsButton = (Button)view.findViewById(R.id.importantTermsButton);
        mTermInfoScroll = (ScrollView)view.findViewById(R.id.termInfoScroll);
        mTermsMessageText = (TextView)view.findViewById(R.id.termsMessageText);
        mTermsLayout = view.findViewById(R.id.keyTermsLayout);

        mTermLayout = view.findViewById(R.id.termLayout);
        mImportantTermsList = (ListView)view.findViewById(R.id.importantTermsList);

        // hook up adapter for key terms
        mTermsAdapter = new ImportantTermsAdapter(this.getActivity(), new ArrayList(){});
        mImportantTermsList.setAdapter(mTermsAdapter);

        mImportantTermsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Project p = app().getSharedProjectManager().getSelectedProject();
                if(p != null) {
                    Term t = p.getTerm(mTermsAdapter.getItem(i));
                    showTerm(t);
                }
            }
        });

        // return to the important terms view
        mImportantTermsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainContext.getContext().setSelectedKeyTerm(null); // needed for device rotations.
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
        Project p = app().getSharedProjectManager().getSelectedProject();
        if(p != null) {
            Chapter c = p.getSelectedChapter();
            if(c != null) {
                Frame f = c.getSelectedFrame();
                if(f != null) {
                    if(!mIsLoaded) return;

                    // load the terms
                    mTermLayout.setVisibility(View.GONE);
                    mImportantTermsList.setVisibility(View.VISIBLE);
                    mImportantTermsList.scrollTo(0, 0);

                    mTermsAdapter.setTermsList(f.getImportantTerms());

                    if(f.getImportantTerms().size() > 0) {
                        mTermsLayout.setVisibility(View.VISIBLE);
                        mTermsMessageText.setVisibility(View.GONE);
                    } else {
                        mTermsLayout.setVisibility(View.GONE);
                        mTermsMessageText.setVisibility(View.VISIBLE);
                    }
                    return;
                }
            }
        }

        // no terms are available
        if(mIsLoaded) {
            mTermsLayout.setVisibility(View.GONE);
            mTermsMessageText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows the term details
     * @param term the term to display
     */
    public void showTerm(Term term) {
        // TODO: this should load asynchronously with a loading indicator
        final Project p = app().getSharedProjectManager().getSelectedProject();
        if(term != null && mIsLoaded && p != null) {
            MainContext.getContext().setShowImportantTerms(false);

            // hide the no term message
            mTermsLayout.setVisibility(View.VISIBLE);
            mTermsMessageText.setVisibility(View.GONE);

            mTermLayout.setVisibility(View.VISIBLE);
            mImportantTermsList.setVisibility(View.GONE);
            mTermInfoScroll.scrollTo(0, 0);

            mRelatedTerms.setText("");
            mTermName.setText(term.getName());
            mTermDescriptionWebView.setVisibility(View.GONE);
            mTermDescriptionWebView.loadData(term.getDefinition(), "text/html", null);
            mTermDescriptionWebView.reload();
            mTermDescriptionWebView.setVisibility(View.VISIBLE);
            mExamplePassagesView.removeAllViews();

            // related terms
            int numRelatedTerms = 0;
            for(final String related:term.getRelatedTerms()) {
                final Term relatedTerm = p.getTerm(related);
                if(relatedTerm != null) {
                    TermSpan span = new TermSpan(relatedTerm.getName(), relatedTerm.getName());
                    span.setOnClickListener(new Span.OnClickListener() {
                        @Override
                        public void onClick(View view, Span span, int start, int end) {
                            showTerm(relatedTerm);
                        }
                    });
                    mRelatedTerms.append(span.toCharSequence());
                    numRelatedTerms++;
                    if(numRelatedTerms < term.numRelatedTerms()) {
                        mRelatedTerms.append(", ");
                    }
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
                    LinearLayout exampleView = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.fragment_pane_right_resources_example_item, null);

                    // link
                    TextView linkText = (TextView)exampleView.findViewById(R.id.examplePassageLinkText);
                    final String linkName = "[" + example.getChapterId() + "-" + example.getFrameId() + "]";
                    TermSpan span = new TermSpan(linkName, linkName);
                    span.setOnClickListener(new Span.OnClickListener() {
                        @Override
                        public void onClick(View view, Span span, int start, int end) {
                            p.setSelectedChapter(example.getChapterId());
                            p.getSelectedChapter().setSelectedFrame(example.getFrameId());
                            ((MainActivity) getActivity()).reloadCenterPane();
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
                    TextView passageText = (TextView)exampleView.findViewById(R.id.examplePassageText);
                    passageText.setText(Html.fromHtml(example.getText()));

                    mExamplePassagesView.addView(exampleView);
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

    @Override
    public void NotifyAdapterDataSetChanged() {
        showTerms();
    }
}
