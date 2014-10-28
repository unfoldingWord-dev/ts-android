package com.door43.translationstudio.panes.right;

import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 10/23/2014.
 */
public class ResourcesFragment extends TranslatorBaseFragment {
    private TextView mTermName;
    private TextView mTermDescription;
    private TextView mRelatedTerms;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_right_resources, container, false);

        mTermName = (TextView)view.findViewById(R.id.termName);
        mTermName.setText("");
        mTermDescription = (TextView)view.findViewById(R.id.termDescription);
        mTermDescription.setText("");
        mRelatedTerms = (TextView)view.findViewById(R.id.relatedTermsText);
        mRelatedTerms.setText("");


        // make links clickable
        MovementMethod m = mRelatedTerms.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (mRelatedTerms.getLinksClickable()) {
                mRelatedTerms.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        return view;
    }

    /**
     * Shows the term details
     * @param term
     */
    public void showTerm(Term term) {
        mTermName.setText("");
        mTermDescription.setText("");
        mRelatedTerms.setText("");

        mTermName.setText(term.getName());
        mTermDescription.setText(Html.fromHtml(term.getDefinition()));

        // related terms
        int numRelatedTerms = 0;
        Project p = app().getSharedProjectManager().getSelectedProject();
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
                Log.w("Resources", "Unknown term " + related);
            }
        }
        if(numRelatedTerms == 0) {
            // TODO: hide the title text for related terms
        }

        // TODO: set up examples
    }
}
