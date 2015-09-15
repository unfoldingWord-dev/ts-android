package com.door43.widget;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;

/**
 * This class provides utilities for views
 */
public class ViewUtil {

    /**
     * Provides a backwards compatable way to tint drawables
     * @param view the view who's background drawable will be tinted
     * @param color the color that will be applied
     */
    public static void tintViewDrawable(View view, int color) {
        final Drawable originalDrawable = view.getBackground();
        final Drawable wrappedDrawable = DrawableCompat.wrap(originalDrawable);
        DrawableCompat.setTintList(wrappedDrawable, ColorStateList.valueOf(color));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(wrappedDrawable);
        } else {
            view.setBackgroundDrawable(wrappedDrawable);
        }
    }
    /**
     * Performs a stacked card animation that brings a bottom card to the front
     * In preperation two views should be stacked on top of each other with appropriate margin
     * so that the bottom card sticks out on the bottom and the right.
     *
     * @param topCard
     * @param bottomCard
     * @param topCardElevation
     * @param bottomCardElevation
     * @param bringLeftCardToFront
     */
    public static void animateCards(final View topCard, final View bottomCard, final int topCardElevation, final int bottomCardElevation, final boolean bringLeftCardToFront) {
        long duration = 400;
        float xMargin = topCard.getX() - bottomCard.getX();
        float yMargin = topCard.getY() - bottomCard.getY();

        final ViewGroup.LayoutParams topLayout = topCard.getLayoutParams();
        final ViewGroup.LayoutParams bottomLayout = bottomCard.getLayoutParams();


        // animate bottom card
        Animation bottomShift = new TranslateAnimation(0f, xMargin, 0f, yMargin);
        bottomShift.setInterpolator(new LinearInterpolator());
        bottomShift.setDuration(duration);

        Animation bottomOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        bottomOut.setInterpolator(new LinearInterpolator());
        bottomOut.setDuration(duration);

        final AnimationSet bottomOutSet = new AnimationSet(false);
        bottomOutSet.addAnimation(bottomOut);

        Animation bottomIn = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -.5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        bottomIn.setInterpolator(new LinearInterpolator());
        bottomIn.setDuration(duration);

        final AnimationSet bottomInSet = new AnimationSet(false);
        bottomInSet.setStartOffset(duration);
        bottomInSet.addAnimation(bottomIn);
        bottomInSet.addAnimation(bottomShift);

        AnimationSet bottomSet = new AnimationSet(false);
        bottomSet.addAnimation(bottomOutSet);
        bottomSet.addAnimation(bottomInSet);

        bottomOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // elevation takes precedence for API 21+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (bringLeftCardToFront) {
                        topCard.setElevation(topCardElevation);
                        bottomCard.setElevation(bottomCardElevation);
                    } else {
                        topCard.setElevation(bottomCardElevation);
                        bottomCard.setElevation(topCardElevation);
                    }
                }
                if (bringLeftCardToFront) {
                    topCard.bringToFront();
                } else {
                    bottomCard.bringToFront();
                }
                ((View) bottomCard.getParent()).requestLayout();
                ((View) bottomCard.getParent()).invalidate();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        bottomInSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                bottomCard.clearAnimation();
                bottomCard.setLayoutParams(topLayout);
                topCard.clearAnimation();
                topCard.setLayoutParams(bottomLayout);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        bottomCard.startAnimation(bottomSet);

        // animate top card
        Animation topShift = new TranslateAnimation(0f, -xMargin, 0f, -yMargin);
        topShift.setInterpolator(new LinearInterpolator());
        topShift.setDuration(duration);

        Animation topOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -.5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        topOut.setDuration(duration);

        final AnimationSet topOutSet = new AnimationSet(false);
        topOutSet.addAnimation(topOut);

        Animation topIn = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        topIn.setDuration(duration);

        AnimationSet topInSet = new AnimationSet(true);
        topInSet.setStartOffset(duration);
        topInSet.addAnimation(topIn);
        topInSet.addAnimation(topShift);

        AnimationSet topSet = new AnimationSet(true);
        topSet.addAnimation(topOutSet);
        topSet.addAnimation(topInSet);
        topCard.startAnimation(topSet);
    }
}
