package com.hoko.blur.util;

import android.opengl.GLES20;
import android.util.Log;

import com.hoko.blur.HokoBlur;
import com.hoko.blur.anno.Mode;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;

/**
 * Created by yuxfzju on 16/9/4.
 */
public class ShaderUtil {

    private static final String TAG = ShaderUtil.class.getSimpleName();

    public static String getVertexCode() {

        return "uniform mat4 uMVPMatrix;   \n" +
                "uniform mat4 uTexMatrix;   \n" +
                "attribute vec2 aTexCoord;   \n" +
                "attribute vec3 aPosition;  \n" +
                "varying vec2 vTexCoord;  \n" +
                "void main() {              \n" +
                "   gl_Position = uMVPMatrix * vec4(aPosition, 1); \n" +
                "   vTexCoord = (uTexMatrix * vec4(aTexCoord, 1, 1)).st;\n" +
                "}  \n";

    }

    /**
     * return true if no GL Error
     */
    public static boolean checkGLError(String msg) {
        int error = GLES20.glGetError();
        if (error != 0) {
            Log.e(TAG, "checkGLError: error=" + error + ", msg=" + msg);
        }

        return error == 0;
    }

    public static boolean checkEGLContext() {
        EGLContext context = ((EGL10) EGLContext.getEGL()).eglGetCurrentContext();
        if (context.equals(EGL10.EGL_NO_CONTEXT)) {
            Log.e(TAG, "This thread is no EGLContext.");
            return false;
        } else {
            return true;
        }
    }

    public static String getFragmentShaderCode(@Mode int mode) {

        StringBuilder sb = new StringBuilder();
        sb.append(" \n")
                .append("precision mediump float;   \n")
                .append("varying vec2 vTexCoord;   \n")
                .append("uniform sampler2D uTexture;   \n")
                .append("uniform int uRadius;   \n")
                .append("uniform float uWidthOffset;  \n")
                .append("uniform float uHeightOffset;  \n")
                .append("mediump float getGaussWeight(mediump float currentPos, mediump float sigma) \n")
                .append("{ \n")
                .append("   return 1.0 / sigma * exp(-(currentPos * currentPos) / (2.0 * sigma * sigma)); \n")
                .append("} \n")

                /**
                 * Android 4.4一下系统编译器优化，这里注释暂时不用的GLSL代码
                 */
                .append("void main() {   \n");

        if (mode == HokoBlur.MODE_BOX) {
            sb.append(ShaderUtil.getBoxSampleCode());
        } else if (mode == HokoBlur.MODE_GAUSSIAN) {
            sb.append(ShaderUtil.getGaussianSampleCode());
        } else if (mode == HokoBlur.MODE_STACK) {
            sb.append(ShaderUtil.getStackSampleCode());
        }
        sb.append("}   \n");

        return sb.toString();
    }



    /**
     * If set kernel weight array in advance, the GPU registers have no enough space.
     * So compute the weight in the code directly.
     */
    private static String getGaussianSampleCode() {

        return "   int diameter = 2 * uRadius + 1;  \n" +
                "   vec4 sampleTex;\n" +
                "   vec3 col;  \n" +
                "   float weightSum = 0.0; \n" +
                "   for(int i = 0; i < diameter; i++) {\n" +
                "       vec2 offset = vec2(float(i - uRadius) * uWidthOffset, float(i - uRadius) * uHeightOffset);  \n" +
                "       sampleTex = vec4(texture2D(uTexture, vTexCoord.st+offset));\n" +
                "       float index = float(i); \n" +
                "       float gaussWeight = getGaussWeight(index - float(diameter - 1)/2.0," +
                "           (float(diameter - 1)/2.0 + 1.0) / 2.0); \n" +
                "       col += sampleTex.rgb * gaussWeight; \n" +
                "       weightSum += gaussWeight;\n" +
                "   }   \n" +
                "   gl_FragColor = vec4(col / weightSum, sampleTex.a);   \n";
    }

    /**
     * If set kernel weight array in advance, the GPU registers have no enough space.
     * So compute the weight in the code directly.
     */
    private static String getBoxSampleCode() {

        return "   int diameter = 2 * uRadius + 1; \n" +
                "   vec4 sampleTex;\n" +
                "   vec3 col;  \n" +
                "   float weightSum = 0.0; \n" +
                "   for(int i = 0; i < diameter; i++) {\n" +
                "       vec2 offset = vec2(float(i - uRadius) * uWidthOffset, float(i - uRadius) * uHeightOffset);  \n" +
                "        sampleTex = vec4(texture2D(uTexture, vTexCoord.st+offset));\n" +
                "       float index = float(i); \n" +
                "       float boxWeight = float(1.0) / float(diameter); \n" +
                "       col += sampleTex.rgb * boxWeight; \n" +
                "       weightSum += boxWeight;\n" +
                "   }   \n" +
                "   gl_FragColor = vec4(col / weightSum, sampleTex.a);   \n";
    }

    /**
     * If set kernel weight array in advance, the GPU registers have no enough space.
     * So compute the weight in the code directly.
     */
    private static String getStackSampleCode() {

        return "int diameter = 2 * uRadius + 1;  \n" +
                "   vec4 sampleTex;\n" +
                "   vec3 col;  \n" +
                "   float weightSum = 0.0; \n" +
                "   for(int i = 0; i < diameter; i++) {\n" +
                "       vec2 offset = vec2(float(i - uRadius) * uWidthOffset, float(i - uRadius) * uHeightOffset);  \n" +
                "       sampleTex = vec4(texture2D(uTexture, vTexCoord.st+offset));\n" +
                "       float index = float(i); \n" +
                "       float boxWeight = float(uRadius) + 1.0 - abs(index - float(uRadius)); \n" +
                "       col += sampleTex.rgb * boxWeight; \n" +
                "       weightSum += boxWeight;\n" +
                "   }   \n" +
                "   gl_FragColor = vec4(col / weightSum, sampleTex.a);   \n";
    }

    public static String getKernelInitCode(float[] kernel) {
        if (kernel == null || kernel.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder("  float kernel[" + kernel.length + "]; \n");

        for (int i = 0; i < kernel.length; i++) {
            sb.append("  kernel[");
            sb.append(i);
            sb.append("] = ");
            sb.append(kernel[i]).append("f; \n");
        }

        return sb.toString();
    }

    public static String getOffsetInitCode(int radius) {
        final int d = 2 * radius + 1;
        StringBuilder sb = new StringBuilder("  vec2 offsets[" + d + "]; \n");

        for (int i = -radius; i <= radius; i++) {
                sb.append("  offsets[")
                    .append(i + radius)
                    .append("] = vec2(")
                    .append(i)
                    .append(".f * uWidthOffset, ")
                    .append(i)
                    .append(".f * uHeightOffset); \n");
        }

        return sb.toString();

    }

    /**
     * copy the texture
     */
    public static String getCopyFragmentCode() {
        return " \n" +
                "precision mediump float;" +
                "varying vec2 vTexCoord;   \n" +
                "uniform sampler2D uTexture;   \n" +
                "uniform lowp float mixPercent;   \n" +
                "uniform vec4 vMixColor;   \n" +
                "void main() {   \n" +
                "   vec4 col = vec4(texture2D(uTexture, vTexCoord.st));\n" +
                "   gl_FragColor = vec4(mix(col.rgb, vMixColor.rgb, vMixColor.a * mixPercent), col.a);   \n" +
                "}   \n";
    }


    /**
     * get color fragment
     */
    public static String getColorFragmentCode() {

        return "precision mediump float;   \n" +
                "uniform vec4 vColor;   \n" +
                "void main() {   \n" +
                "   gl_FragColor = vColor;   \n" +
                "} \n";
    }

//    public static String getSampleCode(int d) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("  vec3 sampleTex[KERNEL_SIZE];\n")
//                .append("  for(int i = 0; i < KERNEL_SIZE; i++) {\n")
//                .append("        sampleTex[i] = vec3(texture2D(uTexture, 1.0f - (vTexCoord.st + offsets[i])));\n")
//                .append("  } \n")
//                .append("  vec3 col;  \n")
//                .append("  for(int i = 0; i < KERNEL_SIZE; i++) \n")
//                .append("        col += sampleTex[i] * kernel[i]; \n")
//                .append("  gl_FragColor = vec4(col, 1.0);   \n");
//
//        return sb.toString().replace("KERNEL_SIZE", d + "");
//    }



}
