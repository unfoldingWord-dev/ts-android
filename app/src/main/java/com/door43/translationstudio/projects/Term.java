package com.door43.translationstudio.projects;

import android.text.Html;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 10/27/2014.
 */
public class Term {
    private String mName;
    private String mDefinition;
    private List<String> mRelated = new ArrayList<String>();
    private List<Example> mExamples = new ArrayList<Example>();

    public Term(String name, String definition, List<String> related, List<Example> examples) {
        mName = name;
        mDefinition = Html.fromHtml(definition).toString();
        mRelated = related;
        mExamples = examples;
    }

    /**
     * Returns the name of the term
     * @return
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns a list of related terms
     * @return
     */
    public List<String> getRelatedTerms() {
        return mRelated;
    }

    /**
     * Returns the number of terms related to this one.
     * @return
     */
    public int numRelatedTerms() {
        return mRelated.size();
    }

    /**
     * Returns the definition of the term
     * @return
     */
    public String getDefinition() {
        return mDefinition;
    }

    public static class Example {
        private String mChapterId;
        private String mFrameId;
        private String mText;

        public Example(String chapterId, String frameId, String text) {
            mChapterId = chapterId;
            mFrameId = frameId;
            mText = text;
        }
    }
}
