package com.door43.translationstudio.projects.imports;

/**
 * This manages importing a chapter
 */
@Deprecated
public class ChapterImport extends ImportRequest {
    public final String chapterId;
    public final String chapterTitle;

    public ChapterImport(String id, String title) {
        chapterId = id;
        chapterTitle = title;
    }

    /**
     * Adds a frame import request to this chapter
     * @param request
     */
    public void addFrameImport(FrameImport request) {
        super.addChildImportRequest(request);
    }

    /**
     * Adds a file import request to this chapter
     * @param request
     */
    public void addFileImport(FileImport request) {
        super.addChildImportRequest(request);
    }

    @Override
    public String getId() {
        return chapterId;
    }

    @Override
    public String getTitle() {
        return chapterTitle;
    }
}
