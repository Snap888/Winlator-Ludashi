package com.winlator.cmod.widget;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.winlator.cmod.R;
import com.winlator.cmod.contentdialog.ContentDialog;

public class FrameRatingSettingsDialog extends ContentDialog {
    private FrameRating frameRating;
    private CheckBox cbFPS, cbRenderer, cbGPU, cbRAM, cbTemperature, cbBatteryLevel, cbBatteryPower, cbShowMangoHud;
    private RadioButton rbVertical, rbHorizontal;
    private SeekBar sbBackgroundAlpha, sbTextAlpha, sbButtonAlpha, sbFpsLimit;
    private TextView tvBackgroundAlphaValue, tvTextAlphaValue, tvButtonAlphaValue, tvFpsLimitValue;

    public FrameRatingSettingsDialog(Context context, FrameRating frameRating) {
        super(context, R.layout.frame_rating_settings_dialog);
        this.frameRating = frameRating;
        
        setTitle(R.string.frame_rating_settings);
        setIcon(R.drawable.icon_gear);
        
        initializeViews();
        loadCurrentSettings();
        setupListeners();
    }
    
    private void initializeViews() {
        cbFPS = findViewById(R.id.CBShowFPS);
        cbRenderer = findViewById(R.id.CBShowRenderer);
        cbGPU = findViewById(R.id.CBShowGPU);
        cbRAM = findViewById(R.id.CBShowRAM);
        cbTemperature = findViewById(R.id.CBShowTemperature);
        cbBatteryLevel = findViewById(R.id.CBShowBatteryLevel);
        cbBatteryPower = findViewById(R.id.CBShowBatteryPower);
        cbShowMangoHud = findViewById(R.id.CBShowMangoHud);
        
        rbVertical = findViewById(R.id.RBVertical);
        rbHorizontal = findViewById(R.id.RBHorizontal);
        
        sbBackgroundAlpha = findViewById(R.id.SBBackgroundAlpha);
        sbTextAlpha = findViewById(R.id.SBTextAlpha);
        sbButtonAlpha = findViewById(R.id.SBButtonAlpha);
        sbFpsLimit = findViewById(R.id.SBFpsLimit);
        
        tvBackgroundAlphaValue = findViewById(R.id.TVBackgroundAlphaValue);
        tvTextAlphaValue = findViewById(R.id.TVTextAlphaValue);
        tvButtonAlphaValue = findViewById(R.id.TVButtonAlphaValue);
        tvFpsLimitValue = findViewById(R.id.TVFpsLimitValue);
        
        // Убедимся, что RadioGroup виден
        RadioGroup radioGroup = findViewById(R.id.RGLayout);
        if (radioGroup != null) {
            radioGroup.setVisibility(View.VISIBLE);
        }
        
        // Настраиваем SeekBar для FPS лимита (0-165)
        if (sbFpsLimit != null) {
            sbFpsLimit.setMax(165); // от 0 до 165
        }
    }
    
    private void loadCurrentSettings() {
        cbFPS.setChecked(frameRating.isShowFPS());
        cbRenderer.setChecked(frameRating.isShowRenderer());
        cbGPU.setChecked(frameRating.isShowGPU());
        cbRAM.setChecked(frameRating.isShowRAM());
        cbTemperature.setChecked(frameRating.isShowTemperature());
        cbBatteryLevel.setChecked(frameRating.isShowBatteryLevel());
        cbBatteryPower.setChecked(frameRating.isShowBatteryPower());
        cbShowMangoHud.setChecked(frameRating.isShowMangoHud());
        
        if (frameRating.isVerticalLayout()) {
            rbVertical.setChecked(true);
        } else {
            rbHorizontal.setChecked(true);
        }
        
        sbBackgroundAlpha.setProgress(frameRating.getBackgroundAlpha());
        sbTextAlpha.setProgress(frameRating.getTextAlpha());
        sbButtonAlpha.setProgress(frameRating.getButtonAlpha());
        sbFpsLimit.setProgress(frameRating.getFpsLimit());
        
        updateAlphaLabels();
        updateFpsLimitLabel();
    }
    
    private void setupListeners() {
        // SeekBar listeners
        sbBackgroundAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                frameRating.setBackgroundAlpha(progress);
                updateAlphaLabels();
                frameRating.saveSettings();
            }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        sbTextAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                frameRating.setTextAlpha(progress);
                updateAlphaLabels();
                frameRating.saveSettings();
            }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        sbButtonAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                frameRating.setButtonAlpha(progress);
                updateAlphaLabels();
                frameRating.saveSettings();
            }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // FPS Limit SeekBar listener
        sbFpsLimit.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                frameRating.setFpsLimit(progress);
                updateFpsLimitLabel();
            }
            
            @Override 
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override 
            public void onStopTrackingTouch(SeekBar seekBar) {
                frameRating.saveSettings();
            }
        });
        
        // CheckBox listeners
        CompoundButton.OnCheckedChangeListener checkListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.getId() == R.id.CBShowMangoHud) {
                    frameRating.setShowMangoHud(isChecked);
                    frameRating.saveSettings();
                } else {
                    applyVisibilitySettings();
                }
            }
        };
        
        cbFPS.setOnCheckedChangeListener(checkListener);
        cbRenderer.setOnCheckedChangeListener(checkListener);
        cbGPU.setOnCheckedChangeListener(checkListener);
        cbRAM.setOnCheckedChangeListener(checkListener);
        cbTemperature.setOnCheckedChangeListener(checkListener);
        cbBatteryLevel.setOnCheckedChangeListener(checkListener);
        cbBatteryPower.setOnCheckedChangeListener(checkListener);
        cbShowMangoHud.setOnCheckedChangeListener(checkListener);
        
        // RadioButton listeners
        View.OnClickListener layoutListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frameRating.setVerticalLayout(v.getId() == R.id.RBVertical);
                frameRating.saveSettings();
            }
        };
        
        rbVertical.setOnClickListener(layoutListener);
        rbHorizontal.setOnClickListener(layoutListener);
    }
    
    private void applyVisibilitySettings() {
        frameRating.setShowFPS(cbFPS.isChecked());
        frameRating.setShowRenderer(cbRenderer.isChecked());
        frameRating.setShowGPU(cbGPU.isChecked());
        frameRating.setShowRAM(cbRAM.isChecked());
        frameRating.setShowTemperature(cbTemperature.isChecked());
        frameRating.setShowBatteryLevel(cbBatteryLevel.isChecked());
        frameRating.setShowBatteryPower(cbBatteryPower.isChecked());
        frameRating.saveSettings();
    }
    
    private void updateAlphaLabels() {
        int bgAlpha = frameRating.getBackgroundAlpha();
        int textAlpha = frameRating.getTextAlpha();
        int buttonAlpha = frameRating.getButtonAlpha();
        
        tvBackgroundAlphaValue.setText(String.format("%d%%", (int)(bgAlpha / 255.0 * 100)));
        tvTextAlphaValue.setText(String.format("%d%%", (int)(textAlpha / 255.0 * 100)));
        tvButtonAlphaValue.setText(String.format("%d%%", (int)(buttonAlpha / 255.0 * 100)));
    }
    
    private void updateFpsLimitLabel() {
        int fpsLimit = frameRating.getFpsLimit();
        String limitText;
        if (fpsLimit == 0) {
            limitText = getContext().getString(R.string.no_limit);
        } else {
            limitText = fpsLimit + " FPS";
        }
        tvFpsLimitValue.setText(limitText);
    }
}