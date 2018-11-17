package com.hoko.blur.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.hoko.blur.opengl.drawable.BlurDrawable;

/**
 * Created by yuxfzju on 16/11/9.
 */
public class BlurRelativeLayout extends RelativeLayout{
    private BlurDrawable mBlurDrawable;

    public BlurRelativeLayout(Context context) {
        super(context);
        init();
    }

    public BlurRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BlurRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBlurDrawable = new BlurDrawable();
        setBackground(mBlurDrawable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBlurDrawable.freeGLResource();
    }

    public BlurDrawable getBlurDrawable() {
        return mBlurDrawable;
    }
}
