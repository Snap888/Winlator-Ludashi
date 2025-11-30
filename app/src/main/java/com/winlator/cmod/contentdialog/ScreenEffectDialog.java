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
import com.winlator.cmod.renderer.effects.FXAAEffect;
import com.winlator.cmod.renderer.effects.NTSCCombinedEffect;
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
    private final SharedPreferences preferences;
    private final Spinner sProfile;
    private final SeekBar sbBrightness;
    private final SeekBar sbContrast;
    private final SeekBar sbGamma;
    private final SeekBar sbSharpness;
    private final TextView tvBrightness;
    private final TextView tvContrast;
    private final TextView tvGamma;
    private final TextView tvSharpness;
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
        tvBrightness = findViewById(R.id.TVBrightness);
        tvContrast = findViewById(R.id.TVContrast);
        tvGamma = findViewById(R.id.TVGamma);
        tvSharpness = findViewById(R.id.TVSharpness);
        cbEnableFXAA = findViewById(R.id.CBEnableFXAA);
        cbEnableCRTShader = findViewById(R.id.CBEnableCRTShader);
        cbEnableToonShader = findViewById(R.id.CBEnableToonShader);
        cbEnableNTSCEffect = findViewById(R.id.CBEnableNTSCEffect);

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

        Log.d(TAG, "ScreenEffectDialog initialized");

        setupSeekBars(colorEffect);
        setupCheckBoxes();

        cbEnableFXAA.setChecked(fxaaEffect != null);
        cbEnableCRTShader.setChecked(crtEffect != null);
        cbEnableToonShader.setChecked(toonEffect != null);
        cbEnableNTSCEffect.setChecked(ntscEffect != null);

        loadProfileSpinner(sProfile, activity.getScreenEffectProfile());

        sProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
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

    private void setupSeekBars(ColorEffect colorEffect) {
        if (colorEffect != null) {
            Log.d(TAG, "ColorEffect found, setting up seek bars with fine-tuned ranges");
            
            // Устанавливаем начальные значения (только положительные)
            sbBrightness.setValue((int)((colorEffect.getBrightness() + 1.0f) * 50)); // Преобразуем [-1,1] в [0,100]
            sbContrast.setValue((int)((colorEffect.getContrast() + 0.5f) * 100)); // Преобразуем [-0.5,0.5] в [0,100]
            sbGamma.setValue((int)(colorEffect.getGamma() * 100));
            
            // Резкость ограничена до 40 (0-40 в значениях ползунка)
            sbSharpness.setMaxValue(40); // Ограничиваем максимальное значение до 40
            sbSharpness.setValue((int)(colorEffect.getSharpness() * 100));
            
            updateDisplayValues();

            // Слушатели для динамического применения
            SeekBar.OnValueChangeListener valueChangeListener = new SeekBar.OnValueChangeListener() {
                @Override
                public void onValueChanged(SeekBar seekBar, int value) {
                    updateDisplayValues();
                    scheduleApplyEffects();
                }
            };

            sbBrightness.setOnValueChangeListener(valueChangeListener);
            sbContrast.setOnValueChangeListener(valueChangeListener);
            sbGamma.setOnValueChangeListener(valueChangeListener);
            sbSharpness.setOnValueChangeListener(valueChangeListener);

        } else {
            Log.d(TAG, "ColorEffect not found, resetting settings");
            resetSettings();
        }
    }

    private void setupCheckBoxes() {
        View.OnClickListener checkboxClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scheduleApplyEffects();
            }
        };

        cbEnableFXAA.setOnClickListener(checkboxClickListener);
        cbEnableCRTShader.setOnClickListener(checkboxClickListener);
        cbEnableToonShader.setOnClickListener(checkboxClickListener);
        cbEnableNTSCEffect.setOnClickListener(checkboxClickListener);
    }

    private void scheduleApplyEffects() {
        handler.removeCallbacks(applyEffectsRunnable);
        handler.postDelayed(applyEffectsRunnable, APPLY_DELAY_MS);
    }

    private void updateDisplayValues() {
        // Преобразуем значения ползунков в реальные значения эффектов
        float brightness = (sbBrightness.getValue() / 50.0f) - 1.0f; // [0,100] -> [-1,1]
        float contrast = (sbContrast.getValue() / 100.0f) - 0.5f; // [0,100] -> [-0.5,0.5]
        float gamma = sbGamma.getValue() / 100.0f;
        float sharpness = sbSharpness.getValue() / 100.0f; // [0,40] -> [0,0.4]
        
        updateBrightnessText(brightness);
        updateContrastText(contrast);
        updateGammaText(gamma);
        updateSharpnessText(sharpness);
    }

    private void applyEffectsInRealTime() {
        GLRenderer renderer = activity.getXServerView().getRenderer();
        if (renderer == null) return;

        ColorEffect colorEffect = (ColorEffect) renderer.getEffectComposer().getEffect(ColorEffect.class);
        FXAAEffect fxaaEffect = (FXAAEffect) renderer.getEffectComposer().getEffect(FXAAEffect.class);
        CRTEffect crtEffect = (CRTEffect) renderer.getEffectComposer().getEffect(CRTEffect.class);
        ToonEffect toonEffect = (ToonEffect) renderer.getEffectComposer().getEffect(ToonEffect.class);
        NTSCCombinedEffect ntscEffect = (NTSCCombinedEffect) renderer.getEffectComposer().getEffect(NTSCCombinedEffect.class);

        applyEffects(colorEffect, renderer, fxaaEffect, crtEffect, toonEffect, ntscEffect);
    }

    private void applyEffectsFinal() {
        GLRenderer renderer = activity.getXServerView().getRenderer();
        if (renderer == null) return;

        ColorEffect colorEffect = (ColorEffect) renderer.getEffectComposer().getEffect(ColorEffect.class);
        FXAAEffect fxaaEffect = (FXAAEffect) renderer.getEffectComposer().getEffect(FXAAEffect.class);
        CRTEffect crtEffect = (CRTEffect) renderer.getEffectComposer().getEffect(CRTEffect.class);
        ToonEffect toonEffect = (ToonEffect) renderer.getEffectComposer().getEffect(ToonEffect.class);
        NTSCCombinedEffect ntscEffect = (NTSCCombinedEffect) renderer.getEffectComposer().getEffect(NTSCCombinedEffect.class);

        applyEffects(colorEffect, renderer, fxaaEffect, crtEffect, toonEffect, ntscEffect);
        
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
        Set<String> profiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
        for (String profile : profiles) {
            String[] parts = profile.split(":");
            if (parts[0].equals(name) && parts.length > 1 && !parts[1].isEmpty()) {
                KeyValueSet settings = new KeyValueSet(parts[1]);
                
                float brightness = settings.getFloat("brightness", 0);
                float contrast = settings.getFloat("contrast", 0);
                float gamma = settings.getFloat("gamma", 1.0f);
                float sharpness = settings.getFloat("sharpness", 0);
                
                // Преобразуем реальные значения в значения ползунков
                sbBrightness.setValue((int)((brightness + 1.0f) * 50));
                sbContrast.setValue((int)((contrast + 0.5f) * 100));
                sbGamma.setValue((int)(gamma * 100));
                sbSharpness.setValue((int)(sharpness * 100)); // [0,0.4] -> [0,40]
                
                updateDisplayValues();
                
                cbEnableFXAA.setChecked(settings.getBoolean("fxaa", false));
                cbEnableCRTShader.setChecked(settings.getBoolean("crt_shader", false));
                cbEnableToonShader.setChecked(settings.getBoolean("toon_shader", false));
                cbEnableNTSCEffect.setChecked(settings.getBoolean("ntsc_effect", false));
                return;
            }
        }
    }

    private void removeProfile(String targetName, Spinner sProfile) {
        Set<String> profiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
        profiles.removeIf(profile -> profile.split(":")[0].equals(targetName));
        preferences.edit().putStringSet("screen_effect_profiles", profiles).apply();
        loadProfileSpinner(sProfile, null);
        resetSettings();
    }

    private void resetSettings() {
        sbBrightness.setValue(50); // 0 в реальных значениях
        sbContrast.setValue(50); // 0 в реальных значениях
        sbGamma.setValue(100); // 1.0
        sbSharpness.setValue(0); // 0 резкости
        
        updateDisplayValues();
        
        cbEnableFXAA.setChecked(false);
        cbEnableCRTShader.setChecked(false);
        cbEnableToonShader.setChecked(false);
        cbEnableNTSCEffect.setChecked(false);
    }

    private void saveProfile(Spinner sProfile) {
        if (sProfile.getSelectedItemPosition() > 0) {
            String selectedProfile = sProfile.getSelectedItem().toString();
            Set<String> oldProfiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
            Set<String> newProfiles = new LinkedHashSet<>();
            
            // Получаем реальные значения из ползунков
            float brightness = (sbBrightness.getValue() / 50.0f) - 1.0f;
            float contrast = (sbContrast.getValue() / 100.0f) - 0.5f;
            float gamma = sbGamma.getValue() / 100.0f;
            float sharpness = sbSharpness.getValue() / 100.0f; // [0,40] -> [0,0.4]
            
            KeyValueSet settings = new KeyValueSet();
            settings.put("brightness", brightness);
            settings.put("contrast", contrast);
            settings.put("gamma", gamma);
            settings.put("sharpness", sharpness);
            settings.put("fxaa", cbEnableFXAA.isChecked());
            settings.put("crt_shader", cbEnableCRTShader.isChecked());
            settings.put("toon_shader", cbEnableToonShader.isChecked());
            settings.put("ntsc_effect", cbEnableNTSCEffect.isChecked());

            for (String profile : oldProfiles) {
                String[] parts = profile.split(":");
                if (parts[0].equals(selectedProfile)) {
                    newProfiles.add(selectedProfile + ":" + settings.toString());
                } else {
                    newProfiles.add(profile);
                }
            }
            preferences.edit().putStringSet("screen_effect_profiles", newProfiles).apply();
            activity.setScreenEffectProfile(selectedProfile);
        }
    }

    public void applyEffects(ColorEffect colorEffect, GLRenderer renderer, FXAAEffect fxaaEffect, CRTEffect crtEffect, ToonEffect toonEffect, NTSCCombinedEffect ntscEffect) {
        if (renderer == null) {
            Log.e(TAG, "Renderer is null!");
            return;
        }

        if (renderer.getEffectComposer() == null) {
            Log.e(TAG, "EffectComposer is null!");
            return;
        }

        // Получаем реальные значения из ползунков
        float brightness = (sbBrightness.getValue() / 50.0f) - 1.0f;
        float contrast = (sbContrast.getValue() / 100.0f) - 0.5f;
        float gamma = sbGamma.getValue() / 100.0f;
        float sharpness = sbSharpness.getValue() / 100.0f; // [0,40] -> [0,0.4]
        boolean enableFXAA = cbEnableFXAA.isChecked();
        boolean enableCRTShader = cbEnableCRTShader.isChecked();
        boolean enableToonShader = cbEnableToonShader.isChecked();
        boolean enableNTSCEffect = cbEnableNTSCEffect.isChecked();

        // Apply or remove ColorEffect
        if (brightness == 0 && contrast == 0 && gamma == 1.0f && sharpness == 0) {
            renderer.getEffectComposer().removeEffect(colorEffect);
        } else {
            if (colorEffect == null) {
                colorEffect = new ColorEffect();
                colorEffect.setRenderer(renderer);
            }
            
            colorEffect.setBrightness(brightness);
            colorEffect.setContrast(contrast);
            colorEffect.setGamma(gamma);
            colorEffect.setSharpness(sharpness);
            renderer.getEffectComposer().addEffect(colorEffect);
        }

        // Apply or remove FXAAEffect
        if (enableFXAA) {
            if (fxaaEffect == null) {
                fxaaEffect = new FXAAEffect();
                renderer.getEffectComposer().addEffect(fxaaEffect);
            }
        } else if (fxaaEffect != null) {
            renderer.getEffectComposer().removeEffect(fxaaEffect);
        }

        // Apply or remove CRTEffect
        if (enableCRTShader) {
            if (crtEffect == null) {
                crtEffect = new CRTEffect();
                renderer.getEffectComposer().addEffect(crtEffect);
            }
        } else if (crtEffect != null) {
            renderer.getEffectComposer().removeEffect(crtEffect);
        }

        // Apply or remove ToonEffect
        if (enableToonShader) {
            if (toonEffect == null) {
                toonEffect = new ToonEffect();
                renderer.getEffectComposer().addEffect(toonEffect);
            }
        } else if (toonEffect != null) {
            renderer.getEffectComposer().removeEffect(toonEffect);
        }

        // Apply or remove NTSCCombinedEffect
        if (enableNTSCEffect) {
            if (ntscEffect == null) {
                ntscEffect = new NTSCCombinedEffect();
                renderer.getEffectComposer().addEffect(ntscEffect);
            }
        } else if (ntscEffect != null) {
            renderer.getEffectComposer().removeEffect(ntscEffect);
        }
    }

    @Override
    public void dismiss() {
        handler.removeCallbacks(applyEffectsRunnable);
        super.dismiss();
    }
}