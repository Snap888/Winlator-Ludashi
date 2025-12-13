package com.winlator.cmod.renderer.effects;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VRProfile {
    private String name;
    private boolean enabled;
    private boolean showCenterLine;
    private float centerLineColor;
    private boolean syncFrames;
    private int vrMode;
    private float leftFrameX, leftFrameY, leftFrameWidth, leftFrameHeight;
    private float rightFrameX, rightFrameY, rightFrameWidth, rightFrameHeight;
    private float leftImageOffsetX, leftImageOffsetY, rightImageOffsetX, rightImageOffsetY;
    private float leftImageClipRight, rightImageClipLeft;
    private float ipdAdjustment; // Межзрачковая коррекция
    private float leftImageCenterOffsetX, leftImageCenterOffsetY; // Смещение изображения от центра фрейма (левый глаз)
    private float rightImageCenterOffsetX, rightImageCenterOffsetY; // Смещение изображения от центра фрейма (правый глаз)
    private boolean useGyroForMovement;
    private float gyroSensitivity;
    private float distortionStrength; // Теперь в диапазоне [-1.0, 1.0]
    private float edgeFeathering;
    private float cornerRadius;

    public VRProfile() {
        resetToDefaults();
    }

    public VRProfile(String name) {
        this.name = name;
        resetToDefaults();
    }

    public VRProfile(String name, VREffect effect) {
        this.name = name;
        loadFromEffect(effect);
    }

    public void loadFromEffect(VREffect effect) {
        this.name = this.name != null ? this.name : "Default";
        this.enabled = effect.isVREnabled();
        this.showCenterLine = effect.isShowCenterLine();
        this.centerLineColor = effect.getCenterLineColor();
        this.syncFrames = effect.isSyncFrames();
        this.vrMode = effect.getVrMode();
        this.leftFrameX = effect.getLeftFrameX();
        this.leftFrameY = effect.getLeftFrameY();
        this.leftFrameWidth = effect.getLeftFrameWidth();
        this.leftFrameHeight = effect.getLeftFrameHeight();
        this.rightFrameX = effect.getRightFrameX();
        this.rightFrameY = effect.getRightFrameY();
        this.rightFrameWidth = effect.getRightFrameWidth();
        this.rightFrameHeight = effect.getRightFrameHeight();
        this.leftImageOffsetX = effect.getLeftImageOffsetX();
        this.leftImageOffsetY = effect.getLeftImageOffsetY();
        this.rightImageOffsetX = effect.getRightImageOffsetX();
        this.rightImageOffsetY = effect.getRightImageOffsetY();
        this.leftImageClipRight = effect.getLeftImageClipRight();
        this.rightImageClipLeft = effect.getRightImageClipLeft();
        this.ipdAdjustment = effect.getIPDAdjustment();
        this.leftImageCenterOffsetX = effect.getLeftImageCenterOffsetX();
        this.leftImageCenterOffsetY = effect.getLeftImageCenterOffsetY();
        this.rightImageCenterOffsetX = effect.getRightImageCenterOffsetX();
        this.rightImageCenterOffsetY = effect.getRightImageCenterOffsetY();
        this.useGyroForMovement = effect.isUseGyroForMovement();
        this.gyroSensitivity = effect.getGyroSensitivity();
        this.distortionStrength = effect.getDistortionStrength();
        this.edgeFeathering = effect.getEdgeFeathering();
        this.cornerRadius = effect.getCornerRadius();
    }

    public void applyToEffect(VREffect effect) {
        effect.setVREnabled(this.enabled);
        effect.setShowCenterLine(this.showCenterLine);
        effect.setCenterLineColor(this.centerLineColor);
        effect.setSyncFrames(this.syncFrames);
        effect.setVrMode(this.vrMode);
        effect.setLeftFrameX(this.leftFrameX);
        effect.setLeftFrameY(this.leftFrameY);
        effect.setLeftFrameWidth(this.leftFrameWidth);
        effect.setLeftFrameHeight(this.leftFrameHeight);
        effect.setRightFrameX(this.rightFrameX);
        effect.setRightFrameY(this.rightFrameY);
        effect.setRightFrameWidth(this.rightFrameWidth);
        effect.setRightFrameHeight(this.rightFrameHeight);
        effect.setLeftImageOffsetX(this.leftImageOffsetX);
        effect.setLeftImageOffsetY(this.leftImageOffsetY);
        effect.setRightImageOffsetX(this.rightImageOffsetX);
        effect.setRightImageOffsetY(this.rightImageOffsetY);
        effect.setLeftImageClipRight(this.leftImageClipRight);
        effect.setRightImageClipLeft(this.rightImageClipLeft);
        effect.setIPDAdjustment(this.ipdAdjustment);
        effect.setLeftImageCenterOffsetX(this.leftImageCenterOffsetX);
        effect.setLeftImageCenterOffsetY(this.leftImageCenterOffsetY);
        effect.setRightImageCenterOffsetX(this.rightImageCenterOffsetX);
        effect.setRightImageCenterOffsetY(this.rightImageCenterOffsetY);
        effect.setUseGyroForMovement(this.useGyroForMovement);
        effect.setGyroSensitivity(this.gyroSensitivity);
        effect.setDistortionStrength(this.distortionStrength);
        effect.setEdgeFeathering(this.edgeFeathering);
        effect.setCornerRadius(this.cornerRadius);
    }

    public void resetToDefaults() {
        this.name = "Default";
        this.enabled = false;
        this.showCenterLine = false;
        this.centerLineColor = 1.0f;
        this.syncFrames = true;
        this.vrMode = 0; // Split screen
        this.ipdAdjustment = 0.0f; // Нет коррекции IPD по умолчанию
        this.leftImageCenterOffsetX = 0.0f;
        this.leftImageCenterOffsetY = 0.0f;
        this.rightImageCenterOffsetX = 0.0f;
        this.rightImageCenterOffsetY = 0.0f;
        this.leftFrameX = 0.0f;
        this.leftFrameY = 0.0f;
        this.leftFrameWidth = 0.5f;  // Левая половина
        this.leftFrameHeight = 1.0f;
        this.rightFrameX = 0.5f;     // Правая половина
        this.rightFrameY = 0.0f;
        this.rightFrameWidth = 0.5f;
        this.rightFrameHeight = 1.0f;
        // Смещения изображения
        this.leftImageOffsetX = 0.02f;  // Немного смещаем вправо (закрывает правую часть)
        this.leftImageOffsetY = 0.0f;
        this.rightImageOffsetX = -0.02f; // Немного смещаем влево (закрывает левую часть)
        this.rightImageOffsetY = 0.0f;
        // Обрезка изображения
        this.leftImageClipRight = 0.0f; // Нет обрезки по умолчанию
        this.rightImageClipLeft = 0.0f; // Нет обрезки по умолчанию
        this.useGyroForMovement = false;
        this.gyroSensitivity = 0.1f;
        this.distortionStrength = 0.3f; // Умеренная выпуклая дисторсия
        this.edgeFeathering = 0.02f;
        this.cornerRadius = 0.05f;
    }

    // Геттеры и сеттеры
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isShowCenterLine() {
        return showCenterLine;
    }

    public void setShowCenterLine(boolean showCenterLine) {
        this.showCenterLine = showCenterLine;
    }

    public float getCenterLineColor() {
        return centerLineColor;
    }

    public void setCenterLineColor(float centerLineColor) {
        this.centerLineColor = centerLineColor;
    }

    public boolean isSyncFrames() {
        return syncFrames;
    }

    public void setSyncFrames(boolean syncFrames) {
        this.syncFrames = syncFrames;
    }

    public int getVrMode() {
        return vrMode;
    }

    public void setVrMode(int vrMode) {
        this.vrMode = vrMode;
    }

    public float getIPDAdjustment() {
        return ipdAdjustment;
    }

    public void setIPDAdjustment(float ipdAdjustment) {
        this.ipdAdjustment = Math.max(-0.5f, Math.min(0.5f, ipdAdjustment));
    }

    public float getLeftImageCenterOffsetX() {
        return leftImageCenterOffsetX;
    }

    public void setLeftImageCenterOffsetX(float leftImageCenterOffsetX) {
        this.leftImageCenterOffsetX = Math.max(-1.0f, Math.min(1.0f, leftImageCenterOffsetX));
    }

    public float getLeftImageCenterOffsetY() {
        return leftImageCenterOffsetY;
    }

    public void setLeftImageCenterOffsetY(float leftImageCenterOffsetY) {
        this.leftImageCenterOffsetY = Math.max(-1.0f, Math.min(1.0f, leftImageCenterOffsetY));
    }

    public float getRightImageCenterOffsetX() {
        return rightImageCenterOffsetX;
    }

    public void setRightImageCenterOffsetX(float rightImageCenterOffsetX) {
        this.rightImageCenterOffsetX = Math.max(-1.0f, Math.min(1.0f, rightImageCenterOffsetX));
    }

    public float getRightImageCenterOffsetY() {
        return rightImageCenterOffsetY;
    }

    public void setRightImageCenterOffsetY(float rightImageCenterOffsetY) {
        this.rightImageCenterOffsetY = Math.max(-1.0f, Math.min(1.0f, rightImageCenterOffsetY));
    }

    public float getLeftFrameX() {
        return leftFrameX;
    }

    public void setLeftFrameX(float leftFrameX) {
        this.leftFrameX = leftFrameX;
    }

    public float getLeftFrameY() {
        return leftFrameY;
    }

    public void setLeftFrameY(float leftFrameY) {
        this.leftFrameY = leftFrameY;
    }

    public float getLeftFrameWidth() {
        return leftFrameWidth;
    }

    public void setLeftFrameWidth(float leftFrameWidth) {
        this.leftFrameWidth = leftFrameWidth;
    }

    public float getLeftFrameHeight() {
        return leftFrameHeight;
    }

    public void setLeftFrameHeight(float leftFrameHeight) {
        this.leftFrameHeight = leftFrameHeight;
    }

    public float getRightFrameX() {
        return rightFrameX;
    }

    public void setRightFrameX(float rightFrameX) {
        this.rightFrameX = rightFrameX;
    }

    public float getRightFrameY() {
        return rightFrameY;
    }

    public void setRightFrameY(float rightFrameY) {
        this.rightFrameY = rightFrameY;
    }

    public float getRightFrameWidth() {
        return rightFrameWidth;
    }

    public void setRightFrameWidth(float rightFrameWidth) {
        this.rightFrameWidth = rightFrameWidth;
    }

    public float getRightFrameHeight() {
        return rightFrameHeight;
    }

    public void setRightFrameHeight(float rightFrameHeight) {
        this.rightFrameHeight = rightFrameHeight;
    }

    public float getLeftImageOffsetX() {
        return leftImageOffsetX;
    }

    public void setLeftImageOffsetX(float leftImageOffsetX) {
        this.leftImageOffsetX = leftImageOffsetX;
    }

    public float getLeftImageOffsetY() {
        return leftImageOffsetY;
    }

    public void setLeftImageOffsetY(float leftImageOffsetY) {
        this.leftImageOffsetY = leftImageOffsetY;
    }

    public float getRightImageOffsetX() {
        return rightImageOffsetX;
    }

    public void setRightImageOffsetX(float rightImageOffsetX) {
        this.rightImageOffsetX = rightImageOffsetX;
    }

    public float getRightImageOffsetY() {
        return rightImageOffsetY;
    }

    public void setRightImageOffsetY(float rightImageOffsetY) {
        this.rightImageOffsetY = rightImageOffsetY;
    }

    public float getLeftImageClipRight() {
        return leftImageClipRight;
    }

    public void setLeftImageClipRight(float leftImageClipRight) {
        this.leftImageClipRight = leftImageClipRight;
    }

    public float getRightImageClipLeft() {
        return rightImageClipLeft;
    }

    public void setRightImageClipLeft(float rightImageClipLeft) {
        this.rightImageClipLeft = rightImageClipLeft;
    }

    public boolean isUseGyroForMovement() {
        return useGyroForMovement;
    }

    public void setUseGyroForMovement(boolean useGyroForMovement) {
        this.useGyroForMovement = useGyroForMovement;
    }

    public float getGyroSensitivity() {
        return gyroSensitivity;
    }

    public void setGyroSensitivity(float gyroSensitivity) {
        this.gyroSensitivity = gyroSensitivity;
    }

    public float getDistortionStrength() {
        return distortionStrength;
    }

    public void setDistortionStrength(float distortionStrength) {
        // Ограничиваем значение в диапазоне от -1.0 до 1.0
        this.distortionStrength = Math.max(-1.0f, Math.min(1.0f, distortionStrength));
    }

    public float getEdgeFeathering() {
        return edgeFeathering;
    }

    public void setEdgeFeathering(float edgeFeathering) {
        this.edgeFeathering = edgeFeathering;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    // JSON serialization
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("enabled", enabled);
        json.put("showCenterLine", showCenterLine);
        json.put("centerLineColor", centerLineColor);
        json.put("syncFrames", syncFrames);
        json.put("vrMode", vrMode);
        json.put("ipdAdjustment", ipdAdjustment);
        json.put("leftImageCenterOffsetX", leftImageCenterOffsetX);
        json.put("leftImageCenterOffsetY", leftImageCenterOffsetY);
        json.put("rightImageCenterOffsetX", rightImageCenterOffsetX);
        json.put("rightImageCenterOffsetY", rightImageCenterOffsetY);
        json.put("leftFrameX", leftFrameX);
        json.put("leftFrameY", leftFrameY);
        json.put("leftFrameWidth", leftFrameWidth);
        json.put("leftFrameHeight", leftFrameHeight);
        json.put("rightFrameX", rightFrameX);
        json.put("rightFrameY", rightFrameY);
        json.put("rightFrameWidth", rightFrameWidth);
        json.put("rightFrameHeight", rightFrameHeight);
        json.put("leftImageOffsetX", leftImageOffsetX);
        json.put("leftImageOffsetY", leftImageOffsetY);
        json.put("rightImageOffsetX", rightImageOffsetX);
        json.put("rightImageOffsetY", rightImageOffsetY);
        json.put("leftImageClipRight", leftImageClipRight);
        json.put("rightImageClipLeft", rightImageClipLeft);
        json.put("useGyroForMovement", useGyroForMovement);
        json.put("gyroSensitivity", gyroSensitivity);
        json.put("distortionStrength", distortionStrength);
        json.put("edgeFeathering", edgeFeathering);
        json.put("cornerRadius", cornerRadius);
        return json;
    }

    public static VRProfile fromJSON(JSONObject json) throws JSONException {
        VRProfile profile = new VRProfile();
        profile.name = json.getString("name");
        profile.enabled = json.getBoolean("enabled");
        profile.showCenterLine = json.getBoolean("showCenterLine");
        profile.centerLineColor = (float) json.getDouble("centerLineColor");
        profile.syncFrames = json.getBoolean("syncFrames");
        profile.vrMode = json.getInt("vrMode");
        profile.ipdAdjustment = (float) json.getDouble("ipdAdjustment");
        profile.leftImageCenterOffsetX = (float) json.getDouble("leftImageCenterOffsetX");
        profile.leftImageCenterOffsetY = (float) json.getDouble("leftImageCenterOffsetY");
        profile.rightImageCenterOffsetX = (float) json.getDouble("rightImageCenterOffsetX");
        profile.rightImageCenterOffsetY = (float) json.getDouble("rightImageCenterOffsetY");
        profile.leftFrameX = (float) json.getDouble("leftFrameX");
        profile.leftFrameY = (float) json.getDouble("leftFrameY");
        profile.leftFrameWidth = (float) json.getDouble("leftFrameWidth");
        profile.leftFrameHeight = (float) json.getDouble("leftFrameHeight");
        profile.rightFrameX = (float) json.getDouble("rightFrameX");
        profile.rightFrameY = (float) json.getDouble("rightFrameY");
        profile.rightFrameWidth = (float) json.getDouble("rightFrameWidth");
        profile.rightFrameHeight = (float) json.getDouble("rightFrameHeight");
        profile.leftImageOffsetX = (float) json.getDouble("leftImageOffsetX");
        profile.leftImageOffsetY = (float) json.getDouble("leftImageOffsetY");
        profile.rightImageOffsetX = (float) json.getDouble("rightImageOffsetX");
        profile.rightImageOffsetY = (float) json.getDouble("rightImageOffsetY");
        profile.leftImageClipRight = (float) json.getDouble("leftImageClipRight");
        profile.rightImageClipLeft = (float) json.getDouble("rightImageClipLeft");
        profile.useGyroForMovement = json.getBoolean("useGyroForMovement");
        profile.gyroSensitivity = (float) json.getDouble("gyroSensitivity");
        profile.distortionStrength = (float) json.getDouble("distortionStrength");
        profile.edgeFeathering = (float) json.getDouble("edgeFeathering");
        profile.cornerRadius = (float) json.getDouble("cornerRadius");
        return profile;
    }
}