package com.winlator.cmod.contentdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.winlator.cmod.R;
import com.winlator.cmod.renderer.effects.VREffect;

public class VRSettingsDialog extends Dialog {
    private final VREffect vrEffect;
    private final OnSettingsChangedListener onSettingsChangedListener;
    private final SharedPreferences prefs;

    private CheckBox vrEnabledCheckBox;
    private CheckBox useGyroCheckBox;
    private Spinner vrModeSpinner;
    private Spinner scaleModeSpinner;
    private SeekBar distortionStrengthSeekBar;
    private TextView distortionStrengthTextView;
    private SeekBar edgeFeatheringSeekBar;
    private TextView edgeFeatheringTextView;
    private SeekBar cornerRadiusSeekBar;
    private TextView cornerRadiusTextView;
    private SeekBar gyroSensitivitySeekBar;
    private TextView gyroSensitivityTextView;
    private SeekBar ipdAdjustmentSeekBar;
    private TextView ipdAdjustmentTextView;
    private Button frameSettingsButton;
    private Button profileSettingsButton;
    private Button resetButton;

    public interface OnSettingsChangedListener {
        void onSettingsChanged(VREffect vrEffect);
    }

    public VRSettingsDialog(Context context, VREffect vrEffect, OnSettingsChangedListener onSettingsChangedListener) {
        super(context);
        this.vrEffect = vrEffect;
        this.onSettingsChangedListener = onSettingsChangedListener;
        this.prefs = context.getSharedPreferences("vr_settings", Context.MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.vr_settings_dialog);
        setTitle(R.string.vr_settings_title);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        vrEnabledCheckBox = findViewById(R.id.vr_enabled_checkbox);
        useGyroCheckBox = findViewById(R.id.use_gyro_checkbox);
        vrModeSpinner = findViewById(R.id.vr_mode_spinner);
        scaleModeSpinner = findViewById(R.id.scale_mode_spinner);
        distortionStrengthSeekBar = findViewById(R.id.distortion_strength_seekbar);
        distortionStrengthTextView = findViewById(R.id.distortion_strength_textview);
        edgeFeatheringSeekBar = findViewById(R.id.edge_feathering_seekbar);
        edgeFeatheringTextView = findViewById(R.id.edge_feathering_textview);
        cornerRadiusSeekBar = findViewById(R.id.corner_radius_seekbar);
        cornerRadiusTextView = findViewById(R.id.corner_radius_textview);
        gyroSensitivitySeekBar = findViewById(R.id.gyro_sensitivity_seekbar);
        gyroSensitivityTextView = findViewById(R.id.gyro_sensitivity_textview);
        ipdAdjustmentSeekBar = findViewById(R.id.ipd_adjustment_seekbar);
        ipdAdjustmentTextView = findViewById(R.id.ipd_adjustment_textview);
        frameSettingsButton = findViewById(R.id.frame_settings_button);
        profileSettingsButton = findViewById(R.id.profile_settings_button);
        resetButton = findViewById(R.id.reset_button);

        // Setup spinner adapter for VR mode
        ArrayAdapter<CharSequence> modeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.vr_modes, android.R.layout.simple_spinner_item);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vrModeSpinner.setAdapter(modeAdapter);

        // Setup spinner adapter for scale mode
        ArrayAdapter<CharSequence> scaleModeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.scale_modes, android.R.layout.simple_spinner_item);
        scaleModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scaleModeSpinner.setAdapter(scaleModeAdapter);
        
        // Устанавливаем диапазон SeekBar для дисторсии: от -100 до 100 (для значений -1.0 до 1.0)
        distortionStrengthSeekBar.setMax(200); // 0-200 для диапазона -1.0 до 1.0
        gyroSensitivitySeekBar.setMax(200); // 0-200 для диапазона 0.0 до 2.0
        ipdAdjustmentSeekBar.setMax(100); // -50 до 50 для диапазона -0.5 до 0.5
    }

    private void loadSettings() {
        vrEnabledCheckBox.setChecked(prefs.getBoolean("vr_enabled", false));
        useGyroCheckBox.setChecked(prefs.getBoolean("use_gyro", false));
        vrModeSpinner.setSelection(prefs.getInt("vr_mode", 0));
        scaleModeSpinner.setSelection(prefs.getInt("scale_mode", 0));
        
        // Загружаем значение дисторсии из диапазона -100 до 100
        int distortionProgress = prefs.getInt("distortion_strength", 30); // 30 соответствует 0.3
        distortionStrengthSeekBar.setProgress(distortionProgress + 100); // Сдвигаем на 100 для внутреннего диапазона 0-200
        float distortionValue = (distortionProgress) / 100.0f;
        distortionStrengthTextView.setText(String.valueOf(distortionValue));
        
        int edgeFeathering = prefs.getInt("edge_feathering", 2); // 0-100 scale
        edgeFeatheringSeekBar.setProgress(edgeFeathering);
        edgeFeatheringTextView.setText(String.valueOf(edgeFeathering / 1000.0f));
        
        int cornerRadius = prefs.getInt("corner_radius", 5); // 0-100 scale
        cornerRadiusSeekBar.setProgress(cornerRadius);
        cornerRadiusTextView.setText(String.valueOf(cornerRadius / 100.0f));
        
        // Загружаем чувствительность гироскопа
        int gyroSensitivityProgress = prefs.getInt("gyro_sensitivity", 10); // 10 соответствует 0.1
        gyroSensitivitySeekBar.setProgress(gyroSensitivityProgress * 10); // 0.1 * 100 = 10
        float gyroSensitivityValue = gyroSensitivityProgress / 100.0f;
        gyroSensitivityTextView.setText(String.valueOf(gyroSensitivityValue));
        
        // Загружаем коррекцию IPD
        int ipdAdjustmentProgress = prefs.getInt("ipd_adjustment", 0); // 0 соответствует 0.0
        ipdAdjustmentSeekBar.setProgress(ipdAdjustmentProgress + 50); // Сдвигаем на 50 для диапазона -50 до 50
        float ipdAdjustmentValue = (ipdAdjustmentProgress) / 100.0f;
        ipdAdjustmentTextView.setText(String.valueOf(ipdAdjustmentValue));
    }

    private void setupListeners() {
        vrEnabledCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vrEffect.setEnabled(isChecked);
            saveSettings();
            if (onSettingsChangedListener != null) {
                onSettingsChangedListener.onSettingsChanged(vrEffect);
            }
        });

        useGyroCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vrEffect.setUseGyroForMovement(isChecked);
            saveSettings();
            if (onSettingsChangedListener != null) {
                onSettingsChangedListener.onSettingsChanged(vrEffect);
            }
        });

        vrModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                vrEffect.setVrMode(position);
                saveSettings();
                if (onSettingsChangedListener != null) {
                    onSettingsChangedListener.onSettingsChanged(vrEffect);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        scaleModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Для VR режима scaleMode не используется, но оставим для совместимости
                saveSettings();
                if (onSettingsChangedListener != null) {
                    onSettingsChangedListener.onSettingsChanged(vrEffect);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        distortionStrengthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Преобразуем из диапазона 0-200 в -1.0..1.0
                float value = (progress - 100) / 100.0f;
                distortionStrengthTextView.setText(String.valueOf(value));
                vrEffect.setDistortionStrength(value);
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

        edgeFeatheringSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 1000.0f;
                edgeFeatheringTextView.setText(String.valueOf(value));
                vrEffect.setEdgeFeathering(value);
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

        cornerRadiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 100.0f;
                cornerRadiusTextView.setText(String.valueOf(value));
                vrEffect.setCornerRadius(value);
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

        gyroSensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Преобразуем из диапазона 0-200 в 0.0..2.0
                float value = progress / 100.0f;
                gyroSensitivityTextView.setText(String.valueOf(value));
                vrEffect.setGyroSensitivity(value);
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

        ipdAdjustmentSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Преобразуем из диапазона 0-100 в -0.5..0.5
                float value = (progress - 50) / 100.0f;
                ipdAdjustmentTextView.setText(String.valueOf(value));
                vrEffect.setIPDAdjustment(value);
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

        frameSettingsButton.setOnClickListener(v -> {
            VRFrameSettingsDialog frameDialog = new VRFrameSettingsDialog(getContext(), vrEffect, 
                    effect -> {
                        saveSettings();
                        if (onSettingsChangedListener != null) {
                            onSettingsChangedListener.onSettingsChanged(vrEffect);
                        }
                    });
            frameDialog.show();
        });

        profileSettingsButton.setOnClickListener(v -> {
            VRProfileDialog profileDialog = new VRProfileDialog(getContext(), vrEffect,
                    effect -> {
                        if (onSettingsChangedListener != null) {
                            onSettingsChangedListener.onSettingsChanged(vrEffect);
                        }
                    });
            profileDialog.show();
        });

        resetButton.setOnClickListener(v -> {
            vrEffect.resetToDefaults();
            loadSettings();
            if (onSettingsChangedListener != null) {
                onSettingsChangedListener.onSettingsChanged(vrEffect);
            }
            saveSettings();
        });
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("vr_enabled", vrEffect.isVREnabled());
        editor.putBoolean("use_gyro", vrEffect.isUseGyroForMovement());
        editor.putInt("vr_mode", vrEffect.getVrMode());
        editor.putInt("scale_mode", 0); // Не используется для VR
        
        // Сохраняем дисторсию в диапазоне -100 до 100
        int distortionProgress = (int)(vrEffect.getDistortionStrength() * 100);
        editor.putInt("distortion_strength", distortionProgress);
        
        editor.putInt("edge_feathering", Math.round(vrEffect.getEdgeFeathering() * 1000));
        editor.putInt("corner_radius", Math.round(vrEffect.getCornerRadius() * 100));
        editor.putInt("gyro_sensitivity", Math.round(vrEffect.getGyroSensitivity() * 100));
        editor.putInt("ipd_adjustment", (int)(vrEffect.getIPDAdjustment() * 100));
        editor.apply();
    }
}