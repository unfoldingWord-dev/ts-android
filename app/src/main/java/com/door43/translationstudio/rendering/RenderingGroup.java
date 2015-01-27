package com.door43.translationstudio.rendering;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 1/26/2015.
 */
public class RenderingGroup {
    private Thread mThread;
    private boolean mStopped = false;
    private boolean mRunning = false;
    private List<RenderingEngine> mEngines =  new ArrayList<RenderingEngine>();
    private CharSequence mInput;
    private Callback mCallback;

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
    public void start() {
        if(mRunning || mInput == null || mCallback == null) return;
        mRunning = true;
        mStopped = false;
        mThread = new Thread() {
            @Override
            public void run() {
                CharSequence rendered = mInput;
                for(RenderingEngine engine:mEngines) {
                    if(mStopped) break;
                    rendered = engine.render(rendered);
                }
                mCallback.onComplete(rendered);
                mRunning = false;
            }
        };
        mThread.run();
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
     * @param callback
     */
    public void init(String input, Callback callback) {
        mCallback = callback;
        mInput = input;
    }

    public static interface Callback {
        public void onComplete(CharSequence output);
    }
}
