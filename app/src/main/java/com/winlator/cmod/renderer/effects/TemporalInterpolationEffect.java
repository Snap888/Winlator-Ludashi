package com.winlator.cmod.renderer.effects;

import android.opengl.GLES20;
import android.util.Log;

import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.renderer.RenderTarget;
import com.winlator.cmod.renderer.material.ScreenMaterial;

public class TemporalInterpolationEffect extends Effect {
    private boolean enabled = false;
    private float blendFactor = 0.1f;
    private RenderTarget previousFrameTexture;
    private long lastFrameTime = 0;
    private GLRenderer renderer;

    public TemporalInterpolationEffect() {
        super();
    }

    public void setRenderer(GLRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    protected ScreenMaterial createMaterial() {
        return new TemporalInterpolationMaterial();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setBlendFactor(float factor) {
        this.blendFactor = Math.max(0.0f, Math.min(1.0f, factor));
    }

    public float getBlendFactor() {
        return blendFactor;
    }

    private class TemporalInterpolationMaterial extends ScreenMaterial {
        public TemporalInterpolationMaterial() {
            super();
            setUniformNames("resolution", "screenTexture", "previousFrame", "blendFactor");
        }

        @Override
        protected String getFragmentShader() {
            return String.join("\n", new CharSequence[]{
                "precision highp float;",
                "uniform vec2 resolution;",
                "uniform sampler2D screenTexture;",
                "uniform sampler2D previousFrame;",
                "uniform float blendFactor;",
                "varying vec2 vUV;",

                "void main() {",
                "    vec4 current = texture2D(screenTexture, vUV);",
                "    vec4 previous = texture2D(previousFrame, vUV);",
                "    ",
                "    // Адаптивное смешивание",
                "    float blend = smoothstep(0.0, 1.0, blendFactor);",
                "    vec4 finalColor = mix(previous, current, blend);",
                "    ",
                "    // Небольшой шарпинг для компенсации размытия",
                "    vec2 texelSize = 1.0 / resolution;",
                "    vec4 top = texture2D(screenTexture, vUV + vec2(0.0, texelSize.y));",
                "    vec4 bottom = texture2D(screenTexture, vUV - vec2(0.0, texelSize.y));",
                "    vec4 left = texture2D(screenTexture, vUV - vec2(texelSize.x, 0.0));",
                "    vec4 right = texture2D(screenTexture, vUV + vec2(texelSize.x, 0.0));",
                "    vec4 center = texture2D(screenTexture, vUV);",
                "    ",
                "    vec4 sharpen = center * 5.0 - top - bottom - left - right;",
                "    finalColor.rgb += (sharpen.rgb - center.rgb) * 0.05;",
                "    ",
                "    gl_FragColor = finalColor;",
                "}"
            });
        }

        @Override
        public void use() {
            super.use();
            setUniformVec2("resolution", renderer.getSurfaceWidth(), renderer.getSurfaceHeight());
            setUniformFloat("blendFactor", blendFactor);
            
            // Привязываем предыдущий кадр
            if (previousFrameTexture != null) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, previousFrameTexture.getTextureId());
                setUniformInt("previousFrame", 1);
            }
        }
    }

    // Метод для сохранения текущего кадра как предыдущего
    // ИСПОЛЬЗУЕТСЯ ТОЛЬКО ДЛЯ ВЕРСИЙ OpenGL ES 3.0+
    // Для ES 2.0 закомментирован
    /*
    public void saveCurrentFrame() {
        if (previousFrameTexture == null) {
            previousFrameTexture = new RenderTarget();
            previousFrameTexture.allocateFramebuffer(renderer.getSurfaceWidth(), renderer.getSurfaceHeight());
        }
        
        // Копируем текущий кадр в предыдущий
        int[] currentFBO = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, currentFBO, 0);
        
        GLES20.glBindFramebuffer(GLES20.GL_READ_FRAMEBUFFER, currentFBO[0]);
        GLES20.glBindFramebuffer(GLES20.GL_DRAW_FRAMEBUFFER, previousFrameTexture.getFramebuffer());
        GLES20.glBlitFramebuffer(0, 0, renderer.getSurfaceWidth(), renderer.getSurfaceHeight(),
                                 0, 0, renderer.getSurfaceWidth(), renderer.getSurfaceHeight(),
                                 GLES20.GL_COLOR_BUFFER_BIT, GLES20.GL_LINEAR);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, currentFBO[0]);
    }
    */
}