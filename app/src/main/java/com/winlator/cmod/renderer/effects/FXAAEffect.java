package com.winlator.cmod.renderer.effects;

import com.winlator.cmod.renderer.material.ScreenMaterial;
import com.winlator.cmod.renderer.material.ShaderMaterial;

public class FXAAEffect extends Effect {
    private float resolutionX = 1920.0f;
    private float resolutionY = 1080.0f;

    public FXAAEffect() {
        super();
    }

    public void setResolution(float width, float height) {
        this.resolutionX = width;
        this.resolutionY = height;
    }

    @Override
    protected ShaderMaterial createMaterial() {
        return new FXAAMaterial();
    }

    private class FXAAMaterial extends ScreenMaterial {
        public FXAAMaterial() {
            super();
            setUniformNames("screenTexture", "resolution");
        }

        @Override
        protected String getFragmentShader() {
            return String.join("\n", new CharSequence[]{
                "precision mediump float;",
                "#define FXAA_MIN_REDUCE (1.0 / 128.0)",
                "#define FXAA_MUL_REDUCE (1.0 / 8.0)",
                "#define MAX_SPAN 8.0",
                "uniform sampler2D screenTexture;",
                "uniform vec2 resolution;",
                "varying vec2 vUV;",
                "const vec3 luma = vec3(0.299, 0.587, 0.114);",
                "void main() {",
                "    vec2 texelSize = 1.0 / resolution;",
                "    vec2 uv = vUV;",
                "    vec3 rgbNW = texture2D(screenTexture, uv + texelSize * vec2(-1.0, -1.0)).rgb;",
                "    vec3 rgbNE = texture2D(screenTexture, uv + texelSize * vec2( 1.0, -1.0)).rgb;",
                "    vec3 rgbSW = texture2D(screenTexture, uv + texelSize * vec2(-1.0,  1.0)).rgb;",
                "    vec3 rgbSE = texture2D(screenTexture, uv + texelSize * vec2( 1.0,  1.0)).rgb;",
                "    vec3 rgbM  = texture2D(screenTexture, uv).rgb;",
                "    float lumaNW = dot(rgbNW, luma);",
                "    float lumaNE = dot(rgbNE, luma);",
                "    float lumaSW = dot(rgbSW, luma);",
                "    float lumaSE = dot(rgbSE, luma);",
                "    float lumaM  = dot(rgbM,  luma);",
                "    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));",
                "    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));",
                "    vec2 dir;",
                "    dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));",
                "    dir.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));",
                "    float dirReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * 0.25 * FXAA_MUL_REDUCE, FXAA_MIN_REDUCE);",
                "    float rcpDirMin = 1.0 / (min(abs(dir.x), abs(dir.y)) + dirReduce);",
                "    dir = clamp(dir * rcpDirMin, vec2(-MAX_SPAN), vec2(MAX_SPAN)) * texelSize;",
                "    vec3 rgbA = (",
                "        texture2D(screenTexture, uv + dir * (1.0 / 3.0 - 0.5)).rgb +",
                "        texture2D(screenTexture, uv + dir * (2.0 / 3.0 - 0.5)).rgb) * 0.5;",
                "    vec3 rgbB = rgbA * 0.5 + (",
                "        texture2D(screenTexture, uv + dir * -0.5).rgb +",
                "        texture2D(screenTexture, uv + dir *  0.5).rgb) * 0.25;",
                "    float lumaB = dot(rgbB, luma);",
                "    gl_FragColor = vec4((lumaB < lumaMin || lumaB > lumaMax) ? rgbA : rgbB, 1.0);",
                "}"
            });
        }

        @Override
        public void use() {
            super.use();
            setUniformVec2("resolution", resolutionX, resolutionY);
        }
    }
}