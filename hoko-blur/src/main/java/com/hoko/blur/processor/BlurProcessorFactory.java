package com.hoko.blur.processor;

import com.hoko.blur.HokoBlur;
import com.hoko.blur.anno.Scheme;

class BlurProcessorFactory {

    static BlurProcessor getBlurProcessor(@Scheme int scheme, HokoBlurBuild builder) {

        BlurProcessor blurProcessor;

        switch (scheme) {
            case HokoBlur.SCHEME_NATIVE:
                blurProcessor = new NativeBlurProcessor(builder);
                break;
            case HokoBlur.SCHEME_JAVA:
                blurProcessor = new OriginBlurProcessor(builder);
                break;
            default:
                throw new IllegalArgumentException("Unsupported blur scheme!");
        }

        return blurProcessor;
    }
}
