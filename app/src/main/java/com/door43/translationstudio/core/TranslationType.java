package com.door43.translationstudio.core;

/**
 * Represents different translation types
 */
@Deprecated
public enum TranslationType {
    TEXT("text", "Text"),
    TRANSLATION_NOTE("tn", "Notes"),
    TRANSLATION_QUESTION("tq", "Questions"),
    TRANSLATION_WORD("tw", "Words"),
    TRANSLATION_ACADEMY("ta", "Translation Academy");

    TranslationType(String id, String name) {
        mId = id;
        mName = name;
    }

    private final String mId;
    private final String mName;

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    @Override
    public String toString() {
        return mId;
    }

    /**
     * Returns a format by it's name
     * @param name
     * @return
     */
    public static TranslationType get(String name) {
        if(name != null) {
            for (TranslationType f : TranslationType.values()) {
                if (f.getId().equals(name.toLowerCase())) {
                    return f;
                }
            }
        }
        return null;
    }
}
