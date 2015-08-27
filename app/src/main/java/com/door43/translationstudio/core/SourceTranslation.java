package com.door43.translationstudio.core;

/**
 * Represents a single source translation.
 */
public class SourceTranslation {
    public final String projectId;
    public final String sourceLanguageId;
    public final String resourceId;

    public SourceTranslation(String projectId, String sourceLanguageId, String resourceId) {
        this.projectId = projectId;
        this.sourceLanguageId = sourceLanguageId;
        this.resourceId = resourceId;
    }
}
