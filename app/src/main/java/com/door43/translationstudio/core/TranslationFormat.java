package com.door43.translationstudio.core;

/**
 * Represents different text formats
 */
public enum TranslationFormat {
    USFM("usfm"),
    MARKDOWN("markdown"),
    UNKNOWN("txt"),
    @Deprecated
    DEFAULT("default"),
    @Deprecated
    USX("usx");

    TranslationFormat(String s) {
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
    public static TranslationFormat get(String name) {
        if(name != null) {
            for (TranslationFormat f : TranslationFormat.values()) {
                if (f.getName().equals(name.toLowerCase())) {
                    return f;
                }
            }
        }
        return null;
    }

    public static TranslationFormat parse(String mimeType) {
        switch(mimeType) {
            case "text/usfm":
                return USFM;
            case "text/markdown":
                return MARKDOWN;
            default:
                // you are crazy!!!
                return UNKNOWN;
        }
    }
}
