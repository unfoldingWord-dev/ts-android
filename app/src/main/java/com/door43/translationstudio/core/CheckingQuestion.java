package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a checking question for a frame
 */
public class CheckingQuestion {

    private final Reference[] mReferences;
    private final String mAnswer;
    private final String mQuestion;
    private final String mFrameId;
    private final String mChapterId;
    private final String mId;

    /**
     *
     * @param id
     * @param chapterId
     * @param frameId
     * @param question
     * @param answer
     * @param references
     */
    private CheckingQuestion(String id, String chapterId, String frameId, String question, String answer, Reference[] references) {
        mId = id;
        mChapterId = chapterId;
        mFrameId = frameId;
        mQuestion = question;
        mAnswer = answer;
        mReferences = references;
    }

    /**
     * Generates a new checking question from json
     * @param chapterId
     * @param frameId
     * @param json
     * @return
     * @throws JSONException
     */
    public static CheckingQuestion generate(String chapterId, String frameId, JSONObject json) throws JSONException {
        String id = json.getString("id");
        String question = json.getString("q");
        String answer = json.getString("a");
        String[] rawReferences = Util.jsonArrayToString(json.getJSONArray("ref"));
        List<Reference> references = new ArrayList<>();
        for(String reference:rawReferences) {
            try {
                references.add(new Reference(reference));
            } catch (InvalidParameterException e) {
                e.printStackTrace();
            }
        }
        return new CheckingQuestion(id, chapterId, frameId, question, answer, references.toArray(new Reference[references.size()]));
    }

    /**
     * Returns the question id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the chapter id
     * @return
     */
    public String getChapterId() {
        return mChapterId;
    }

    /**
     * Returns the frame id
     * @return
     */
    public String getFrameId() {
        return mFrameId;
    }

    /**
     * Returns the question
     * @return
     */
    public String getQuestion() {
        return mQuestion;
    }

    /**
     * Returns the answer
     * @return
     */
    public String getAnswer() {
        return mAnswer;
    }

    /**
     * Returns an array of references for this question
     * @return
     */
    public Reference[] getReferences() {
        return mReferences;
    }

    /**
     * Represents a single reference in a question
     */
    public static class Reference {
        private final String mChapterId;
        private final String mFrameId;
        private final String mReference;

        public Reference(String reference) throws InvalidParameterException {
            mReference = reference;
            String[] complexId = reference.split("-");
            if(complexId.length == 2) {
                this.mChapterId = complexId[0];
                this.mFrameId = complexId[1];
            } else {
                throw new InvalidParameterException("The reference '" + reference + "' is invalid");
            }
        }

        /**
         * Returns the raw reference
         * @return
         */
        public String getReference() {
            return this.mReference;
        }

        /**
         * Returns the chapter id of the reference
         * @return
         */
        public String getChapterId() {
            return this.mChapterId;
        }

        /**
         * Returns the frame id of the reference
         * @return
         */
        public String getFrameId() {
            return this.mFrameId;
        }
    }
}
