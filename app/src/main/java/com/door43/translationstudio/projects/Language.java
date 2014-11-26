package com.door43.translationstudio.projects;

/**
 * Created by joel on 9/5/2014.
 */
public class Language {
    private String mCode;
    private String mName;
    private Direction mDirection;
    private int mSessionVersion; // this is used for target language

    public enum Direction {
        LeftToRight, RightToLeft
    }

    /**
     * Create a new language
     * @param code the language code
     * @param name the name of the language
     * @param direction the text direction
     */
    public Language(String code, String name, Direction direction) {
        mCode = code;
        mName = name;
        mDirection = direction;
    }

    /**
     * Returns the text direction
     * @return
     */
    public Direction getDirection() {
        return mDirection;
    }

    /**
     * Returns the language code
     * @return
     */
    public String getId() {
        return mCode;
    }

    /**
     * Returns the human readable language name
     * @return
    */
    public String getName() {
        return mName;
    }

    /**
     * Increases the session version so that cached translations in this language will become invalid and re-loaded from the disk
     */
    public void touch() {
        mSessionVersion ++;
    }

    /**
     * Returns the session version of the target language
     * @return
     */
    public int getSessionVersion() {
        return mSessionVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() == Language.class) {
            return ((Language)obj).getId().equals(getId());
        }
        return false;
    }
}
