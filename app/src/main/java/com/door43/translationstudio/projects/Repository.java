package com.door43.translationstudio.projects;

/**
 * Created by joel on 5/17/2015.
 */
@Deprecated
public interface Repository {
    SourceLanguage getSelectedSourceLanguage();
    Language getSelectedTargetLanguage();
    boolean translationIsReady();
}
