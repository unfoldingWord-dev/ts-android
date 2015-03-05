package com.door43.translationstudio.rendering;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 1/26/2015.
 */
public class RenderingGroup {
    private boolean mStopped = false;
    private boolean mRunning = false;
    private List<RenderingEngine> mEngines =  new ArrayList<RenderingEngine>();
    private CharSequence mInput;

    /**
     * Adds a rendering engine to the queue
     * @param engine
     */
    public void addEngine(RenderingEngine engine) {
        mEngines.add(engine);
    }

    /**
     * Begins the rendering operations
     */
    public CharSequence start() {
        if(mRunning || mInput == null) return "";
        mRunning = true;
        mStopped = false;
        CharSequence rendered = mInput;
        for(RenderingEngine engine:mEngines) {
            if(mStopped) break;
            rendered = engine.render(rendered);
        }
        mRunning = false;
        return rendered;
    }

    /**
     * Stops the rendering operations
     */
    public void stop() {
        mStopped = true;
        for(RenderingEngine engine:mEngines) {
            engine.stop();
        }
    }

    /**
     * Initializes the rendering group
     * @param input
     */
    public void init(String input) {
        mInput = input;
    }
}
