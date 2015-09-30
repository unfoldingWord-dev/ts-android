package com.door43.widget;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * This class provides utilities for views
 */
public class ViewUtil {

    /**
     * Makes links in a textview clickable
     * @param view
     */
    public static void makeLinksClickable(TextView view) {
        MovementMethod m = view.getMovementMethod();
        if(m == null || !(m instanceof LinkMovementMethod)) {
            if(view.getLinksClickable()) {
                view.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    /**
     * Sets the color of the snackbar text
     * @param snack
     * @param color
     */
    public static void setSnackBarTextColor(Snackbar snack, int color) {
        TextView tv = (TextView) snack.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(color);
    }

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
     * @param leftToRight indicates which direction the animation of the top card should go.
     * @param listener
     */
    public static void animateSwapCards(final View topCard, final View bottomCard, final int topCardElevation, final int bottomCardElevation, final boolean leftToRight, Animation.AnimationListener listener) {
        long duration = 400;
        float xMargin = topCard.getX() - bottomCard.getX();
        float yMargin = topCard.getY() - bottomCard.getY();

        topCard.clearAnimation();
        bottomCard.clearAnimation();

        final ViewGroup.LayoutParams topLayout = topCard.getLayoutParams();
        final ViewGroup.LayoutParams bottomLayout = bottomCard.getLayoutParams();

        // bottom animation
        Animation upLeft = new TranslateAnimation(0f, xMargin, 0f, yMargin);
        upLeft.setDuration(duration);
        Animation bottomCardRight = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        bottomCardRight.setDuration(duration);
        Animation bottomCardLeft = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -.5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        bottomCardLeft.setDuration(duration);

        AnimationSet bottomOutSet = new AnimationSet(false);
        if(leftToRight) {
            bottomOutSet.addAnimation(bottomCardLeft);
        } else {
            bottomOutSet.addAnimation(bottomCardRight);
        }
        AnimationSet bottomInSet = new AnimationSet(false);
        bottomInSet.setStartOffset(duration);
        if(leftToRight) {
            bottomInSet.addAnimation(bottomCardRight);
            bottomInSet.addAnimation(upLeft);
        } else {
            bottomInSet.addAnimation(bottomCardLeft);
            bottomInSet.addAnimation(upLeft);
        }
        AnimationSet bottomSet = new AnimationSet(false);
        bottomSet.setInterpolator(new LinearInterpolator());
        bottomSet.addAnimation(bottomOutSet);
        bottomSet.addAnimation(bottomInSet);
        bottomSet.setAnimationListener(listener);

        bottomOutSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // elevation takes precedence for API 21+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    topCard.setElevation(bottomCardElevation);
                    bottomCard.setElevation(topCardElevation);
                }
                bottomCard.bringToFront();
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

        // top animation
        Animation downRight = new TranslateAnimation(0f, -xMargin, 0f, -yMargin);
        downRight.setDuration(duration);
        Animation topCardRight = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        topCardRight.setDuration(duration);
        Animation topCardLeft = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -.5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        topCardLeft.setDuration(duration);

        AnimationSet topOutSet = new AnimationSet(false);
        if(leftToRight) {
            topOutSet.addAnimation(topCardRight);
        } else {
            topOutSet.addAnimation(topCardLeft);
        }
        AnimationSet topInSet = new AnimationSet(false);
        topInSet.setStartOffset(duration);
        if(leftToRight) {
            topInSet.addAnimation(topCardLeft);
            topInSet.addAnimation(downRight);
        } else {
            topInSet.addAnimation(topCardRight);
            topInSet.addAnimation(downRight);
        }

        AnimationSet topSet = new AnimationSet(false);
        topSet.setInterpolator(new LinearInterpolator());
        topSet.addAnimation(topOutSet);
        topSet.addAnimation(topInSet);

        // start animations
        bottomCard.startAnimation(bottomSet);
        topCard.startAnimation(topSet);
    }

    /**
     * Forces a popup menu to display it's icons
     * @param popup
     */
    public static void forcePopupMenuIcons(PopupMenu popup) {
        try {
            Field[] fields = popup.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popup);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper
                            .getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod(
                            "setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
