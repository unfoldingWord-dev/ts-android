package com.door43.translationstudio.util;

import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

/**
 * Created by joel on 12/2/2014.
 */
public class AnimationUtilities {
    private static final int ANIMATION_SPEED = 100;

    /**
     * This method will cause a view to fade out after which it fires a callback where operations can be performed then it will fade back in
     * @param view the view to fade
     * @param callback the callback to execute after the view has faded out
     */
    public static void fadeOutIn(final View view, final Handler.Callback callback) {
        final Animation in = new AlphaAnimation(0.0f, 1.0f);
        in.setDuration(ANIMATION_SPEED);
        final Animation out = new AlphaAnimation(1.0f, 0.0f);
        out.setDuration(ANIMATION_SPEED);
        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                callback.handleMessage(null);
                view.startAnimation(in);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(out);
    }

    public static void slideInLeft(View view) {
        slideInLeft(view, ANIMATION_SPEED);
    }

    /**
     * Animates a view sliding in from the left
     * @param view
     */
    public static void slideInLeft(final View view, int duration) {
        final Animation inLeft = new TranslateAnimation(view.getX() - view.getWidth(), 0, 0, view.getX());
        inLeft.setDuration(duration);
        view.startAnimation(inLeft);
    }

    /**
     * Fades in the view
     * @param view
     */
    public static void fadeIn(final View view, int duration) {
        final Animation in = new AlphaAnimation(0.0f, 1.0f);
        in.setDuration(duration);
        view.setAnimation(in);
    }

    public static void resizeWidth(View view, float oldWidth, float newWidth) {
        resizeWidth(view, oldWidth, newWidth, ANIMATION_SPEED);
    }

    /**
     * resizes the width of a view
     * @param view
     * @param oldWidth
     * @param newWidth
     */
    public static void resizeWidth(View view, float oldWidth, float newWidth, int duration) {
        final Animation resizeX = new ResizeAnimation(view, oldWidth, view.getHeight(), newWidth, view.getHeight());
        resizeX.setDuration(duration);
        view.setAnimation(resizeX);
    }
}
