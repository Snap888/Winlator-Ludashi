package com.winlator.cmod;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.cmod.R;
import com.winlator.cmod.dialog.MultiBindingDialog;
import com.winlator.cmod.inputcontrols.Binding;
import com.winlator.cmod.inputcontrols.ControlElement;
import com.winlator.cmod.inputcontrols.ControlsProfile;
import com.winlator.cmod.inputcontrols.InputControlsManager;
import com.winlator.cmod.inputcontrols.MultiBinding;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.widget.InputControlsView;
import com.winlator.cmod.widget.NumberPicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ControlsEditorActivity extends AppCompatActivity implements View.OnClickListener {
    private InputControlsView inputControlsView;
    private ControlsProfile profile;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private String selectedCustomIconId = null;
    private InputControlsManager inputControlsManager;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        AppUtils.hideSystemUI(this);
        setContentView(R.layout.controls_editor_activity);

        inputControlsView = new InputControlsView(this);
        inputControlsView.setEditMode(true);
        inputControlsView.setOverlayOpacity(0.6f);

        profile = InputControlsManager.loadProfile(this, ControlsProfile.getProfileFile(this, getIntent().getIntExtra("profile_id", 0)));
        ((TextView)findViewById(R.id.TVProfileName)).setText(profile.getName());
        inputControlsView.setProfile(profile);

        // Initialize InputControlsManager for profile listing
        inputControlsManager = new InputControlsManager(this);

        FrameLayout container = findViewById(R.id.FLContainer);
        container.addView(inputControlsView, 0);

        container.findViewById(R.id.BTAddElement).setOnClickListener(this);
        container.findViewById(R.id.BTRemoveElement).setOnClickListener(this);
        container.findViewById(R.id.BTElementSettings).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.BTAddElement:
                if (!inputControlsView.addElement()) {
                    AppUtils.showToast(this, R.string.no_profile_selected);
                }
                break;
            case R.id.BTRemoveElement:
                if (!inputControlsView.removeElement()) {
                    AppUtils.showToast(this, R.string.no_control_element_selected);
                }
                break;
            case R.id.BTElementSettings:
                ControlElement selectedElement = inputControlsView.getSelectedElement();
                if (selectedElement != null) {
                    showControlElementSettings(v);
                }
                else AppUtils.showToast(this, R.string.no_control_element_selected);
                break;
        }
    }

    private void showControlElementSettings(View anchorView) {
        final ControlElement element = inputControlsView.getSelectedElement();
        View view = LayoutInflater.from(this).inflate(R.layout.control_element_settings, null);

        final Runnable updateLayout = () -> {
            ControlElement.Type type = element.getType();
            view.findViewById(R.id.LLShape).setVisibility(View.GONE);
            view.findViewById(R.id.CBToggleSwitch).setVisibility(View.GONE);
            view.findViewById(R.id.LLCustomTextIcon).setVisibility(View.GONE);
            view.findViewById(R.id.LLRangeOptions).setVisibility(View.GONE);
            view.findViewById(R.id.LLShowOutline).setVisibility(View.GONE);
            view.findViewById(R.id.LLDynamicStickOptions).setVisibility(View.GONE);
            view.findViewById(R.id.LLDynamicStickVisual).setVisibility(View.GONE);
            view.findViewById(R.id.LLProfileSwitching).setVisibility(View.GONE);
            view.findViewById(R.id.LLMultiBinding).setVisibility(View.GONE);
            view.findViewById(R.id.LLScrollBarOptions).setVisibility(View.GONE);

            if (type == ControlElement.Type.BUTTON) {
                view.findViewById(R.id.LLShape).setVisibility(View.VISIBLE);
                view.findViewById(R.id.CBToggleSwitch).setVisibility(View.VISIBLE);
                view.findViewById(R.id.LLCustomTextIcon).setVisibility(View.VISIBLE);
                view.findViewById(R.id.LLShowOutline).setVisibility(View.VISIBLE);
                view.findViewById(R.id.LLProfileSwitching).setVisibility(View.VISIBLE);
                view.findViewById(R.id.LLMultiBinding).setVisibility(View.VISIBLE);
            }
            else if (type == ControlElement.Type.RANGE_BUTTON) {
                view.findViewById(R.id.LLRangeOptions).setVisibility(View.VISIBLE);
                view.findViewById(R.id.LLShowOutline).setVisibility(View.VISIBLE);
            }
            else if (type == ControlElement.Type.D_PAD || type == ControlElement.Type.STICK || type == ControlElement.Type.TRACKPAD) {
                view.findViewById(R.id.LLShowOutline).setVisibility(View.VISIBLE);
            }
            else if (type == ControlElement.Type.DYNAMIC_STICK) {
                view.findViewById(R.id.LLDynamicStickOptions).setVisibility(View.VISIBLE);
                view.findViewById(R.id.LLDynamicStickVisual).setVisibility(View.VISIBLE);
                view.findViewById(R.id.LLShowOutline).setVisibility(View.VISIBLE);
            }
            else if (type == ControlElement.Type.VERTICAL_SCROLL_BAR) {
                view.findViewById(R.id.LLScrollBarOptions).setVisibility(View.VISIBLE);
                view.findViewById(R.id.LLShowOutline).setVisibility(View.VISIBLE);
            }

            loadBindingSpinners(element, view);
        };

        loadTypeSpinner(element, view.findViewById(R.id.SType), updateLayout);
        loadShapeSpinner(element, view.findViewById(R.id.SShape));
        loadRangeSpinner(element, view.findViewById(R.id.SRange));

        // Profile Switching Controls
        final LinearLayout llProfileSwitching = view.findViewById(R.id.LLProfileSwitching);
        final CheckBox cbEnableProfileSwitching = view.findViewById(R.id.CBEnableProfileSwitching);
        final Spinner sTargetProfile = view.findViewById(R.id.STargetProfile);
        final TextView tvSwitchDelay = view.findViewById(R.id.TVSwitchDelay);
        final SeekBar sbSwitchDelay = view.findViewById(R.id.SBSwitchDelay);

        // Initialize Profile Switching controls
        cbEnableProfileSwitching.setChecked(element.isEnableProfileSwitching());
        cbEnableProfileSwitching.setOnCheckedChangeListener((buttonView, isChecked) -> {
            element.setEnableProfileSwitching(isChecked);
            profile.save();
            updateProfileSwitchingLayout(view, element);
        });

        // Load target profile spinner
        loadTargetProfileSpinner(element, sTargetProfile);

        // Initialize switch delay control
        int delayProgress = (int)(element.getSwitchDelay() * 10); // Convert to 0-30 range
        sbSwitchDelay.setProgress(delayProgress);
        updateSwitchDelayText(tvSwitchDelay, element.getSwitchDelay());

        sbSwitchDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float delay = progress / 10.0f; // Convert to 0.0-3.0 seconds
                    element.setSwitchDelay(delay);
                    updateSwitchDelayText(tvSwitchDelay, delay);
                    profile.save();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Update profile switching layout visibility
        updateProfileSwitchingLayout(view, element);

        // Multi-Binding Controls
        final LinearLayout llMultiBinding = view.findViewById(R.id.LLMultiBinding);
        final CheckBox cbUseMultiBinding = view.findViewById(R.id.CBUseMultiBinding);
        final Button btnConfigureMultiBinding = view.findViewById(R.id.BTConfigureMultiBinding);
        final TextView tvMultiBindingPreview = view.findViewById(R.id.TVMultiBindingPreview);

        // Initialize Multi-Binding controls
        cbUseMultiBinding.setChecked(element.isUseMultiBinding());
        btnConfigureMultiBinding.setEnabled(element.isUseMultiBinding());
        updateMultiBindingPreview(tvMultiBindingPreview, element);

        cbUseMultiBinding.setOnCheckedChangeListener((buttonView, isChecked) -> {
            element.setUseMultiBinding(isChecked);
            btnConfigureMultiBinding.setEnabled(isChecked);
            profile.save();
            updateMultiBindingPreview(tvMultiBindingPreview, element);
            inputControlsView.invalidate();
        });

        btnConfigureMultiBinding.setOnClickListener(v -> {
            showMultiBindingDialog(element, tvMultiBindingPreview);
        });

        // DYNAMIC_STICK Activation Radius Control
        final TextView tvActivationRadius = view.findViewById(R.id.TVActivationRadius);
        SeekBar sbActivationRadius = view.findViewById(R.id.SBActivationRadius);
        
        if (element.getType() == ControlElement.Type.DYNAMIC_STICK) {
            int progress = (int)((element.getStickActivationRadius() - ControlElement.MIN_ACTIVATION_RADIUS) / 
                               (ControlElement.MAX_ACTIVATION_RADIUS - ControlElement.MIN_ACTIVATION_RADIUS) * 100);
            sbActivationRadius.setProgress(progress);
            tvActivationRadius.setText((int)element.getStickActivationRadius() + "px");
        }
        
        sbActivationRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && element.getType() == ControlElement.Type.DYNAMIC_STICK) {
                    float radius = ControlElement.MIN_ACTIVATION_RADIUS + 
                                 (progress / 100.0f) * (ControlElement.MAX_ACTIVATION_RADIUS - ControlElement.MIN_ACTIVATION_RADIUS);
                    element.setStickActivationRadius(radius);
                    tvActivationRadius.setText((int)radius + "px");
                    profile.save();
                    inputControlsView.invalidate();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // NEW: Show Activation Zone Checkbox
        CheckBox cbShowActivationZone = view.findViewById(R.id.CBShowActivationZone);
        if (element.getType() == ControlElement.Type.DYNAMIC_STICK) {
            cbShowActivationZone.setChecked(element.isShowActivationZone());
            cbShowActivationZone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                element.setShowActivationZone(isChecked);
                profile.save();
                inputControlsView.invalidate();
            });
        }

        // NEW: Activation Zone Width Control
        final TextView tvZoneWidth = view.findViewById(R.id.TVZoneWidth);
        SeekBar sbZoneWidth = view.findViewById(R.id.SBZoneWidth);
        if (element.getType() == ControlElement.Type.DYNAMIC_STICK) {
            int widthProgress = (int)((element.getActivationZoneWidth() - ControlElement.MIN_ACTIVATION_ZONE_SIZE) / 
                                    (ControlElement.MAX_ACTIVATION_ZONE_SIZE - ControlElement.MIN_ACTIVATION_ZONE_SIZE) * 100);
            sbZoneWidth.setProgress(widthProgress);
            tvZoneWidth.setText((int)element.getActivationZoneWidth() + "px");
            
            sbZoneWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && element.getType() == ControlElement.Type.DYNAMIC_STICK) {
                        float width = ControlElement.MIN_ACTIVATION_ZONE_SIZE + 
                                    (progress / 100.0f) * (ControlElement.MAX_ACTIVATION_ZONE_SIZE - ControlElement.MIN_ACTIVATION_ZONE_SIZE);
                        element.setActivationZoneWidth(width);
                        tvZoneWidth.setText((int)width + "px");
                        profile.save();
                        inputControlsView.invalidate();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // NEW: Activation Zone Height Control
        final TextView tvZoneHeight = view.findViewById(R.id.TVZoneHeight);
        SeekBar sbZoneHeight = view.findViewById(R.id.SBZoneHeight);
        if (element.getType() == ControlElement.Type.DYNAMIC_STICK) {
            int heightProgress = (int)((element.getActivationZoneHeight() - ControlElement.MIN_ACTIVATION_ZONE_SIZE) / 
                                     (ControlElement.MAX_ACTIVATION_ZONE_SIZE - ControlElement.MIN_ACTIVATION_ZONE_SIZE) * 100);
            sbZoneHeight.setProgress(heightProgress);
            tvZoneHeight.setText((int)element.getActivationZoneHeight() + "px");
            
            sbZoneHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && element.getType() == ControlElement.Type.DYNAMIC_STICK) {
                        float height = ControlElement.MIN_ACTIVATION_ZONE_SIZE + 
                                     (progress / 100.0f) * (ControlElement.MAX_ACTIVATION_ZONE_SIZE - ControlElement.MIN_ACTIVATION_ZONE_SIZE);
                        element.setActivationZoneHeight(height);
                        tvZoneHeight.setText((int)height + "px");
                        profile.save();
                        inputControlsView.invalidate();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // VERTICAL SCROLL BAR Controls
        final LinearLayout llScrollBarOptions = view.findViewById(R.id.LLScrollBarOptions);
        final TextView tvScrollBarWidth = view.findViewById(R.id.TVScrollBarWidth);
        final SeekBar sbScrollBarWidth = view.findViewById(R.id.SBScrollBarWidth);
        final TextView tvScrollBarHeight = view.findViewById(R.id.TVScrollBarHeight);
        final SeekBar sbScrollBarHeight = view.findViewById(R.id.SBScrollBarHeight);
        final TextView tvScrollSensitivity = view.findViewById(R.id.TVScrollSensitivity);
        final SeekBar sbScrollSensitivity = view.findViewById(R.id.SBScrollSensitivity);

        if (element.getType() == ControlElement.Type.VERTICAL_SCROLL_BAR) {
            // Initialize Scroll Bar Width
            int widthProgress = (int)((element.getScrollBarWidth() - ControlElement.MIN_SCROLLBAR_WIDTH) / 
                                    (ControlElement.MAX_SCROLLBAR_WIDTH - ControlElement.MIN_SCROLLBAR_WIDTH) * 100);
            sbScrollBarWidth.setProgress(widthProgress);
            tvScrollBarWidth.setText((int)element.getScrollBarWidth() + "px");
            
            sbScrollBarWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && element.getType() == ControlElement.Type.VERTICAL_SCROLL_BAR) {
                        float width = ControlElement.MIN_SCROLLBAR_WIDTH + 
                                    (progress / 100.0f) * (ControlElement.MAX_SCROLLBAR_WIDTH - ControlElement.MIN_SCROLLBAR_WIDTH);
                        element.setScrollBarWidth(width);
                        tvScrollBarWidth.setText((int)width + "px");
                        profile.save();
                        inputControlsView.invalidate();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            // Initialize Scroll Bar Height
            int heightProgress = (int)((element.getScrollBarHeight() - ControlElement.MIN_SCROLLBAR_HEIGHT) / 
                                     (ControlElement.MAX_SCROLLBAR_HEIGHT - ControlElement.MIN_SCROLLBAR_HEIGHT) * 100);
            sbScrollBarHeight.setProgress(heightProgress);
            tvScrollBarHeight.setText((int)element.getScrollBarHeight() + "px");
            
            sbScrollBarHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && element.getType() == ControlElement.Type.VERTICAL_SCROLL_BAR) {
                        float height = ControlElement.MIN_SCROLLBAR_HEIGHT + 
                                     (progress / 100.0f) * (ControlElement.MAX_SCROLLBAR_HEIGHT - ControlElement.MIN_SCROLLBAR_HEIGHT);
                        element.setScrollBarHeight(height);
                        tvScrollBarHeight.setText((int)height + "px");
                        profile.save();
                        inputControlsView.invalidate();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            // Initialize Scroll Sensitivity
            int sensitivityProgress = (int)((element.getScrollSensitivity() - ControlElement.MIN_SCROLL_SENSITIVITY) / 
                                          (ControlElement.MAX_SCROLL_SENSITIVITY - ControlElement.MIN_SCROLL_SENSITIVITY) * 100);
            sbScrollSensitivity.setProgress(sensitivityProgress);
            updateScrollSensitivityText(tvScrollSensitivity, element.getScrollSensitivity());
            
            sbScrollSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && element.getType() == ControlElement.Type.VERTICAL_SCROLL_BAR) {
                        float sensitivity = ControlElement.MIN_SCROLL_SENSITIVITY + 
                                          (progress / 100.0f) * (ControlElement.MAX_SCROLL_SENSITIVITY - ControlElement.MIN_SCROLL_SENSITIVITY);
                        element.setScrollSensitivity(sensitivity);
                        updateScrollSensitivityText(tvScrollSensitivity, sensitivity);
                        profile.save();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Show Outline Checkbox
        CheckBox cbShowOutline = view.findViewById(R.id.CBShowOutline);
        cbShowOutline.setChecked(element.isShowOutline());
        cbShowOutline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            element.setShowOutline(isChecked);
            profile.save();
            inputControlsView.invalidate();
        });

        RadioGroup rgOrientation = view.findViewById(R.id.RGOrientation);
        rgOrientation.check(element.getOrientation() == 1 ? R.id.RBVertical : R.id.RBHorizontal);
        rgOrientation.setOnCheckedChangeListener((group, checkedId) -> {
            element.setOrientation((byte)(checkedId == R.id.RBVertical ? 1 : 0));
            profile.save();
            inputControlsView.invalidate();
        });

        NumberPicker npColumns = view.findViewById(R.id.NPColumns);
        npColumns.setValue(element.getBindingCount());
        npColumns.setOnValueChangeListener((numberPicker, value) -> {
            element.setBindingCount(value);
            profile.save();
            inputControlsView.invalidate();
        });

        final TextView tvScale = view.findViewById(R.id.TVScale);
        SeekBar sbScale = view.findViewById(R.id.SBScale);
        sbScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvScale.setText(progress+"%");
                if (fromUser) {
                    progress = (int)Mathf.roundTo(progress, 5);
                    seekBar.setProgress(progress);
                    element.setScale(progress / 100.0f);
                    profile.save();
                    inputControlsView.invalidate();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        sbScale.setProgress((int)(element.getScale() * 100));

        // Icon Size Control
        final TextView tvIconSize = view.findViewById(R.id.TVIconSize);
        SeekBar sbIconSize = view.findViewById(R.id.SBIconSize);
        sbIconSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvIconSize.setText(progress + "%");
                if (fromUser) {
                    element.setIconSizeMultiplier(progress / 100.0f);
                    profile.save();
                    inputControlsView.invalidate();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        sbIconSize.setProgress((int)(element.getIconSizeMultiplier() * 100));

        // Button Opacity Control
        final TextView tvButtonOpacity = view.findViewById(R.id.TVButtonOpacity);
        SeekBar sbButtonOpacity = view.findViewById(R.id.SBButtonOpacity);
        sbButtonOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvButtonOpacity.setText(progress + "%");
                if (fromUser) {
                    element.setButtonOpacity(progress / 100.0f);
                    profile.save();
                    inputControlsView.invalidate();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        sbButtonOpacity.setProgress((int)(element.getButtonOpacity() * 100));

        // Icon Opacity Control
        final TextView tvIconOpacity = view.findViewById(R.id.TVIconOpacity);
        SeekBar sbIconOpacity = view.findViewById(R.id.SBIconOpacity);
        sbIconOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvIconOpacity.setText(progress + "%");
                if (fromUser) {
                    element.setIconOpacity(progress / 100.0f);
                    profile.save();
                    inputControlsView.invalidate();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        sbIconOpacity.setProgress((int)(element.getIconOpacity() * 100));

        CheckBox cbToggleSwitch = view.findViewById(R.id.CBToggleSwitch);
        cbToggleSwitch.setChecked(element.isToggleSwitch());
        cbToggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            element.setToggleSwitch(isChecked);
            profile.save();
        });

        final EditText etCustomText = view.findViewById(R.id.ETCustomText);
        etCustomText.setText(element.getText());
        
        // Reset selected custom icon ID
        selectedCustomIconId = null;
        
        // Custom Icon Management
        Button btnCustomIcon = view.findViewById(R.id.BTCustomIcon);
        btnCustomIcon.setOnClickListener(v -> selectCustomIcon(element));
        
        Button btnRemoveCustomIcon = view.findViewById(R.id.BTRemoveCustomIcon);
        btnRemoveCustomIcon.setOnClickListener(v -> removeCustomIcon(element));
        updateRemoveCustomIconButton(btnRemoveCustomIcon, element);
        
        // Custom Icon Preview
        ImageView ivCustomIconPreview = view.findViewById(R.id.IVCustomIconPreview);
        TextView tvNoCustomIcon = view.findViewById(R.id.TVNoCustomIcon);
        updateCustomIconPreview(ivCustomIconPreview, tvNoCustomIcon, element);

        // Load icons into separate containers
        final LinearLayout llDefaultIcons = view.findViewById(R.id.LLDefaultIcons);
        final LinearLayout llCustomIcons = view.findViewById(R.id.LLCustomIcons);
        TextView tvNoCustomIcons = view.findViewById(R.id.TVNoCustomIcons);
        
        loadDefaultIcons(llDefaultIcons, element);
        loadCustomIcons(llCustomIcons, tvNoCustomIcons, element);

        updateLayout.run();

        PopupWindow popupWindow = AppUtils.showPopupWindow(anchorView, view, 340, 0);
        popupWindow.setOnDismissListener(() -> {
            String text = etCustomText.getText().toString().trim();
            byte iconId = 0;
            boolean hasCustomIcon = false;
            String customIconId = null;
            
            // Find selected icon from default icons
            for (int i = 0; i < llDefaultIcons.getChildCount(); i++) {
                View child = llDefaultIcons.getChildAt(i);
                if (child.isSelected()) {
                    Object tag = child.getTag();
                    if (tag instanceof Byte) {
                        iconId = (Byte) tag;
                        hasCustomIcon = false;
                        customIconId = null;
                    }
                    break;
                }
            }
            
            // Find selected icon from custom icons
            for (int i = 0; i < llCustomIcons.getChildCount(); i++) {
                View child = llCustomIcons.getChildAt(i);
                if (child.isSelected()) {
                    Object tag = child.getTag();
                    if (tag instanceof String) {
                        String customTag = (String) tag;
                        if (customTag.startsWith("custom_")) {
                            hasCustomIcon = true;
                            customIconId = customTag.substring(7); // Remove "custom_" prefix
                            iconId = 0; // Reset default icon ID
                        }
                    }
                    break;
                }
            }

            element.setText(text);
            element.setIconId(iconId);
            element.setHasCustomIcon(hasCustomIcon);
            element.setCustomIconId(customIconId);
            profile.save();
            inputControlsView.invalidate();
        });
    }

    private void updateProfileSwitchingLayout(View view, ControlElement element) {
        LinearLayout llProfileSwitching = view.findViewById(R.id.LLProfileSwitching);
        CheckBox cbEnableProfileSwitching = view.findViewById(R.id.CBEnableProfileSwitching);
        LinearLayout llProfileSwitchingContent = view.findViewById(R.id.LLProfileSwitching);
        
        // Show profile switching section only for BUTTON type and when enabled
        if (element.getType() == ControlElement.Type.BUTTON) {
            llProfileSwitching.setVisibility(View.VISIBLE);
            
            // Enable/disable content based on checkbox state
            boolean enabled = cbEnableProfileSwitching.isChecked();
            ViewGroup contentContainer = (ViewGroup) llProfileSwitchingContent;
            for (int i = 0; i < contentContainer.getChildCount(); i++) {
                View child = contentContainer.getChildAt(i);
                if (child.getId() != R.id.CBEnableProfileSwitching) {
                    child.setEnabled(enabled);
                    child.setAlpha(enabled ? 1.0f : 0.5f);
                }
            }
        } else {
            llProfileSwitching.setVisibility(View.GONE);
        }
    }

    private void updateMultiBindingPreview(TextView preview, ControlElement element) {
        if (element.isUseMultiBinding()) {
            MultiBinding multiBinding = element.getMultiBindingAt(0);
            if (multiBinding != null && !multiBinding.isEmpty()) {
                preview.setText("Actions: " + multiBinding.toString());
            } else {
                preview.setText("No multi-actions configured");
            }
        } else {
            preview.setText("Multi-actions disabled");
        }
    }

    private void showMultiBindingDialog(ControlElement element, TextView preview) {
        MultiBindingDialog dialog = MultiBindingDialog.newInstance(element.getMultiBindingAt(0));
        dialog.setOnMultiBindingSetListener(multiBinding -> {
            element.setMultiBindingAt(0, multiBinding);
            profile.save();
            updateMultiBindingPreview(preview, element);
            inputControlsView.invalidate();
        });
        dialog.show(getSupportFragmentManager(), "multi_binding");
    }

    private void updateSwitchDelayText(TextView textView, float delay) {
        if (delay == 0.0f) {
            textView.setText("Instant");
        } else {
            textView.setText(String.format("%.1fs", delay));
        }
    }

    private void updateScrollSensitivityText(TextView textView, float sensitivity) {
        textView.setText(String.format("%.1fx", sensitivity));
    }

    private void loadTargetProfileSpinner(ControlElement element, Spinner spinner) {
        // Get all available profiles excluding the current one
        java.util.List<ControlsProfile> allProfiles = inputControlsManager.getProfiles(true); // ignoreTemplates = true
        java.util.List<ControlsProfile> availableProfiles = new java.util.ArrayList<>();
        java.util.List<String> profileNames = new java.util.ArrayList<>();
        
        // Add "None" option
        profileNames.add("None");
        
        for (ControlsProfile profile : allProfiles) {
            if (profile.id != this.profile.id) { // Exclude current profile
                availableProfiles.add(profile);
                profileNames.add(profile.getName());
            }
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, profileNames);
        spinner.setAdapter(adapter);
        
        // Set current selection
        int selectedPosition = 0; // Default to "None"
        for (int i = 0; i < availableProfiles.size(); i++) {
            if (availableProfiles.get(i).id == element.getTargetProfileId()) {
                selectedPosition = i + 1; // +1 because of "None" option
                break;
            }
        }
        spinner.setSelection(selectedPosition, false);
        
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int targetProfileId = 0;
                if (position > 0) { // Not "None"
                    targetProfileId = availableProfiles.get(position - 1).id;
                }
                element.setTargetProfileId(targetProfileId);
                profile.save();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateRemoveCustomIconButton(Button btn, ControlElement element) {
        btn.setVisibility(element.hasCustomIcon() ? View.VISIBLE : View.GONE);
    }

    private void updateCustomIconPreview(ImageView preview, TextView noIconText, ControlElement element) {
        if (element.hasCustomIcon()) {
            Bitmap customIcon = inputControlsView.getCustomIcon(element.getCustomIconId());
            if (customIcon != null) {
                preview.setImageBitmap(customIcon);
                preview.setVisibility(View.VISIBLE);
                noIconText.setVisibility(View.GONE);
                return;
            }
        }
        preview.setVisibility(View.GONE);
        noIconText.setVisibility(View.VISIBLE);
    }

    private void selectCustomIcon(ControlElement element) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(
                Intent.createChooser(intent, "Select Custom Icon"),
                PICK_IMAGE_REQUEST
            );
        } catch (android.content.ActivityNotFoundException ex) {
            AppUtils.showToast(this, "No file browser available");
        }
    }

    private void removeCustomIcon(ControlElement element) {
        if (element.hasCustomIcon()) {
            // Remove from element
            String customIconId = element.getCustomIconId();
            element.setHasCustomIcon(false);
            element.setCustomIconId(null);
            
            // Remove from storage and cache
            inputControlsView.removeCustomIconFromLibrary(customIconId);
            
            profile.save();
            inputControlsView.invalidate();
            AppUtils.showToast(this, "Custom icon removed");
            
            // Close and reopen popup to refresh UI
            showControlElementSettings(findViewById(R.id.BTElementSettings));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                processSelectedImage(imageUri);
            }
        }
    }

    private void processSelectedImage(Uri imageUri) {
        ControlElement element = inputControlsView.getSelectedElement();
        if (element == null) return;
        
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            
            if (originalBitmap != null) {
                // Generate unique ID for the custom icon
                String customIconId = "custom_" + System.currentTimeMillis();
                
                // Resize to appropriate size for icons (e.g., 128x128 for better quality)
                int targetSize = 128;
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetSize, targetSize, true);
                
                // Set as custom icon
                inputControlsView.setCustomIcon(customIconId, resizedBitmap);
                element.setHasCustomIcon(true);
                element.setCustomIconId(customIconId);
                
                profile.save();
                inputControlsView.invalidate();
                AppUtils.showToast(this, "Custom icon set");
                
                // Close and reopen popup to refresh UI
                showControlElementSettings(findViewById(R.id.BTElementSettings));
                
                // Clean up
                if (inputStream != null) {
                    inputStream.close();
                }
                if (originalBitmap != null && originalBitmap != resizedBitmap) {
                    originalBitmap.recycle();
                }
            }
        } catch (IOException e) {
            AppUtils.showToast(this, "Error loading image");
            Log.e("ControlsEditor", "Error processing selected image", e);
        }
    }

    private void loadTypeSpinner(final ControlElement element, Spinner spinner, Runnable callback) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ControlElement.Type.names()));
        spinner.setSelection(element.getType().ordinal(), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                element.setType(ControlElement.Type.values()[position]);
                profile.save();
                callback.run();
                inputControlsView.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadShapeSpinner(final ControlElement element, Spinner spinner) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ControlElement.Shape.names()));
        spinner.setSelection(element.getShape().ordinal(), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                element.setShape(ControlElement.Shape.values()[position]);
                profile.save();
                inputControlsView.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadBindingSpinners(ControlElement element, View view) {
        LinearLayout container = view.findViewById(R.id.LLBindings);
        container.removeAllViews();

        ControlElement.Type type = element.getType();
        if (type == ControlElement.Type.BUTTON) {
            loadBindingSpinner(element, container, 0, R.string.binding);
        }
        else if (type == ControlElement.Type.D_PAD || type == ControlElement.Type.STICK || 
                 type == ControlElement.Type.TRACKPAD || type == ControlElement.Type.DYNAMIC_STICK) {
            loadBindingSpinner(element, container, 0, R.string.binding_up);
            loadBindingSpinner(element, container, 1, R.string.binding_right);
            loadBindingSpinner(element, container, 2, R.string.binding_down);
            loadBindingSpinner(element, container, 3, R.string.binding_left);
        }
        else if (type == ControlElement.Type.VERTICAL_SCROLL_BAR) {
            loadBindingSpinner(element, container, 0, R.string.binding_scroll_up);
            loadBindingSpinner(element, container, 1, R.string.binding_scroll_down);
        }
    }

    private void loadBindingSpinner(final ControlElement element, LinearLayout container, final int index, int titleResId) {
        View view = LayoutInflater.from(this).inflate(R.layout.binding_field, container, false);
        ((TextView)view.findViewById(R.id.TVTitle)).setText(titleResId);
        final Spinner sBindingType = view.findViewById(R.id.SBindingType);
        final Spinner sBinding = view.findViewById(R.id.SBinding);

        Runnable update = () -> {
            String[] bindingEntries = null;
            switch (sBindingType.getSelectedItemPosition()) {
                case 0:
                    bindingEntries = Binding.keyboardBindingLabels();
                    break;
                case 1:
                    bindingEntries = Binding.mouseBindingLabels();
                    break;
                case 2:
                    bindingEntries = Binding.gamepadBindingLabels();
                    break;
            }

            sBinding.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bindingEntries));
            AppUtils.setSpinnerSelectionFromValue(sBinding, element.getBindingAt(index).toString());
        };

        sBindingType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                update.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Binding selectedBinding = element.getBindingAt(index);
        if (selectedBinding.isKeyboard()) {
            sBindingType.setSelection(0, false);
        }
        else if (selectedBinding.isMouse()) {
            sBindingType.setSelection(1, false);
        }
        else if (selectedBinding.isGamepad()) {
            sBindingType.setSelection(2, false);
        }

        sBinding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Binding binding = Binding.NONE;
                switch (sBindingType.getSelectedItemPosition()) {
                    case 0:
                        binding = Binding.keyboardBindingValues()[position];
                        break;
                    case 1:
                        binding = Binding.mouseBindingValues()[position];
                        break;
                    case 2:
                        binding = Binding.gamepadBindingValues()[position];
                        break;
                }

                if (binding != element.getBindingAt(index)) {
                    element.setBindingAt(index, binding);
                    profile.save();
                    inputControlsView.invalidate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        update.run();
        container.addView(view);
    }

    private void loadRangeSpinner(final ControlElement element, Spinner spinner) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ControlElement.Range.names()));
        spinner.setSelection(element.getRange().ordinal(), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                element.setRange(ControlElement.Range.values()[position]);
                profile.save();
                inputControlsView.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadDefaultIcons(final LinearLayout parent, ControlElement element) {
        byte selectedId = element.getIconId();
        boolean hasCustomIcon = element.hasCustomIcon();

        byte[] iconIds = new byte[0];
        try {
            String[] filenames = getAssets().list("inputcontrols/icons/");
            iconIds = new byte[filenames.length];
            for (int i = 0; i < filenames.length; i++) {
                iconIds[i] = Byte.parseByte(FileUtils.getBasename(filenames[i]));
            }
        }
        catch (IOException e) {}

        Arrays.sort(iconIds);

        int size = (int)UnitUtils.dpToPx(40);
        int margin = (int)UnitUtils.dpToPx(2);
        int padding = (int)UnitUtils.dpToPx(4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(margin, 0, margin, 0);

        // Load default icons
        for (final byte id : iconIds) {
            ImageView imageView = createIconImageView(id, selectedId, hasCustomIcon, params, padding);
            
            try (InputStream is = getAssets().open("inputcontrols/icons/"+id+".png")) {
                imageView.setImageBitmap(BitmapFactory.decodeStream(is));
            }
            catch (IOException e) {}
            
            parent.addView(imageView);
        }
    }

    private void loadCustomIcons(LinearLayout parent, TextView noIconsText, ControlElement element) {
        parent.removeAllViews();
        
        File iconsDir = new File(getFilesDir(), "custom_icons");
        if (iconsDir.exists() && iconsDir.isDirectory()) {
            File[] customIconFiles = iconsDir.listFiles();
            if (customIconFiles != null && customIconFiles.length > 0) {
                noIconsText.setVisibility(View.GONE);
                
                int size = (int)UnitUtils.dpToPx(40);
                int margin = (int)UnitUtils.dpToPx(2);
                int padding = (int)UnitUtils.dpToPx(4);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(margin, 0, margin, 0);

                for (File iconFile : customIconFiles) {
                    if (iconFile.isFile() && iconFile.getName().toLowerCase().endsWith(".png")) {
                        ImageView imageView = createCustomIconImageView(iconFile, element, params, padding);
                        parent.addView(imageView);
                    }
                }
            } else {
                noIconsText.setVisibility(View.VISIBLE);
            }
        } else {
            noIconsText.setVisibility(View.VISIBLE);
        }
    }

    private ImageView createIconImageView(final byte id, byte selectedId, boolean hasCustomIcon, 
                                        LinearLayout.LayoutParams params, int padding) {
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(params);
        imageView.setPadding(padding, padding, padding, padding);
        imageView.setBackgroundResource(R.drawable.icon_background);
        imageView.setTag(id);
        
        // Select only if this is the selected default icon AND no custom icon is set
        imageView.setSelected(id == selectedId && !hasCustomIcon);
        
        imageView.setOnClickListener((v) -> {
            // Deselect all icons in both containers
            deselectAllIconsInParent((ViewGroup)v.getParent());
            deselectAllIconsInParent((ViewGroup)findViewById(R.id.LLCustomIcons));
            v.setSelected(true);
        });
        return imageView;
    }

    private ImageView createCustomIconImageView(File iconFile, ControlElement element, 
                                              LinearLayout.LayoutParams params, int padding) {
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(params);
        imageView.setPadding(padding, padding, padding, padding);
        imageView.setBackgroundResource(R.drawable.icon_background);
        
        String iconId = iconFile.getName().replace(".png", "");
        imageView.setTag("custom_" + iconId);
        
        // Set selection state based on current element's custom icon
        boolean isSelected = element.hasCustomIcon() && iconId.equals(element.getCustomIconId());
        imageView.setSelected(isSelected);
        
        // Click listener for selection
        imageView.setOnClickListener((v) -> {
            // Deselect all icons in both containers
            deselectAllIconsInParent((ViewGroup)v.getParent());
            deselectAllIconsInParent((ViewGroup)findViewById(R.id.LLDefaultIcons));
            v.setSelected(true);
        });
        
        // Long-press listener for deletion
        imageView.setOnLongClickListener(v -> {
            showCustomIconContextMenu(v, iconId);
            return true;
        });
        
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(iconFile));
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        } catch (IOException e) {
            Log.e("ControlsEditor", "Error loading custom icon: " + iconFile.getName(), e);
        }
        
        return imageView;
    }

    private void deselectAllIconsInParent(ViewGroup parent) {
        if (parent != null) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                parent.getChildAt(i).setSelected(false);
            }
        }
    }

    private void showCustomIconContextMenu(View iconView, String customIconId) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Custom Icon");
        builder.setItems(new String[]{"Delete from Library"}, (dialog, which) -> {
            if (which == 0) {
                deleteCustomIconFromLibrary(customIconId);
            }
        });
        builder.show();
    }

    private void deleteCustomIconFromLibrary(String customIconId) {
        // Remove from InputControlsView cache and storage
        inputControlsView.removeCustomIconFromLibrary(customIconId);
        
        // Update any elements that were using this icon
        if (profile != null) {
            for (ControlElement element : profile.getElements()) {
                if (element.hasCustomIcon() && customIconId.equals(element.getCustomIconId())) {
                    element.setHasCustomIcon(false);
                    element.setCustomIconId(null);
                }
            }
            profile.save();
        }
        
        // Update UI
        showControlElementSettings(findViewById(R.id.BTElementSettings));
        AppUtils.showToast(this, "Custom icon deleted from library");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
    }
}