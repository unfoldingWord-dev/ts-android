package com.door43.translationstudio.core;

/**
 * Represents different translation types
 */
public enum TranslationType {
    TEXT("text"),
    TRANSLATION_NOTE("tn"),
    TRANSLATION_QUESTION("tq"),
    TRANSLATION_WORD("tw"),
    TRANSLATION_ACADEMY("ta");

    TranslationType(String s) {
        mName = s;
    }

    private final String mName;

    public String getName() {
        return mName;
    }

    @Override
    public String toString() {
        return mName;
    }

    /**
     * Returns a format by it's name
     * @param name
     * @return
     */
    public static TranslationType get(String name) {
        if(name != null) {
            for (TranslationType f : TranslationType.values()) {
                if (f.getName().equals(name.toLowerCase())) {
                    return f;
                }
            }
        }
        return null;
    }
}
