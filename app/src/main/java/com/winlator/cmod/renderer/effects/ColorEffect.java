package com.winlator.cmod.renderer.effects;

import com.winlator.cmod.renderer.material.ScreenMaterial;
import com.winlator.cmod.renderer.material.ShaderMaterial;

public class ColorEffect extends Effect {
    private float brightness = 0.0f;
    private float contrast = 0.0f;
    private float gamma = 1.0f;
    private float sharpness = 0.0f;
    private float textureWidth = 1920.0f;
    private float textureHeight = 1080.0f;

    public ColorEffect() {
        super();
    }

    public void setTextureSize(int width, int height) {
        this.textureWidth = width;
        this.textureHeight = height;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = Math.max(-1.0f, Math.min(brightness, 1.0f));
    }

    public float getContrast() {
        return contrast;
    }

    public void setContrast(float contrast) {
        this.contrast = Math.max(-0.5f, Math.min(contrast, 1.0f));
    }

    public float getGamma() {
        return gamma;
    }

    public void setGamma(float gamma) {
        this.gamma = Math.max(0.1f, Math.min(gamma, 3.0f));
    }

    public float getSharpness() {
        return sharpness;
    }

    public void setSharpness(float sharpness) {
        this.sharpness = Math.max(0.0f, Math.min(sharpness, 0.4f));
    }

    @Override
    protected ShaderMaterial createMaterial() {
        return new ColorEffectMaterial();
    }

    private class ColorEffectMaterial extends ScreenMaterial {
        public ColorEffectMaterial() {
            super();
            setUniformNames("brightness", "contrast", "gamma", "sharpness", "screenTexture", "textureSize");
        }

        @Override
        protected String getFragmentShader() {
            return String.join("\n", new CharSequence[]{
                "precision mediump float;",
                "uniform sampler2D screenTexture;",
                "uniform vec2 textureSize;",
                "uniform float brightness;",
                "uniform float contrast;",
                "uniform float gamma;",
                "uniform float sharpness;",
                "varying vec2 vUV;",
                
                "vec3 applySharpness(vec2 uv, vec3 originalColor) {",
                "    if (sharpness <= 0.0) return originalColor;",
                "    vec2 pixelSize = 1.0 / textureSize;",
                "    // High-quality 3x3 Laplacian kernel",
                "    vec3 sampleTop = texture2D(screenTexture, uv + vec2(0.0, pixelSize.y)).rgb;",
                "    vec3 sampleBottom = texture2D(screenTexture, uv - vec2(0.0, pixelSize.y)).rgb;",
                "    vec3 sampleLeft = texture2D(screenTexture, uv - vec2(pixelSize.x, 0.0)).rgb;",
                "    vec3 sampleRight = texture2D(screenTexture, uv + vec2(pixelSize.x, 0.0)).rgb;",
                "    vec3 sampleTopLeft = texture2D(screenTexture, uv + vec2(-pixelSize.x, pixelSize.y)).rgb;",
                "    vec3 sampleTopRight = texture2D(screenTexture, uv + vec2(pixelSize.x, pixelSize.y)).rgb;",
                "    vec3 sampleBottomLeft = texture2D(screenTexture, uv + vec2(-pixelSize.x, -pixelSize.y)).rgb;",
                "    vec3 sampleBottomRight = texture2D(screenTexture, uv + vec2(pixelSize.x, -pixelSize.y)).rgb;",
                "    vec3 sampleCenter = texture2D(screenTexture, uv).rgb;",
                "    // Enhanced Laplacian with strong center weight",
                "    vec3 laplacian = 8.0 * sampleCenter - sampleTop - sampleBottom - sampleLeft - sampleRight - sampleTopLeft - sampleTopRight - sampleBottomLeft - sampleBottomRight;",
                "    // Non-linear sharpness boost",
                "    float enhancedSharpness = sharpness * 2.5;",
                "    vec3 sharpened = sampleCenter + enhancedSharpness * laplacian;",
                "    return clamp(sharpened, 0.0, 1.0);",
                "}",
                
                "void main() {",
                "    vec3 color = texture2D(screenTexture, vUV).rgb;",
                "    ",
                "    // Apply high-quality sharpness FIRST",
                "    color = applySharpness(vUV, color);",
                "    ",
                "    // Brightness: symmetric additive model",
                "    color += brightness;",
                "    ",
                "    // Contrast: scale around 0.5",
                "    float contrastFactor = contrast * 2.0 + 1.0;",
                "    color = (color - 0.5) * contrastFactor + 0.5;",
                "    ",
                "    // Gamma correction with safeguard",
                "    float gammaFactor = 1.0 / max(gamma, 0.1);",
                "    color = pow(max(color, 0.0), vec3(gammaFactor));",
                "    ",
                "    // Final clamp",
                "    color = clamp(color, 0.0, 1.0);",
                "    gl_FragColor = vec4(color, 1.0);",
                "}"
            });
        }

        @Override
        public void use() {
            super.use();
            setUniformFloat("brightness", brightness);
            setUniformFloat("contrast", contrast);
            setUniformFloat("gamma", gamma);
            setUniformFloat("sharpness", sharpness);
            setUniformVec2("textureSize", textureWidth, textureHeight);
        }
    }
}