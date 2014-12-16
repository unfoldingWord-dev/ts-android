package com.door43.translationstudio.projects;

/**
 * Created by joel on 12/15/2014.
 */
public interface LanguageInterface {
    public String getId();
    public String getName();
    public boolean isTranslating(final Project project);

}
