package com.winlator.cmod.inputcontrols;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.widget.InputControlsView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ControlsProfile implements Comparable<ControlsProfile> {
    public final int id;
    private String name;
    private float cursorSpeed = 1.0f;
    private final ArrayList<ControlElement> elements = new ArrayList<>();
    private final ArrayList<ExternalController> controllers = new ArrayList<>();
    private final List<ControlElement> immutableElements = Collections.unmodifiableList(elements);
    private boolean elementsLoaded = false;
    private boolean controllersLoaded = false;
    private boolean virtualGamepad = false;
    private final Context context;
    private GamepadState gamepadState;

    public ControlsProfile(Context context, int id) {
        this.context = context;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getCursorSpeed() {
        return cursorSpeed;
    }

    public void setCursorSpeed(float cursorSpeed) {
        this.cursorSpeed = cursorSpeed;
    }

    public boolean isVirtualGamepad() {
        return virtualGamepad;
    }

    public GamepadState getGamepadState() {
        if (gamepadState == null) gamepadState = new GamepadState();
        return gamepadState;
    }

    public ExternalController addController(String id) {
        ExternalController controller = getController(id);
        if (controller == null) controllers.add(controller = ExternalController.getController(id));
        controllersLoaded = true;
        return controller;
    }

    public void removeController(ExternalController controller) {
        if (!controllersLoaded) loadControllers();
        controllers.remove(controller);
    }

    public ExternalController getController(String id) {
        if (!controllersLoaded) loadControllers();
        for (ExternalController controller : controllers) if (controller.getId().equals(id)) return controller;
        return null;
    }

    public ExternalController getController(int deviceId) {
        if (!controllersLoaded) loadControllers();
        for (ExternalController controller : controllers) if (controller.getDeviceId() == deviceId) return controller;
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(ControlsProfile o) {
        return Integer.compare(id, o.id);
    }

    public boolean isElementsLoaded() {
        return elementsLoaded;
    }

    public void save() {
        File file = getProfileFile(context, id);

        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("name", name);
            data.put("cursorSpeed", Float.valueOf(cursorSpeed));

            JSONArray elementsJSONArray = new JSONArray();
            if (!elementsLoaded && file.isFile()) {
                JSONObject profileJSONObject = new JSONObject(FileUtils.readString(file));
                elementsJSONArray = profileJSONObject.getJSONArray("elements");
            }
            else for (ControlElement element : elements) elementsJSONArray.put(element.toJSONObject());
            data.put("elements", elementsJSONArray);

            JSONArray controllersJSONArray = new JSONArray();
            if (!controllersLoaded && file.isFile()) {
                JSONObject profileJSONObject = new JSONObject(FileUtils.readString(file));
                if (profileJSONObject.has("controllers")) controllersJSONArray = profileJSONObject.getJSONArray("controllers");
            }
            else {
                for (ExternalController controller : controllers) {
                    JSONObject controllerJSONObject = controller.toJSONObject();
                    if (controllerJSONObject != null) controllersJSONArray.put(controllerJSONObject);
                }
            }
            if (controllersJSONArray.length() > 0) data.put("controllers", controllersJSONArray);

            FileUtils.writeString(file, data.toString());
        }
        catch (JSONException e) {}
    }

    public static File getProfileFile(Context context, int id) {
        return new File(InputControlsManager.getProfilesDir(context), "controls-"+id+".icp");
    }

    public void addElement(ControlElement element) {
        elements.add(element);
        elementsLoaded = true;
    }

    public void removeElement(ControlElement element) {
        elements.remove(element);
        elementsLoaded = true;
    }

    public List<ControlElement> getElements() {
        return immutableElements;
    }

    public boolean isTemplate() {
        return name.toLowerCase(Locale.ENGLISH).contains("template");
    }

    public ArrayList<ExternalController> loadControllers() {
        controllers.clear();
        controllersLoaded = false;

        File file = getProfileFile(context, id);
        if (!file.isFile()) return controllers;

        try {
            JSONObject profileJSONObject = new JSONObject(FileUtils.readString(file));
            if (!profileJSONObject.has("controllers")) return controllers;
            JSONArray controllersJSONArray = profileJSONObject.getJSONArray("controllers");
            for (int i = 0; i < controllersJSONArray.length(); i++) {
                JSONObject controllerJSONObject = controllersJSONArray.getJSONObject(i);
                String id = controllerJSONObject.getString("id");
                ExternalController controller = new ExternalController();
                controller.setId(id);
                controller.setName(controllerJSONObject.getString("name"));

                JSONArray controllerBindingsJSONArray = controllerJSONObject.getJSONArray("controllerBindings");
                for (int j = 0; j < controllerBindingsJSONArray.length(); j++) {
                    JSONObject controllerBindingJSONObject = controllerBindingsJSONArray.getJSONObject(j);
                    ExternalControllerBinding controllerBinding = new ExternalControllerBinding();
                    controllerBinding.setKeyCode(controllerBindingJSONObject.getInt("keyCode"));
                    controllerBinding.setBinding(Binding.fromString(controllerBindingJSONObject.getString("binding")));
                    controller.addControllerBinding(controllerBinding);
                }
                controllers.add(controller);
            }
            controllersLoaded = true;
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return controllers;
    }

    public void loadElements(InputControlsView inputControlsView) {
        elements.clear();
        elementsLoaded = false;
        virtualGamepad = false;

        File file = getProfileFile(context, id);
        if (!file.isFile()) return;

        try {
            JSONObject profileJSONObject = new JSONObject(FileUtils.readString(file));
            JSONArray elementsJSONArray = profileJSONObject.getJSONArray("elements");
            for (int i = 0; i < elementsJSONArray.length(); i++) {
                JSONObject elementJSONObject = elementsJSONArray.getJSONObject(i);
                ControlElement element = new ControlElement(inputControlsView, elementJSONObject);
                element.setShape(ControlElement.Shape.valueOf(elementJSONObject.getString("shape")));
                element.setToggleSwitch(elementJSONObject.getBoolean("toggleSwitch"));
                element.setX((int)(elementJSONObject.getDouble("x") * inputControlsView.getMaxWidth()));
                element.setY((int)(elementJSONObject.getDouble("y") * inputControlsView.getMaxHeight()));
                element.setScale((float)elementJSONObject.getDouble("scale"));
                element.setText(elementJSONObject.getString("text"));
                element.setIconId(elementJSONObject.getInt("iconId"));
                
                // Load profile switching settings
                if (elementJSONObject.has("enableProfileSwitching")) {
                    element.setEnableProfileSwitching(elementJSONObject.getBoolean("enableProfileSwitching"));
                }
                if (elementJSONObject.has("targetProfileId")) {
                    element.setTargetProfileId(elementJSONObject.getInt("targetProfileId"));
                }
                if (elementJSONObject.has("switchDelay")) {
                    element.setSwitchDelay((float)elementJSONObject.getDouble("switchDelay"));
                }
                
                // Load DYNAMIC_STICK specific settings
                if (element.getType() == ControlElement.Type.DYNAMIC_STICK) {
                    if (elementJSONObject.has("stickActivationRadius")) {
                        element.setStickActivationRadius((float)elementJSONObject.getDouble("stickActivationRadius"));
                    }
                    
                    // Load new activation zone settings
                    if (elementJSONObject.has("showActivationZone")) {
                        element.setShowActivationZone(elementJSONObject.getBoolean("showActivationZone"));
                    }
                    if (elementJSONObject.has("activationZoneWidth")) {
                        element.setActivationZoneWidth((float)elementJSONObject.getDouble("activationZoneWidth"));
                    }
                    if (elementJSONObject.has("activationZoneHeight")) {
                        element.setActivationZoneHeight((float)elementJSONObject.getDouble("activationZoneHeight"));
                    }
                }
                
                // Load Vertical Scroll Bar specific settings
                if (element.getType() == ControlElement.Type.VERTICAL_SCROLL_BAR) {
                    if (elementJSONObject.has("scrollBarWidth")) {
                        element.setScrollBarWidth((float)elementJSONObject.getDouble("scrollBarWidth"));
                    }
                    if (elementJSONObject.has("scrollBarHeight")) {
                        element.setScrollBarHeight((float)elementJSONObject.getDouble("scrollBarHeight"));
                    }
                    if (elementJSONObject.has("scrollSensitivity")) {
                        element.setScrollSensitivity((float)elementJSONObject.getDouble("scrollSensitivity"));
                    }
                }
                
                // Load custom icon data if present
                if (elementJSONObject.has("customIconId")) {
                    element.setCustomIconId(elementJSONObject.getString("customIconId"));
                }
                if (elementJSONObject.has("hasCustomIcon")) {
                    element.setHasCustomIcon(elementJSONObject.getBoolean("hasCustomIcon"));
                }
                
                // Load icon size multiplier if present
                if (elementJSONObject.has("iconSizeMultiplier")) {
                    element.setIconSizeMultiplier((float)elementJSONObject.getDouble("iconSizeMultiplier"));
                }
                
                // Load opacity settings if present
                if (elementJSONObject.has("buttonOpacity")) {
                    element.setButtonOpacity((float)elementJSONObject.getDouble("buttonOpacity"));
                }
                if (elementJSONObject.has("iconOpacity")) {
                    element.setIconOpacity((float)elementJSONObject.getDouble("iconOpacity"));
                }
                
                // Load outline visibility if present
                if (elementJSONObject.has("showOutline")) {
                    element.setShowOutline(elementJSONObject.getBoolean("showOutline"));
                }
                
                // Load MultiBinding settings if present
                if (elementJSONObject.has("useMultiBinding")) {
                    element.setUseMultiBinding(elementJSONObject.getBoolean("useMultiBinding"));
                    if (element.isUseMultiBinding() && elementJSONObject.has("multiBindings")) {
                        JSONArray multiBindingsArray = elementJSONObject.getJSONArray("multiBindings");
                        for (int j = 0; j < multiBindingsArray.length() && j < 4; j++) {
                            JSONObject multiBindingJSON = multiBindingsArray.getJSONObject(j);
                            MultiBinding multiBinding = MultiBinding.fromJSONObject(multiBindingJSON);
                            element.setMultiBindingAt(j, multiBinding);
                        }
                    }
                }
                
                if (elementJSONObject.has("range")) element.setRange(ControlElement.Range.valueOf(elementJSONObject.getString("range")));
                if (elementJSONObject.has("orientation")) element.setOrientation((byte)elementJSONObject.getInt("orientation"));

                boolean hasGamepadBinding = true;
                JSONArray bindingsJSONArray = elementJSONObject.getJSONArray("bindings");
                for (int j = 0; j < bindingsJSONArray.length(); j++) {
                    Binding binding = Binding.fromString(bindingsJSONArray.getString(j));
                    element.setBindingAt(j, Binding.fromString(bindingsJSONArray.getString(j)));
                    if (!binding.isGamepad()) hasGamepadBinding = false;
                }

                if (!virtualGamepad && hasGamepadBinding) virtualGamepad = true;
                elements.add(element);
            }
            elementsLoaded = true;
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if this profile has any elements with profile switching enabled
     * @return true if at least one element has profile switching enabled
     */
    public boolean hasProfileSwitchingElements() {
        if (!elementsLoaded) return false;
        
        for (ControlElement element : elements) {
            if (element.isEnableProfileSwitching() && element.getTargetProfileId() != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get list of elements that have profile switching enabled
     * @return list of profile switching elements
     */
    public List<ControlElement> getProfileSwitchingElements() {
        List<ControlElement> switchingElements = new ArrayList<>();
        if (!elementsLoaded) return switchingElements;
        
        for (ControlElement element : elements) {
            if (element.isEnableProfileSwitching() && element.getTargetProfileId() != 0) {
                switchingElements.add(element);
            }
        }
        return switchingElements;
    }

    /**
     * Get the target profile ID for a specific element
     * @param elementId the element ID to check
     * @return target profile ID, or 0 if not found or not enabled
     */
    public int getTargetProfileIdForElement(String elementId) {
        if (!elementsLoaded) return 0;
        
        for (ControlElement element : elements) {
            if (element.getId().equals(elementId) && 
                element.isEnableProfileSwitching() && 
                element.getTargetProfileId() != 0) {
                return element.getTargetProfileId();
            }
        }
        return 0;
    }

    /**
     * Check if this profile has any elements with multi-binding enabled
     * @return true if at least one element has multi-binding enabled
     */
    public boolean hasMultiBindingElements() {
        if (!elementsLoaded) return false;
        
        for (ControlElement element : elements) {
            if (element.isUseMultiBinding() && !element.getMultiBindingAt(0).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get list of elements that have multi-binding enabled
     * @return list of multi-binding elements
     */
    public List<ControlElement> getMultiBindingElements() {
        List<ControlElement> multiBindingElements = new ArrayList<>();
        if (!elementsLoaded) return multiBindingElements;
        
        for (ControlElement element : elements) {
            if (element.isUseMultiBinding() && !element.getMultiBindingAt(0).isEmpty()) {
                multiBindingElements.add(element);
            }
        }
        return multiBindingElements;
    }

    /**
     * Check if this profile has any Vertical Scroll Bar elements
     * @return true if at least one element is a Vertical Scroll Bar
     */
    public boolean hasVerticalScrollBarElements() {
        if (!elementsLoaded) return false;
        
        for (ControlElement element : elements) {
            if (element.getType() == ControlElement.Type.VERTICAL_SCROLL_BAR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get list of Vertical Scroll Bar elements
     * @return list of Vertical Scroll Bar elements
     */
    public List<ControlElement> getVerticalScrollBarElements() {
        List<ControlElement> scrollBarElements = new ArrayList<>();
        if (!elementsLoaded) return scrollBarElements;
        
        for (ControlElement element : elements) {
            if (element.getType() == ControlElement.Type.VERTICAL_SCROLL_BAR) {
                scrollBarElements.add(element);
            }
        }
        return scrollBarElements;
    }
}