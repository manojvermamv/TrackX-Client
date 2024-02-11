package com.android.sus_client.utils;

import android.graphics.Color;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.android.sus_client.commonutility.basic.Function2;
import com.android.sus_client.commonutility.basic.Invoker;

public class ColorAnimator {

    private final int defaultColor;
    private int currentColor = -1;

    public ColorAnimator(int defaultColor) {
        this.defaultColor = defaultColor;
    }

    private int mixSearchBarColor(int requestedColor, int defaultColor) {
        if (requestedColor == defaultColor) {
            return Color.WHITE;
        } else {
            return Utils.mixColorForDrawable(0.25f, requestedColor, Color.WHITE);
        }
    }

    /**
     * Creates an animation that animates from the current color to the new [color].
     */
    public Animation animateTo(int color) {
        return animateTo(color, null);
    }

    public Animation animateTo(int color, Function2<Void, Integer, Integer> onChange) {
        int currentUiColor = currentColor < 0 ? defaultColor : currentColor;
        int finalUiColor = Utils.isColorGrayscale(color) ? defaultColor : color;

        int startSearchColor = mixSearchBarColor(currentUiColor, defaultColor);
        int finalSearchColor = mixSearchBarColor(finalUiColor, defaultColor);
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                int mainColor = Utils.mixColorForDrawable(interpolatedTime, currentUiColor, finalUiColor);
                int secondaryColor = Utils.mixColorForDrawable(interpolatedTime, startSearchColor, finalSearchColor);
                if (onChange != null) {
                    onChange.invoke(new Invoker<>(mainColor), new Invoker<>(secondaryColor));
                }
                currentColor = mainColor;
            }
        };
        animation.setDuration(300);
        return animation;
    }

}