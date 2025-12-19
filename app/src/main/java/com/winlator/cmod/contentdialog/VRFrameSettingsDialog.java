package com.winlator.cmod.contentdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.winlator.cmod.R;
import com.winlator.cmod.renderer.effects.VREffect;

public class VRFrameSettingsDialog extends Dialog {
    private final VREffect vrEffect;
    private final OnSettingsChangedListener onSettingsChangedListener;
    private final SharedPreferences prefs;

    private CheckBox syncFramesCheckBox;
    private TextView leftFrameXTextView, leftFrameYTextView, leftFrameWidthTextView, leftFrameHeightTextView;
    private SeekBar leftFrameXSeekBar, leftFrameYSeekBar, leftFrameWidthSeekBar, leftFrameHeightSeekBar;
    private TextView rightFrameXTextView, rightFrameYTextView, rightFrameWidthTextView, rightFrameHeightTextView;
    private SeekBar rightFrameXSeekBar, rightFrameYSeekBar, rightFrameWidthSeekBar, rightFrameHeightSeekBar;
    private TextView leftImageClipRightTextView, rightImageClipLeftTextView;
    private SeekBar leftImageClipRightSeekBar, rightImageClipLeftSeekBar;
    private TextView leftImageCenterOffsetXTextView, leftImageCenterOffsetYTextView;
    private SeekBar leftImageCenterOffsetXSeekBar, leftImageCenterOffsetYSeekBar;
    private TextView rightImageCenterOffsetXTextView, rightImageCenterOffsetYTextView;
    private SeekBar rightImageCenterOffsetXSeekBar, rightImageCenterOffsetYSeekBar;
    private Button centerFramesButton, preset640x480Button, preset800x600Button, preset960x720Button;
    private Button resetButton;

    public interface OnSettingsChangedListener {
        void onSettingsChanged(VREffect vrEffect);
    }

    public VRFrameSettingsDialog(Context context, VREffect vrEffect, OnSettingsChangedListener onSettingsChangedListener) {
        super(context);
        this.vrEffect = vrEffect;
        this.onSettingsChangedListener = onSettingsChangedListener;
        this.prefs = context.getSharedPreferences("vr_frame_settings", Context.MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.vr_frame_settings_dialog);
        setTitle(R.string.vr_frame_settings);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        syncFramesCheckBox = findViewById(R.id.sync_frames_checkbox);
        
        // Left frame controls
        leftFrameXTextView = findViewById(R.id.left_frame_x_textview);
        leftFrameYTextView = findViewById(R.id.left_frame_y_textview);
        leftFrameWidthTextView = findViewById(R.id.left_frame_width_textview);
        leftFrameHeightTextView = findViewById(R.id.left_frame_height_textview);
        
        leftFrameXSeekBar = findViewById(R.id.left_frame_x_seekbar);
        leftFrameYSeekBar = findViewById(R.id.left_frame_y_seekbar);
        leftFrameWidthSeekBar = findViewById(R.id.left_frame_width_seekbar);
        leftFrameHeightSeekBar = findViewById(R.id.left_frame_height_seekbar);
        
        // Right frame controls
        rightFrameXTextView = findViewById(R.id.right_frame_x_textview);
        rightFrameYTextView = findViewById(R.id.right_frame_y_textview);
        rightFrameWidthTextView = findViewById(R.id.right_frame_width_textview);
        rightFrameHeightTextView = findViewById(R.id.right_frame_height_textview);
        
        rightFrameXSeekBar = findViewById(R.id.right_frame_x_seekbar);
        rightFrameYSeekBar = findViewById(R.id.right_frame_y_seekbar);
        rightFrameWidthSeekBar = findViewById(R.id.right_frame_width_seekbar);
        rightFrameHeightSeekBar = findViewById(R.id.right_frame_height_seekbar);
        
        // Image clip controls
        leftImageClipRightTextView = findViewById(R.id.left_image_clip_right_textview);
        rightImageClipLeftTextView = findViewById(R.id.right_image_clip_left_textview);
        leftImageClipRightSeekBar = findViewById(R.id.left_image_clip_right_seekbar);
        rightImageClipLeftSeekBar = findViewById(R.id.right_image_clip_left_seekbar);
        
        // Image center offset controls
        leftImageCenterOffsetXTextView = findViewById(R.id.left_image_center_offset_x_textview);
        leftImageCenterOffsetYTextView = findViewById(R.id.left_image_center_offset_y_textview);
        leftImageCenterOffsetXSeekBar = findViewById(R.id.left_image_center_offset_x_seekbar);
        leftImageCenterOffsetYSeekBar = findViewById(R.id.left_image_center_offset_y_seekbar);
        
        rightImageCenterOffsetXTextView = findViewById(R.id.right_image_center_offset_x_textview);
        rightImageCenterOffsetYTextView = findViewById(R.id.right_image_center_offset_y_textview);
        rightImageCenterOffsetXSeekBar = findViewById(R.id.right_image_center_offset_x_seekbar);
        rightImageCenterOffsetYSeekBar = findViewById(R.id.right_image_center_offset_y_seekbar);
        
        // Buttons
        centerFramesButton = findViewById(R.id.center_frames_button);
        preset640x480Button = findViewById(R.id.preset_640x480_button);
        preset800x600Button = findViewById(R.id.preset_800x600_button);
        preset960x720Button = findViewById(R.id.preset_960x720_button);
        resetButton = findViewById(R.id.reset_button);
    }

    private void loadSettings() {
        syncFramesCheckBox.setChecked(prefs.getBoolean("sync_frames", true));
        
        // Load left frame settings
        int leftFrameX = prefs.getInt("left_frame_x", 0);
        int leftFrameY = prefs.getInt("left_frame_y", 0);
        int leftFrameWidth = prefs.getInt("left_frame_width", 50);
        int leftFrameHeight = prefs.getInt("left_frame_height", 100);
        
        leftFrameXSeekBar.setProgress(leftFrameX);
        leftFrameYSeekBar.setProgress(leftFrameY);
        leftFrameWidthSeekBar.setProgress(leftFrameWidth);
        leftFrameHeightSeekBar.setProgress(leftFrameHeight);
        
        leftFrameXTextView.setText(String.valueOf(leftFrameX / 100.0f));
        leftFrameYTextView.setText(String.valueOf(leftFrameY / 100.0f));
        leftFrameWidthTextView.setText(String.valueOf(leftFrameWidth / 100.0f));
        leftFrameHeightTextView.setText(String.valueOf(leftFrameHeight / 100.0f));
        
        // Load right frame settings
        int rightFrameX = prefs.getInt("right_frame_x", 50);
        int rightFrameY = prefs.getInt("right_frame_y", 0);
        int rightFrameWidth = prefs.getInt("right_frame_width", 50);
        int rightFrameHeight = prefs.getInt("right_frame_height", 100);
        
        rightFrameXSeekBar.setProgress(rightFrameX);
        rightFrameYSeekBar.setProgress(rightFrameY);
        rightFrameWidthSeekBar.setProgress(rightFrameWidth);
        rightFrameHeightSeekBar.setProgress(rightFrameHeight);
        
        rightFrameXTextView.setText(String.valueOf(rightFrameX / 100.0f));
        rightFrameYTextView.setText(String.valueOf(rightFrameY / 100.0f));
        rightFrameWidthTextView.setText(String.valueOf(rightFrameWidth / 100.0f));
        rightFrameHeightTextView.setText(String.valueOf(rightFrameHeight / 100.0f));
        
        // Load image clip settings
        int leftImageClipRight = prefs.getInt("left_image_clip_right", 0);
        int rightImageClipLeft = prefs.getInt("right_image_clip_left", 0);
        
        leftImageClipRightSeekBar.setProgress(leftImageClipRight);
        rightImageClipLeftSeekBar.setProgress(rightImageClipLeft);
        
        leftImageClipRightTextView.setText(String.valueOf(leftImageClipRight / 100.0f));
        rightImageClipLeftTextView.setText(String.valueOf(rightImageClipLeft / 100.0f));
        
        // Load image center offset settings
        int leftImageCenterOffsetX = prefs.getInt("left_image_center_offset_x", 0);
        int leftImageCenterOffsetY = prefs.getInt("left_image_center_offset_y", 0);
        int rightImageCenterOffsetX = prefs.getInt("right_image_center_offset_x", 0);
        int rightImageCenterOffsetY = prefs.getInt("right_image_center_offset_y", 0);
        
        leftImageCenterOffsetXSeekBar.setProgress(leftImageCenterOffsetX + 50);
        leftImageCenterOffsetYSeekBar.setProgress(leftImageCenterOffsetY + 50);
        rightImageCenterOffsetXSeekBar.setProgress(rightImageCenterOffsetX + 50);
        rightImageCenterOffsetYSeekBar.setProgress(rightImageCenterOffsetY + 50);
        
        leftImageCenterOffsetXTextView.setText(String.valueOf(leftImageCenterOffsetX / 100.0f));
        leftImageCenterOffsetYTextView.setText(String.valueOf(leftImageCenterOffsetY / 100.0f));
        rightImageCenterOffsetXTextView.setText(String.valueOf(rightImageCenterOffsetX / 100.0f));
        rightImageCenterOffsetYTextView.setText(String.valueOf(rightImageCenterOffsetY / 100.0f));
        
        updateSyncControls(!syncFramesCheckBox.isChecked());
    }

    private void setupListeners() {
        syncFramesCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vrEffect.setSyncFrames(isChecked);
            updateSyncControls(!isChecked);
            saveSettings();
            if (onSettingsChangedListener != null) {
                onSettingsChangedListener.onSettingsChanged(vrEffect);
            }
        });

        setupSeekBarListeners();
        setupButtons();
    }

    private void setupSeekBarListeners() {
        // Left frame listeners - всегда активны
        setupSeekBarWithTextView(leftFrameXSeekBar, leftFrameXTextView, 
            value -> vrEffect.setLeftFrameX(value / 100.0f));
        setupSeekBarWithTextView(leftFrameYSeekBar, leftFrameYTextView, 
            value -> {
                vrEffect.setLeftFrameY(value / 100.0f);
                if (vrEffect.isSyncFrames()) {
                    vrEffect.setRightFrameY(value / 100.0f);
                    rightFrameYSeekBar.setProgress(value);
                    rightFrameYTextView.setText(String.valueOf(value / 100.0f));
                }
            });
        setupSeekBarWithTextView(leftFrameWidthSeekBar, leftFrameWidthTextView, 
            value -> {
                vrEffect.setLeftFrameWidth(value / 100.0f);
                if (vrEffect.isSyncFrames()) {
                    vrEffect.setRightFrameWidth(value / 100.0f);
                    rightFrameWidthSeekBar.setProgress(value);
                    rightFrameWidthTextView.setText(String.valueOf(value / 100.0f));
                }
            });
        setupSeekBarWithTextView(leftFrameHeightSeekBar, leftFrameHeightTextView, 
            value -> {
                vrEffect.setLeftFrameHeight(value / 100.0f);
                if (vrEffect.isSyncFrames()) {
                    vrEffect.setRightFrameHeight(value / 100.0f);
                    rightFrameHeightSeekBar.setProgress(value);
                    rightFrameHeightTextView.setText(String.valueOf(value / 100.0f));
                }
            });

        // Right frame listeners - X всегда активен, остальные только при выключенной синхронизации
        setupSeekBarWithTextView(rightFrameXSeekBar, rightFrameXTextView, 
            value -> vrEffect.setRightFrameX(value / 100.0f));
            
        setupSeekBarWithTextView(rightFrameYSeekBar, rightFrameYTextView, 
            value -> {
                if (!vrEffect.isSyncFrames()) {
                    vrEffect.setRightFrameY(value / 100.0f);
                }
            });
        setupSeekBarWithTextView(rightFrameWidthSeekBar, rightFrameWidthTextView, 
            value -> {
                if (!vrEffect.isSyncFrames()) {
                    vrEffect.setRightFrameWidth(value / 100.0f);
                }
            });
        setupSeekBarWithTextView(rightFrameHeightSeekBar, rightFrameHeightTextView, 
            value -> {
                if (!vrEffect.isSyncFrames()) {
                    vrEffect.setRightFrameHeight(value / 100.0f);
                }
            });
            
        // Image clip listeners - всегда активны
        setupSeekBarWithTextView(leftImageClipRightSeekBar, leftImageClipRightTextView,
            value -> vrEffect.setLeftImageClipRight(value / 100.0f));
        setupSeekBarWithTextView(rightImageClipLeftSeekBar, rightImageClipLeftTextView,
            value -> vrEffect.setRightImageClipLeft(value / 100.0f));
            
        // Image center offset listeners (left eye) - всегда активны
        setupSeekBarWithTextView(leftImageCenterOffsetXSeekBar, leftImageCenterOffsetXTextView,
            value -> {
                float offset = (value - 50) / 100.0f;
                vrEffect.setLeftImageCenterOffsetX(offset);
            });
        setupSeekBarWithTextView(leftImageCenterOffsetYSeekBar, leftImageCenterOffsetYTextView,
            value -> {
                float offset = (value - 50) / 100.0f;
                vrEffect.setLeftImageCenterOffsetY(offset);
                if (vrEffect.isSyncFrames()) {
                    vrEffect.setRightImageCenterOffsetY(offset);
                    rightImageCenterOffsetYSeekBar.setProgress(value);
                    rightImageCenterOffsetYTextView.setText(String.valueOf(offset));
                }
            });
            
        // Image center offset listeners (right eye) - X всегда активен, Y только при выключенной синхронизации
        setupSeekBarWithTextView(rightImageCenterOffsetXSeekBar, rightImageCenterOffsetXTextView,
            value -> {
                float offset = (value - 50) / 100.0f;
                vrEffect.setRightImageCenterOffsetX(offset);
            });
        setupSeekBarWithTextView(rightImageCenterOffsetYSeekBar, rightImageCenterOffsetYTextView,
            value -> {
                if (!vrEffect.isSyncFrames()) {
                    float offset = (value - 50) / 100.0f;
                    vrEffect.setRightImageCenterOffsetY(offset);
                }
            });
    }

    private void setupSeekBarWithTextView(SeekBar seekBar, TextView textView, ValueSetter setter) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress;
                if (textView.getText().toString().contains("offset")) {
                    value = progress - 50;
                }
                textView.setText(String.valueOf(value / 100.0f));
                if (fromUser) {
                    setter.setValue(value);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
                if (onSettingsChangedListener != null) {
                    onSettingsChangedListener.onSettingsChanged(vrEffect);
                }
            }
        });
    }

    private void setupButtons() {
        centerFramesButton.setOnClickListener(v -> {
            float leftWidth = 0.5f;
            float rightWidth = 0.5f;
            
            if (vrEffect.isSyncFrames()) {
                rightWidth = vrEffect.getLeftFrameWidth();
            }
            
            vrEffect.setLeftFrameX(0.0f);
            vrEffect.setLeftFrameY(0.0f);
            vrEffect.setLeftFrameWidth(leftWidth);
            vrEffect.setLeftFrameHeight(1.0f);
            
            vrEffect.setRightFrameX(1.0f - rightWidth);
            vrEffect.setRightFrameY(0.0f);
            vrEffect.setRightFrameWidth(rightWidth);
            vrEffect.setRightFrameHeight(1.0f);
            
            updateSeekBarsFromEffect();
            saveSettings();
            if (onSettingsChangedListener != null) {
                onSettingsChangedListener.onSettingsChanged(vrEffect);
            }
        });

        preset640x480Button.setOnClickListener(v -> applyPreset(640, 480));
        preset800x600Button.setOnClickListener(v -> applyPreset(800, 600));
        preset960x720Button.setOnClickListener(v -> applyPreset(960, 720));

        resetButton.setOnClickListener(v -> {
            vrEffect.resetToDefaults();
            loadSettings();
            if (onSettingsChangedListener != null) {
                onSettingsChangedListener.onSettingsChanged(vrEffect);
            }
            saveSettings();
        });
    }

    private void applyPreset(int width, int height) {
        float aspectRatio = (float) width / height;
        float screenWidth = 1.0f;
        float screenHeight = 1.0f;
        
        float frameWidth, frameHeight;
        if (aspectRatio > 1.0f) {
            frameWidth = screenWidth * 0.5f;
            frameHeight = frameWidth / aspectRatio;
        } else {
            frameHeight = screenHeight * 0.5f;
            frameWidth = frameHeight * aspectRatio;
        }
        
        float offsetY = (screenHeight - frameHeight) / 2.0f;
        
        vrEffect.setLeftFrameX(0.0f);
        vrEffect.setLeftFrameY(offsetY);
        vrEffect.setLeftFrameWidth(frameWidth);
        vrEffect.setLeftFrameHeight(frameHeight);
        
        if (!vrEffect.isSyncFrames()) {
            float rightFrameWidth = frameWidth;
            vrEffect.setRightFrameX(1.0f - rightFrameWidth);
            vrEffect.setRightFrameY(offsetY);
            vrEffect.setRightFrameWidth(rightFrameWidth);
            vrEffect.setRightFrameHeight(frameHeight);
        } else {
            vrEffect.setRightFrameX(1.0f - frameWidth);
            vrEffect.setRightFrameY(offsetY);
            vrEffect.setRightFrameWidth(frameWidth);
            vrEffect.setRightFrameHeight(frameHeight);
        }
        
        updateSeekBarsFromEffect();
        saveSettings();
        if (onSettingsChangedListener != null) {
            onSettingsChangedListener.onSettingsChanged(vrEffect);
        }
    }

    private void updateSeekBarsFromEffect() {
        leftFrameXSeekBar.setProgress(Math.round(vrEffect.getLeftFrameX() * 100));
        leftFrameYSeekBar.setProgress(Math.round(vrEffect.getLeftFrameY() * 100));
        leftFrameWidthSeekBar.setProgress(Math.round(vrEffect.getLeftFrameWidth() * 100));
        leftFrameHeightSeekBar.setProgress(Math.round(vrEffect.getLeftFrameHeight() * 100));
        
        rightFrameXSeekBar.setProgress(Math.round(vrEffect.getRightFrameX() * 100));
        rightFrameYSeekBar.setProgress(Math.round(vrEffect.getRightFrameY() * 100));
        rightFrameWidthSeekBar.setProgress(Math.round(vrEffect.getRightFrameWidth() * 100));
        rightFrameHeightSeekBar.setProgress(Math.round(vrEffect.getRightFrameHeight() * 100));
        
        leftImageClipRightSeekBar.setProgress(Math.round(vrEffect.getLeftImageClipRight() * 100));
        rightImageClipLeftSeekBar.setProgress(Math.round(vrEffect.getRightImageClipLeft() * 100));
        
        leftImageCenterOffsetXSeekBar.setProgress(Math.round(vrEffect.getLeftImageCenterOffsetX() * 100) + 50);
        leftImageCenterOffsetYSeekBar.setProgress(Math.round(vrEffect.getLeftImageCenterOffsetY() * 100) + 50);
        rightImageCenterOffsetXSeekBar.setProgress(Math.round(vrEffect.getRightImageCenterOffsetX() * 100) + 50);
        rightImageCenterOffsetYSeekBar.setProgress(Math.round(vrEffect.getRightImageCenterOffsetY() * 100) + 50);
    }

    private void updateSyncControls(boolean enabled) {
        // Всегда активны: leftFrameX, rightFrameX, все clip и centerOffsetX
        
        // Зависит от синхронизации:
        rightFrameYSeekBar.setEnabled(enabled);
        rightFrameWidthSeekBar.setEnabled(enabled);
        rightFrameHeightSeekBar.setEnabled(enabled);
        
        rightFrameYTextView.setEnabled(enabled);
        rightFrameWidthTextView.setEnabled(enabled);
        rightFrameHeightTextView.setEnabled(enabled);
        
        rightImageCenterOffsetYSeekBar.setEnabled(enabled);
        rightImageCenterOffsetYTextView.setEnabled(enabled);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("sync_frames", vrEffect.isSyncFrames());
        
        editor.putInt("left_frame_x", Math.round(vrEffect.getLeftFrameX() * 100));
        editor.putInt("left_frame_y", Math.round(vrEffect.getLeftFrameY() * 100));
        editor.putInt("left_frame_width", Math.round(vrEffect.getLeftFrameWidth() * 100));
        editor.putInt("left_frame_height", Math.round(vrEffect.getLeftFrameHeight() * 100));
        
        editor.putInt("right_frame_x", Math.round(vrEffect.getRightFrameX() * 100));
        editor.putInt("right_frame_y", Math.round(vrEffect.getRightFrameY() * 100));
        editor.putInt("right_frame_width", Math.round(vrEffect.getRightFrameWidth() * 100));
        editor.putInt("right_frame_height", Math.round(vrEffect.getRightFrameHeight() * 100));
        
        editor.putInt("left_image_clip_right", Math.round(vrEffect.getLeftImageClipRight() * 100));
        editor.putInt("right_image_clip_left", Math.round(vrEffect.getRightImageClipLeft() * 100));
        
        editor.putInt("left_image_center_offset_x", Math.round(vrEffect.getLeftImageCenterOffsetX() * 100));
        editor.putInt("left_image_center_offset_y", Math.round(vrEffect.getLeftImageCenterOffsetY() * 100));
        editor.putInt("right_image_center_offset_x", Math.round(vrEffect.getRightImageCenterOffsetX() * 100));
        editor.putInt("right_image_center_offset_y", Math.round(vrEffect.getRightImageCenterOffsetY() * 100));
        
        editor.apply();
    }

    private interface ValueSetter {
        void setValue(int value);
    }
}