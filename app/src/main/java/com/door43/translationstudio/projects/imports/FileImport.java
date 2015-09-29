package com.door43.translationstudio.projects.imports;

/**
 * This is used for manage importing the chapter title and reference
 */
@Deprecated
public class FileImport extends ImportRequest {
    public final String title;
    public final String id;

    public FileImport(String id, String title) {
        this.id = id;
        this.title = title;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
