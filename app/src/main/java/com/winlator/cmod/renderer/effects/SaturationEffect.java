package com.winlator.cmod.renderer.effects;

import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.renderer.material.ScreenMaterial;
import com.winlator.cmod.renderer.material.ShaderMaterial;

public class SaturationEffect extends Effect {
    private float saturation; // [0.0, 2.0], где 1.0 - норма
    private GLRenderer renderer;

    public SaturationEffect() {
        super();
        this.saturation = 1.0f; // Нормальный уровень насыщенности
    }

    public void setRenderer(GLRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    protected ShaderMaterial createMaterial() {
        return new SaturationEffectMaterial();
    }

    // Getters and Setters
    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = Math.max(0.0f, Math.min(2.0f, saturation)); // Ограничение [0.0, 2.0]
    }

    private class SaturationEffectMaterial extends ScreenMaterial {
        public SaturationEffectMaterial() {
            super();
            setUniformNames("saturation", "screenTexture", "textureSize");
        }

        @Override
        protected String getFragmentShader() {
            return String.join("\n", new CharSequence[]{
                    "precision highp float;",
                    "uniform sampler2D screenTexture;",
                    "uniform vec2 textureSize;",
                    "uniform float saturation;",
                    "varying vec2 vUV;",

                    // Функция для вычисления яркости (luminance)
                    "float luminance(vec3 color) {",
                    "    return dot(color, vec3(0.299, 0.587, 0.114));",
                    "}",

                    "void main() {",
                    "    vec4 texelColor = texture2D(screenTexture, vUV);",
                    "    vec3 color = texelColor.rgb;",

                    // Вычисляем яркость
                    "    float lum = luminance(color);",

                    // Интерполируем между яркостью (оттенок серого) и исходным цветом
                    "    vec3 saturatedColor = mix(vec3(lum), color, saturation);",

                    "    gl_FragColor = vec4(saturatedColor, texelColor.a);",
                    "}"
            });
        }

        @Override
        public void use() {
            super.use();

            float saturation = SaturationEffect.this.getSaturation();

            setUniformFloat("saturation", saturation);

            // Используем фиксированные размеры текстуры
            setUniformVec2("textureSize", 1920.0f, 1080.0f);
        }
    }
}