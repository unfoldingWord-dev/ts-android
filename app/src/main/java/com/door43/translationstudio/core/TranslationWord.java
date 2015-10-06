package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 9/19/2015.
 */
public class TranslationWord {

    private final String mId;
    private final String mWord;
    private final String mDef;
    private final Example[] mExamples;
    private final String[] mAliases;
    private final String[] mSeeAlso;
    private final String mDefTitle;

    /**
     *
     * @param id translationWord id
     * @param word the title of the translationWord
     * @param def the word definition
     * @param defTitle the definition title
     * @param seeAlso id's of related translationWords
     * @param aliases id's of translationWord aliases
     * @param examples examples passages
     */
    private TranslationWord(String id, String word, String def, String defTitle, String[] seeAlso, String[] aliases, Example[] examples) {
        mId = id;
        mWord = word;
        mDef = def;
        mDefTitle = defTitle;
        mSeeAlso = seeAlso;
        mAliases = aliases;
        mExamples = examples;
    }

    /**
     * Generates a translationWord from json
     * @param json
     * @return
     * @throws JSONException
     */
    public static TranslationWord generate(JSONObject json) throws JSONException {
        if(json == null) {
            return null;
        }
        String id = json.getString("id");
        List<Example> examples = new ArrayList<>();
        if(json.has("ex")) {
            JSONArray examplesJson = json.getJSONArray("ex");
            for (int i = 0; i < examplesJson.length(); i++) {
                JSONObject exampleJson = examplesJson.getJSONObject(i);
                try {
                    examples.add(new Example(exampleJson.getString("ref"), exampleJson.getString("text")));
                } catch (InvalidParameterException e) {
                    Logger.e(TranslationWord.class.getName(), "Failed to parse a translation word example for " + id, e);
                }
            }
        }
        String[] aliases = Util.jsonArrayToString(json.getJSONArray("aliases"));
        String[] seeAlso = Util.jsonArrayToString(json.getJSONArray("cf"));
        String word = json.getString("term");
        String def = json.getString("def");
        String defTitle = json.getString("def_title");
        return new TranslationWord(id, word, def, defTitle, seeAlso, aliases, examples.toArray(new Example[examples.size()]));
    }

    /**
     * Returns the title of the translationWord
     * @return
     */
    public String getTitle() {
        return mWord;
    }

    /**
     * Returns the definition of the translationWord
     * @return
     */
    public String getDefinition() {
        return mDef;
    }

    /**
     * Returns the definition title
     * This is used for correctly titling the definition section
     * @return
     */
    public String getDefinitionTitle() {
        return mDefTitle;
    }

    /**
     * Returns the id of this translation word
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns an array of example passages where this translationWord is used
     * @return
     */
    public Example[] getExamples() {
        return mExamples;
    }

    /**
     * Returns an array of id's for aliases for this translationWord
     * @return
     */
    public String[] getAliases() {
        return mAliases;
    }

    /**
     * Returns an array of id's for related translationWords
     * @return
     */
    public String[] getSeeAlso() {
        return mSeeAlso;
    }

    /**
     * Represents an example passage for a translationWord
     */
    public static class Example {
        private final String mReference;
        private final String mPassage;
        private final String mChapterId;
        private final String mFrameId;

        /**
         *
         * @param reference
         * @param passage
         * @throws InvalidParameterException
         */
        public Example(String reference, String passage) throws InvalidParameterException {
            mReference = reference;
            mPassage = passage;
            String[] complexId = reference.split("-");
            if(complexId.length == 2) {
                mChapterId = complexId[0];
                mFrameId = complexId[1];
            } else {
                throw new InvalidParameterException("The reference '" + reference + "' is invalid");
            }
        }

        /**
         * Returns the reference of the example
         * @return
         */
        public String getReference() {
            return mReference;
        }

        /**
         * Returns the passage body of the example
         * @return
         */
        public String getPassage() {
            return mPassage;
        }

        /**
         * Returns the chapter id of the reference
         * @return
         */
        public String getChapterId() {
            return mChapterId;
        }

        /**
         * Returns the frame id of the reference
         * @return
         */
        public String getFrameId() {
            return mFrameId;
        }
    }
}
