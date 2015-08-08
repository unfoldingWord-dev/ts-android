package com.door43.translationstudio.rendering;

/**
 * Created by joel on 1/26/2015.
 */
public abstract class RenderingEngine {
    private OnRenderCallback mCallback;
    private boolean mStopped = false;
    private boolean mRunning = false;

    /**
     * Begins the rendering process
     * @param input the raw input string
     * @param callback the callback that will reveive events regarding the rendering
     */
    public final void start(final CharSequence input, OnRenderCallback callback) {
        if(mRunning) return;
        mCallback = callback;
        mRunning = true;
        mStopped = false;
        new Thread() {
            @Override
            public void run() {
                CharSequence output = render(input);
                if(output != null) {
                    mCallback.onComplete(output);
                } else {
                    mCallback.onError(input);
                }
                mRunning = false;
            }
        }.start();
    }

    /**
     * Stops the rendering process
     */
    public void stop() {
        mStopped = true;
    }

    /**
     * Checks if the rendering engine has been notified to stop
     * Implimentations of this class should check the value of this method at the begining of each loop
     * in order to provide stopping support.
     * @return
     */
    public boolean isStopped() {
        return mStopped;
    }

    /**
     * Renders the input string
     * @param in the raw input string
     * @return the rendered output
     */
    public CharSequence render(CharSequence in) {
        return in;
    }

    /**
     * An interface for callbacks issued durring the rendering process
     */
    public static interface OnRenderCallback {
        /**
         * Called when the rendering has finished
         * @param output the rendered output
         */
        public void onComplete(CharSequence output);

        /**
         * Called when an exception occured durring rendering
         * @param input the raw input string
         */
        public void onError(CharSequence input);
    }
}
