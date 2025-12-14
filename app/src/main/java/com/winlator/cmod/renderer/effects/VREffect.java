package com.winlator.cmod.renderer.effects;

import android.opengl.GLES20;
import android.util.Log;

import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.renderer.material.ScreenMaterial;

public class VREffect extends Effect {
    private boolean vrEnabled = false;
    private boolean showCenterLine = false;
    private float centerLineColor = 1.0f;
    private boolean syncFrames = true;
    private int vrMode = 0; // 0=Split Screen, 1=Side-by-Side, 2=Over-Under
    private float leftFrameX = 0.0f, leftFrameY = 0.0f, leftFrameWidth = 0.5f, leftFrameHeight = 1.0f;
    private float rightFrameX = 0.5f, rightFrameY = 0.0f, rightFrameWidth = 0.5f, rightFrameHeight = 1.0f;
    private float distortionStrength = 0.3f; // Диапазон от -1.0 (вогнутая) до 1.0 (выпуклая)
    private float edgeFeathering = 0.02f;
    private float cornerRadius = 0.05f; // Меньше закругление
    private float leftImageOffsetX = 0.0f, leftImageOffsetY = 0.0f; // Смещение изображения в левом фрейме
    private float rightImageOffsetX = 0.0f, rightImageOffsetY = 0.0f; // Смещение изображения в правом фрейме
    private boolean useGyroForMovement = false; // Использовать гироскоп для движения
    private float gyroX = 0.0f, gyroY = 0.0f; // Значения гироскопа (интегрированные углы)
    private float gyroAngleX = 0.0f, gyroAngleY = 0.0f; // Интегрированные углы
    private long lastGyroTime = 0; // Время последнего обновления
    private float gyroSensitivity = 0.1f; // Чувствительность гироскопа
    private float leftImageClipRight = 0.0f; // Обрезка правой части изображения для левого фрейма
    private float rightImageClipLeft = 0.0f; // Обрезка левой части изображения для правого фрейма
    private float ipdAdjustment = 0.0f; // Межзрачковая коррекция (-0.5 до 0.5)
    private float leftImageCenterOffsetX = 0.0f, leftImageCenterOffsetY = 0.0f; // Смещение изображения от центра фрейма (левый глаз)
    private float rightImageCenterOffsetX = 0.0f, rightImageCenterOffsetY = 0.0f; // Смещение изображения от центра фрейма (правый глаз)
    // НОВОЕ: Поле для режима гироскопа
    private int gyroMode = 0; // 0 = для фреймов, 1 = для мыши
    private GLRenderer renderer;

    public VREffect() {
        super();
    }

    public void setRenderer(GLRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    protected ScreenMaterial createMaterial() {
        return new VREffectMaterial();
    }

    // Методы для управления VR режимом
    public void setEnabled(boolean enabled) {
        this.vrEnabled = enabled;
    }

    public boolean isEnabled() {
        return vrEnabled;
    }

    public void setVREnabled(boolean enabled) {
        if (!this.vrEnabled && enabled) {
            // При включении VR-режима сбрасываем углы
            resetGyroAngles();
        }
        this.vrEnabled = enabled;
    }

    public boolean isVREnabled() {
        return vrEnabled;
    }

    // Методы для отображения центральной линии
    public void setShowCenterLine(boolean show) {
        this.showCenterLine = show;
    }

    public boolean isShowCenterLine() {
        return showCenterLine;
    }

    public void setCenterLineColor(float color) {
        this.centerLineColor = color;
    }

    public float getCenterLineColor() {
        return centerLineColor;
    }

    // Методы для VR режима
    public void setVrMode(int mode) {
        this.vrMode = mode;
    }

    public int getVrMode() {
        return vrMode;
    }

    // Методы для синхронизации фреймов
    public void setSyncFrames(boolean sync) {
        this.syncFrames = sync;
    }

    public boolean isSyncFrames() {
        return syncFrames;
    }

    // Методы для межзрачковой коррекции
    public void setIPDAdjustment(float adjustment) {
        this.ipdAdjustment = Math.max(-0.5f, Math.min(0.5f, adjustment));
    }

    public float getIPDAdjustment() {
        return ipdAdjustment;
    }

    // Методы для смещения изображения в фреймах (относительно центра фрейма)
    public void setLeftImageCenterOffsetX(float offsetX) {
        this.leftImageCenterOffsetX = Math.max(-1.0f, Math.min(1.0f, offsetX));
    }

    public float getLeftImageCenterOffsetX() {
        return leftImageCenterOffsetX;
    }

    public void setLeftImageCenterOffsetY(float offsetY) {
        this.leftImageCenterOffsetY = Math.max(-1.0f, Math.min(1.0f, offsetY));
    }

    public float getLeftImageCenterOffsetY() {
        return leftImageCenterOffsetY;
    }

    public void setRightImageCenterOffsetX(float offsetX) {
        this.rightImageCenterOffsetX = Math.max(-1.0f, Math.min(1.0f, offsetX));
    }

    public float getRightImageCenterOffsetX() {
        return rightImageCenterOffsetX;
    }

    public void setRightImageCenterOffsetY(float offsetY) {
        this.rightImageCenterOffsetY = Math.max(-1.0f, Math.min(1.0f, offsetY));
    }

    public float getRightImageCenterOffsetY() {
        return rightImageCenterOffsetY;
    }

    // Методы для левого фрейма
    public void setLeftFrameX(float x) {
        this.leftFrameX = x;
    }

    public float getLeftFrameX() {
        return leftFrameX;
    }

    public void setLeftFrameY(float y) {
        this.leftFrameY = y;
    }

    public float getLeftFrameY() {
        return leftFrameY;
    }

    public void setLeftFrameWidth(float width) {
        this.leftFrameWidth = width;
    }

    public float getLeftFrameWidth() {
        return leftFrameWidth;
    }

    public void setLeftFrameHeight(float height) {
        this.leftFrameHeight = height;
    }

    public float getLeftFrameHeight() {
        return leftFrameHeight;
    }

    // Методы для правого фрейма
    public void setRightFrameX(float x) {
        this.rightFrameX = x;
    }

    public float getRightFrameX() {
        return rightFrameX;
    }

    public void setRightFrameY(float y) {
        this.rightFrameY = y;
    }

    public float getRightFrameY() {
        return rightFrameY;
    }

    public void setRightFrameWidth(float width) {
        this.rightFrameWidth = width;
    }

    public float getRightFrameWidth() {
        return rightFrameWidth;
    }

    public void setRightFrameHeight(float height) {
        this.rightFrameHeight = height;
    }

    public float getRightFrameHeight() {
        return rightFrameHeight;
    }

    // Методы для смещения изображения в фреймах
    public void setLeftImageOffsetX(float offsetX) {
        this.leftImageOffsetX = offsetX;
    }

    public float getLeftImageOffsetX() {
        return leftImageOffsetX;
    }

    public void setLeftImageOffsetY(float offsetY) {
        this.leftImageOffsetY = offsetY;
    }

    public float getLeftImageOffsetY() {
        return leftImageOffsetY;
    }

    public void setRightImageOffsetX(float offsetX) {
        this.rightImageOffsetX = offsetX;
    }

    public float getRightImageOffsetX() {
        return rightImageOffsetX;
    }

    public void setRightImageOffsetY(float offsetY) {
        this.rightImageOffsetY = offsetY;
    }

    public float getRightImageOffsetY() {
        return rightImageOffsetY;
    }

    // Методы для обрезки изображения
    public void setLeftImageClipRight(float clip) {
        this.leftImageClipRight = Math.max(0.0f, Math.min(1.0f, clip));
    }

    public float getLeftImageClipRight() {
        return leftImageClipRight;
    }

    public void setRightImageClipLeft(float clip) {
        this.rightImageClipLeft = Math.max(0.0f, Math.min(1.0f, clip));
    }

    public float getRightImageClipLeft() {
        return rightImageClipLeft;
    }

    // Методы для гироскопа
    public void setUseGyroForMovement(boolean use) {
        this.useGyroForMovement = use;
    }

    public boolean isUseGyroForMovement() {
        return useGyroForMovement;
    }

    public void setGyroValues(float gyroX, float gyroY) {
        long currentTime = System.currentTimeMillis();
        if (lastGyroTime == 0) {
            lastGyroTime = currentTime;
            return;
        }
        
        float deltaTime = (currentTime - lastGyroTime) / 1000.0f; // В секундах
        lastGyroTime = currentTime;
        
        // Интегрируем угловые скорости для получения углов
        gyroAngleX += gyroX * deltaTime;
        gyroAngleY += gyroY * deltaTime;
        
        // Ограничиваем углы, чтобы избежать больших значений
        gyroAngleX = Math.max(-1.0f, Math.min(1.0f, gyroAngleX));
        gyroAngleY = Math.max(-1.0f, Math.min(1.0f, gyroAngleY));
        
        // Используем углы для смещения изображения
        this.gyroX = gyroAngleX * gyroSensitivity;
        this.gyroY = gyroAngleY * gyroSensitivity;
    }

    public float getGyroX() {
        return gyroX;
    }

    public float getGyroY() {
        return gyroY;
    }

    public void setGyroSensitivity(float sensitivity) {
        this.gyroSensitivity = sensitivity;
    }

    public float getGyroSensitivity() {
        return gyroSensitivity;
    }

    // Метод для сброса углов гироскопа
    public void resetGyroAngles() {
        this.gyroAngleX = 0.0f;
        this.gyroAngleY = 0.0f;
        this.gyroX = 0.0f;
        this.gyroY = 0.0f;
        this.lastGyroTime = 0;
    }

    // Методы для параметров дисторсии - теперь с расширенным диапазоном
    public void setDistortionStrength(float strength) {
        // Ограничиваем значение в диапазоне от -1.0 до 1.0
        this.distortionStrength = Math.max(-1.0f, Math.min(1.0f, strength));
    }

    public float getDistortionStrength() {
        return distortionStrength;
    }

    public void setEdgeFeathering(float feathering) {
        this.edgeFeathering = feathering;
    }

    public float getEdgeFeathering() {
        return edgeFeathering;
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    // Методы для режима гироскопа - НОВОЕ
    public void setGyroMode(int mode) {
        this.gyroMode = mode;
    }

    public int getGyroMode() {
        return this.gyroMode;
    }

    // Метод для сброса к значениям по умолчанию
    public void resetToDefaults() {
        vrEnabled = false;
        showCenterLine = false;
        centerLineColor = 1.0f;
        vrMode = 0; // Split screen
        syncFrames = true;
        ipdAdjustment = 0.0f; // Нет коррекции IPD по умолчанию
        
        // Установка стандартных значений для Google Cardboard
        leftFrameX = 0.0f;
        leftFrameY = 0.0f;
        leftFrameWidth = 0.5f;  // Левая половина
        leftFrameHeight = 1.0f;
        
        rightFrameX = 0.5f;     // Правая половина
        rightFrameY = 0.0f;
        rightFrameWidth = 0.5f;
        rightFrameHeight = 1.0f;
        
        // Смещения изображения
        leftImageOffsetX = 0.02f;  // Немного смещаем вправо (закрывает правую часть)
        leftImageOffsetY = 0.0f;
        rightImageOffsetX = -0.02f; // Немного смещаем влево (закрывает левую часть)
        rightImageOffsetY = 0.0f;
        
        // Смещения изображения от центра фрейма
        leftImageCenterOffsetX = 0.0f;
        leftImageCenterOffsetY = 0.0f;
        rightImageCenterOffsetX = 0.0f;
        rightImageCenterOffsetY = 0.0f;
        
        // Обрезка изображения
        leftImageClipRight = 0.0f; // Нет обрезки по умолчанию
        rightImageClipLeft = 0.0f; // Нет обрезки по умолчанию
        
        useGyroForMovement = false;
        resetGyroAngles();
        gyroSensitivity = 0.1f;
        
        distortionStrength = 0.3f; // Умеренная выпуклая дисторсия для компенсации линз
        edgeFeathering = 0.02f;   // Небольшое размытие краев
        cornerRadius = 0.05f;     // Малое закругление
        // Сброс gyroMode
        gyroMode = 0; // По умолчанию для фреймов
    }

    // Методы для установки параметров фрейма (для совместимости)
    public void setLeftFrameParams(float x, float y, float width, float height) {
        this.leftFrameX = x;
        this.leftFrameY = y;
        this.leftFrameWidth = width;
        this.leftFrameHeight = height;
    }

    public void setRightFrameParams(float x, float y, float width, float height) {
        this.rightFrameX = x;
        this.rightFrameY = y;
        this.rightFrameWidth = width;
        this.rightFrameHeight = height;
    }

    // Методы для работы с профилями
    public VRProfile createProfile(String name) {
        return new VRProfile(name, this);
    }

    public void loadFromProfile(VRProfile profile) {
        profile.applyToEffect(this);
    }

    private class VREffectMaterial extends ScreenMaterial {
        public VREffectMaterial() {
            super();
            // Добавляем gyroMode в список uniform
            setUniformNames("resolution", "screenTexture", "vrEnabled", "showCenterLine", "centerLineColor", "vrMode",
                          "syncFrames",
                          "leftFrameX", "leftFrameY", "leftFrameWidth", "leftFrameHeight",
                          "rightFrameX", "rightFrameY", "rightFrameWidth", "rightFrameHeight",
                          "leftImageOffsetX", "leftImageOffsetY", "rightImageOffsetX", "rightImageOffsetY",
                          "leftImageClipRight", "rightImageClipLeft", "ipdAdjustment",
                          "leftImageCenterOffsetX", "leftImageCenterOffsetY", "rightImageCenterOffsetX", "rightImageCenterOffsetY",
                          "useGyroForMovement", "gyroX", "gyroY", "gyroSensitivity",
                          "distortionStrength", "edgeFeathering", "cornerRadius",
                          "gyroMode"); // Добавлен gyroMode
        }

        @Override
        protected String getFragmentShader() {
            return String.join("\n", new CharSequence[]{
                "precision highp float;",
                "uniform vec2 resolution;",
                "uniform sampler2D screenTexture;",
                "uniform bool vrEnabled;",
                "uniform bool showCenterLine;",
                "uniform float centerLineColor;",
                "uniform int vrMode;",
                "uniform bool syncFrames;",
                "uniform float leftFrameX, leftFrameY, leftFrameWidth, leftFrameHeight;",
                "uniform float rightFrameX, rightFrameY, rightFrameWidth, rightFrameHeight;",
                "uniform float leftImageOffsetX, leftImageOffsetY, rightImageOffsetX, rightImageOffsetY;",
                "uniform float leftImageClipRight, rightImageClipLeft, ipdAdjustment;",
                "uniform float leftImageCenterOffsetX, leftImageCenterOffsetY, rightImageCenterOffsetX, rightImageCenterOffsetY;",
                "uniform bool useGyroForMovement;",
                "uniform float gyroX, gyroY, gyroSensitivity;",
                "uniform float distortionStrength;",
                "uniform float edgeFeathering;",
                "uniform float cornerRadius;",
                // Добавляем uniform для gyroMode
                "uniform int gyroMode;",
                "varying vec2 vUV;",

                // Функция коррекции дисторсии - от вогнутой до выпуклой линзы
                "vec2 distortionCorrection(vec2 uv, float strength) {",
                "    vec2 centered = uv * 2.0 - 1.0; // Центрируем координаты",
                "    float r = length(centered); // Расстояние от центра",
                "    float theta = atan(centered.y, centered.x); // Угол",
                "    ",
                "    float correctedR = r;",
                "    ",
                "    if (strength > 0.0) {",
                "        // Выпуклая дисторсия (рыбий глаз) - положительная сила",
                "        correctedR = r * (1.0 + strength * r * r + strength * strength * r * r * r * r);",
                "        correctedR = min(correctedR, 2.0); // Ограничиваем искажение",
                "    } else if (strength < 0.0) {",
                "        // Вогнутая дисторсия - отрицательная сила",
                "        float absStrength = abs(strength);",
                "        correctedR = r * (1.0 - absStrength * r * r);",
                "        correctedR = max(correctedR, 0.0); // Не допускаем отрицательные значения",
                "    }",
                "    ",
                "    vec2 corrected = correctedR * vec2(cos(theta), sin(theta));",
                "    ",
                "    // Возвращаем к нормализованным координатам",
                "    return (corrected + 1.0) * 0.5;",
                "}",

                // Функция для создания маски с закругленными углами для конкретного фрейма
                "float roundedFrameMask(vec2 uv, float frameX, float frameY, float frameWidth, float frameHeight, float radius) {",
                "    // Нормализуем координаты в систему координат фрейма",
                "    vec2 frameUV = (uv - vec2(frameX, frameY)) / vec2(frameWidth, frameHeight);",
                "    ",
                "    // Проверяем, внутри ли точка фрейма",
                "    if (frameUV.x < 0.0 || frameUV.x > 1.0 || frameUV.y < 0.0 || frameUV.y > 1.0) {",
                "        return 0.0; // Вне фрейма",
                "    }",
                "    ",
                "    // Рассчитываем радиус закругления в координатах фрейма",
                "    float cornerRadius = radius;",
                "    ",
                "    // Проверяем углы фрейма",
                "    vec2 cornerDist = vec2(0.0);",
                "    ",
                "    // Левый верхний угол",
                "    cornerDist = vec2(cornerRadius, cornerRadius) - frameUV;",
                "    if (cornerDist.x > 0.0 && cornerDist.y > 0.0) {",
                "        float dist = length(cornerDist);",
                "        if (dist > cornerRadius) return 0.0;",
                "    }",
                "    ",
                "    // Правый верхний угол",
                "    cornerDist = vec2(1.0 - cornerRadius, cornerRadius) - frameUV;",
                "    if (cornerDist.x < 0.0 && cornerDist.y > 0.0) {",
                "        float dist = length(cornerDist);",
                "        if (dist > cornerRadius) return 0.0;",
                "    }",
                "    ",
                "    // Левый нижний угол",
                "    cornerDist = vec2(cornerRadius, 1.0 - cornerRadius) - frameUV;",
                "    if (cornerDist.x > 0.0 && cornerDist.y < 0.0) {",
                "        float dist = length(cornerDist);",
                "        if (dist > cornerRadius) return 0.0;",
                "    }",
                "    ",
                "    // Правый нижний угол",
                "    cornerDist = vec2(1.0 - cornerRadius, 1.0 - cornerRadius) - frameUV;",
                "    if (cornerDist.x < 0.0 && cornerDist.y < 0.0) {",
                "        float dist = length(cornerDist);",
                "        if (dist > cornerRadius) return 0.0;",
                "    }",
                "    ",
                "    // Если мы дошли сюда, точка внутри фрейма с закругленными углами",
                "    return 1.0;",
                "}",

                "void main() {",
                "    if (!vrEnabled) {",
                "        // Обычный режим - просто выводим оригинальное изображение",
                "        gl_FragColor = texture2D(screenTexture, vUV);",
                "        return;",
                "    }",
                "",
                "    // Используем gyroMode для определения, применять ли смещение от гироскопа к фрейму",
                "    bool applyGyroToFrame = useGyroForMovement && gyroMode == 0; // Только если режим 0 (для фреймов)",
                "",
                "    vec4 finalColor = vec4(0.0, 0.0, 0.0, 1.0);",
                "    ",
                "    // Применяем межзрачковую коррекцию (IPD)",
                "    float adjustedLeftFrameX = leftFrameX - ipdAdjustment * 0.1; // Сдвигаем левый фрейм",
                "    float adjustedRightFrameX = rightFrameX + ipdAdjustment * 0.1; // Сдвигаем правый фрейм",
                "    ",
                "    // Обрабатываем левую половину экрана (левый глаз)",
                "    if (vUV.x < 0.5) {",
                "        // Вычисляем UV координаты для левого изображения",
                "        vec2 leftUV = vec2(",
                "            (vUV.x - adjustedLeftFrameX) / leftFrameWidth,",
                "            (vUV.y - leftFrameY) / leftFrameHeight",
                "        );",
                "        ",
                "        // Применяем смещение изображения в фрейме",
                "        vec2 leftImageOffset = vec2(leftImageOffsetX, leftImageOffsetY);",
                "        ",
                "        // Применяем смещение от гироскопа, если включено И режим 0",
                "        if (applyGyroToFrame) {",
                "            leftImageOffset += vec2(gyroX, -gyroY); // Инвертируем Y для правильного направления",
                "        }",
                "        ",
                "        leftUV += leftImageOffset;",
                "        ",
                "        // Применяем смещение изображения относительно центра фрейма",
                "        vec2 frameCenter = vec2(0.5, 0.5); // Центр фрейма в UV координатах",
                "        vec2 imageCenterOffset = vec2(leftImageCenterOffsetX, leftImageCenterOffsetY);",
                "        leftUV = leftUV - frameCenter + imageCenterOffset;",
                "        leftUV = leftUV + frameCenter; // Возвращаем к фрейм координатам",
                "        ",
                "        // Проверяем, находится ли точка внутри левого фрейма",
                "        if (leftUV.x >= 0.0 && leftUV.x <= 1.0 && leftUV.y >= 0.0 && leftUV.y <= 1.0) {",
                "            // Применяем дисторсию - от вогнутой до выпуклой линзы",
                "            vec2 distortedLeftUV = distortionCorrection(leftUV, distortionStrength);",
                "            ",
                "            // Проверяем, остались ли координаты в пределах",
                "            if (distortedLeftUV.x >= 0.0 && distortedLeftUV.x <= 1.0 &&",
                "                distortedLeftUV.y >= 0.0 && distortedLeftUV.y <= 1.0) {",
                "                ",
                "                // Применяем маску с закругленными углами для левого фрейма",
                "                float leftMask = roundedFrameMask(vUV, adjustedLeftFrameX, leftFrameY, leftFrameWidth, leftFrameHeight, cornerRadius);",
                "                ",
                "                vec4 color = texture2D(screenTexture, distortedLeftUV);",
                "                finalColor = vec4(color.rgb * leftMask, color.a);",
                "            }",
                "        }",
                "    } else {",
                "        // Обрабатываем правую половину экрана (правый глаз)",
                "        vec2 rightUV;",
                "        ",
                "        if (syncFrames) {",
                "            // Если синхронизация включена, используем левые параметры для правого глаза",
                "            rightUV = vec2(",
                "                (vUV.x - (adjustedLeftFrameX + 0.5)) / leftFrameWidth,",
                "                (vUV.y - leftFrameY) / leftFrameHeight",
                "            );",
                "            ",
                "            // Применяем смещение изображения в фрейме",
                "            vec2 rightImageOffset = vec2(leftImageOffsetX, leftImageOffsetY);",
                "            ",
                "            // Применяем смещение от гироскопа, если включено И режим 0",
                "            if (applyGyroToFrame) {",
                "                rightImageOffset += vec2(gyroX, -gyroY); // Инвертируем Y для правильного направления",
                "            }",
                "            ",
                "            rightUV += rightImageOffset;",
                "            ",
                "            // Применяем смещение изображения относительно центра фрейма",
                "            vec2 frameCenter = vec2(0.5, 0.5); // Центр фрейма в UV координатах",
                "            vec2 imageCenterOffset = vec2(leftImageCenterOffsetX, leftImageCenterOffsetY);",
                "            rightUV = rightUV - frameCenter + imageCenterOffset;",
                "            rightUV = rightUV + frameCenter; // Возвращаем к фрейм координатам",
                "        } else {",
                "            // Используем отдельные параметры для правого глаза",
                "            rightUV = vec2(",
                "                (vUV.x - adjustedRightFrameX) / rightFrameWidth,",
                "                (vUV.y - rightFrameY) / rightFrameHeight",
                "            );",
                "            ",
                "            // Применяем смещение изображения в фрейме",
                "            vec2 rightImageOffset = vec2(rightImageOffsetX, rightImageOffsetY);",
                "            ",
                "            // Применяем смещение от гироскопа, если включено И режим 0",
                "            if (applyGyroToFrame) {",
                "                rightImageOffset += vec2(gyroX, -gyroY); // Инвертируем Y для правильного направления",
                "            }",
                "            ",
                "            rightUV += rightImageOffset;",
                "            ",
                "            // Применяем смещение изображения относительно центра фрейма",
                "            vec2 frameCenter = vec2(0.5, 0.5); // Центр фрейма в UV координатах",
                "            vec2 imageCenterOffset = vec2(rightImageCenterOffsetX, rightImageCenterOffsetY);",
                "            rightUV = rightUV - frameCenter + imageCenterOffset;",
                "            rightUV = rightUV + frameCenter; // Возвращаем к фрейм координатам",
                "        }",
                "        ",
                "        // Проверяем, находится ли точка внутри правого фрейма",
                "        if (rightUV.x >= 0.0 && rightUV.x <= 1.0 && rightUV.y >= 0.0 && rightUV.y <= 1.0) {",
                "            // Применяем дисторсию - от вогнутой до выпуклой линзы",
                "            vec2 distortedRightUV = distortionCorrection(rightUV, distortionStrength);",
                "            ",
                "            // Проверяем, остались ли координаты в пределах",
                "            if (distortedRightUV.x >= 0.0 && distortedRightUV.x <= 1.0 &&",
                "                distortedRightUV.y >= 0.0 && distortedRightUV.y <= 1.0) {",
                "                ",
                "                // Применяем маску с закругленными углами для правого фрейма",
                "                float rightMask;",
                "                if (syncFrames) {",
                "                    rightMask = roundedFrameMask(vUV, adjustedLeftFrameX + 0.5, leftFrameY, leftFrameWidth, leftFrameHeight, cornerRadius);",
                "                } else {",
                "                    rightMask = roundedFrameMask(vUV, adjustedRightFrameX, rightFrameY, rightFrameWidth, rightFrameHeight, cornerRadius);",
                "                }",
                "                ",
                "                vec4 color = texture2D(screenTexture, distortedRightUV);",
                "                finalColor = vec4(color.rgb * rightMask, color.a);",
                "            }",
                "        }",
                "    }",
                "    ",
                "    // Добавляем вертикальную линию по центру экрана, если нужно",
                "    if (showCenterLine) {",
                "        float centerLineWidth = 0.002; // Толщина линии (0.2% ширины экрана)",
                "        float centerLinePosition = 0.5; // Центр экрана",
                "        float distanceFromCenter = abs(vUV.x - centerLinePosition);",
                "        float lineMask = 1.0 - smoothstep(0.0, centerLineWidth, distanceFromCenter);",
                "        vec3 lineColor = vec3(centerLineColor);",
                "        finalColor = mix(finalColor, vec4(lineColor, 1.0), lineMask);",
                "    }",
                "    ",
                "    gl_FragColor = finalColor;",
                "}"
            });
        }

        @Override
        public void use() {
            super.use();
            setUniformInt("vrEnabled", vrEnabled ? 1 : 0);
            setUniformInt("showCenterLine", showCenterLine ? 1 : 0);
            setUniformFloat("centerLineColor", centerLineColor);
            setUniformInt("vrMode", vrMode);
            setUniformInt("syncFrames", syncFrames ? 1 : 0);
            
            // Устанавливаем параметры фреймов
            setUniformFloat("leftFrameX", leftFrameX);
            setUniformFloat("leftFrameY", leftFrameY);
            setUniformFloat("leftFrameWidth", leftFrameWidth);
            setUniformFloat("leftFrameHeight", leftFrameHeight);
            
            setUniformFloat("rightFrameX", rightFrameX);
            setUniformFloat("rightFrameY", rightFrameY);
            setUniformFloat("rightFrameWidth", rightFrameWidth);
            setUniformFloat("rightFrameHeight", rightFrameHeight);
            
            // Устанавливаем параметры смещения изображения
            setUniformFloat("leftImageOffsetX", leftImageOffsetX);
            setUniformFloat("leftImageOffsetY", leftImageOffsetY);
            setUniformFloat("rightImageOffsetX", rightImageOffsetX);
            setUniformFloat("rightImageOffsetY", rightImageOffsetY);
            
            // Устанавливаем параметры смещения изображения от центра фрейма
            setUniformFloat("leftImageCenterOffsetX", leftImageCenterOffsetX);
            setUniformFloat("leftImageCenterOffsetY", leftImageCenterOffsetY);
            setUniformFloat("rightImageCenterOffsetX", rightImageCenterOffsetX);
            setUniformFloat("rightImageCenterOffsetY", rightImageCenterOffsetY);
            
            // Устанавливаем параметры обрезки изображения
            setUniformFloat("leftImageClipRight", leftImageClipRight);
            setUniformFloat("rightImageClipLeft", rightImageClipLeft);
            
            // Устанавливаем параметры IPD
            setUniformFloat("ipdAdjustment", ipdAdjustment);
            
            // Устанавливаем параметры гироскопа
            setUniformInt("useGyroForMovement", useGyroForMovement ? 1 : 0);
            setUniformFloat("gyroX", gyroX);
            setUniformFloat("gyroY", gyroY);
            setUniformFloat("gyroSensitivity", gyroSensitivity);
            
            // Устанавливаем параметры дисторсии и закругления
            setUniformFloat("distortionStrength", distortionStrength);
            setUniformFloat("edgeFeathering", edgeFeathering);
            setUniformFloat("cornerRadius", cornerRadius);
            
            // Устанавливаем gyroMode
            setUniformInt("gyroMode", gyroMode);
        }
    }
}