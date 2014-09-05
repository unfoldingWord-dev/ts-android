package com.door43.translationstudio.projects;

/**
 * Created by joel on 9/5/2014.
 */
public class Language {
    private String mCode;
    private String mName;
    private Direction mDirection;

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
    public String getCode() {
        return mCode;
    }

    /**
     * Returns the human readable language name
     * @return
     */
    public String getName() {
        return mName;
    }
}
