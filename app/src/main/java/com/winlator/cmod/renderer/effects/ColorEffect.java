package com.winlator.cmod.renderer.effects;

import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.renderer.material.ScreenMaterial;
import com.winlator.cmod.renderer.material.ShaderMaterial;

public class ColorEffect extends Effect {
    private float brightness;
    private float contrast;
    private float gamma;
    private float sharpness; // Теперь используется для изначальной резкости
    private GLRenderer renderer;

    public ColorEffect() {
        super();
        this.brightness = 0.0f;
        this.contrast = 0.0f;
        this.gamma = 1.0f;
        this.sharpness = 0.0f; // [0.0, 0.4]
    }

    public void setRenderer(GLRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    protected ShaderMaterial createMaterial() {
        return new ColorEffectMaterial();
    }

    // Getters and Setters
    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = Math.max(-1.0f, Math.min(1.0f, brightness)); // Ограничение [-1.0, 1.0]
    }

    public float getContrast() {
        return contrast;
    }

    public void setContrast(float contrast) {
        this.contrast = Math.max(-0.5f, Math.min(1.0f, contrast)); // Ограничение [-0.5, 1.0]
    }

    public float getGamma() {
        return gamma;
    }

    public void setGamma(float gamma) {
        this.gamma = Math.max(0.1f, Math.min(3.0f, gamma)); // Ограничение [0.1, 3.0]
    }

    public float getSharpness() {
        return sharpness;
    }

    public void setSharpness(float sharpness) {
        // Ограничиваем резкость до 40% (0.4 в реальных значениях)
        this.sharpness = Math.min(Math.max(0.0f, sharpness), 0.4f);
    }

    private class ColorEffectMaterial extends ScreenMaterial {
        public ColorEffectMaterial() {
            super();
            setUniformNames("brightness", "contrast", "gamma", "sharpness", "screenTexture", "textureSize");
        }

        @Override
        protected String getFragmentShader() {
            return String.join("\n", new CharSequence[]{
                    "precision highp float;",
                    "uniform sampler2D screenTexture;",
                    "uniform vec2 textureSize;",
                    "uniform float brightness;",
                    "uniform float contrast;",
                    "uniform float gamma;",
                    "uniform float sharpness;",
                    "varying vec2 vUV;",
                    
                    // Усиленный фильтр резкости с расширенным ядром (изначальная реализация)
                    "vec3 applySharpness(vec2 uv, vec3 originalColor) {",
                    "    if (sharpness == 0.0) return originalColor;",
                    "    ",
                    "    vec2 pixelSize = 1.0 / textureSize;",
                    "    ",
                    "    // Расширенное ядро резкости 3x3",
                    "    vec3 sampleTop = texture2D(screenTexture, uv + vec2(0.0, pixelSize.y)).rgb;",
                    "    vec3 sampleBottom = texture2D(screenTexture, uv - vec2(0.0, pixelSize.y)).rgb;",
                    "    vec3 sampleLeft = texture2D(screenTexture, uv - vec2(pixelSize.x, 0.0)).rgb;",
                    "    vec3 sampleRight = texture2D(screenTexture, uv + vec2(pixelSize.x, 0.0)).rgb;",
                    "    vec3 sampleTopLeft = texture2D(screenTexture, uv + vec2(-pixelSize.x, pixelSize.y)).rgb;",
                    "    vec3 sampleTopRight = texture2D(screenTexture, uv + vec2(pixelSize.x, pixelSize.y)).rgb;",
                    "    vec3 sampleBottomLeft = texture2D(screenTexture, uv + vec2(-pixelSize.x, -pixelSize.y)).rgb;",
                    "    vec3 sampleBottomRight = texture2D(screenTexture, uv + vec2(pixelSize.x, -pixelSize.y)).rgb;",
                    "    vec3 sampleCenter = texture2D(screenTexture, uv).rgb;",
                    "    ",
                    "    // Усиленный лапласиан с большим весом",
                    "    vec3 laplacian = 8.0 * sampleCenter - sampleTop - sampleBottom - sampleLeft - sampleRight - sampleTopLeft - sampleTopRight - sampleBottomLeft - sampleBottomRight;",
                    "    ",
                    "    // Усиленный коэффициент резкости с нелинейной кривой",
                    "    float enhancedSharpness = sharpness * 2.5; // Усиление эффекта",
                    "    vec3 sharpened = sampleCenter + enhancedSharpness * laplacian;",
                    "    ",
                    "    // Дополнительное усиление контраста для краев",
                    "    vec3 edgeEnhancement = mix(sharpened, clamp(sharpened, 0.0, 1.0), 0.7);",
                    "    ",
                    "    return clamp(edgeEnhancement, 0.0, 1.0);",
                    "}",
                    
                    "void main() {",
                    "    vec4 texelColor = texture2D(screenTexture, vUV);",
                    "    vec3 color = texelColor.rgb;",
                    "    ",
                    "    // Применяем усиленную резкость ДО других корректировок",
                    "    color = applySharpness(vUV, color);",
                    "    ",
                    "    // Тонкая настройка яркости с нелинейной кривой",
                    "    float brightnessFactor = brightness * 0.5 + 0.5;", // Преобразуем [-1,1] в [0,1]
                    "    color = mix(color, vec3(1.0), max(brightness, 0.0) * 0.3);", // Увеличение яркости
                    "    color = mix(color, vec3(0.0), max(-brightness, 0.0) * 0.3);", // Уменьшение яркости",
                    "    ",
                    "    // Улучшенная настройка контраста",
                    "    float contrastFactor = (contrast * 2.0 + 1.0);", // [0,3]
                    "    color = (color - 0.5) * contrastFactor + 0.5;",
                    "    ",
                    "    // Тонкая настройка гаммы с защитой от крайних значений",
                    "    float gammaFactor = 1.0 / max(gamma, 0.1);",
                    "    color = pow(max(color, 0.0), vec3(gammaFactor));",
                    "    ",
                    "    // Финальное ограничение значений",
                    "    color = clamp(color, 0.0, 1.0);",
                    "    ",
                    "    gl_FragColor = vec4(color, texelColor.a);",
                    "}"
            });
        }

        @Override
        public void use() {
            super.use();

            float brightness = ColorEffect.this.getBrightness();
            float contrast = ColorEffect.this.getContrast();
            float gamma = ColorEffect.this.getGamma();
            float sharpness = ColorEffect.this.getSharpness();

            // Более точные диапазоны для тонкой настройки
            brightness = Math.max(-1.0f, Math.min(brightness, 1.0f));        // [-1.0, 1.0]
            contrast = Math.max(-0.5f, Math.min(contrast, 1.0f));           // [-0.5, 1.0]
            gamma = Math.max(0.1f, Math.min(gamma, 3.0f));                  // [0.1, 3.0]
            sharpness = Math.max(0.0f, Math.min(sharpness, 0.4f));          // [0.0, 0.4] - ограничение до 40

            setUniformFloat("brightness", brightness);
            setUniformFloat("contrast", contrast);
            setUniformFloat("gamma", gamma);
            setUniformFloat("sharpness", sharpness);
            
            // Используем фиксированные размеры текстуры
            setUniformVec2("textureSize", 1920.0f, 1080.0f);
        }
    }
}