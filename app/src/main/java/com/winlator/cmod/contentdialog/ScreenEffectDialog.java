package com.winlator.cmod.contentdialog;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.renderer.effects.ColorEffect;
import com.winlator.cmod.renderer.effects.CRTEffect;
import com.winlator.cmod.renderer.effects.DepthEffect; // Импорт DepthEffect
import com.winlator.cmod.renderer.effects.FXAAEffect;
import com.winlator.cmod.renderer.effects.NTSCCombinedEffect;
import com.winlator.cmod.renderer.effects.SaturationEffect; // Импорт SaturationEffect
import com.winlator.cmod.renderer.effects.ToonEffect;
import com.winlator.cmod.widget.SeekBar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class ScreenEffectDialog extends ContentDialog {
    private final XServerDisplayActivity activity;
    private final CheckBox cbEnableCRTShader;
    private final CheckBox cbEnableFXAA;
    private final CheckBox cbEnableToonShader;
    private final CheckBox cbEnableNTSCEffect;
    private final CheckBox cbEnableDepthEffect; // Новый чекбокс
    private final CheckBox cbEnableSaturation; // Новый чекбокс
    private final SharedPreferences preferences;
    private final Spinner sProfile;
    private final SeekBar sbBrightness;
    private final SeekBar sbContrast;
    private final SeekBar sbGamma;
    private final SeekBar sbSharpness;
    private final SeekBar sbDepthStrength; // Новый SeekBar
    private final SeekBar sbDepthFocus;    // Новый SeekBar
    private final SeekBar sbSaturation; // Новый SeekBar
    private final TextView tvBrightness;
    private final TextView tvContrast;
    private final TextView tvGamma;
    private final TextView tvSharpness;
    private final TextView tvDepthStrength; // Новый TextView
    private final TextView tvDepthFocus;    // Новый TextView
    private final TextView tvSaturation; // Новый TextView
    private final Handler handler;
    private final Runnable applyEffectsRunnable;
    private boolean isApplyingEffects = false;

    private static final String TAG = "ScreenEffectDialog";
    private static final int APPLY_DELAY_MS = 100; // Задержка для динамического применения

    public ScreenEffectDialog(XServerDisplayActivity activity) {
        super(activity, R.layout.screen_effect_dialog);
        this.activity = activity;

        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        handler = new Handler(Looper.getMainLooper());
        
        applyEffectsRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isApplyingEffects) {
                    isApplyingEffects = true;
                    Log.d(TAG, "Running applyEffectsRunnable");
                    applyEffectsInRealTime();
                    isApplyingEffects = false;
                }
            }
        };

        boolean isDarkMode = preferences.getBoolean("dark_mode", false);

        TextView lblColorAdjustment = findViewById(R.id.LBLColorAdjustment);
        applyFieldSetLabelStyle(lblColorAdjustment, isDarkMode);

        sProfile = findViewById(R.id.SProfile);
        sbBrightness = findViewById(R.id.SBBrightness);
        sbContrast = findViewById(R.id.SBContrast);
        sbGamma = findViewById(R.id.SBGamma);
        sbSharpness = findViewById(R.id.SBSharpness);
        sbDepthStrength = findViewById(R.id.SBDepthStrength); // Инициализация
        sbDepthFocus = findViewById(R.id.SBDepthFocus);       // Инициализация
        sbSaturation = findViewById(R.id.SBSaturation); // Инициализация
        tvBrightness = findViewById(R.id.TVBrightness);
        tvContrast = findViewById(R.id.TVContrast);
        tvGamma = findViewById(R.id.TVGamma);
        tvSharpness = findViewById(R.id.TVSharpness);
        tvDepthStrength = findViewById(R.id.TVDepthStrength); // Инициализация
        tvDepthFocus = findViewById(R.id.TVDepthFocus);       // Инициализация
        tvSaturation = findViewById(R.id.TVSaturation); // Инициализация
        cbEnableFXAA = findViewById(R.id.CBEnableFXAA);
        cbEnableCRTShader = findViewById(R.id.CBEnableCRTShader);
        cbEnableToonShader = findViewById(R.id.CBEnableToonShader);
        cbEnableNTSCEffect = findViewById(R.id.CBEnableNTSCEffect);
        cbEnableDepthEffect = findViewById(R.id.CBEnableDepthEffect); // Инициализация
        cbEnableSaturation = findViewById(R.id.CBEnableSaturation); // Инициализация

        GLRenderer renderer = activity.getXServerView().getRenderer();
        if (renderer == null) {
            Log.e(TAG, "Renderer is null in ScreenEffectDialog initialization!");
            return;
        }

        ColorEffect colorEffect = (ColorEffect) renderer.getEffectComposer().getEffect(ColorEffect.class);
        FXAAEffect fxaaEffect = (FXAAEffect) renderer.getEffectComposer().getEffect(FXAAEffect.class);
        CRTEffect crtEffect = (CRTEffect) renderer.getEffectComposer().getEffect(CRTEffect.class);
        ToonEffect toonEffect = (ToonEffect) renderer.getEffectComposer().getEffect(ToonEffect.class);
        NTSCCombinedEffect ntscEffect = (NTSCCombinedEffect) renderer.getEffectComposer().getEffect(NTSCCombinedEffect.class);
        DepthEffect depthEffect = (DepthEffect) renderer.getEffectComposer().getEffect(DepthEffect.class); // Новый эффект
        SaturationEffect saturationEffect = (SaturationEffect) renderer.getEffectComposer().getEffect(SaturationEffect.class); // Новый эффект

        Log.d(TAG, "ScreenEffectDialog initialized. Initial effects - Color: " + (colorEffect != null) + ", Depth: " + (depthEffect != null) + ", Saturation: " + (saturationEffect != null));

        setupSeekBars(colorEffect, depthEffect, saturationEffect);
        setupCheckBoxes();

        cbEnableFXAA.setChecked(fxaaEffect != null);
        cbEnableCRTShader.setChecked(crtEffect != null);
        cbEnableToonShader.setChecked(toonEffect != null);
        cbEnableNTSCEffect.setChecked(ntscEffect != null);
        cbEnableDepthEffect.setChecked(depthEffect != null); // Установка состояния чекбокса
        cbEnableSaturation.setChecked(saturationEffect != null); // Установка состояния чекбокса

        loadProfileSpinner(sProfile, activity.getScreenEffectProfile());

        sProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Log.d(TAG, "Profile selected: " + sProfile.getSelectedItem().toString());
                    loadProfile(sProfile.getSelectedItem().toString());
                    scheduleApplyEffects();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button resetButton = findViewById(R.id.BTReset);
        resetButton.setVisibility(View.VISIBLE);
        resetButton.setOnClickListener(v -> {
            resetSettings();
            scheduleApplyEffects();
        });

        // Убираем дублирующиеся кнопки - используем только стандартные кнопки диалога
        setOnConfirmCallback(() -> {
            Log.d(TAG, "OnConfirm callback triggered. Saving profile and applying effects.");
            saveProfile(sProfile);
            applyEffectsFinal();
            Log.d(TAG, "Effects applied from callback.");
        });

        findViewById(R.id.BTAddProfile).setOnClickListener(v -> promptAddProfile());
        findViewById(R.id.BTRemoveProfile).setOnClickListener(v -> promptDeleteProfile());
    }

    private void setupSeekBars(ColorEffect colorEffect, DepthEffect depthEffect, SaturationEffect saturationEffect) {
        if (colorEffect != null) {
            Log.d(TAG, "ColorEffect found, setting up seek bars with fine-tuned ranges");
            
            // Устанавливаем начальные значения (только положительные)
            sbBrightness.setValue((int)((colorEffect.getBrightness() + 1.0f) * 50)); // Преобразуем [-1,1] в [0,100]
            sbContrast.setValue((int)((colorEffect.getContrast() + 0.5f) * 100)); // Преобразуем [-0.5,0.5] в [0,100]
            sbGamma.setValue((int)(colorEffect.getGamma() * 100));
            
            // Резкость ограничена до 40 (0-40 в значениях ползунка)
            sbSharpness.setMaxValue(40); // Ограничиваем максимальное значение до 40
            sbSharpness.setValue((int)(colorEffect.getSharpness() * 100));
            
        } else {
            Log.d(TAG, "ColorEffect not found, resetting settings");
            resetSettings();
        }

        // Устанавливаем начальные значения для эффекта глубины
        if (depthEffect != null) {
            sbDepthStrength.setValue((int)(depthEffect.getDepthStrength() * 100)); // [0,1] -> [0,100]
            sbDepthFocus.setValue((int)(depthEffect.getDepthFocus() * 100));       // [0,1] -> [0,100]
            Log.d(TAG, "DepthEffect found, setting up seek bars. Strength: " + depthEffect.getDepthStrength() + ", Focus: " + depthEffect.getDepthFocus());
        } else {
            Log.d(TAG, "DepthEffect not found, resetting depth settings");
            sbDepthStrength.setValue(0);
            sbDepthFocus.setValue(50); // 0.5 по умолчанию
        }

        // Устанавливаем начальные значения для эффекта насыщенности
        if (saturationEffect != null) {
            // Преобразуем [0.0, 2.0] в [0, 100] (0.0 -> 0, 1.0 -> 50, 2.0 -> 100)
            sbSaturation.setValue((int)(saturationEffect.getSaturation() * 50.0f));
            Log.d(TAG, "SaturationEffect found, setting up seek bar. Saturation: " + saturationEffect.getSaturation());
        } else {
            Log.d(TAG, "SaturationEffect not found, resetting saturation settings");
            sbSaturation.setValue(50); // 1.0 по умолчанию
        }

        updateDisplayValues();

        // Слушатели для динамического применения
        SeekBar.OnValueChangeListener valueChangeListener = new SeekBar.OnValueChangeListener() {
            @Override
            public void onValueChanged(SeekBar seekBar, int value) {
                Log.d(TAG, "SeekBar " + seekBar.getId() + " changed to value: " + value);
                updateDisplayValues();
                scheduleApplyEffects();
            }
        };

        sbBrightness.setOnValueChangeListener(valueChangeListener);
        sbContrast.setOnValueChangeListener(valueChangeListener);
        sbGamma.setOnValueChangeListener(valueChangeListener);
        sbSharpness.setOnValueChangeListener(valueChangeListener);
        sbDepthStrength.setOnValueChangeListener(valueChangeListener); // Новый слушатель
        sbDepthFocus.setOnValueChangeListener(valueChangeListener);    // Новый слушатель
        sbSaturation.setOnValueChangeListener(valueChangeListener); // Новый слушатель
    }

    private void setupCheckBoxes() {
        View.OnClickListener checkboxClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "CheckBox " + v.getId() + " clicked. State: " + ((CheckBox)v).isChecked());
                scheduleApplyEffects();
            }
        };

        cbEnableFXAA.setOnClickListener(checkboxClickListener);
        cbEnableCRTShader.setOnClickListener(checkboxClickListener);
        cbEnableToonShader.setOnClickListener(checkboxClickListener);
        cbEnableNTSCEffect.setOnClickListener(checkboxClickListener);
        cbEnableDepthEffect.setOnClickListener(checkboxClickListener); // Новый слушатель
        cbEnableSaturation.setOnClickListener(checkboxClickListener); // Новый слушатель
    }

    private void scheduleApplyEffects() {
        Log.d(TAG, "Scheduling applyEffectsRunnable");
        handler.removeCallbacks(applyEffectsRunnable);
        handler.postDelayed(applyEffectsRunnable, APPLY_DELAY_MS);
    }

    private void updateDisplayValues() {
        // Преобразуем значения ползунков в реальные значения эффектов
        float brightness = (sbBrightness.getValue() / 50.0f) - 1.0f; // [0,100] -> [-1,1]
        float contrast = (sbContrast.getValue() / 100.0f) - 0.5f; // [0,100] -> [-0.5,0.5]
        float gamma = sbGamma.getValue() / 100.0f;
        float sharpness = sbSharpness.getValue() / 100.0f; // [0,40] -> [0,0.4]
        float depthStrength = sbDepthStrength.getValue() / 100.0f; // [0,100] -> [0,1]
        float depthFocus = sbDepthFocus.getValue() / 100.0f;       // [0,100] -> [0,1]
        float saturation = sbSaturation.getValue() / 50.0f; // [0,100] -> [0,2]
        
        updateBrightnessText(brightness);
        updateContrastText(contrast);
        updateGammaText(gamma);
        updateSharpnessText(sharpness);
        updateDepthStrengthText(depthStrength); // Новый вызов
        updateDepthFocusText(depthFocus);       // Новый вызов
        updateSaturationText(saturation); // Новый вызов
    }

    private void applyEffectsInRealTime() {
        Log.d(TAG, "applyEffectsInRealTime called");
        GLRenderer renderer = activity.getXServerView().getRenderer();
        if (renderer == null) {
            Log.e(TAG, "Renderer is null in applyEffectsInRealTime!");
            return;
        }

        // Получаем текущие эффекты из композера *в этот момент*
        ColorEffect colorEffect = (ColorEffect) renderer.getEffectComposer().getEffect(ColorEffect.class);
        FXAAEffect fxaaEffect = (FXAAEffect) renderer.getEffectComposer().getEffect(FXAAEffect.class);
        CRTEffect crtEffect = (CRTEffect) renderer.getEffectComposer().getEffect(CRTEffect.class);
        ToonEffect toonEffect = (ToonEffect) renderer.getEffectComposer().getEffect(ToonEffect.class);
        NTSCCombinedEffect ntscEffect = (NTSCCombinedEffect) renderer.getEffectComposer().getEffect(NTSCCombinedEffect.class);
        DepthEffect depthEffect = (DepthEffect) renderer.getEffectComposer().getEffect(DepthEffect.class); // Новый эффект
        SaturationEffect saturationEffect = (SaturationEffect) renderer.getEffectComposer().getEffect(SaturationEffect.class); // Новый эффект

        Log.d(TAG, "Current effects in composer for RT: Color: " + (colorEffect != null) + ", Depth: " + (depthEffect != null) + ", Saturation: " + (saturationEffect != null));

        applyEffects(colorEffect, renderer, fxaaEffect, crtEffect, toonEffect, ntscEffect, depthEffect, saturationEffect); // Передаем depthEffect и saturationEffect
    }

    private void applyEffectsFinal() {
        Log.d(TAG, "applyEffectsFinal called");
        GLRenderer renderer = activity.getXServerView().getRenderer();
        if (renderer == null) {
            Log.e(TAG, "Renderer is null in applyEffectsFinal!");
            return;
        }

        // Получаем текущие эффекты из композера *в этот момент*
        ColorEffect colorEffect = (ColorEffect) renderer.getEffectComposer().getEffect(ColorEffect.class);
        FXAAEffect fxaaEffect = (FXAAEffect) renderer.getEffectComposer().getEffect(FXAAEffect.class);
        CRTEffect crtEffect = (CRTEffect) renderer.getEffectComposer().getEffect(CRTEffect.class);
        ToonEffect toonEffect = (ToonEffect) renderer.getEffectComposer().getEffect(ToonEffect.class);
        NTSCCombinedEffect ntscEffect = (NTSCCombinedEffect) renderer.getEffectComposer().getEffect(NTSCCombinedEffect.class);
        DepthEffect depthEffect = (DepthEffect) renderer.getEffectComposer().getEffect(DepthEffect.class); // Новый эффект
        SaturationEffect saturationEffect = (SaturationEffect) renderer.getEffectComposer().getEffect(SaturationEffect.class); // Новый эффект

        Log.d(TAG, "Current effects in composer for Final: Color: " + (colorEffect != null) + ", Depth: " + (depthEffect != null) + ", Saturation: " + (saturationEffect != null));

        applyEffects(colorEffect, renderer, fxaaEffect, crtEffect, toonEffect, ntscEffect, depthEffect, saturationEffect); // Передаем depthEffect и saturationEffect
        
        // Принудительно запрашиваем рендер для немедленного отображения
        activity.getXServerView().requestRender();
    }

    private void updateBrightnessText(float brightness) {
        tvBrightness.setText(String.format(Locale.ENGLISH, "%.2f", brightness));
    }

    private void updateContrastText(float contrast) {
        tvContrast.setText(String.format(Locale.ENGLISH, "%.2f", contrast));
    }

    private void updateGammaText(float gamma) {
        tvGamma.setText(String.format(Locale.ENGLISH, "%.2f", gamma));
    }

    private void updateSharpnessText(float sharpness) {
        tvSharpness.setText(String.format(Locale.ENGLISH, "%.2f", sharpness));
    }

    private void updateDepthStrengthText(float depthStrength) { // Новый метод
        tvDepthStrength.setText(String.format(Locale.ENGLISH, "%.2f", depthStrength));
    }

    private void updateDepthFocusText(float depthFocus) { // Новый метод
        tvDepthFocus.setText(String.format(Locale.ENGLISH, "%.2f", depthFocus));
    }

    private void updateSaturationText(float saturation) { // Новый метод
        tvSaturation.setText(String.format(Locale.ENGLISH, "%.2f", saturation));
    }

    private static void applyFieldSetLabelStyle(TextView textView, boolean isDarkMode) {
        if (isDarkMode) {
            textView.setTextColor(Color.parseColor("#cccccc"));
            textView.setBackgroundResource(R.color.window_background_color_dark);
        } else {
            textView.setTextColor(Color.parseColor("#bdbdbd"));
            textView.setBackgroundResource(R.color.window_background_color);
        }
    }

    private void promptAddProfile() {
        ContentDialog.prompt(activity, R.string.do_you_want_to_add_a_new_profile, null, name -> addProfile(name, sProfile));
    }

    private void promptDeleteProfile() {
        if (sProfile.getSelectedItemPosition() > 0) {
            String selectedProfile = sProfile.getSelectedItem().toString();
            ContentDialog.confirm(activity, R.string.do_you_want_to_remove_this_profile, () -> removeProfile(selectedProfile, sProfile));
        } else {
            AppUtils.showToast(activity, R.string.no_profile_selected);
        }
    }

    private void addProfile(String newName, Spinner sProfile) {
        Set<String> profiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
        for (String profile : profiles) {
            String[] parts = profile.split(":");
            if (parts[0].equals(newName)) {
                return;
            }
        }
        profiles.add(newName + ":");
        preferences.edit().putStringSet("screen_effect_profiles", profiles).apply();
        loadProfileSpinner(sProfile, newName);
    }

    private void loadProfileSpinner(Spinner sProfile, String selectedName) {
        Set<String> profiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
        ArrayList<String> items = new ArrayList<>();
        items.add("-- " + activity.getString(R.string.default_profile) + " --");
        int selectedPosition = 0, position = 1;
        for (String profile : profiles) {
            String[] parts = profile.split(":");
            items.add(parts[0]);
            if (parts[0].equals(selectedName)) {
                selectedPosition = position;
            }
            position++;
        }
        sProfile.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, items));
        sProfile.setSelection(selectedPosition);
    }

    private void loadProfile(String name) {
        Log.d(TAG, "Loading profile: " + name);
        Set<String> profiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
        for (String profile : profiles) {
            String[] parts = profile.split(":");
            if (parts[0].equals(name) && parts.length > 1 && !parts[1].isEmpty()) {
                KeyValueSet settings = new KeyValueSet(parts[1]);
                
                float brightness = settings.getFloat("brightness", 0);
                float contrast = settings.getFloat("contrast", 0);
                float gamma = settings.getFloat("gamma", 1.0f);
                float sharpness = settings.getFloat("sharpness", 0);
                float depthStrength = settings.getFloat("depth_strength", 0); // Новое значение
                float depthFocus = settings.getFloat("depth_focus", 0.5f);    // Новое значение
                float saturation = settings.getFloat("saturation", 1.0f); // Новое значение
                
                Log.d(TAG, "Loaded settings - Bright: " + brightness + ", Contrast: " + contrast + ", Gamma: " + gamma + ", Sharp: " + sharpness + ", DepthStr: " + depthStrength + ", DepthFocus: " + depthFocus + ", Saturation: " + saturation);
                
                // Преобразуем реальные значения в значения ползунков
                sbBrightness.setValue((int)((brightness + 1.0f) * 50));
                sbContrast.setValue((int)((contrast + 0.5f) * 100));
                sbGamma.setValue((int)(gamma * 100));
                sbSharpness.setValue((int)(sharpness * 100)); // [0,0.4] -> [0,40]
                sbDepthStrength.setValue((int)(depthStrength * 100)); // [0,1] -> [0,100]
                sbDepthFocus.setValue((int)(depthFocus * 100));       // [0,1] -> [0,100]
                sbSaturation.setValue((int)(saturation * 50.0f)); // [0,2] -> [0,100]
                
                updateDisplayValues();
                
                cbEnableFXAA.setChecked(settings.getBoolean("fxaa", false));
                cbEnableCRTShader.setChecked(settings.getBoolean("crt_shader", false));
                cbEnableToonShader.setChecked(settings.getBoolean("toon_shader", false));
                cbEnableNTSCEffect.setChecked(settings.getBoolean("ntsc_effect", false));
                cbEnableDepthEffect.setChecked(settings.getBoolean("depth_effect", false)); // Новое значение
                cbEnableSaturation.setChecked(settings.getBoolean("saturation_effect", false)); // Новое значение
                return;
            }
        }
        Log.d(TAG, "Profile not found: " + name);
    }

    private void removeProfile(String targetName, Spinner sProfile) {
        Log.d(TAG, "Removing profile: " + targetName);
        Set<String> profiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
        profiles.removeIf(profile -> profile.split(":")[0].equals(targetName));
        preferences.edit().putStringSet("screen_effect_profiles", profiles).apply();
        loadProfileSpinner(sProfile, null);
        resetSettings();
    }

    private void resetSettings() {
        Log.d(TAG, "Resetting all settings to default");
        sbBrightness.setValue(50); // 0 в реальных значениях
        sbContrast.setValue(50); // 0 в реальных значениях
        sbGamma.setValue(100); // 1.0
        sbSharpness.setValue(0); // 0 резкости
        sbDepthStrength.setValue(0); // 0 силы глубины
        sbDepthFocus.setValue(50);   // 0.5 фокуса глубины
        sbSaturation.setValue(50); // 1.0 насыщенности
        
        updateDisplayValues();
        
        cbEnableFXAA.setChecked(false);
        cbEnableCRTShader.setChecked(false);
        cbEnableToonShader.setChecked(false);
        cbEnableNTSCEffect.setChecked(false);
        cbEnableDepthEffect.setChecked(false); // Сброс чекбокса
        cbEnableSaturation.setChecked(false); // Сброс чекбокса
    }

    private void saveProfile(Spinner sProfile) {
        if (sProfile.getSelectedItemPosition() > 0) {
            String selectedProfile = sProfile.getSelectedItem().toString();
            Log.d(TAG, "Saving profile: " + selectedProfile);

            Set<String> oldProfiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
            Set<String> newProfiles = new LinkedHashSet<>();
            
            // Получаем реальные значения из ползунков *в момент сохранения*
            float brightness = (sbBrightness.getValue() / 50.0f) - 1.0f;
            float contrast = (sbContrast.getValue() / 100.0f) - 0.5f;
            float gamma = sbGamma.getValue() / 100.0f;
            float sharpness = sbSharpness.getValue() / 100.0f; // [0,40] -> [0,0.4]
            float depthStrength = sbDepthStrength.getValue() / 100.0f; // [0,100] -> [0,1]
            float depthFocus = sbDepthFocus.getValue() / 100.0f;       // [0,100] -> [0,1]
            float saturation = sbSaturation.getValue() / 50.0f; // [0,100] -> [0,2]
            
            boolean enableFXAA = cbEnableFXAA.isChecked();
            boolean enableCRTShader = cbEnableCRTShader.isChecked();
            boolean enableToonShader = cbEnableToonShader.isChecked();
            boolean enableNTSCEffect = cbEnableNTSCEffect.isChecked();
            boolean enableDepthEffect = cbEnableDepthEffect.isChecked(); // Новое значение
            boolean enableSaturation = cbEnableSaturation.isChecked(); // Новое значение

            Log.d(TAG, "Saving settings - Bright: " + brightness + ", Contrast: " + contrast + ", Gamma: " + gamma + ", Sharp: " + sharpness + ", DepthStr: " + depthStrength + ", DepthFocus: " + depthFocus + ", Saturation: " + saturation + ", DepthEnabled: " + enableDepthEffect + ", SaturationEnabled: " + enableSaturation);

            KeyValueSet settings = new KeyValueSet();
            settings.put("brightness", brightness);
            settings.put("contrast", contrast);
            settings.put("gamma", gamma);
            settings.put("sharpness", sharpness);
            settings.put("depth_strength", depthStrength); // Новое значение
            settings.put("depth_focus", depthFocus);       // Новое значение
            settings.put("saturation", saturation); // Новое значение
            settings.put("fxaa", enableFXAA);
            settings.put("crt_shader", enableCRTShader);
            settings.put("toon_shader", enableToonShader);
            settings.put("ntsc_effect", enableNTSCEffect);
            settings.put("depth_effect", enableDepthEffect); // Новое значение
            settings.put("saturation_effect", enableSaturation); // Новое значение

            for (String profile : oldProfiles) {
                String[] parts = profile.split(":");
                if (parts[0].equals(selectedProfile)) {
                    Log.d(TAG, "Updating existing profile: " + selectedProfile);
                    newProfiles.add(selectedProfile + ":" + settings.toString());
                } else {
                    newProfiles.add(profile);
                }
            }
            preferences.edit().putStringSet("screen_effect_profiles", newProfiles).apply();
            activity.setScreenEffectProfile(selectedProfile);
        } else {
            Log.d(TAG, "No profile selected for saving.");
        }
    }

    // Обновленный метод applyEffects, включающий DepthEffect и SaturationEffect
    public void applyEffects(ColorEffect colorEffect, GLRenderer renderer, FXAAEffect fxaaEffect, CRTEffect crtEffect, ToonEffect toonEffect, NTSCCombinedEffect ntscEffect, DepthEffect depthEffect, SaturationEffect saturationEffect) {
        Log.d(TAG, "applyEffects called. Effects passed - Color: " + (colorEffect != null) + ", Depth: " + (depthEffect != null) + ", Saturation: " + (saturationEffect != null));

        if (renderer == null) {
            Log.e(TAG, "Renderer is null in applyEffects!");
            return;
        }

        if (renderer.getEffectComposer() == null) {
            Log.e(TAG, "EffectComposer is null in applyEffects!");
            return;
        }

        // Получаем реальные значения из ползунков *в момент вызова applyEffects*
        float brightness = (sbBrightness.getValue() / 50.0f) - 1.0f;
        float contrast = (sbContrast.getValue() / 100.0f) - 0.5f;
        float gamma = sbGamma.getValue() / 100.0f;
        float sharpness = sbSharpness.getValue() / 100.0f; // [0,40] -> [0,0.4]
        float depthStrength = sbDepthStrength.getValue() / 100.0f; // [0,100] -> [0,1]
        float depthFocus = sbDepthFocus.getValue() / 100.0f;       // [0,100] -> [0,1]
        float saturation = sbSaturation.getValue() / 50.0f; // [0,100] -> [0,2]
        boolean enableFXAA = cbEnableFXAA.isChecked();
        boolean enableCRTShader = cbEnableCRTShader.isChecked();
        boolean enableToonShader = cbEnableToonShader.isChecked();
        boolean enableNTSCEffect = cbEnableNTSCEffect.isChecked();
        boolean enableDepthEffect = cbEnableDepthEffect.isChecked(); // Новое значение
        boolean enableSaturation = cbEnableSaturation.isChecked(); // Новое значение

        Log.d(TAG, "Applying settings - Bright: " + brightness + ", Contrast: " + contrast + ", Gamma: " + gamma + ", Sharp: " + sharpness + ", DepthStr: " + depthStrength + ", DepthFocus: " + depthFocus + ", Saturation: " + saturation + ", DepthEnabled: " + enableDepthEffect + ", SaturationEnabled: " + enableSaturation);

        // Apply or remove ColorEffect
        if (brightness == 0 && contrast == 0 && gamma == 1.0f && sharpness == 0) {
            Log.d(TAG, "Removing ColorEffect");
            if (colorEffect != null) {
                renderer.getEffectComposer().removeEffect(colorEffect);
            }
        } else {
            if (colorEffect == null) {
                Log.d(TAG, "Creating new ColorEffect");
                colorEffect = new ColorEffect();
                colorEffect.setRenderer(renderer);
            }
            
            colorEffect.setBrightness(brightness);
            colorEffect.setContrast(contrast);
            colorEffect.setGamma(gamma);
            colorEffect.setSharpness(sharpness);
            Log.d(TAG, "Adding/Updating ColorEffect in composer");
            renderer.getEffectComposer().addEffect(colorEffect);
        }

        // Apply or remove FXAAEffect
        if (enableFXAA) {
            if (fxaaEffect == null) {
                Log.d(TAG, "Creating new FXAAEffect");
                fxaaEffect = new FXAAEffect();
                renderer.getEffectComposer().addEffect(fxaaEffect);
            }
        } else if (fxaaEffect != null) {
            Log.d(TAG, "Removing FXAAEffect");
            renderer.getEffectComposer().removeEffect(fxaaEffect);
        }

        // Apply or remove CRTEffect
        if (enableCRTShader) {
            if (crtEffect == null) {
                Log.d(TAG, "Creating new CRTEffect");
                crtEffect = new CRTEffect();
                renderer.getEffectComposer().addEffect(crtEffect);
            }
        } else if (crtEffect != null) {
            Log.d(TAG, "Removing CRTEffect");
            renderer.getEffectComposer().removeEffect(crtEffect);
        }

        // Apply or remove ToonEffect
        if (enableToonShader) {
            if (toonEffect == null) {
                Log.d(TAG, "Creating new ToonEffect");
                toonEffect = new ToonEffect();
                renderer.getEffectComposer().addEffect(toonEffect);
            }
        } else if (toonEffect != null) {
            Log.d(TAG, "Removing ToonEffect");
            renderer.getEffectComposer().removeEffect(toonEffect);
        }

        // Apply or remove NTSCCombinedEffect
        if (enableNTSCEffect) {
            if (ntscEffect == null) {
                Log.d(TAG, "Creating new NTSCCombinedEffect");
                ntscEffect = new NTSCCombinedEffect();
                renderer.getEffectComposer().addEffect(ntscEffect);
            }
        } else if (ntscEffect != null) {
            Log.d(TAG, "Removing NTSCCombinedEffect");
            renderer.getEffectComposer().removeEffect(ntscEffect);
        }

        // Apply or remove DepthEffect
        if (enableDepthEffect) {
            if (depthEffect == null) { // Проверяем, существует ли уже эффект в композере
                Log.d(TAG, "Creating new DepthEffect and adding to composer");
                depthEffect = new DepthEffect(); // Создаём новый
                renderer.getEffectComposer().addEffect(depthEffect); // Добавляем в композер
            } else {
                Log.d(TAG, "Using existing DepthEffect from composer, updating parameters");
            }
            // Настройка параметров всегда происходит, если эффект включён
            depthEffect.setDepthStrength(depthStrength);
            depthEffect.setDepthFocus(depthFocus);
        } else if (depthEffect != null) { // Если эффект выключен и он существовал в композере
            Log.d(TAG, "Removing DepthEffect from composer");
            renderer.getEffectComposer().removeEffect(depthEffect); // Удаляем его
        } else {
            Log.d(TAG, "DepthEffect is disabled and not present in composer, nothing to do.");
        }

        // Apply or remove SaturationEffect
        if (enableSaturation) {
            if (saturationEffect == null) { // Проверяем, существует ли уже эффект в композере
                Log.d(TAG, "Creating new SaturationEffect and adding to composer");
                saturationEffect = new SaturationEffect(); // Создаём новый
                renderer.getEffectComposer().addEffect(saturationEffect); // Добавляем в композер
            } else {
                Log.d(TAG, "Using existing SaturationEffect from composer, updating parameters");
            }
            // Настройка параметров всегда происходит, если эффект включён
            saturationEffect.setSaturation(saturation);
        } else if (saturationEffect != null) { // Если эффект выключен и он существовал в композере
            Log.d(TAG, "Removing SaturationEffect from composer");
            renderer.getEffectComposer().removeEffect(saturationEffect); // Удаляем его
        } else {
            Log.d(TAG, "SaturationEffect is disabled and not present in composer, nothing to do.");
        }
    }

    @Override
    public void dismiss() {
        Log.d(TAG, "Dialog dismissed, removing callbacks.");
        handler.removeCallbacks(applyEffectsRunnable);
        super.dismiss();
    }
}