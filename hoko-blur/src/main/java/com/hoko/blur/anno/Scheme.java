package com.hoko.blur.anno;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.hoko.blur.HokoBlur.SCHEME_JAVA;
import static com.hoko.blur.HokoBlur.SCHEME_NATIVE;

/**
 * Created by yuxfzju on 2017/2/9.
 */

@IntDef({SCHEME_NATIVE, SCHEME_JAVA})
@Retention(RetentionPolicy.SOURCE)
public @interface Scheme {
}
