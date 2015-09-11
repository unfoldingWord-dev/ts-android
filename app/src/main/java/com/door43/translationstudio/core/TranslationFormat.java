package com.door43.translationstudio.core;

/**
 * Represents different text formats
 */
public enum TranslationFormat {
    DEFAULT("default"),
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
        for(TranslationFormat f : TranslationFormat.values()) {
            if(f.getName().equals(name.toLowerCase())) {
                return f;
            }
        }
        return null;
    }
}
