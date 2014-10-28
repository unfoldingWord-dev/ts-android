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
        mDefinition = definition; // NOTE: if the input html has been encoded to html entities we'll need to parse this. Html.fromHtml()
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
     * Returns a list of example passages
     * @return
     */
    public List<Example> getExamples() {
        return mExamples;
    }

    /**
     * Returns the number of examples passages for this term
     * @return
     */
    public int numExamples() {
        return mExamples.size();
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

        /**
         * Returns the related chapter id
         * @return
         */
        public String getChapterId() {
            return mChapterId;
        }

        /**
         * Returns the related frame id
         * @return
         */
        public String getFrameId() {
            return mFrameId;
        }

        /**
         * Returns the passage text
         * @return
         */
        public String getText() {
            return mText;
        }
    }
}
