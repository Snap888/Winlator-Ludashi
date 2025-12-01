package com.winlator.cmod.renderer.effects;

import android.util.Log;

import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.renderer.material.ScreenMaterial;
import com.winlator.cmod.renderer.material.ShaderMaterial;

public class DepthEffect extends Effect {
    private float depthStrength;
    private float depthFocus;
    private GLRenderer renderer;

    public DepthEffect() {
        super();
        this.depthStrength = 0.0f; // [0.0, 1.0]
        this.depthFocus = 0.5f;    // [0.0, 1.0]
    }

    public void setRenderer(GLRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    protected ShaderMaterial createMaterial() {
        return new DepthEffectMaterial();
    }

    // Getters and Setters
    public float getDepthStrength() {
        return depthStrength;
    }

    public void setDepthStrength(float depthStrength) {
        this.depthStrength = Math.max(0.0f, Math.min(1.0f, depthStrength)); // Ограничение значений
        Log.d("DepthEffect", "setDepthStrength called, value: " + this.depthStrength); // Лог
    }

    public float getDepthFocus() {
        return depthFocus;
    }

    public void setDepthFocus(float depthFocus) {
        this.depthFocus = Math.max(0.0f, Math.min(1.0f, depthFocus)); // Ограничение значений
        Log.d("DepthEffect", "setDepthFocus called, value: " + this.depthFocus); // Лог
    }

    private class DepthEffectMaterial extends ScreenMaterial {
        public DepthEffectMaterial() {
            super();
            // Убедимся, что мы добавляем все нужные uniform
            setUniformNames("depthStrength", "depthFocus", "screenTexture", "textureSize", "resolution"); // Добавим resolution сюда для ясности
        }

        @Override
        protected String getFragmentShader() {
            return String.join("\n", new CharSequence[]{
                    "precision highp float;",
                    "uniform sampler2D screenTexture;",
                    "uniform vec2 textureSize;",
                    "uniform vec2 resolution;", // Добавлено
                    "uniform float depthStrength;",
                    "uniform float depthFocus;",
                    "varying vec2 vUV;",

                    // Функция для размытия (простое квадратное ядро 3x3)
                    "vec3 applyBlur(sampler2D tex, vec2 uv, vec2 pixelSize, float blurAmount) {",
                    "    if (blurAmount == 0.0) return texture2D(tex, uv).rgb;",
                    "    vec3 sum = vec3(0.0);",
                    "    float totalWeight = 0.0;",
                    "    float kernel[9];",
                    "    kernel[0] = 1.0; kernel[1] = 2.0; kernel[2] = 1.0;",
                    "    kernel[3] = 2.0; kernel[4] = 4.0; kernel[5] = 2.0;",
                    "    kernel[6] = 1.0; kernel[7] = 2.0; kernel[8] = 1.0;",
                    "    int index = 0;",
                    "    for (int y = -1; y <= 1; y++) {",
                    "        for (int x = -1; x <= 1; x++) {",
                    "            vec2 offset = vec2(float(x), float(y)) * pixelSize;",
                    "            float weight = kernel[index];",
                    "            sum += texture2D(tex, uv + offset).rgb * weight;",
                    "            totalWeight += weight;",
                    "            index++;",
                    "        }",
                    "    }",
                    "    return sum / totalWeight;",
                    "}",

                    "void main() {",
                    "    vec2 texelSize = 1.0 / textureSize;",
                    "    vec3 color = texture2D(screenTexture, vUV).rgb;",

                    "    // Рассчитываем относительную глубину на основе Y-координаты",
                    "    float depthValue = 1.0 - abs(vUV.y - depthFocus) / max(depthFocus, 1.0 - depthFocus);",
                    "    // Усиление эффекта в зависимости от параметра depthStrength",
                    "    float blurAmount = depthStrength * (1.0 - depthValue);", // Чем дальше от фокуса, тем больше размытие

                    "    // Применяем размытие",
                    "    vec3 blurredColor = applyBlur(screenTexture, vUV, texelSize, blurAmount);",

                    "    // Интерполируем между оригинальным и размытым изображением",
                    "    vec3 finalColor = mix(color, blurredColor, blurAmount);",

                    "    gl_FragColor = vec4(finalColor, 1.0);", // Альфа-канал остаётся 1.0
                    "}"
            });
        }

        @Override
        public void use() {
            super.use(); // Вызывает базовый use, который компилирует шейдер и получает локации

            float strength = DepthEffect.this.getDepthStrength();
            float focus = DepthEffect.this.getDepthFocus();

            // Ограничение значений для шейдера
            strength = Math.max(0.0f, Math.min(strength, 1.0f)); // [0.0, 1.0]
            focus = Math.max(0.0f, Math.min(focus, 1.0f));       // [0.0, 1.0]

            Log.d("DepthEffectMaterial", "use() called. Setting strength: " + strength + ", focus: " + focus);

            setUniformFloat("depthStrength", strength);
            setUniformFloat("depthFocus", focus);

            // Используем фиксированные размеры текстуры
            setUniformVec2("textureSize", 1920.0f, 1080.0f);
            // resolution передаётся в renderEffect EffectComposer
        }
    }
}