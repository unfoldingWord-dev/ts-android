package com.door43.translationstudio.panes.right;



import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.projects.TranslationNote;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class KeyTermFragment extends TranslatorBaseFragment {
    private TextView mTermName;
    private WebView mTermDescriptionWebView;
    private TextView mRelatedTerms;
    private TextView mRelatedTermsTitle;
    private TextView mExamplePassagesTitle;
    private LinearLayout mExamplePassagesView;
    private View mainView;
    private boolean mStartHidden = true;
    private Term mTerm;
    private Handler.Callback mOnShowCallback;
    private TextView mImportantTerms;
    private TextView mImportantTermsTitle;
    private View mTermLayout;
    private View mImportantTermsLayout;
    private Button mImportantTermsButton;
    private boolean mHasLoaded = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_right_resources_term, container, false);

        mTermName = (TextView)view.findViewById(R.id.termName);
        mRelatedTermsTitle = (TextView)view.findViewById(R.id.relatedTermsTitleText);
        mExamplePassagesView = (LinearLayout)view.findViewById(R.id.examplePassagesView);
        mExamplePassagesTitle = (TextView)view.findViewById(R.id.examplePassagesTitleText);
        mTermDescriptionWebView = (WebView)view.findViewById(R.id.termDescriptionWebView);
        mRelatedTerms = (TextView)view.findViewById(R.id.relatedTermsText);
        mainView = view.findViewById(R.id.keyTermsLayout);
        mImportantTermsButton = (Button)view.findViewById(R.id.importantTermsButton);

        // these are for the default page
        mImportantTermsTitle = (TextView)view.findViewById(R.id.importantTermsTitleText);
        mImportantTermsTitle.setText(R.string.translation_notes_important_terms_title);
        mImportantTerms = (TextView)view.findViewById(R.id.importantTermsText);

        mTermLayout = view.findViewById(R.id.termLayout);
        mImportantTermsLayout = view.findViewById(R.id.importantTermsLayout);

        // return to the important terms view
        mImportantTermsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTerm = null;
                MainContext.getContext().setSelectedKeyTerm(null); // needed for device rotations.
                showTerm();
            }
        });

        // make important term links clickable
        MovementMethod movementMethodImportantTerms = mImportantTerms.getMovementMethod();
        if ((movementMethodImportantTerms == null) || !(movementMethodImportantTerms instanceof LinkMovementMethod)) {
            if (mImportantTerms.getLinksClickable()) {
                mImportantTerms.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        // make related links clickable
        MovementMethod movementMethodRelatedTerms = mRelatedTerms.getMovementMethod();
        if ((movementMethodRelatedTerms == null) || !(movementMethodRelatedTerms instanceof LinkMovementMethod)) {
            if (mRelatedTerms.getLinksClickable()) {
                mRelatedTerms.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        mHasLoaded = true;

        if(mStartHidden) {
            hide();
        } else {
            showTerm();
        }

        return view;
    }

    public void hide() {
        // TODO: fade out
        if(mainView != null) {
            mainView.setVisibility(View.GONE);
        }
    }

    public void show() {
        if(mainView != null) {
            mainView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows the term details.
     * Will show the related terms of the current term if one has already been selected.
     */
    public void showTerm() {
        if(mTerm != null) {
            showTerm(mTerm);
        } else {
            // Show the important terms
            MainContext.getContext().setShowImportantTerms(true);

            // let things load first
            if(!mHasLoaded) {
                mStartHidden = false;
                return;
            }

            final Project p = app().getSharedProjectManager().getSelectedProject();
            if(p != null && p.getSelectedChapter() != null && p.getSelectedChapter().getSelectedFrame() != null) {
                Frame frame = p.getSelectedChapter().getSelectedFrame();
                TranslationNote note = frame.getTranslationNotes();

                mTermLayout.setVisibility(View.GONE);
                mImportantTermsLayout.setVisibility(View.VISIBLE);

                mImportantTerms.setText("");
                int numImportantTerms = 0;

                // show the related terms for this frame
                for (String term : frame.getImportantTerms()) {
                    final Term importantTerm = p.getTerm(term);
                    if (importantTerm != null) {
                        final String termName = term;
                        SpannableString link = new SpannableString(importantTerm.getName());
                        ClickableSpan cs = new ClickableSpan() {
                            @Override
                            public void onClick(View widget) {
                                ((MainActivity) getActivity()).showTermDetails(termName);
                            }
                        };
                        link.setSpan(cs, 0, importantTerm.getName().length(), 0);
                        mImportantTerms.append(link);
                    } else {
                        mImportantTerms.append(term);
                    }
                    numImportantTerms++;
                    if (numImportantTerms < frame.getImportantTerms().size()) {
                        mImportantTerms.append(", ");
                    }
                }
                onShow();
            } else {
                // the frame is not selected
            }
        }
    }

    /**
     * Shows the term details
     * @param term the term to display
     */
    public void showTerm(Term term) {
        if(term == null) {
            app().showToastMessage(getResources().getString(R.string.error_term_missing));
            ((MainActivity)getActivity()).closeDrawers();
            return;
        }

        mTerm = term;

        MainContext.getContext().setShowImportantTerms(false);

        // let things load first
        if(!mHasLoaded) {
            mStartHidden = false;
            return;
        }

        mTermLayout.setVisibility(View.VISIBLE);
        mImportantTermsLayout.setVisibility(View.GONE);


        final Project p = app().getSharedProjectManager().getSelectedProject();
        if(p == null) {
            hide();
            return;
        };
        mRelatedTerms.setText("");
        mTermName.setText(term.getName());
        mTermDescriptionWebView.setVisibility(View.GONE);
        mTermDescriptionWebView.loadData(term.getDefinition(), "text/html", null);
        mTermDescriptionWebView.reload();
        mTermDescriptionWebView.setVisibility(View.VISIBLE);
        mExamplePassagesView.removeAllViews();

        // related terms
        int numRelatedTerms = 0;
        for(String related:term.getRelatedTerms()) {
            final Term relatedTerm = p.getTerm(related);
            if(relatedTerm != null) {
                SpannableString link = new SpannableString(relatedTerm.getName());
                ClickableSpan cs = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        showTerm(relatedTerm);
                    }
                };
                link.setSpan(cs, 0, relatedTerm.getName().length(), 0);
                mRelatedTerms.append(link);
                numRelatedTerms++;
                if(numRelatedTerms < term.numRelatedTerms()) {
                    mRelatedTerms.append(", ");
                }
            } else {
//                Log.w("Resources", "Unknown term " + related);
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
                SpannableString link = new SpannableString(linkName);
                ClickableSpan cs = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        p.setSelectedChapter(example.getChapterId());
                        p.getSelectedChapter().setSelectedFrame(example.getFrameId());
                        ((MainActivity)getActivity()).reloadCenterPane();
                        ((MainActivity)getActivity()).closeDrawers();
                        Chapter c = p.getChapter(example.getChapterId());
                        if(c != null) {
                            app().showToastMessage(String.format(getResources().getString(R.string.now_viewing_frame), example.getFrameId(), c.getTitle()));
                        } else {
                            app().showToastMessage(R.string.missing_chapter);
                        }
                    }
                };
                link.setSpan(cs, 0, linkName.length(), 0);
                linkText.setText(link);

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

        onShow();
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
