package com.door43.translationstudio.tasks;

import com.door43.util.threads.ManagedTask;

/**
 * Created by joel on 3/9/2015.
 */
public class DownloadProjectSourceTask extends ManagedTask {

    @Override
    public void start() {
        // TODO: begin executing code
        onProgress(1, "downloading projects source");
    }
}
