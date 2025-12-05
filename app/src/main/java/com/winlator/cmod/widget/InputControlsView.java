package com.winlator.cmod.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.dialog.EditControlElementDialog;
import com.winlator.cmod.inputcontrols.Binding;
import com.winlator.cmod.inputcontrols.ControlElement;
import com.winlator.cmod.inputcontrols.ControlsProfile;
import com.winlator.cmod.inputcontrols.ExternalController;
import com.winlator.cmod.inputcontrols.ExternalControllerBinding;
import com.winlator.cmod.inputcontrols.GamepadState;
import com.winlator.cmod.inputcontrols.InputControlsManager;
import com.winlator.cmod.inputcontrols.MultiBinding;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.winhandler.MouseEventFlags;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xserver.Pointer;
import com.winlator.cmod.xserver.XServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class InputControlsView extends View {
    public static final float DEFAULT_OVERLAY_OPACITY = 0.4f;
    private static final byte MOUSE_WHEEL_DELTA = 120;
    private boolean editMode = false;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final ColorFilter colorFilter = new PorterDuffColorFilter(0xffffffff, PorterDuff.Mode.SRC_IN);
    private final Point cursor = new Point();
    private boolean readyToDraw = false;
    private boolean moveCursor = false;
    private int snappingSize;
    private float offsetX;
    private float offsetY;
    private ControlElement selectedElement;
    private ControlsProfile profile;
    private float overlayOpacity = DEFAULT_OVERLAY_OPACITY;
    private TouchpadView touchpadView;
    private XServer xServer;
    private final Bitmap[] icons = new Bitmap[17];
    private final Map<String, Bitmap> customIcons = new HashMap<>();
    private Timer mouseMoveTimer;
    private final PointF mouseMoveOffset = new PointF();
    private boolean showTouchscreenControls = true;

    private Handler timeoutHandler; // Reference to the activity's timeout handler
    private Runnable hideControlsRunnable; // Runnable to hide the controls

    private SharedPreferences preferences;

    private ControlElement stickElement;

    private boolean focusOnStick = false; // A flag to determine if we are focusing on the stick

    // Profile switching handler
    private Handler profileSwitchHandler = new Handler();

    // Interface for element editing callbacks
    public interface OnElementEditListener {
        void onEditElement(ControlElement element);
        void onElementUpdated(ControlElement element);
    }
    
    private OnElementEditListener elementEditListener;

    private boolean overlayMode = false;

    public boolean isFocusedOnStick() {
        return focusOnStick;
    }

    public void setFocusOnStick(boolean focus) {
        this.focusOnStick = focus;
        invalidate(); // Redraw the view with the new focus setting
    }

    public void setOnElementEditListener(OnElementEditListener listener) {
        this.elementEditListener = listener;
    }

    @SuppressLint("ResourceType")
    public InputControlsView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus(); // Add this line to request focus
        setBackgroundColor(0x00000000);
        setPointerIcon(PointerIcon.load(getResources(), R.drawable.hidden_pointer_arrow));
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        preferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        loadCustomIcons();
    }

    @SuppressLint("ResourceType")
    public InputControlsView(Context context, Handler timeoutHandler, Runnable hideControlsRunnable) {
        super(context);
        this.timeoutHandler = timeoutHandler; // Store the reference to timeout handler
        this.hideControlsRunnable = hideControlsRunnable; // Store the reference to the hide controls runnable
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus(); // Add this line to request focus
        setBackgroundColor(0x00000000);
        setPointerIcon(PointerIcon.load(getResources(), R.drawable.hidden_pointer_arrow));
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        preferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        loadCustomIcons();
    }

    public InputControlsView(Context context, boolean focusOnStick) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus(); // Add this line to request focus
        setBackgroundColor(0x00000000);
        setPointerIcon(PointerIcon.load(getResources(), R.drawable.hidden_pointer_arrow));

        // If focusOnStick is true, adjust the layout params to match the stick element size
        if (focusOnStick) {
            setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        loadCustomIcons();
    }

    /**
     * Open edit dialog for selected element
     */
    public void editSelectedElement() {
        if (selectedElement != null && getContext() instanceof FragmentActivity) {
            if (elementEditListener != null) {
                elementEditListener.onEditElement(selectedElement);
            } else {
                // Fallback to default dialog
                showEditElementDialog(selectedElement);
            }
        }
    }

    /**
     * Show edit element dialog
     */
    private void showEditElementDialog(ControlElement element) {
        if (getContext() instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) getContext();
            EditControlElementDialog dialog = EditControlElementDialog.newInstance(element, this);
            dialog.setOnElementUpdatedListener(new EditControlElementDialog.OnElementUpdatedListener() {
                @Override
                public void onElementUpdated(ControlElement element) {
                    if (profile != null) {
                        profile.save();
                    }
                    invalidate();
                    
                    if (elementEditListener != null) {
                        elementEditListener.onElementUpdated(element);
                    }
                }
            });
            dialog.show(activity.getSupportFragmentManager(), "edit_element");
        }
    }

    /**
     * Load custom icons from app's internal storage with improved error handling
     */
    private void loadCustomIcons() {
        customIcons.clear();
        File iconsDir = new File(getContext().getFilesDir(), "custom_icons");
        if (iconsDir.exists() && iconsDir.isDirectory()) {
            File[] iconFiles = iconsDir.listFiles();
            if (iconFiles != null) {
                for (File iconFile : iconFiles) {
                    if (iconFile.isFile() && iconFile.getName().toLowerCase().endsWith(".png")) {
                        try (FileInputStream fis = new FileInputStream(iconFile)) {
                            Bitmap bitmap = BitmapFactory.decodeStream(fis);
                            if (bitmap != null) {
                                String elementId = iconFile.getName().replace(".png", "");
                                customIcons.put(elementId, bitmap);
                                Log.d("InputControlsView", "Loaded custom icon: " + elementId);
                            }
                        } catch (IOException e) {
                            Log.e("InputControlsView", "Error loading custom icon: " + iconFile.getName(), e);
                        }
                    }
                }
            }
        } else {
            // Create directory if it doesn't exist
            if (!iconsDir.exists()) {
                iconsDir.mkdirs();
            }
        }
        Log.d("InputControlsView", "Loaded " + customIcons.size() + " custom icons");
    }

    /**
     * Set custom icon for a control element with improved quality
     */
    public void setCustomIcon(String elementId, Bitmap icon) {
        if (icon != null) {
            // Use higher quality bitmap for better rendering
            Bitmap highQualityIcon = ensureHighQualityBitmap(icon);
            customIcons.put(elementId, highQualityIcon);
            // Save to internal storage
            saveCustomIconToStorage(elementId, highQualityIcon);
            invalidate();
            Log.d("InputControlsView", "Custom icon set for: " + elementId);
        }
    }

    /**
     * Ensure bitmap has good quality for display
     */
    private Bitmap ensureHighQualityBitmap(Bitmap original) {
        // If bitmap is too small, scale it up for better quality
        if (original.getWidth() < 64 || original.getHeight() < 64) {
            int targetSize = Math.max(64, Math.max(original.getWidth(), original.getHeight()));
            return Bitmap.createScaledBitmap(original, targetSize, targetSize, true);
        }
        return original;
    }

    /**
     * Remove custom icon for a control element
     */
    public void removeCustomIcon(String elementId) {
        Bitmap removed = customIcons.remove(elementId);
        if (removed != null) {
            // Remove from internal storage
            removeCustomIconFromStorage(elementId);
            invalidate();
            Log.d("InputControlsView", "Custom icon removed for: " + elementId);
        }
    }

    /**
     * Remove custom icon completely from library (file system and cache)
     */
    public void removeCustomIconFromLibrary(String elementId) {
        // Remove from memory cache
        Bitmap removed = customIcons.remove(elementId);
        
        // Remove from file system
        removeCustomIconFromStorage(elementId);
        
        if (removed != null) {
            invalidate();
            Log.d("InputControlsView", "Custom icon deleted from library: " + elementId);
        }
    }

    /**
     * Get custom icon for a control element
     */
    public Bitmap getCustomIcon(String elementId) {
        return customIcons.get(elementId);
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        if (!editMode) {
            deselectAllElements();
        }
    }

    public void setOverlayOpacity(float overlayOpacity) {
        this.overlayOpacity = overlayOpacity;
    }

    public void setOverlayMode(boolean overlayMode) {
        this.overlayMode = overlayMode;
        invalidate();
    }

    public int getSnappingSize() {
        return snappingSize;
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        int width, height;

        if (stickElement != null && isFocusedOnStick()) {
            // If focusing on the stick, set width and height to the stick's bounding box size
            Rect boundingBox = stickElement.getBoundingBox();
            width = boundingBox.width();
            height = boundingBox.height();
        } else {
            // Default behavior for full screen
            width = getWidth();
            height = getHeight();
        }

        if (width == 0 || height == 0) {
            readyToDraw = false;
            return;
        }

        snappingSize = width / 100;
        readyToDraw = true;

        if (editMode) {
            drawGrid(canvas);
            drawCursor(canvas);
        }

        if (stickElement != null) {
            // Draw only the stick element if focus mode is active
            stickElement.draw(canvas);
        }

        if (profile != null && showTouchscreenControls && !isFocusedOnStick()) {
            if (!profile.isElementsLoaded()) profile.loadElements(this);
            
            // Сначала отрисовываем все TOUCH_AREA элементы (только в режиме редактирования)
            if (editMode) {
                for (ControlElement element : profile.getElements()) {
                    // Set edit mode for all elements to enable activation zone drawing
                    element.setEditMode(editMode);
                    
                    // Отрисовываем только TOUCH_AREA элементы в режиме редактирования
                    if (element.getType() == ControlElement.Type.TOUCH_AREA) {
                        // Ограничиваем размеры при отрисовке, но не изменяем значения элемента
                        float maxTouchAreaWidth = width * 1.2f;
                        float maxTouchAreaHeight = height * 1.2f;
                        
                        float drawWidth = Math.min(element.getActivationZoneWidth(), maxTouchAreaWidth);
                        float drawHeight = Math.min(element.getActivationZoneHeight(), maxTouchAreaHeight);
                        
                        // Временно изменяем размеры для отрисовки, если нужно
                        // Но не сохраняем изменения в элементе
                        float originalWidth = element.getActivationZoneWidth();
                        float originalHeight = element.getActivationZoneHeight();
                        
                        // Если размеры для отрисовки отличаются от оригинальных, временно меняем
                        if (drawWidth != originalWidth || drawHeight != originalHeight) {
                            element.setActivationZoneWidth(drawWidth);
                            element.setActivationZoneHeight(drawHeight);
                            element.draw(canvas);
                            // Восстанавливаем оригинальные размеры
                            element.setActivationZoneWidth(originalWidth);
                            element.setActivationZoneHeight(originalHeight);
                        } else {
                            element.draw(canvas);
                        }
                    }
                }
            }
            
            // Затем отрисовываем все остальные элементы поверх остальных (кроме TOUCH_AREA в режиме управления)
            for (ControlElement element : profile.getElements()) {
                // Отрисовываем все элементы, кроме TOUCH_AREA (они уже отрисованы выше в режиме редактирования)
                if (element.getType() != ControlElement.Type.TOUCH_AREA) {
                    element.setEditMode(editMode);
                    element.draw(canvas);
                } else if (editMode) {
                    // Если это TOUCH_AREA и мы в режиме редактирования, 
                    // элемент уже отрисован выше, поэтому пропускаем
                    continue;
                }
            }
        }

        super.onDraw(canvas);
    }

    public void resetStickPosition() {
        if (stickElement != null) {
            Rect boundingBox = stickElement.getBoundingBox();
            float centerX = boundingBox.centerX();
            float centerY = boundingBox.centerY();

            stickElement.setCurrentPosition(centerX, centerY); // Reset to the center of the bounding box
            invalidate(); // Redraw the stick in the centered position
        }
    }

    public void initializeStickElement(float x, float y, float scale) {
        stickElement = new ControlElement(this);
        stickElement.setType(ControlElement.Type.STICK); // Set type to STICK
        stickElement.setX((int) x);
        stickElement.setY((int) y);
        stickElement.setScale(scale);
        invalidate(); // Force the view to redraw with the stick
    }

    public void updateStickPosition(float x, float y) {
        if (stickElement != null) {
            stickElement.getCurrentPosition().x = x;  // Update the thumbstick's position
            stickElement.getCurrentPosition().y = y;  // Update the thumbstick's position
            invalidate(); // Redraw the view
        }
    }

    public ControlElement getStickElement() {
        return stickElement;
    }

    private void drawGrid(Canvas canvas) {
        if (overlayMode) {
            // В Move Mode — НЕ рисуем чёрный фон и сетку
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(snappingSize * 0.0625f);
        paint.setColor(0xff000000);
        canvas.drawColor(Color.BLACK);

        paint.setAntiAlias(false);
        paint.setColor(0xff303030);

        int width = getMaxWidth();
        int height = getMaxHeight();

        for (int i = 0; i < width; i += snappingSize) {
            canvas.drawLine(i, 0, i, height, paint);
            canvas.drawLine(0, i, width, i, paint);
        }

        float cx = Mathf.roundTo(width * 0.5f, snappingSize);
        float cy = Mathf.roundTo(height * 0.5f, snappingSize);
        paint.setColor(0xff424242);

        for (int i = 0; i < width; i += snappingSize * 2) {
            canvas.drawLine(cx, i, cx, i + snappingSize, paint);
            canvas.drawLine(i, cy, i + snappingSize, cy, paint);
        }

        paint.setAntiAlias(true);
    }

    private void drawCursor(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(snappingSize * 0.0625f);
        paint.setColor(0xffc62828);

        paint.setAntiAlias(false);
        canvas.drawLine(0, cursor.y, getMaxWidth(), cursor.y, paint);
        canvas.drawLine(cursor.x, 0, cursor.x, getMaxHeight(), paint);

        paint.setAntiAlias(true);
    }

    public synchronized boolean addElement() {
        if (editMode && profile != null) {
            ControlElement element = new ControlElement(this);
            element.setX(cursor.x);
            element.setY(cursor.y);
            profile.addElement(element);
            profile.save();
            selectElement(element);
            return true;
        }
        else return false;
    }
    
    /**
     * Создать элемент TOUCH
     */
    public synchronized boolean addTouchElement() {
        if (editMode && profile != null) {
            ControlElement element = new ControlElement(this);
            element.setType(ControlElement.Type.TOUCH);
            element.setX(cursor.x);
            element.setY(cursor.y);
            
            // Установить специальные значения по умолчанию для Touch элемента
            element.setButtonOpacity(ControlElement.DEFAULT_TOUCH_BUTTON_OPACITY);
            element.setIconSizeMultiplier(ControlElement.DEFAULT_TOUCH_ICON_SIZE_MULTIPLIER);
            
            profile.addElement(element);
            profile.save();
            selectElement(element);
            return true;
        }
        else return false;
    }

    /**
     * Создать элемент TOUCH_AREA
     */
    public synchronized boolean addTouchAreaElement() {
        if (editMode && profile != null) {
            ControlElement element = new ControlElement(this);
            element.setType(ControlElement.Type.TOUCH_AREA);
            element.setX(cursor.x);
            element.setY(cursor.y);
            
            // Установить специальные значения по умолчанию для Touch Area элемента
            element.setButtonOpacity(ControlElement.DEFAULT_TOUCH_BUTTON_OPACITY);
            element.setIconSizeMultiplier(ControlElement.DEFAULT_TOUCH_ICON_SIZE_MULTIPLIER);
            // Установить большие размеры области по умолчанию
            element.setActivationZoneWidth(ControlElement.DEFAULT_ACTIVATION_ZONE_WIDTH * 2);
            element.setActivationZoneHeight(ControlElement.DEFAULT_ACTIVATION_ZONE_HEIGHT * 2);
            
            profile.addElement(element);
            profile.save();
            selectElement(element);
            return true;
        }
        else return false;
    }

    public synchronized boolean removeElement() {
        if (editMode && selectedElement != null && profile != null) {
            // Remove any custom icon associated with this element
            if (selectedElement.hasCustomIcon()) {
                removeCustomIcon(selectedElement.getCustomIconId());
            }
            
            profile.removeElement(selectedElement);
            selectedElement = null;
            profile.save();
            invalidate();
            return true;
        }
        else return false;
    }

    public ControlElement getSelectedElement() {
        return selectedElement;
    }

    private synchronized void deselectAllElements() {
        selectedElement = null;
        if (profile != null) {
            for (ControlElement element : profile.getElements()) element.setSelected(false);
        }
    }

    private void selectElement(ControlElement element) {
        deselectAllElements();
        if (element != null) {
            selectedElement = element;
            selectedElement.setSelected(true);
        }
        invalidate();
    }

    public synchronized ControlsProfile getProfile() {
        return profile;
    }

    public synchronized void setProfile(ControlsProfile profile) {
        if (profile != null) {
            this.profile = profile;
            deselectAllElements();
            // Reload custom icons when profile changes
            loadCustomIcons();
        }
        else this.profile = null;
    }

    public boolean isShowTouchscreenControls() {
        return showTouchscreenControls;
    }

    public void setShowTouchscreenControls(boolean showTouchscreenControls) {
        this.showTouchscreenControls = showTouchscreenControls;
    }

    public int getPrimaryColor() {
        return Color.argb((int)(overlayOpacity * 255), 255, 255, 255);
    }

    public int getSecondaryColor() {
        return Color.argb((int)(overlayOpacity * 255), 2, 119, 189);
    }

    private synchronized ControlElement intersectElement(float x, float y) {
        if (profile != null) {
            for (ControlElement element : profile.getElements()) {
                if (element.containsPoint(x, y)) return element;
            }
        }
        return null;
    }

    // New method to find DYNAMIC_STICK and TOUCH_AREA elements in activation zone
    private synchronized ControlElement findElementInActivationZone(float x, float y) {
        if (profile != null) {
            for (ControlElement element : profile.getElements()) {
                if ((element.getType() == ControlElement.Type.DYNAMIC_STICK || 
                     element.getType() == ControlElement.Type.TOUCH_AREA) && 
                    element.isInActivationZone(x, y) && 
                    element.getCurrentPointerId() == -1) {
                    return element;
                }
            }
        }
        return null;
    }

    public Paint getPaint() {
        return paint;
    }

    public Path getPath() {
        return path;
    }

    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    public TouchpadView getTouchpadView() {
        return touchpadView;
    }

    public void setTouchpadView(TouchpadView touchpadView) {
        this.touchpadView = touchpadView;
    }

    public XServer getXServer() {
        return xServer;
    }

    public void setXServer(XServer xServer) {
        this.xServer = xServer;
        createMouseMoveTimer();
    }

    public int getMaxWidth() {
        return (int)Mathf.roundTo(getWidth(), snappingSize);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mouseMoveTimer != null)
            mouseMoveTimer.cancel();
        if (profileSwitchHandler != null) {
            profileSwitchHandler.removeCallbacksAndMessages(null);
        }
        super.onDetachedFromWindow();
    }

    public int getMaxHeight() {
        return (int)Mathf.roundTo(getHeight(), snappingSize);
    }

    private void createMouseMoveTimer() {
        WinHandler winHandler = xServer.getWinHandler();
        if (mouseMoveTimer == null && profile != null) {
            final float cursorSpeed = profile.getCursorSpeed();
            mouseMoveTimer = new Timer();
            mouseMoveTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mouseMoveOffset.x != 0 || mouseMoveOffset.y != 0) {// Only move if there's an offsete if there's an offset
                        if (xServer.isRelativeMouseMovement())
                            winHandler.mouseEvent(MouseEventFlags.MOVE, (int) (mouseMoveOffset.x * cursorSpeed * 10), (int) (mouseMoveOffset.y * cursorSpeed * 10), 0);
                        else
                            xServer.injectPointerMoveDelta(
                                (int) (mouseMoveOffset.x * cursorSpeed * 10),
                                (int) (mouseMoveOffset.y * cursorSpeed * 10)
                            );
                    }
                }
            }, 0, 1000 / 60); // 60 FPS
        }
    }

    private void processJoystickInput(ExternalController controller) {
        final int[] axes = {
                MotionEvent.AXIS_X, MotionEvent.AXIS_Y,
                MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
                MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y
        };
        final float[] values = {
                controller.state.thumbLX, controller.state.thumbLY,
                controller.state.thumbRX, controller.state.thumbRY,
                controller.state.getDPadX(), controller.state.getDPadY()
        };

        for (int i = 0; i < axes.length; i++) {
            float value = values[i];
            if (Math.abs(value) > ControlElement.STICK_DEAD_ZONE) {
                byte sign = Mathf.sign(value);
                int keyCode = ExternalControllerBinding.getKeyCodeForAxis(axes[i], sign);
                ExternalControllerBinding controllerBinding = controller.getControllerBinding(keyCode);
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.getBinding(), true, value);
                }
            } else {
                // Handle releasing the bindings when the axis returns to deadzone
                for (byte sign = -1; sign <= 1; sign += 2) {
                    int keyCode = ExternalControllerBinding.getKeyCodeForAxis(axes[i], sign);
                    ExternalControllerBinding controllerBinding = controller.getControllerBinding(keyCode);
                    if (controllerBinding != null) {
                        handleInputEvent(controllerBinding.getBinding(), false, value);
                    }
                }
            }
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        Log.d("InputControlsView", "dispatchGenericMotionEvent called. Source: " + event.getSource());
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        Log.d("InputControlsView", "Motion event received. Source: " + event.getSource());
        Log.d("InputControlsView", "Device ID: " + event.getDeviceId());
        Log.d("InputControlsView", "Profile is " + (profile != null ? "set" : "null"));

        if (!editMode && profile != null) {
            // Retrieve the associated controller for this event
            ExternalController controller = profile.getController(event.getDeviceId());

            if (controller != null && controller.updateStateFromMotionEvent(event)) {
                // Process L2 and R2 button bindings
                ExternalControllerBinding controllerBinding;

                // L2 button
                controllerBinding = controller.getControllerBinding(KeyEvent.KEYCODE_BUTTON_L2);
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.getBinding(), controller.state.isPressed(ExternalController.IDX_BUTTON_L2));
                }

                // R2 button
                controllerBinding = controller.getControllerBinding(KeyEvent.KEYCODE_BUTTON_R2);
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.getBinding(), controller.state.isPressed(ExternalController.IDX_BUTTON_R2));
                }

                Log.d("InputEvent", "Event source: " + event.getSource());
                Log.d("InputEvent", "Device ID: " + event.getDeviceId());
                Log.d("InputEvent", "Action: " + event.getAction());

                // Process joystick inputs for mouse movement and other bindings
                processJoystickInput(controller);

                // Return true to indicate the motion event was handled
                return true;
            }
        }

        // Pass the event to the super method if not handled
        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean hapticsEnabled = preferences.getBoolean("touchscreen_haptics_enabled", true);

        // Reset the timeout when touch events occur within InputControlsView
        resetTouchscreenTimeout();

        if (editMode && readyToDraw) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    float x = event.getX();
                    float y = event.getY();

                    ControlElement element = intersectElement(x, y);
                    moveCursor = true;
                    if (element != null) {
                        offsetX = x - element.getX();
                        offsetY = y - element.getY();
                        moveCursor = false;
                    }

                    selectElement(element);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (selectedElement != null) {
                        selectedElement.setX((int)Mathf.roundTo(event.getX() - offsetX, snappingSize));
                        selectedElement.setY((int)Mathf.roundTo(event.getY() - offsetY, snappingSize));
                        invalidate();
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (selectedElement != null && profile != null) profile.save();
                    if (moveCursor) cursor.set((int)Mathf.roundTo(event.getX(), snappingSize), (int)Mathf.roundTo(event.getY(), snappingSize));
                    invalidate();
                    break;
                }
            }
        }
        
        if (!editMode && profile != null) {
            int actionIndex = event.getActionIndex();
            int pointerId = event.getPointerId(actionIndex);
            int actionMasked = event.getActionMasked();
            boolean handled = false;

            switch (actionMasked) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    float x = event.getX(actionIndex);
                    float y = event.getY(actionIndex);

                    touchpadView.setPointerButtonLeftEnabled(true);
                    
                    // First, check for elements under the touch point, excluding TOUCH_AREA and DYNAMIC_STICK
                    // This ensures that elements drawn "on top" of TOUCH_AREA can be clicked
                    ControlElement elementAtPoint = null;
                    // Iterate from the end of the list (elements drawn last, thus "on top")
                    for (int i = profile.getElements().size() - 1; i >= 0; i--) {
                        ControlElement element = profile.getElements().get(i);
                        if (element.getType() != ControlElement.Type.TOUCH_AREA && 
                            element.getType() != ControlElement.Type.DYNAMIC_STICK && 
                            element.containsPoint(x, y)) {
                            elementAtPoint = element;
                            break;
                        }
                    }
                    
                    // If there is an element under the point (excluding TOUCH_AREA and DYNAMIC_STICK), handle it first
                    if (elementAtPoint != null) {
                        if (elementAtPoint.handleTouchDown(pointerId, x, y)) {
                            handled = true;

                            // Check for MultiBinding first
                            if (elementAtPoint.isUseMultiBinding() && !elementAtPoint.getMultiBindingAt(0).isEmpty()) {
                                // MultiBinding handled in handleTouchDown
                            }
                            // Check for profile switching
                            else if (elementAtPoint.isEnableProfileSwitching() && elementAtPoint.getTargetProfileId() != 0) {
                                scheduleProfileSwitch(elementAtPoint);
                            }

                            // Trigger haptic feedback for input controls
                            if (hapticsEnabled) {
                                Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                                if (vibrator != null && vibrator.hasVibrator()) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                                    } else {
                                        vibrator.vibrate(50); // Legacy method for older Android versions
                                    }
                                }
                            }
                            
                            if (elementAtPoint.getBindingAt(0) == Binding.MOUSE_LEFT_BUTTON) {
                                touchpadView.setPointerButtonLeftEnabled(false);
                            }
                        }
                    }
                    
                    // If the element under the point did not handle the touch, check for DYNAMIC_STICK and TOUCH_AREA
                    if (!handled) {
                        ControlElement elementInZone = findElementInActivationZone(x, y);
                        if (elementInZone != null) {
                            if (elementInZone.handleTouchDown(pointerId, x, y)) {
                                handled = true;

                                // Trigger haptic feedback for input controls
                                if (hapticsEnabled) {
                                    Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                                    if (vibrator != null && vibrator.hasVibrator()) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                                        } else {
                                            vibrator.vibrate(50); // Legacy method for older Android versions
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!handled) touchpadView.onTouchEvent(event);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    for (byte i = 0, count = (byte)event.getPointerCount(); i < count; i++) {
                        float x = event.getX(i);
                        float y = event.getY(i);

                        handled = false;
                        for (ControlElement element : profile.getElements()) {
                            if (element.handleTouchMove(i, x, y)) handled = true;
                        }
                        if (!handled) touchpadView.onTouchEvent(event);
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    for (ControlElement element : profile.getElements()) if (element.handleTouchUp(pointerId)) handled = true;
                    if (!handled) touchpadView.onTouchEvent(event);
                    break;
            }
        }
        return true;
    }

    /**
     * Schedule profile switch with delay
     */
    private void scheduleProfileSwitch(ControlElement element) {
        float delay = element.getSwitchDelay();
        
        // Remove any existing callbacks for this element
        profileSwitchHandler.removeCallbacksAndMessages(element);
        
        if (delay == 0.0f) {
            // Instant switch
            switchProfileImmediately(element.getTargetProfileId());
        } else {
            // Delayed switch
            profileSwitchHandler.postDelayed(() -> {
                switchProfileImmediately(element.getTargetProfileId());
            }, (long)(delay * 1000));
        }
    }

    /**
     * Switch to target profile immediately
     */
    private void switchProfileImmediately(int targetProfileId) {
        if (targetProfileId == 0) return;
        
        Log.d("InputControlsView", "Switching to profile ID: " + targetProfileId);
        
        // Load the target profile
        ControlsProfile newProfile = InputControlsManager.loadProfile(getContext(), 
            ControlsProfile.getProfileFile(getContext(), targetProfileId));
        
        if (newProfile != null) {
            setProfile(newProfile);
            invalidate();
            
            // Show visual feedback (optional)
            if (getContext() != null) {
                // You could add a toast or other visual feedback here
                Log.d("InputControlsView", "Successfully switched to profile: " + newProfile.getName());
            }
        } else {
            Log.e("InputControlsView", "Failed to load target profile with ID: " + targetProfileId);
        }
    }

    private void resetTouchscreenTimeout() {
        Log.d("InputControlsView", "Touch detected, resetting timeout.");
        if (timeoutHandler != null && hideControlsRunnable != null) {
            // Cancel any pending hide requests
            timeoutHandler.removeCallbacks(hideControlsRunnable);
            // Post a new request to hide the controls after 5 seconds
            timeoutHandler.postDelayed(hideControlsRunnable, 5000); // Adjust timeout as necessary
        }
    }

    public boolean onKeyEvent(KeyEvent event) {
        if (profile != null && event.getRepeatCount() == 0) {
            ExternalController controller = profile.getController(event.getDeviceId());
            if (controller != null) {
                ExternalControllerBinding controllerBinding = controller.getControllerBinding(event.getKeyCode());
                if (controllerBinding != null) {
                    int action = event.getAction();

                    if (action == KeyEvent.ACTION_DOWN) {
                        handleInputEvent(controllerBinding.getBinding(), true);
                    }
                    else if (action == KeyEvent.ACTION_UP) {
                        handleInputEvent(controllerBinding.getBinding(), false);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public void handleInputEvent(Binding binding, boolean isActionDown) {
        handleInputEvent(binding, isActionDown, 0);
    }

    public void handleInputEvent(Binding binding, boolean isActionDown, float offset) {
        WinHandler winHandler = xServer != null ? xServer.getWinHandler() : null;
        if (binding.isGamepad()) {
            GamepadState state = profile.getGamepadState();

            int buttonIdx = binding.ordinal() - Binding.GAMEPAD_BUTTON_A.ordinal();
            if (buttonIdx <= ExternalController.IDX_BUTTON_R2) {
                if (buttonIdx == ExternalController.IDX_BUTTON_L2)
                    state.triggerL = isActionDown ? 1.0f : 0f;
                else if (buttonIdx == ExternalController.IDX_BUTTON_R2)
                    state.triggerR = isActionDown ? 1.0f : 0f;
                else
                    state.setPressed(buttonIdx, isActionDown);
            }
            else if (binding == Binding.GAMEPAD_LEFT_THUMB_UP || binding == Binding.GAMEPAD_LEFT_THUMB_DOWN) {
                state.thumbLY = isActionDown ? offset : 0;
            }
            else if (binding == Binding.GAMEPAD_LEFT_THUMB_LEFT || binding == Binding.GAMEPAD_LEFT_THUMB_RIGHT) {
                state.thumbLX = isActionDown ? offset : 0;
            }
            else if (binding == Binding.GAMEPAD_RIGHT_THUMB_UP || binding == Binding.GAMEPAD_RIGHT_THUMB_DOWN) {
                state.thumbRY = isActionDown ? offset : 0;
            }
            else if (binding == Binding.GAMEPAD_RIGHT_THUMB_LEFT || binding == Binding.GAMEPAD_RIGHT_THUMB_RIGHT) {
                state.thumbRX = isActionDown ? offset : 0;
            }
            else if (binding == Binding.GAMEPAD_DPAD_UP || binding == Binding.GAMEPAD_DPAD_RIGHT ||
                     binding == Binding.GAMEPAD_DPAD_DOWN || binding == Binding.GAMEPAD_DPAD_LEFT) {
                state.dpad[binding.ordinal() - Binding.GAMEPAD_DPAD_UP.ordinal()] = isActionDown;
            }

            if (winHandler != null) {
                ExternalController controller = winHandler.getCurrentController();
                if (controller != null) controller.state.copy(state);
                winHandler.sendGamepadState();
            }
        }
        else {
            if (binding == Binding.MOUSE_MOVE_LEFT || binding == Binding.MOUSE_MOVE_RIGHT) {
                mouseMoveOffset.x = isActionDown ? (offset != 0 ? offset : (binding == Binding.MOUSE_MOVE_LEFT ? -1 : 1)) : 0;
                if (isActionDown) createMouseMoveTimer();
            }
            else if (binding == Binding.MOUSE_MOVE_DOWN || binding == Binding.MOUSE_MOVE_UP) {
                mouseMoveOffset.y = isActionDown ? (offset != 0 ? offset : (binding == Binding.MOUSE_MOVE_UP ? -1 : 1)) : 0;
                if (isActionDown) createMouseMoveTimer();
            }
            else {
                Pointer.Button pointerButton = binding.getPointerButton();
                if (isActionDown) {
                    if (pointerButton != null) {
                        if (xServer.isRelativeMouseMovement()) {
                            int wheelDelta = 0;
                            if (binding.isMouseScrollContinuous()) {
                                // Continuous scroll - use offset as intensity
                                wheelDelta = (int)(offset * MOUSE_WHEEL_DELTA);
                                if (binding == Binding.MOUSE_SCROLL_DOWN_CONTINUOUS) {
                                    wheelDelta = -wheelDelta;
                                }
                            } else {
                                // Discrete scroll
                                wheelDelta = pointerButton == Pointer.Button.BUTTON_SCROLL_UP ? MOUSE_WHEEL_DELTA : 
                                           (pointerButton == Pointer.Button.BUTTON_SCROLL_DOWN ? -MOUSE_WHEEL_DELTA : 0);
                            }
                            winHandler.mouseEvent(MouseEventFlags.getFlagFor(pointerButton, true), 0, 0, wheelDelta);
                        } else {
                            xServer.injectPointerButtonPress(pointerButton);
                        }
                    }
                    else xServer.injectKeyPress(binding.keycode);
                }
                else {
                    if (pointerButton != null) {
                        if (xServer.isRelativeMouseMovement()) {
                            winHandler.mouseEvent(MouseEventFlags.getFlagFor(pointerButton, false), 0, 0, 0);
                        } else {
                            xServer.injectPointerButtonRelease(pointerButton);
                        }
                    }
                    else xServer.injectKeyRelease(binding.keycode);
                }
            }
        }
    }

    /**
     * Handle MultiBinding input events
     */
    public void handleMultiBinding(MultiBinding multiBinding, boolean isActionDown) {
        if (multiBinding == null || multiBinding.isEmpty()) return;
        
        if (multiBinding.isSimultaneous()) {
            // Одновременное нажатие всех клавиш
            for (Binding binding : multiBinding.getBindings()) {
                handleInputEvent(binding, isActionDown);
            }
        } else {
            // Последовательное нажатие
            if (isActionDown) {
                // Для последовательных - запускаем в отдельном потоке чтобы не блокировать UI
                new Thread(() -> {
                    for (Binding binding : multiBinding.getBindings()) {
                        // Нажимаем клавишу
                        post(() -> handleInputEvent(binding, true));
                        
                        // Небольстая задержка между нажатиями
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        
                        // Отпускаем клавишу
                        post(() -> handleInputEvent(binding, false));
                        
                        // Задержка между действиями
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }).start();
            }
            // Для отпускания последовательных комбинаций ничего не делаем,
            // так как каждая клавиша уже была отпущена в процессе нажатия
        }
    }

    public Bitmap getIcon(byte id) {
        // Check array bounds to prevent ArrayIndexOutOfBoundsException
        if (id < 0 || id >= icons.length) {
            return null;
        }
        if (icons[id] == null) {
            Context context = getContext();
            try (InputStream is = context.getAssets().open("inputcontrols/icons/"+id+".png")) {
                icons[id] = BitmapFactory.decodeStream(is);
            }
            catch (IOException e) {}
        }
        return icons[id];
    }

    // Add getter for currentPointerId to use in findElementInActivationZone
    public int getCurrentPointerId(ControlElement element) {
        // This method would need to be implemented in ControlElement class
        // For now, we'll use reflection or add the method to ControlElement
        return element.getCurrentPointerId();
    }

    /**
     * Save custom icon to internal storage with improved quality
     */
    private void saveCustomIconToStorage(String elementId, Bitmap icon) {
        File iconsDir = new File(getContext().getFilesDir(), "custom_icons");
        if (!iconsDir.exists()) {
            iconsDir.mkdirs();
        }
        
        File iconFile = new File(iconsDir, elementId + ".png");
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(iconFile)) {
            // Use PNG format with high quality
            icon.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Log.d("InputControlsView", "Custom icon saved: " + iconFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("InputControlsView", "Error saving custom icon: " + elementId, e);
        }
    }

    /**
     * Remove custom icon from internal storage
     */
    private void removeCustomIconFromStorage(String elementId) {
        File iconFile = new File(getContext().getFilesDir(), "custom_icons/" + elementId + ".png");
        if (iconFile.exists()) {
            boolean deleted = iconFile.delete();
            if (deleted) {
                Log.d("InputControlsView", "Custom icon file deleted: " + elementId);
            } else {
                Log.e("InputControlsView", "Failed to delete custom icon file: " + elementId);
            }
        }
    }
}