package com.door43.translationstudio.util;

import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/**
 * Created by joel on 12/2/2014.
 */
public class AnimationUtilities {
    private static final int FADE_SPEED = 100;

    /**
     * This method will cause a view to fade out after which it fires a callback where operations can be performed then it will fade back in
     * @param view the view to fade
     * @param callback the callback to execute after the view has faded out
     */
    public static void fadeOutIn(final View view, final Handler.Callback callback) {
        final Animation in = new AlphaAnimation(0.0f, 1.0f);
        in.setDuration(FADE_SPEED);
        final Animation out = new AlphaAnimation(1.0f, 0.0f);
        out.setDuration(FADE_SPEED);
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
}
