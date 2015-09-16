package com.door43.translationstudio.core;

/**
 * Represents different visual modes for target translations.
 * These modes are used to keep track of where the user was at in the ui
 */
public enum TranslationViewMode {
    READ("read"),
    CHUNK("chunk"),
    REVIEW("review");

    TranslationViewMode(String s) {
        mId = s;
    }

    private final String mId;

    @Override
    public String toString() {
        return mId;
    }

    /**
     * Returns a view mode by it's id
     * @param id
     * @return
     */
    public static TranslationViewMode get(String id) {
        if(id != null) {
            for (TranslationViewMode f : TranslationViewMode.values()) {
                if (f.toString().equals(id.toLowerCase())) {
                    return f;
                }
            }
        }
        return null;
    }
}