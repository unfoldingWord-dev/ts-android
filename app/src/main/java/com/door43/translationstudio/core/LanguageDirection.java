package com.door43.translationstudio.core;

/**
 * Specifies the direction in which a language is read
 */
@Deprecated
public enum LanguageDirection {
    LeftToRight("ltr"),
    RightToLeft("rtl");

    LanguageDirection(String label) {
        this.label = label;
    }

    private String label;

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    /**
     * Returns a direction by it's label
     * @param label
     * @return
     */
    public static LanguageDirection get(String label) {
        if(label != null) {
            for (LanguageDirection l : LanguageDirection.values()) {
                if (l.getLabel().equals(label.toLowerCase())) {
                    return l;
                }
            }
        }
        return null;
    }
}
