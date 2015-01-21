package com.door43.translationstudio.projects.imports;

import java.io.File;

/**
 * This manages importing a frame
 */
public class FrameImport extends ImportRequest {
    public final String frameId;
    public final String frameTitle;

    public FrameImport(String frameId, String frameTitle) {
        this.frameId = frameId;
        this.frameTitle = frameTitle;
    }

    @Override
    public String getId() {
        return frameId;
    }

    @Override
    public String getTitle() {
        return frameTitle;
    }
}
