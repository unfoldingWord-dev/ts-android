package com.door43.translationstudio.rendering;

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
     * if set to false verses will not be displayed in the output.
     *
     * @param enable default is true
     */
    public void setVersesEnabled(boolean enable) {
        for (RenderingEngine engine : mEngines) {
            if(engine instanceof ClickableRenderingEngine) {
                ((ClickableRenderingEngine) engine).setVersesEnabled(enable);
            }
        }
    }

    /**
     * If set to not null matched strings will be highlighted.
     *
     * @param searchString - null is disable
     * @param highlightColor
     */
    public void setSearchString(CharSequence searchString, int highlightColor) {
        for (RenderingEngine engine : mEngines) {
            if(engine instanceof ClickableRenderingEngine) {
                ((ClickableRenderingEngine) engine).setSearchString(searchString, highlightColor);
            }
        }
    }

    /**
     * if set to true, then line breaks will be shown in the output.
     *
     * @param enable default is false
     */
    public void setLinebreaksEnabled(boolean enable) {
        for (RenderingEngine engine : mEngines) {
            if(engine instanceof ClickableRenderingEngine) {
                ((ClickableRenderingEngine) engine).setLinebreaksEnabled(enable);
            }
        }
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
