package com.door43.translationstudio.projects;

/**
 * Created by joel on 12/15/2014.
 */
public class SourceLanguage extends Language {
    private String mVariant;
    private final int mDateModified;

    public SourceLanguage(String code, String name, Direction direction, String variant,  int dateModified) {
        super(code, name, direction);
        if(variant != null && variant.isEmpty()) variant = null;
        mVariant = variant;
        mDateModified = dateModified;
    }

    /**
     * Returns the combination of language code and variant name.
     * If there is no variant then just the language id is returned.
     * @return
     */
    public String getVariantId() {
        if(mVariant != null) {
            return getId() + "_" + mVariant;
        } else {
            return getId();
        }
    }

    /**
     * Returns the timestamp when the language was last modified
     * @return
     */
    public int getDateModified() {
        return mDateModified;
    }
}
