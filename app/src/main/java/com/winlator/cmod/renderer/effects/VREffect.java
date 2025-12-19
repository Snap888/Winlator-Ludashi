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
    private int vrMode = 0;
    private float leftFrameX = 0.0f, leftFrameY = 0.0f, leftFrameWidth = 0.5f, leftFrameHeight = 1.0f;
    private float rightFrameX = 0.5f, rightFrameY = 0.0f, rightFrameWidth = 0.5f, rightFrameHeight = 1.0f;
    private float distortionStrength = 0.3f;
    private float edgeFeathering = 0.02f;
    private float cornerRadius = 0.05f;
    private float leftImageOffsetX = 0.0f, leftImageOffsetY = 0.0f;
    private float rightImageOffsetX = 0.0f, rightImageOffsetY = 0.0f;
    private boolean useGyroForMovement = false;
    private float gyroX = 0.0f, gyroY = 0.0f;
    private float gyroAngleX = 0.0f, gyroAngleY = 0.0f;
    private long lastGyroTime = 0;
    private float gyroSensitivity = 0.1f;
    private float leftImageClipRight = 0.0f;
    private float rightImageClipLeft = 0.0f;
    private float ipdAdjustment = 0.0f;
    private float leftImageCenterOffsetX = 0.0f, leftImageCenterOffsetY = 0.0f;
    private float rightImageCenterOffsetX = 0.0f, rightImageCenterOffsetY = 0.0f;
    private int gyroMode = 0;
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

    public void setEnabled(boolean enabled) {
        this.vrEnabled = enabled;
    }

    public boolean isEnabled() {
        return vrEnabled;
    }

    public void setVREnabled(boolean enabled) {
        if (!this.vrEnabled && enabled) {
            resetGyroAngles();
        }
        this.vrEnabled = enabled;
    }

    public boolean isVREnabled() {
        return vrEnabled;
    }

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

    public void setVrMode(int mode) {
        this.vrMode = mode;
    }

    public int getVrMode() {
        return vrMode;
    }

    public void setSyncFrames(boolean sync) {
        this.syncFrames = sync;
        // При включении синхронизации копируем Y, высоту и ширину из левого фрейма в правый
        if (sync) {
            this.rightFrameY = this.leftFrameY;
            this.rightFrameWidth = this.leftFrameWidth;
            this.rightFrameHeight = this.leftFrameHeight;
            this.rightImageCenterOffsetY = this.leftImageCenterOffsetY;
        }
    }

    public boolean isSyncFrames() {
        return syncFrames;
    }

    public void setIPDAdjustment(float adjustment) {
        this.ipdAdjustment = Math.max(-0.5f, Math.min(0.5f, adjustment));
    }

    public float getIPDAdjustment() {
        return ipdAdjustment;
    }

    public void setLeftImageCenterOffsetX(float offsetX) {
        this.leftImageCenterOffsetX = Math.max(-1.0f, Math.min(1.0f, offsetX));
    }

    public float getLeftImageCenterOffsetX() {
        return leftImageCenterOffsetX;
    }

    public void setLeftImageCenterOffsetY(float offsetY) {
        this.leftImageCenterOffsetY = Math.max(-1.0f, Math.min(1.0f, offsetY));
        // При синхронизации копируем в правый глаз
        if (syncFrames) {
            this.rightImageCenterOffsetY = this.leftImageCenterOffsetY;
        }
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

    public void setLeftFrameX(float x) {
        this.leftFrameX = x;
    }

    public float getLeftFrameX() {
        return leftFrameX;
    }

    public void setLeftFrameY(float y) {
        this.leftFrameY = y;
        // При синхронизации копируем в правый глаз
        if (syncFrames) {
            this.rightFrameY = y;
        }
    }

    public float getLeftFrameY() {
        return leftFrameY;
    }

    public void setLeftFrameWidth(float width) {
        this.leftFrameWidth = width;
        // При синхронизации копируем в правый глаз
        if (syncFrames) {
            this.rightFrameWidth = width;
        }
    }

    public float getLeftFrameWidth() {
        return leftFrameWidth;
    }

    public void setLeftFrameHeight(float height) {
        this.leftFrameHeight = height;
        // При синхронизации копируем в правый глаз
        if (syncFrames) {
            this.rightFrameHeight = height;
        }
    }

    public float getLeftFrameHeight() {
        return leftFrameHeight;
    }

    public void setRightFrameX(float x) {
        this.rightFrameX = x;
    }

    public float getRightFrameX() {
        return rightFrameX;
    }

    public void setRightFrameY(float y) {
        if (!syncFrames) {
            this.rightFrameY = y;
        }
    }

    public float getRightFrameY() {
        return rightFrameY;
    }

    public void setRightFrameWidth(float width) {
        if (!syncFrames) {
            this.rightFrameWidth = width;
        }
    }

    public float getRightFrameWidth() {
        return rightFrameWidth;
    }

    public void setRightFrameHeight(float height) {
        if (!syncFrames) {
            this.rightFrameHeight = height;
        }
    }

    public float getRightFrameHeight() {
        return rightFrameHeight;
    }

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
        
        float deltaTime = (currentTime - lastGyroTime) / 1000.0f;
        lastGyroTime = currentTime;
        
        gyroAngleX += gyroX * deltaTime;
        gyroAngleY += gyroY * deltaTime;
        
        gyroAngleX = Math.max(-1.0f, Math.min(1.0f, gyroAngleX));
        gyroAngleY = Math.max(-1.0f, Math.min(1.0f, gyroAngleY));
        
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

    public void resetGyroAngles() {
        this.gyroAngleX = 0.0f;
        this.gyroAngleY = 0.0f;
        this.gyroX = 0.0f;
        this.gyroY = 0.0f;
        this.lastGyroTime = 0;
    }

    public void setDistortionStrength(float strength) {
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

    public void setGyroMode(int mode) {
        this.gyroMode = mode;
    }

    public int getGyroMode() {
        return this.gyroMode;
    }

    public void resetToDefaults() {
        vrEnabled = false;
        showCenterLine = false;
        centerLineColor = 1.0f;
        vrMode = 0;
        syncFrames = true;
        ipdAdjustment = 0.0f;
        
        leftFrameX = 0.0f;
        leftFrameY = 0.0f;
        leftFrameWidth = 0.5f;
        leftFrameHeight = 1.0f;
        
        rightFrameX = 0.5f;
        rightFrameY = 0.0f;
        rightFrameWidth = 0.5f;
        rightFrameHeight = 1.0f;
        
        leftImageOffsetX = 0.02f;
        leftImageOffsetY = 0.0f;
        rightImageOffsetX = -0.02f;
        rightImageOffsetY = 0.0f;
        
        leftImageCenterOffsetX = 0.0f;
        leftImageCenterOffsetY = 0.0f;
        rightImageCenterOffsetX = 0.0f;
        rightImageCenterOffsetY = 0.0f;
        
        leftImageClipRight = 0.0f;
        rightImageClipLeft = 0.0f;
        
        useGyroForMovement = false;
        resetGyroAngles();
        gyroSensitivity = 0.1f;
        
        distortionStrength = 0.3f;
        edgeFeathering = 0.02f;
        cornerRadius = 0.05f;
        gyroMode = 0;
    }

    public void setLeftFrameParams(float x, float y, float width, float height) {
        this.leftFrameX = x;
        this.leftFrameY = y;
        this.leftFrameWidth = width;
        this.leftFrameHeight = height;
    }

    public void setRightFrameParams(float x, float y, float width, float height) {
        this.rightFrameX = x;
        if (!syncFrames) {
            this.rightFrameY = y;
            this.rightFrameWidth = width;
            this.rightFrameHeight = height;
        } else {
            this.rightFrameY = this.leftFrameY;
            this.rightFrameWidth = this.leftFrameWidth;
            this.rightFrameHeight = this.leftFrameHeight;
        }
    }

    public VRProfile createProfile(String name) {
        return new VRProfile(name, this);
    }

    public void loadFromProfile(VRProfile profile) {
        profile.applyToEffect(this);
    }

    private class VREffectMaterial extends ScreenMaterial {
        public VREffectMaterial() {
            super();
            setUniformNames("resolution", "screenTexture", "vrEnabled", "showCenterLine", "centerLineColor", "vrMode",
                          "syncFrames",
                          "leftFrameX", "leftFrameY", "leftFrameWidth", "leftFrameHeight",
                          "rightFrameX", "rightFrameY", "rightFrameWidth", "rightFrameHeight",
                          "leftImageOffsetX", "leftImageOffsetY", "rightImageOffsetX", "rightImageOffsetY",
                          "leftImageClipRight", "rightImageClipLeft", "ipdAdjustment",
                          "leftImageCenterOffsetX", "leftImageCenterOffsetY", "rightImageCenterOffsetX", "rightImageCenterOffsetY",
                          "useGyroForMovement", "gyroX", "gyroY", "gyroSensitivity",
                          "distortionStrength", "edgeFeathering", "cornerRadius",
                          "gyroMode");
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
                "uniform int gyroMode;",
                "varying vec2 vUV;",

                "vec2 distortionCorrection(vec2 uv, float strength) {",
                "    vec2 centered = uv * 2.0 - 1.0;",
                "    float r = length(centered);",
                "    float theta = atan(centered.y, centered.x);",
                "    ",
                "    float correctedR = r;",
                "    ",
                "    if (strength > 0.0) {",
                "        correctedR = r * (1.0 + strength * r * r + strength * strength * r * r * r * r);",
                "        correctedR = min(correctedR, 2.0);",
                "    } else if (strength < 0.0) {",
                "        float absStrength = abs(strength);",
                "        correctedR = r * (1.0 - absStrength * r * r);",
                "        correctedR = max(correctedR, 0.0);",
                "    }",
                "    ",
                "    vec2 corrected = correctedR * vec2(cos(theta), sin(theta));",
                "    return (corrected + 1.0) * 0.5;",
                "}",

                "float roundedFrameMask(vec2 uv, float frameX, float frameY, float frameWidth, float frameHeight, float radius) {",
                "    vec2 frameUV = (uv - vec2(frameX, frameY)) / vec2(frameWidth, frameHeight);",
                "    ",
                "    if (frameUV.x < 0.0 || frameUV.x > 1.0 || frameUV.y < 0.0 || frameUV.y > 1.0) {",
                "        return 0.0;",
                "    }",
                "    ",
                "    float cornerRadius = radius;",
                "    vec2 cornerDist = vec2(0.0);",
                "    ",
                "    cornerDist = vec2(cornerRadius, cornerRadius) - frameUV;",
                "    if (cornerDist.x > 0.0 && cornerDist.y > 0.0) {",
                "        float dist = length(cornerDist);",
                "        if (dist > cornerRadius) return 0.0;",
                "    }",
                "    ",
                "    cornerDist = vec2(1.0 - cornerRadius, cornerRadius) - frameUV;",
                "    if (cornerDist.x < 0.0 && cornerDist.y > 0.0) {",
                "        float dist = length(cornerDist);",
                "        if (dist > cornerRadius) return 0.0;",
                "    }",
                "    ",
                "    cornerDist = vec2(cornerRadius, 1.0 - cornerRadius) - frameUV;",
                "    if (cornerDist.x > 0.0 && cornerDist.y < 0.0) {",
                "        float dist = length(cornerDist);",
                "        if (dist > cornerRadius) return 0.0;",
                "    }",
                "    ",
                "    cornerDist = vec2(1.0 - cornerRadius, 1.0 - cornerRadius) - frameUV;",
                "    if (cornerDist.x < 0.0 && cornerDist.y < 0.0) {",
                "        float dist = length(cornerDist);",
                "        if (dist > cornerRadius) return 0.0;",
                "    }",
                "    ",
                "    return 1.0;",
                "}",

                "void main() {",
                "    if (!vrEnabled) {",
                "        gl_FragColor = texture2D(screenTexture, vUV);",
                "        return;",
                "    }",
                "",
                "    bool applyGyroToFrame = useGyroForMovement && gyroMode == 0;",
                "",
                "    vec4 finalColor = vec4(0.0, 0.0, 0.0, 1.0);",
                "    ",
                "    // Применяем IPD коррекцию к X-координатам фреймов",
                "    float adjustedLeftFrameX = leftFrameX - ipdAdjustment * 0.1;",
                "    float adjustedRightFrameX = rightFrameX + ipdAdjustment * 0.1;",
                "    ",
                "    // Определяем, в каком фрейме находимся",
                "    bool isInLeftFrame = false;",
                "    bool isInRightFrame = false;",
                "    vec2 frameUV;",
                "    ",
                "    // Проверяем левый фрейм",
                "    if (vUV.x >= adjustedLeftFrameX && vUV.x <= adjustedLeftFrameX + leftFrameWidth &&",
                "        vUV.y >= leftFrameY && vUV.y <= leftFrameY + leftFrameHeight) {",
                "        isInLeftFrame = true;",
                "        frameUV = vec2(",
                "            (vUV.x - adjustedLeftFrameX) / leftFrameWidth,",
                "            (vUV.y - leftFrameY) / leftFrameHeight",
                "        );",
                "    }",
                "    ",
                "    // Проверяем правый фрейм",
                "    if (!isInLeftFrame && vUV.x >= adjustedRightFrameX && vUV.x <= adjustedRightFrameX + rightFrameWidth &&",
                "        vUV.y >= rightFrameY && vUV.y <= rightFrameY + rightFrameHeight) {",
                "        isInRightFrame = true;",
                "        frameUV = vec2(",
                "            (vUV.x - adjustedRightFrameX) / rightFrameWidth,",
                "            (vUV.y - rightFrameY) / rightFrameHeight",
                "        );",
                "    }",
                "    ",
                "    if (isInLeftFrame) {",
                "        vec2 imageUV = frameUV;",
                "        ",
                "        // Применяем смещение изображения",
                "        vec2 imageOffset = vec2(leftImageOffsetX, leftImageOffsetY);",
                "        if (applyGyroToFrame) {",
                "            imageOffset += vec2(gyroX, -gyroY);",
                "        }",
                "        imageUV += imageOffset;",
                "        ",
                "        // Применяем смещение от центра фрейма",
                "        vec2 frameCenter = vec2(0.5, 0.5);",
                "        vec2 centerOffset = vec2(leftImageCenterOffsetX, leftImageCenterOffsetY);",
                "        imageUV = imageUV - frameCenter + centerOffset;",
                "        imageUV = imageUV + frameCenter;",
                "        ",
                "        // Применяем обрезку правой части",
                "        if (imageUV.x > 1.0 - leftImageClipRight) {",
                "            imageUV.x = 1.0 - leftImageClipRight;",
                "        }",
                "        ",
                "        // Применяем дисторсию",
                "        vec2 distortedUV = distortionCorrection(imageUV, distortionStrength);",
                "        ",
                "        if (distortedUV.x >= 0.0 && distortedUV.x <= 1.0 &&",
                "            distortedUV.y >= 0.0 && distortedUV.y <= 1.0) {",
                "            ",
                "            float mask = roundedFrameMask(vUV, adjustedLeftFrameX, leftFrameY, leftFrameWidth, leftFrameHeight, cornerRadius);",
                "            ",
                "            vec4 color = texture2D(screenTexture, distortedUV);",
                "            finalColor = vec4(color.rgb * mask, color.a);",
                "        }",
                "    } else if (isInRightFrame) {",
                "        vec2 imageUV = frameUV;",
                "        ",
                "        // Применяем смещение изображения",
                "        vec2 imageOffset = vec2(rightImageOffsetX, rightImageOffsetY);",
                "        if (applyGyroToFrame) {",
                "            imageOffset += vec2(gyroX, -gyroY);",
                "        }",
                "        imageUV += imageOffset;",
                "        ",
                "        // Применяем смещение от центра фрейма",
                "        vec2 frameCenter = vec2(0.5, 0.5);",
                "        vec2 centerOffset;",
                "        if (syncFrames) {",
                "            centerOffset = vec2(rightImageCenterOffsetX, leftImageCenterOffsetY);",
                "        } else {",
                "            centerOffset = vec2(rightImageCenterOffsetX, rightImageCenterOffsetY);",
                "        }",
                "        imageUV = imageUV - frameCenter + centerOffset;",
                "        imageUV = imageUV + frameCenter;",
                "        ",
                "        // Применяем обрезку левой части",
                "        if (imageUV.x < rightImageClipLeft) {",
                "            imageUV.x = rightImageClipLeft;",
                "        }",
                "        ",
                "        // Применяем дисторсию",
                "        vec2 distortedUV = distortionCorrection(imageUV, distortionStrength);",
                "        ",
                "        if (distortedUV.x >= 0.0 && distortedUV.x <= 1.0 &&",
                "            distortedUV.y >= 0.0 && distortedUV.y <= 1.0) {",
                "            ",
                "            float mask = roundedFrameMask(vUV, adjustedRightFrameX, rightFrameY, rightFrameWidth, rightFrameHeight, cornerRadius);",
                "            ",
                "            vec4 color = texture2D(screenTexture, distortedUV);",
                "            finalColor = vec4(color.rgb * mask, color.a);",
                "        }",
                "    }",
                "    ",
                "    // Добавляем вертикальную линию по центру экрана, если нужно",
                "    if (showCenterLine) {",
                "        float centerLineWidth = 0.002;",
                "        float centerLinePosition = 0.5;",
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
            
            setUniformFloat("leftFrameX", leftFrameX);
            setUniformFloat("leftFrameY", leftFrameY);
            setUniformFloat("leftFrameWidth", leftFrameWidth);
            setUniformFloat("leftFrameHeight", leftFrameHeight);
            
            setUniformFloat("rightFrameX", rightFrameX);
            setUniformFloat("rightFrameY", rightFrameY);
            setUniformFloat("rightFrameWidth", rightFrameWidth);
            setUniformFloat("rightFrameHeight", rightFrameHeight);
            
            setUniformFloat("leftImageOffsetX", leftImageOffsetX);
            setUniformFloat("leftImageOffsetY", leftImageOffsetY);
            setUniformFloat("rightImageOffsetX", rightImageOffsetX);
            setUniformFloat("rightImageOffsetY", rightImageOffsetY);
            
            setUniformFloat("leftImageCenterOffsetX", leftImageCenterOffsetX);
            setUniformFloat("leftImageCenterOffsetY", leftImageCenterOffsetY);
            setUniformFloat("rightImageCenterOffsetX", rightImageCenterOffsetX);
            setUniformFloat("rightImageCenterOffsetY", rightImageCenterOffsetY);
            
            setUniformFloat("leftImageClipRight", leftImageClipRight);
            setUniformFloat("rightImageClipLeft", rightImageClipLeft);
            
            setUniformFloat("ipdAdjustment", ipdAdjustment);
            
            setUniformInt("useGyroForMovement", useGyroForMovement ? 1 : 0);
            setUniformFloat("gyroX", gyroX);
            setUniformFloat("gyroY", gyroY);
            setUniformFloat("gyroSensitivity", gyroSensitivity);
            
            setUniformFloat("distortionStrength", distortionStrength);
            setUniformFloat("edgeFeathering", edgeFeathering);
            setUniformFloat("cornerRadius", cornerRadius);
            
            setUniformInt("gyroMode", gyroMode);
        }
    }
}