package com.door43.widget;

import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;

/**
 * This class provides utilities for views
 */
public class ViewUtil {

    /**
     * Performs a stacked card animation that brings a bottom card to the front
     * In preperation two views should be stacked on top of each other with appropriate margin
     * so that the bottom card sticks out on the bottom and the right.
     *
     * @param leftCard
     * @param rightCard
     * @param topCardElevation
     * @param bottomCardElevation
     * @param bringLeftCardToFront
     */
    public static void animateCards(final View leftCard, final View rightCard, final int topCardElevation, final int bottomCardElevation, final boolean bringLeftCardToFront) {
        long duration = 700;
        // animate bottom card up
        Animation bottomOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        bottomOut.setDuration(duration);

        Animation bottomIn = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -.5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        bottomIn.setDuration(duration);

        final AnimationSet bottomFinishSet = new AnimationSet(true);
        bottomFinishSet.setStartOffset(duration);
        bottomFinishSet.addAnimation(bottomIn);

        AnimationSet bottomSet = new AnimationSet(true);
        bottomSet.addAnimation(bottomOut);
        bottomSet.addAnimation(bottomFinishSet);

        bottomOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // elevation takes precedence for API 21+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if(bringLeftCardToFront) {
                        leftCard.setElevation(topCardElevation);
                        rightCard.setElevation(bottomCardElevation);
                    } else {
                        leftCard.setElevation(bottomCardElevation);
                        rightCard.setElevation(topCardElevation);
                    }
                }
                if(bringLeftCardToFront) {
                    leftCard.bringToFront();
                } else {
                    rightCard.bringToFront();
                }
                ((View) rightCard.getParent()).requestLayout();
                ((View) rightCard.getParent()).invalidate();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rightCard.startAnimation(bottomSet);

        // animate top card down
        Animation topOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -.5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        topOut.setDuration(duration);

        Animation topIn = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        topIn.setDuration(duration);
        AnimationSet topFinishSet = new AnimationSet(true);
        topFinishSet.setStartOffset(duration);
        topFinishSet.addAnimation(topIn);

        AnimationSet topSet = new AnimationSet(true);
        topSet.addAnimation(topOut);
        topSet.addAnimation(topFinishSet);
        leftCard.startAnimation(topSet);
    }
}
