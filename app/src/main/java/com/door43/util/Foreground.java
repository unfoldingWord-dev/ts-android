package com.door43.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps track of whether or not the application is in the foreground.
 * Inspired by http://steveliles.github.io/is_my_android_app_currently_foreground_or_background.html
 * with modifications to make it more reliable.
 *
 * Usage:
 * 1. register with the Application object like Foreground.init(app).
 * 2. check for foreground state like Foreground.get().isForeground().
 * 3. or register for state changes like Foreground.get().addListener(some listener).
 *
 *
 */
public class Foreground implements Application.ActivityLifecycleCallbacks {

    private static long checkDelay = 500;
    private static final String TAG = Foreground.class.getName();
    private static Foreground instance;

    private int counter = 0;
    private boolean foreground = false;
    private Handler handler = new Handler();
    private Runnable check;
    private List<Listener> listeners = new CopyOnWriteArrayList<>();


    /**
     * Receives foreground events
     */
    public interface Listener {
        void onBecameForeground();
        void onBecameBackground();
    }

    /**
     * Adds a listener to receive foreground events
     * @param listener the listener to be added
     */
    public void addListener(Listener listener){
        listeners.add(listener);
    }

    /**
     * Removes a listener from receiving foreground events
     * @param listener the listener to be removed
     */
    public void removeListener(Listener listener){
        listeners.remove(listener);
    }

    /**
     * Sets the amount of time to wait between the app enters the background and checking to see if
     * it is still in the background before notifying listeners.
     *
     * Setting this to a higher value prevents false positives in cases where the entire app
     * is re-built as is the case for screen rotations.
     *
     * Setting a value of 0 or less will disable the delay.
     *
     * @param delay the delay between entering the background and notifying listeners.
     */
    public void setDelay(long delay) {
        checkDelay = delay;
    }

    /**
     * Initializes the foreground
     * @param app the app
     */
    public static Foreground init(Application app) {
        if(instance == null) {
            instance = new Foreground();
            app.registerActivityLifecycleCallbacks(instance);
        }
        return instance;
    }

    /**
     * Returns an instance of the foreground or creates a new one
     * @param app the Application
     * @return an instance of Foreground
     */
    public static Foreground get(Application app) {
        return init(app);
    }

    /**
     * Returns or create an instance from the context
     * @param c the context
     * @return an instance of Foreground
     */
    public static Foreground get(Context c){
        if (instance == null) {
            Context appContext = c.getApplicationContext();
            if (appContext instanceof Application) {
                init((Application)appContext);
            }
            throw new IllegalStateException(
                    "Foreground is not initialised and " +
                            "cannot obtain the Application object");
        }
        return instance;
    }

    /**
     * Returns the instance
     * @return an instance of Foreground.
     * @throws IllegalStateException if the singleton has not been initialized
     */
    public static Foreground get() throws IllegalStateException {
        if (instance == null) {
            throw new IllegalStateException(
                    "Foreground is not initialised - invoke " +
                            "at least once with parameterised init/get");
        }
        return instance;
    }

    private Foreground() {}

    /**
     * Check if the application is currently in the foreground
     * @return true if the application is in the foreground.
     */
    public boolean isForeground() {
        return counter > 0;
    }

    /**
     * Checks if the application is currently in the background
     * @return true if the application is in the background.
     */
    public boolean isBackground() {
        return counter <= 0;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        counter ++;

        if(check != null) handler.removeCallbacks(check);

        if(!foreground && counter > 0) {
            foreground = true;
            Log.i(TAG, "went foreground");
            for (Listener l : listeners) {
                try {
                    l.onBecameForeground();
                } catch (Exception e) {
                    Log.e("Foreground", "Listener threw exception", e);
                }
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        counter --;

        if(check != null) handler.removeCallbacks(check);

        handler.postDelayed(check = new Runnable() {
            @Override
            public void run() {
                if(foreground && counter <= 0) {
                    foreground = false;
                    Log.i(TAG, "went background");
                    for (Listener l : listeners){
                        try {
                            l.onBecameBackground();
                        } catch (Exception e) {
                            Log.e("Foreground", "Listener threw exception", e);
                        }
                    }
                }
            }
        }, checkDelay);
    }

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

}
