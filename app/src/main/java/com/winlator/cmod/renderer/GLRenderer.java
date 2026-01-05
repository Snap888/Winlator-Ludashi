package com.winlator.cmod.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.winlator.cmod.R;
import com.winlator.cmod.XrActivity;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.math.XForm;
import com.winlator.cmod.renderer.effects.VREffect;
import com.winlator.cmod.renderer.material.CursorMaterial;
import com.winlator.cmod.renderer.material.ShaderMaterial;
import com.winlator.cmod.renderer.material.WindowMaterial;
import com.winlator.cmod.widget.XServerView;
import com.winlator.cmod.xserver.Bitmask;
import com.winlator.cmod.xserver.Cursor;
import com.winlator.cmod.xserver.Drawable;
import com.winlator.cmod.xserver.Pointer;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.WindowAttributes;
import com.winlator.cmod.xserver.WindowManager;
import com.winlator.cmod.xserver.XLock;
import com.winlator.cmod.xserver.XServer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer, WindowManager.OnWindowModificationListener, Pointer.OnPointerMotionListener {
    public final XServerView xServerView;
    private final XServer xServer;
    public final VertexAttribute quadVertices = new VertexAttribute("position", 2);
    private final float[] tmpXForm1 = XForm.getInstance();
    private final float[] tmpXForm2 = XForm.getInstance();
    private final CursorMaterial cursorMaterial = new CursorMaterial();
    private final WindowMaterial windowMaterial = new WindowMaterial();
    public final ViewTransformation viewTransformation = new ViewTransformation();
    private final Drawable rootCursorDrawable;
    private final ArrayList<RenderableWindow> renderableWindows = new ArrayList<>();
    private boolean fullscreen = false;
    private boolean toggleFullscreen = false;
    public boolean viewportNeedsUpdate = true;
    private boolean cursorVisible = true;
    private boolean screenOffsetYRelativeToCursor = false; // Поле для отслеживания смещения экрана относительно курсора
    private String[] unviewableWMClasses = null;
    private float magnifierZoom = 1.0f;
    private boolean magnifierEnabled = true;
    public int surfaceWidth;
    public int surfaceHeight;
    private final EffectComposer effectComposer;
    
    // Поле для текстуры фона
    private int backgroundTextureId = 0;

    // --- ПОЛЯ ДЛЯ ИНТЕРПОЛЯЦИИ ---
    private boolean frameInterpolationEnabled = false;
    private boolean adaptiveInterpolation = false;
    private boolean motionPredictionEnabled = false;
    private boolean temporalAAEnabled = false;
    private int interpolationQuality = 1; // 0=off, 1=low, 2=medium, 3=high
    private int interpolationAlgorithm = 0; // 0=linear, 1=cubic, 2=spline, 3=hermite
    private static final int INTERPOLATION_HERMITE = 3;
    // ИСПРАВЛЕНО: Увеличено максимальное количество промежуточных кадров
    private int maxIntermediateFrames = 6; // Увеличено в 3 раза (было 2)
    private int numIntermediateFrames = 1;
    private long lastFrameTime = 0;
    private long currentFrameTime = 0;
    private float[] currentTransform = XForm.getInstance();
    private float[] previousTransform = XForm.getInstance();
    private float[] nextTransform = XForm.getInstance();
    private boolean transformChanged = false;
    private float motionIntensity = 0.0f;
    private float predictionFactor = 0.1f;
    private float[] transformHistory = new float[6 * 5]; // 5 последних трансформаций (XForm - 6 элементов)
    private int historyIndex = 0;
    private float[][] interpolationCache = new float[10][];
    private ExecutorService interpolationExecutor = Executors.newSingleThreadExecutor();
    private Future<?> interpolationFuture;
    private boolean dynamicFPSAdjustment = false;
    private int dynamicTargetFPS = 60;
    private float performanceScore = 1.0f;
    private long[] frameTimes = new long[60];
    private int frameTimeIndex = 0;
    private boolean shouldSkipFrame = false;
    private long lastSignificantChange = 0;
    private float sceneChangeThreshold = 0.01f;
    private float[] currentWindowPositions = new float[2]; // x, y
    private float[] previousWindowPositions = new float[2]; // x, y
    private float[] nextWindowPositions = new float[2]; // x, y
    private boolean windowPositionsChanged = false;
    private float[] cursorHistory = new float[10]; // последние 5 позиций курсора (x, y)
    private int cursorHistoryIndex = 0;
    private float[] currentCursor = new float[2];
    private float[] previousCursor = new float[2];
    private float[] nextCursor = new float[2];
    private boolean cursorChanged = false;
    
    // Поля для улучшенной интерполяции
    private float[] velocityHistory = new float[6 * 5]; // История скоростей трансформаций (5 векторов по 6)
    private float[] cursorVelocityHistory = new float[10]; // История скоростей курсора (5 векторов по 2)
    private float[] currentVelocity = new float[6]; // Текущая скорость трансформации
    private float[] previousVelocity = new float[6]; // Предыдущая скорость трансформации
    private float[] currentCursorVelocity = new float[2]; // Текущая скорость курсора
    private float[] previousCursorVelocity = new float[2]; // Предыдущая скорость курсора
    private long lastVelocityTime = 0; // Время последнего вычисления скорости
    private float smoothnessFactor = 0.8f; // Фактор сглаживания
    private float motionSensitivity = 0.3f; // Чувствительность к движению
    private boolean useVelocityBasedInterpolation = true; // Использовать интерполяцию на основе скорости
    
    // --- ОПТИМИЗАЦИИ ВЫВОДА ИЗОБРАЖЕНИЯ ---
    private boolean useFastRendering = true; // Использовать быструю отрисовку
    private boolean useFrameSkipping = true; // Использовать пропуск кадров при высокой частоте
    private boolean useTextureCaching = true; // Использовать кэширование текстур
    private boolean useBatchRendering = true; // Использовать пакетную отрисовку
    private boolean usePrecomputedTransforms = true; // Использовать предвычисленные трансформации
    private int renderOptimizationLevel = 2; // Уровень оптимизации (0=off, 1=low, 2=medium, 3=high)
    private boolean useAsyncTextureUpdate = true; // Использовать асинхронное обновление текстур
    private boolean useRenderCulling = true; // Использовать отсечение невидимых элементов
    private boolean useEarlyZTest = true; // Использовать раннее Z-тестирование
    private long lastRenderTime = 0; // Время последнего рендеринга
    private float renderFrequency = 60.0f; // Частота рендеринга
    private float targetRenderInterval = 16.67f; // Целевой интервал рендеринга в мс
    private boolean frameRateLimited = true; // Ограничивать частоту кадров
    private long frameRateLimit = 60; // Целевая частота кадров
    private long frameRateInterval = 16666666; // Интервал между кадрами в нс (1000000000 / 60)
    private long lastFrameRenderTime = 0; // Время последнего рендеринга кадра
    
    // --- ПОЛЯ ДЛЯ ИНТЕРПОЛЯЦИИ СОДЕРЖИМОГО ---
    private boolean contentChanged = false; // Изменилось ли содержимое окон
    private long lastContentChangeTime = 0; // Время последнего изменения содержимого
    private boolean interpolationActive = false; // Активна ли интерполяция
    private int activeIntermediateFramesCount = 0; // Количество активных промежуточных кадров
    private float currentInterpolationFactor = 0.0f; // Текущий фактор интерполяции
    private boolean sceneStableFlag = true; // Стабильна ли сцена
    private float[] lastSceneStateArray = new float[6]; // Последнее состояние сцены для сравнения
    
    // --- ПОЛЯ ДЛЯ МОНИТОРИНГА ПРОИЗВОДИТЕЛЬНОСТИ ---
    private long[] renderTimes = new long[60];
    private int renderTimeIndex = 0;
    private float currentFPS = 0;
    private float avgFrameTime = 0;
    private float minFrameTime = Float.MAX_VALUE;
    private float maxFrameTime = 0;
    private int frameCount = 0;
    private long lastFPSCheck = 0;
    private boolean performanceMonitoringEnabled = true;

    // --- НОВЫЕ ПОЛЯ ДЛЯ ИНТЕРПОЛЯЦИИ ---
    private long lastGameFrameTime = 0; // Время последнего кадра игры
    private long interpolationStartTime = 0; // Время начала интерполяции между кадрами игры
    private float displayRefreshRate = 60.0f; // Целевая частота (частота обновления экрана)
    private long targetInterpolationInterval = 16_666_666; // 1000000000 / 60 в наносекундах
    private boolean sceneStable = true; // Стабильна ли сцена
    private float[] lastSceneState = new float[6]; // Последнее состояние сцены для сравнения

    public GLRenderer(XServerView xServerView, XServer xServer) {
        this.xServerView = xServerView;
        this.xServer = xServer;
        this.effectComposer = new EffectComposer(this);
        rootCursorDrawable = createRootCursorDrawable();

        quadVertices.put(new float[]{
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
        });

        xServer.windowManager.addOnWindowModificationListener(this);
        xServer.pointer.addOnPointerMotionListener(this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GPUImage.checkIsSupported();

        GLES20.glFrontFace(GLES20.GL_CCW);
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        
        // Инициализируем текстуру фона
        initBackgroundTexture();
        
        // Устанавливаем оптимизации OpenGL
        setupOpenGLOptimizations();
    }

    private void setupOpenGLOptimizations() {
        // Устанавливаем оптимизации OpenGL для быстрого рендеринга
        if (renderOptimizationLevel > 0) {
            // Включаем оптимизации на основе уровня
            if (renderOptimizationLevel >= 2) {
                GLES20.glEnable(GLES20.GL_TEXTURE_2D);
                GLES20.glHint(GLES20.GL_GENERATE_MIPMAP_HINT, GLES20.GL_FASTEST);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (XrActivity.isEnabled(null)) {
            XrActivity activity = XrActivity.getInstance();
            activity.init();
            width = activity.getWidth();
            height = activity.getHeight();
            GLES20.glViewport(0, 0, width, height);
            magnifierEnabled = false;
        }

        surfaceWidth = width;
        surfaceHeight = height;
        viewTransformation.update(width, height, xServer.screenInfo.width, xServer.screenInfo.height);
        viewportNeedsUpdate = true;
        
        // Обновляем размеры буферов интерполяции при изменении размера
        updateInterpolationBuffers();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (toggleFullscreen) {
            fullscreen = !fullscreen;
            toggleFullscreen = false;
            viewportNeedsUpdate = true;
        }

        drawFrame();
    }

    public void drawFrame() {
        currentFrameTime = System.nanoTime();
        
        // Проверяем ограничение частоты кадров
        if (frameRateLimited && (currentFrameTime - lastFrameRenderTime) < frameRateInterval) {
            return; // Пропускаем рендеринг если слишком быстро
        }
        
        lastFrameRenderTime = currentFrameTime;

        boolean xrFrame = false;
        boolean xrImmersive = false;
        if (XrActivity.isEnabled(null)) {
            xrImmersive = XrActivity.getImmersive();
            xrFrame = XrActivity.getInstance().beginFrame(xrImmersive, XrActivity.getSBS());
        }

        // Update the viewport if necessary
        if (viewportNeedsUpdate && magnifierEnabled) {
            if (fullscreen) {
                GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
            } else {
                GLES20.glViewport(viewTransformation.viewOffsetX, viewTransformation.viewOffsetY, viewTransformation.viewWidth, viewTransformation.viewHeight);
            }
            viewportNeedsUpdate = false;
        }

        // Clear the screen before drawing
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Apply basic transformations and draw windows
        if (magnifierEnabled) {
            // Apply magnifier transformations if enabled
            float pointerX = 0;
            float pointerY = 0;
            float magnifierZoom = !screenOffsetYRelativeToCursor ? this.magnifierZoom : 1.0f;

            if (magnifierZoom != 1.0f) {
                pointerX = Mathf.clamp(xServer.pointer.getX() * magnifierZoom - xServer.screenInfo.width * 0.5f, 0, xServer.screenInfo.width * Math.abs(1.0f - magnifierZoom));
            }

            if (screenOffsetYRelativeToCursor || magnifierZoom != 1.0f) {
                float scaleY = magnifierZoom != 1.0f ? Math.abs(1.0f - magnifierZoom) : 0.5f;
                float offsetY = xServer.screenInfo.height * (screenOffsetYRelativeToCursor ? 0.25f : 0.5f);
                pointerY = Mathf.clamp(xServer.pointer.getY() * magnifierZoom - offsetY, 0, xServer.screenInfo.height * scaleY);
            }

            XForm.makeTransform(tmpXForm2, -pointerX, -pointerY, magnifierZoom, magnifierZoom, 0);
        } else {
            if (!fullscreen) {
                int pointerY = 0;
                if (screenOffsetYRelativeToCursor) {
                    short halfScreenHeight = (short)(xServer.screenInfo.height / 2);
                    pointerY = Mathf.clamp(xServer.pointer.getY() - halfScreenHeight / 2, 0, halfScreenHeight);
                }

                XForm.makeTransform(tmpXForm2, viewTransformation.sceneOffsetX, viewTransformation.sceneOffsetY - pointerY, viewTransformation.sceneScaleX, viewTransformation.sceneScaleY, 0);

                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                GLES20.glScissor(viewTransformation.viewOffsetX, viewTransformation.viewOffsetY, viewTransformation.viewWidth, viewTransformation.viewHeight);
            } else {
                XForm.identity(tmpXForm2);
            }
        }

        // Store current transform for interpolation
        if (frameInterpolationEnabled) {
            // Calculate velocity if needed
            calculateVelocity();
            
            // Проверяем, изменился ли игровой кадр
            if (hasGameFrameChanged()) { // Новый метод
                // Это новый игровой кадр!
                lastGameFrameTime = currentFrameTime; // Обновляем время последнего кадра игры
                
                // Копируем текущую трансформацию в предыдущую (для интерполяции с предыдущего кадра игры)
                System.arraycopy(tmpXForm2, 0, previousTransform, 0, 6);
                
                // Копируем новую трансформацию в currentTransform
                System.arraycopy(tmpXForm2, 0, currentTransform, 0, 6);
                
                // Обновляем историю трансформаций
                System.arraycopy(currentTransform, 0, transformHistory, historyIndex * 6, 6);
                historyIndex = (historyIndex + 1) % 5; // 5 элементов в истории
                
                // Обновляем историю курсора
                previousCursor[0] = currentCursor[0];
                previousCursor[1] = currentCursor[1];
                currentCursor[0] = xServer.pointer.getX();
                currentCursor[1] = xServer.pointer.getY();
                
                System.arraycopy(currentCursor, 0, cursorHistory, cursorHistoryIndex * 2, 2);
                cursorHistoryIndex = (cursorHistoryIndex + 1) % 5; // 5 элементов в истории
                
                // Начинаем интерполяцию с этого момента
                interpolationStartTime = currentFrameTime;
                
                // Сбрасываем счетчик промежуточных кадров
                activeIntermediateFramesCount = 0;
            } else {
                // Это тот же самый игровой кадр, возможно, интерполяция между ним и следующим
                // или мы просто рендерим последний кадр игры до следующего изменения
                // Ничего не меняем в текущих трансформациях
            }
        }

        // ОБЯЗАТЕЛЬНО РЕНДЕРИМ ОКНА - это ключевая строка
        renderWindows();

        // Render cursor if enabled
        if (cursorVisible) {
            renderCursor();
        }

        // Disable scissor test if magnifier is disabled and not in fullscreen mode
        if (!magnifierEnabled && !fullscreen) {
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        }
        
        // Apply all the effects using EffectComposer
        if (effectComposer.hasEffects()) {
            effectComposer.render();  // <-- This line applies the effects
        }

        // Handle frame interpolation if enabled
        if (frameInterpolationEnabled && numIntermediateFrames > 0) {
            performFrameInterpolationBasedOnTime(); // Новый метод
        }

        // Finalize XR frame if supported
        if (xrFrame) {
            XrActivity.getInstance().endFrame();
            XrActivity.updateControllers();
            xServerView.requestRender();
        }
        
        // Update frame timing for dynamic FPS adjustment
        if (frameInterpolationEnabled) {
            updateFrameTiming();
        }
        
        // Обновляем метрики производительности после рендеринга
        updatePerformanceMetrics();
    }

    // НОВЫЙ МЕТОД: Проверяет, изменился ли игровой кадр
    private boolean hasGameFrameChanged() {
        // Простая проверка - изменилась ли позиция курсора
        // В реальности это может быть более сложной логикой, зависящей от изменений в окнах
        boolean cursorChanged = (xServer.pointer.getX() != currentCursor[0] || xServer.pointer.getY() != currentCursor[1]);
        
        // Можно добавить проверки изменений в Drawable-ах окон, если это возможно
        // Или использовать флаги из XServer, если они доступны
        
        return cursorChanged;
    }

    // НОВЫЙ МЕТОД: Интерполяция на основе времени
    private void performFrameInterpolationBasedOnTime() {
        if (lastGameFrameTime == 0) {
            // Первый кадр, нечего интерполировать
            return;
        }

        long currentTime = System.nanoTime();
        long elapsedSinceLastGameFrame = currentTime - lastGameFrameTime;
        long elapsedSinceInterpolationStart = currentTime - interpolationStartTime;

        // Рассчитываем, сколько времени прошло в цикле интерполяции
        long interpolationCycleProgress = elapsedSinceInterpolationStart % targetInterpolationInterval;

        // Рассчитываем фактор интерполяции для текущего момента
        float interpolationFactor = (float) interpolationCycleProgress / targetInterpolationInterval;

        // Рассчитываем количество промежуточных кадров, которые нужно было бы сгенерировать
        // с начала интерполяции до текущего времени
        long totalInterpolationTime = elapsedSinceInterpolationStart;
        int totalExpectedFrames = (int) (totalInterpolationTime / targetInterpolationInterval);

        // Проверяем, не пора ли нам сгенерировать следующий кадр интерполяции
        if (totalExpectedFrames > activeIntermediateFramesCount) {
            // Да, пора сгенерировать кадр
            activeIntermediateFramesCount = totalExpectedFrames;

            // Рассчитываем промежуточную трансформацию и позицию курсора
            float[] interpolatedTransform = calculateTransformForFactor(interpolationFactor);
            float[] interpolatedCursor = calculateCursorForFactor(interpolationFactor);

            // Рендерим промежуточный кадр
            renderIntermediateFrame(interpolatedTransform, interpolatedCursor);
        }
    }


    private void checkContentChange() {
        // Проверяем, изменилось ли содержимое окон
        boolean contentChangedNow = false;
        
        try (XLock lock = xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
            for (RenderableWindow window : renderableWindows) {
                if (window.content != null) {
                    // В текущей архитектуре просто проверяем наличие содержимого как признак активности
                    contentChangedNow = true;
                    break;
                }
            }
        }
        
        if (contentChangedNow) {
            contentChanged = true;
            lastContentChangeTime = System.nanoTime();
            sceneStableFlag = false;
        } else {
            // Если содержимое не изменилось долгое время, сцена стабильна
            if (System.nanoTime() - lastContentChangeTime > 1_000_000_000L) { // 1 секунда
                sceneStableFlag = true;
            }
        }
    }

    private void renderWindows() {
        windowMaterial.use();
        GLES20.glUniform2f(windowMaterial.getUniformLocation("viewSize"), xServer.screenInfo.width, xServer.screenInfo.height);
        quadVertices.bind(windowMaterial.programId);

        try (XLock lock = xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
            for (RenderableWindow window : renderableWindows) {
                renderDrawable(window.content, window.rootX, window.rootY, windowMaterial);
            }
        }

        quadVertices.disable();

        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e("GLRenderer", "OpenGL Error: " + error);
        }
    }

    private void renderCursor() {
        cursorMaterial.use();
        GLES20.glUniform2f(cursorMaterial.getUniformLocation("viewSize"), xServer.screenInfo.width, xServer.screenInfo.height);
        quadVertices.bind(cursorMaterial.programId);

        try (XLock lock = xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
            Window pointWindow = xServer.inputDeviceManager.getPointWindow();
            Cursor cursor = pointWindow != null ? pointWindow.attributes.getCursor() : null;
            short x = xServer.pointer.getClampedX();
            short y = xServer.pointer.getClampedY();

            if (cursor != null) {
                if (cursor.isVisible()) renderDrawable(cursor.cursorImage, x - cursor.hotSpotX, y - cursor.hotSpotY, cursorMaterial);
            } else renderDrawable(rootCursorDrawable, x, y, cursorMaterial);
        }

        quadVertices.disable();
    }

    private void renderDrawable(Drawable drawable, int x, int y, ShaderMaterial material) {
        if (drawable == null) return;
        synchronized (drawable.renderLock) {
            Texture texture = drawable.getTexture();
            texture.updateFromDrawable(drawable);

            XForm.set(tmpXForm1, x, y, drawable.width, drawable.height);
            XForm.multiply(tmpXForm1, tmpXForm1, tmpXForm2);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.getTextureId());
            GLES20.glUniform1i(material.getUniformLocation("texture"), 0);
            GLES20.glUniform1fv(material.getUniformLocation("xform"), tmpXForm1.length, tmpXForm1, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, quadVertices.count());
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }

    private void calculateVelocity() {
        if (lastVelocityTime == 0) {
            lastVelocityTime = currentFrameTime;
            // Инициализируем начальные значения скорости
            for (int i = 0; i < 6; i++) {
                currentVelocity[i] = 0;
                previousVelocity[i] = 0;
            }
            for (int i = 0; i < 2; i++) {
                currentCursorVelocity[i] = 0;
                previousCursorVelocity[i] = 0;
            }
            return;
        }
        
        float deltaTime = (currentFrameTime - lastVelocityTime) / 1_000_000_000.0f; // в секундах
        lastVelocityTime = currentFrameTime;
        
        if (deltaTime > 0) {
            // Calculate transform velocity
            for (int i = 0; i < 6; i++) {
                previousVelocity[i] = currentVelocity[i];
                // Используем текущую и предыдущую трансформации для вычисления скорости
                float deltaTransform = currentTransform[i] - previousTransform[i];
                currentVelocity[i] = deltaTransform / deltaTime;
            }
            
            // Calculate cursor velocity
            previousCursorVelocity[0] = currentCursorVelocity[0];
            previousCursorVelocity[1] = currentCursorVelocity[1];
            float deltaCursorX = currentCursor[0] - previousCursor[0];
            float deltaCursorY = currentCursor[1] - previousCursor[1];
            currentCursorVelocity[0] = deltaCursorX / deltaTime;
            currentCursorVelocity[1] = deltaCursorY / deltaTime;
            
            // Сохраняем скорости в историю
            System.arraycopy(currentVelocity, 0, velocityHistory, historyIndex * 6, 6);
            System.arraycopy(currentCursorVelocity, 0, cursorVelocityHistory, cursorHistoryIndex * 2, 2);
        }
    }

    // --- НОВЫЕ/ОБНОВЛЕННЫЕ МЕТОДЫ ДЛЯ ИНТЕРПОЛЯЦИИ ---
    
    private void performFrameInterpolation() {
        // Старый метод, не используем для новой интерполяции на основе времени
        // Оставлен для совместимости, если где-то используется
        Log.d("GLRenderer", "performFrameInterpolation called. This should not be used with time-based interpolation.");
    }

    private float[] calculateTransformForFactor(float factor) {
        float[] result = XForm.getInstance();
        
        switch (interpolationAlgorithm) {
            case 0: // Linear
                lerpXForm(result, previousTransform, currentTransform, factor);
                break;
            case 1: // Cubic (Catmull-Rom)
                cubicCatmullRomInterpolate(result, factor);
                break;
            case 2: // Spline
                splineInterpolate(result, factor);
                break;
            case INTERPOLATION_HERMITE: // Hermite
                hermiteInterpolate(result, factor);
                break;
            default:
                lerpXForm(result, previousTransform, currentTransform, factor);
                break;
        }
        
        return result;
    }

    private float[] calculateCursorForFactor(float factor) {
        float[] result = new float[2];
        
        switch (interpolationAlgorithm) {
            case 0: // Linear
                result[0] = previousCursor[0] + (currentCursor[0] - previousCursor[0]) * factor;
                result[1] = previousCursor[1] + (currentCursor[1] - previousCursor[1]) * factor;
                break;
            case 1: // Cubic (Catmull-Rom)
                result[0] = cubicCatmullRomInterpolateValue(getCursorHistoryX(), factor);
                result[1] = cubicCatmullRomInterpolateValue(getCursorHistoryY(), factor);
                break;
            case 2: // Spline
                result[0] = splineInterpolateValue(getCursorHistoryX(), factor);
                result[1] = splineInterpolateValue(getCursorHistoryY(), factor);
                break;
            case INTERPOLATION_HERMITE: // Hermite
                result[0] = hermiteInterpolateValue(getCursorHistoryX(), getCursorVelocityHistoryX(), factor);
                result[1] = hermiteInterpolateValue(getCursorHistoryY(), getCursorVelocityHistoryY(), factor);
                break;
            default:
                result[0] = previousCursor[0] + (currentCursor[0] - previousCursor[0]) * factor;
                result[1] = previousCursor[1] + (currentCursor[1] - previousCursor[1]) * factor;
                break;
        }
        
        return result;
    }

    private void renderIntermediateFrame(float[] transform, float[] cursor) {
        // Применяем промежуточную трансформацию
        System.arraycopy(transform, 0, tmpXForm2, 0, 6);
        
        // Обновляем позицию курсора
        short cursorX = (short) cursor[0];
        short cursorY = (short) cursor[1];
        
        // Рендерим окна с промежуточной трансформацией
        renderWindows();
        
        // Рендерим курсор в промежуточной позиции
        renderCursorAtPosition(cursorX, cursorY);
    }

    private void renderCursorAtPosition(short x, short y) {
        cursorMaterial.use();
        GLES20.glUniform2f(cursorMaterial.getUniformLocation("viewSize"), xServer.screenInfo.width, xServer.screenInfo.height);
        quadVertices.bind(cursorMaterial.programId);

        try (XLock lock = xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
            Window pointWindow = xServer.inputDeviceManager.getPointWindow();
            Cursor cursor = pointWindow != null ? pointWindow.attributes.getCursor() : null;

            if (cursor != null) {
                if (cursor.isVisible()) renderDrawable(cursor.cursorImage, x - cursor.hotSpotX, y - cursor.hotSpotY, cursorMaterial);
            } else renderDrawable(rootCursorDrawable, x, y, cursorMaterial);
        }

        quadVertices.disable();
    }

    // Вспомогательные методы интерполяции
    private void lerpXForm(float[] result, float[] a, float[] b, float factor) {
        for (int i = 0; i < 6; i++) {
            result[i] = a[i] + (b[i] - a[i]) * factor;
        }
    }

    private void cubicCatmullRomInterpolate(float[] result, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        
        for (int i = 0; i < 6; i++) {
            float p0 = transformHistory[((historyIndex - 3 + 5) % 5) * 6 + i];
            float p1 = transformHistory[((historyIndex - 2 + 5) % 5) * 6 + i];
            float p2 = transformHistory[((historyIndex - 1 + 5) % 5) * 6 + i];
            float p3 = transformHistory[(historyIndex * 6) + i];
            
            result[i] = 0.5f * (
                (-p0 + 3f*p1 - 3f*p2 + p3) * t3 +
                (2f*p0 - 5f*p1 + 4f*p2 - p3) * t2 +
                (-p0 + p2) * t +
                2f*p1
            );
        }
    }

    private float cubicCatmullRomInterpolateValue(float[] values, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        
        int idx = (cursorHistoryIndex + 2) % 5; // current
        float p0 = values[(idx - 3 + 5) % 5]; // previous previous
        float p1 = values[(idx - 2 + 5) % 5]; // previous
        float p2 = values[(idx - 1 + 5) % 5]; // current
        float p3 = values[idx]; // next
        
        return 0.5f * (
            (-p0 + 3f*p1 - 3f*p2 + p3) * t3 +
            (2f*p0 - 5f*p1 + 4f*p2 - p3) * t2 +
            (-p0 + p2) * t +
            2f*p1
        );
    }

    private float splineInterpolateValue(float[] values, float t) {
        // Простая кубическая сплайн-интерполяция
        int idx = (cursorHistoryIndex + 2) % 5; // current
        float p0 = values[(idx - 1 + 5) % 5]; // previous
        float p1 = values[idx]; // current
        float p2 = values[(idx + 1) % 5]; // next
        float p3 = values[(idx + 2) % 5]; // next next
        
        // Используем кубическую интерполяцию
        float t2 = t * t;
        float t3 = t2 * t;
        
        return 0.5f * (
            (-p0 + 3f*p1 - 3f*p2 + p3) * t3 +
            (2f*p0 - 5f*p1 + 4f*p2 - p3) * t2 +
            (-p0 + p2) * t +
            2f*p1
        );
    }

    private void splineInterpolate(float[] result, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        
        for (int i = 0; i < 6; i++) {
            float p0 = transformHistory[((historyIndex - 3 + 5) % 5) * 6 + i];
            float p1 = transformHistory[((historyIndex - 2 + 5) % 5) * 6 + i];
            float p2 = transformHistory[((historyIndex - 1 + 5) % 5) * 6 + i];
            float p3 = transformHistory[(historyIndex * 6) + i];
            
            result[i] = 0.5f * (
                (-p0 + 3f*p1 - 3f*p2 + p3) * t3 +
                (2f*p0 - 5f*p1 + 4f*p2 - p3) * t2 +
                (-p0 + p2) * t +
                2f*p1
            );
        }
    }

    private void hermiteInterpolate(float[] result, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        
        // Hermite basis functions
        float h00 = 2 * t3 - 3 * t2 + 1;      // p0
        float h10 = t3 - 2 * t2 + t;          // m0
        float h01 = -2 * t3 + 3 * t2;         // p1
        float h11 = t3 - t2;                  // m1
        
        for (int i = 0; i < 6; i++) {
            float p0 = transformHistory[((historyIndex - 1 + 5) % 5) * 6 + i];
            float p1 = transformHistory[(historyIndex * 6) + i];
            float m0 = velocityHistory[((historyIndex - 1 + 5) % 5) * 6 + i] * 0.5f; // Уменьшаем влияние
            float m1 = velocityHistory[(historyIndex * 6) + i] * 0.5f;
            
            result[i] = h00 * p0 + h10 * m0 + h01 * p1 + h11 * m1;
        }
    }

    private float hermiteInterpolateValue(float[] values, float[] velocities, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        
        // Hermite basis functions
        float h00 = 2 * t3 - 3 * t2 + 1;
        float h10 = t3 - 2 * t2 + t;
        float h01 = -2 * t3 + 3 * t2;
        float h11 = t3 - t2;
        
        int idx = (cursorHistoryIndex + 2) % 5; // current
        float p0 = values[(idx - 1 + 5) % 5]; // previous
        float p1 = values[idx]; // current
        float m0 = velocities[(idx - 1 + 5) % 5] * 0.5f;
        float m1 = velocities[idx] * 0.5f;
        
        return h00 * p0 + h10 * m0 + h01 * p1 + h11 * m1;
    }

    private float[] getCursorHistoryX() {
        float[] xValues = new float[5];
        for (int i = 0; i < 5; i++) {
            xValues[i] = cursorHistory[(cursorHistoryIndex + i) % 5 * 2];
        }
        return xValues;
    }

    private float[] getCursorHistoryY() {
        float[] yValues = new float[5];
        for (int i = 0; i < 5; i++) {
            yValues[i] = cursorHistory[(cursorHistoryIndex + i) % 5 * 2 + 1];
        }
        return yValues;
    }

    private float[] getCursorVelocityHistoryX() {
        float[] xValues = new float[5];
        for (int i = 0; i < 5; i++) {
            xValues[i] = cursorVelocityHistory[(cursorHistoryIndex + i) % 5 * 2];
        }
        return xValues;
    }

    private float[] getCursorVelocityHistoryY() {
        float[] yValues = new float[5];
        for (int i = 0; i < 5; i++) {
            yValues[i] = cursorVelocityHistory[(cursorHistoryIndex + i) % 5 * 2 + 1];
        }
        return yValues;
    }

    private void updateInterpolationMethod() {
        // Анализируем скорость изменения сцены
        float motionIntensity = calculateMotionIntensity();
        float motionDirection = calculateMotionDirection(); // Новый метод
        
        if (motionIntensity > 0.7f) {
            // Очень быстрое движение - кубическая интерполяция
            interpolationAlgorithm = 1;
            // Увеличиваем количество кадров для быстрого движения
            numIntermediateFrames = Math.min(6, maxIntermediateFrames); // Максимум 6
        } else if (motionIntensity > 0.3f) {
            // Среднее движение - сплайн-интерполяция
            interpolationAlgorithm = 2;
            numIntermediateFrames = Math.min(4, maxIntermediateFrames); // 4 кадра
        } else if (motionIntensity > 0.1f) {
            // Медленное движение - интерполяция Эрмита
            interpolationAlgorithm = INTERPOLATION_HERMITE;
            numIntermediateFrames = Math.min(3, maxIntermediateFrames); // 3 кадра
        } else {
            // Очень медленное движение - линейная интерполяция
            interpolationAlgorithm = 0;
            numIntermediateFrames = 1; // 1 кадр
        }
    }

    private float calculateMotionIntensity() {
        // Простая проверка изменения позиции курсора
        float cursorChange = Math.abs(currentCursor[0] - previousCursor[0]) + 
                            Math.abs(currentCursor[1] - previousCursor[1]);
        return cursorChange / (xServer.screenInfo.width + xServer.screenInfo.height);
    }

    private float calculateMotionDirection() {
        // Вычисляем направление движения
        float deltaX = currentCursor[0] - previousCursor[0];
        float deltaY = currentCursor[1] - previousCursor[1];
        return (float) Math.atan2(deltaY, deltaX);
    }

    private void updateFrameTiming() {
        long frameDuration = System.nanoTime() - currentFrameTime;
        frameTimes[frameTimeIndex] = frameDuration;
        frameTimeIndex = (frameTimeIndex + 1) % frameTimes.length;
        
        if (frameTimeIndex == 0 && dynamicFPSAdjustment) {
            adjustFrameRateBasedOnPerformance();
        }
    }

    private void adjustFrameRateBasedOnPerformance() {
        long total = 0;
        for (long time : frameTimes) {
            total += time;
        }
        long averageTime = total / frameTimes.length;
        
        // Целевое время на кадр для 60 FPS (16.67ms)
        long targetTime = 16_666_666; // наносекунды
        
        if (averageTime > targetTime * 1.2f) {
            // Снижаем FPS если не успеваем
            dynamicTargetFPS = Math.max(30, dynamicTargetFPS - 15);
        } else if (averageTime < targetTime * 0.8f && dynamicTargetFPS < 120) {
            // Повышаем FPS если есть запас
            dynamicTargetFPS = Math.min(120, dynamicTargetFPS + 15);
        }
        
        // Обновляем количество промежуточных кадров, но не превышаем maxIntermediateFrames
        numIntermediateFrames = Math.min(calculateIntermediateFrames(), maxIntermediateFrames);
    }

    private int calculateIntermediateFrames() {
        switch (interpolationQuality) {
            case 1: return 3; // Low - 3 кадра
            case 2: return 4; // Medium - 4 кадра
            case 3: return 6; // High - 6 кадров (увеличено)
            default: return 0; // Off
        }
    }

    private void updateInterpolationBuffers() {
        // Обновляем размеры буферов интерполяции при изменении размера экрана
        interpolationCache = new float[10][];
        for (int i = 0; i < 10; i++) {
            interpolationCache[i] = new float[6];
        }
    }

    @Override
    public void onMapWindow(Window window) {
        xServerView.queueEvent(this::updateScene);
        xServerView.requestRender();
    }

    @Override
    public void onUnmapWindow(Window window) {
        xServerView.queueEvent(this::updateScene);
        xServerView.requestRender();
    }

    @Override
    public void onChangeWindowZOrder(Window window) {
        xServerView.queueEvent(this::updateScene);
        xServerView.requestRender();
    }

    @Override
    public void onUpdateWindowContent(Window window) {
        xServerView.requestRender();
    }

    @Override
    public void onUpdateWindowGeometry(final Window window, boolean resized) {
        if (resized) {
            xServerView.queueEvent(this::updateScene);
        } else xServerView.queueEvent(() -> updateWindowPosition(window));
        xServerView.requestRender();
    }

    @Override
    public void onUpdateWindowAttributes(Window window, Bitmask mask) {
        if (mask.isSet(WindowAttributes.FLAG_CURSOR)) xServerView.requestRender();
    }

    @Override
    public void onPointerMove(short x, short y) {
        xServerView.requestRender();
    }

    private void renderWindowEffect(Drawable drawable, int x, int y, ShaderMaterial material) {
        synchronized (drawable.renderLock) {
            Texture texture = drawable.getTexture();
            texture.updateFromDrawable(drawable);

            XForm.set(tmpXForm1, x, y, drawable.width, drawable.height);
            XForm.multiply(tmpXForm1, tmpXForm1, tmpXForm2);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.getTextureId());
            if (GLES20.glIsTexture(texture.getTextureId()) == false) {
                Log.e("GLRenderer", "Invalid texture binding!");
            }

            GLES20.glUniform1i(material.getUniformLocation("texture"), 0);
            GLES20.glUniform1fv(material.getUniformLocation("xform"), tmpXForm1.length, tmpXForm1, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, quadVertices.count());
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }

    public void setUnviewableWMClasses(String... unviewableWMNames) {
        this.unviewableWMClasses = unviewableWMNames;
    }

    // --- Добавлено для поддержки фонового изображения ---
    
    private void initBackgroundTexture() {
        if (backgroundTextureId == 0) {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            backgroundTextureId = textures[0];
            
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            
            // Создаем начальную текстуру (черный квадрат)
            ByteBuffer buffer = ByteBuffer.allocateDirect(64 * 64 * 4);
            buffer.order(ByteOrder.nativeOrder());
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 64, 64, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            
            // Передаем ID текстуры в VREffect
            VREffect vrEffect = effectComposer.getVREffect();
            if (vrEffect != null) {
                vrEffect.setBackgroundTextureId(backgroundTextureId);
            }
        }
    }
    
    public void loadBackgroundImage(Bitmap bitmap) {
        if (backgroundTextureId == 0 || bitmap == null) {
            return;
        }
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(buffer);
        buffer.rewind();
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 
                           bitmap.getWidth(), bitmap.getHeight(), 0, 
                           GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
    
    public void loadDefaultBackground() {
        // Загрузка стандартного фона из ресурсов
        Context context = xServerView.getContext();
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.vr_background);
        if (bitmap != null) {
            loadBackgroundImage(bitmap);
        }
    }
    
    public void cleanup() {
        // Освобождаем текстуру фона
        if (backgroundTextureId != 0) {
            int[] textures = {backgroundTextureId};
            GLES20.glDeleteTextures(1, textures, 0);
            backgroundTextureId = 0;
        }
    }
    
    // --- Методы для совместимости ---
    public int getViewportWidth() {
        return surfaceWidth;
    }

    public int getViewportHeight() {
        return surfaceHeight;
    }
    
    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }
    
    public boolean isFullscreen() {
        return fullscreen;
    }
    
    public void setCursorVisible(boolean visible) {
        this.cursorVisible = visible;
        xServerView.requestRender();
    }
    
    public boolean isCursorVisible() {
        return cursorVisible;
    }
    
    public void toggleFullscreen() {
        toggleFullscreen = true;
        xServerView.requestRender();
    }
    
    public float getMagnifierZoom() {
        return magnifierZoom;
    }
    
    public void setMagnifierZoom(float zoom) {
        this.magnifierZoom = zoom;
        xServerView.requestRender();
    }
    
    public boolean isViewportNeedsUpdate() {
        return viewportNeedsUpdate;
    }
    
    public void setViewportNeedsUpdate(boolean needsUpdate) {
        this.viewportNeedsUpdate = needsUpdate;
    }
    
    public VertexAttribute getQuadVertices() {
        return quadVertices;
    }
    
    public EffectComposer getEffectComposer() {
        return effectComposer;
    }
    
    // --- ДОБАВЛЕННЫЙ МЕТОД ДЛЯ РЕШЕНИЯ ПРОБЛЕМЫ ---
    public void setScreenOffsetYRelativeToCursor(boolean enabled) {
        this.screenOffsetYRelativeToCursor = enabled;
        xServerView.requestRender();
    }
    
    // --- МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ ИНТЕРПОЛЯЦИЕЙ ---
    
    public void setFrameInterpolationEnabled(boolean enabled) {
        this.frameInterpolationEnabled = enabled;
        if (!enabled) {
            numIntermediateFrames = 0;
        } else {
            numIntermediateFrames = Math.min(calculateIntermediateFrames(), maxIntermediateFrames);
            if (dynamicFPSAdjustment) {
                 adjustFrameRateBasedOnPerformance();
            }
        }
    }

    public boolean isFrameInterpolationEnabled() {
        return frameInterpolationEnabled;
    }

    public void setAdaptiveInterpolation(boolean enabled) {
        this.adaptiveInterpolation = enabled;
    }

    public boolean isAdaptiveInterpolation() {
        return adaptiveInterpolation;
    }

    public void setMotionPredictionEnabled(boolean enabled) {
        this.motionPredictionEnabled = enabled;
    }

    public boolean isMotionPredictionEnabled() {
        return motionPredictionEnabled;
    }

    public void setTemporalAAEnabled(boolean enabled) {
        this.temporalAAEnabled = enabled;
    }

    public boolean isTemporalAAEnabled() {
        return temporalAAEnabled;
    }

    public void setInterpolationQuality(int quality) {
        this.interpolationQuality = quality;
        if (frameInterpolationEnabled) {
            numIntermediateFrames = Math.min(calculateIntermediateFrames(), maxIntermediateFrames);
            if (dynamicFPSAdjustment) {
                 adjustFrameRateBasedOnPerformance();
            }
        }
    }

    public int getInterpolationQuality() {
        return interpolationQuality;
    }

    public void setInterpolationAlgorithm(int algorithm) {
        this.interpolationAlgorithm = algorithm;
    }

    public int getInterpolationAlgorithm() {
        return interpolationAlgorithm;
    }

    public void setMaxIntermediateFrames(int maxFrames) {
        this.maxIntermediateFrames = maxFrames;
        if (frameInterpolationEnabled) {
            numIntermediateFrames = Math.min(calculateIntermediateFrames(), maxIntermediateFrames);
            if (dynamicFPSAdjustment) {
                 adjustFrameRateBasedOnPerformance();
            }
        }
    }

    public int getMaxIntermediateFrames() {
        return maxIntermediateFrames;
    }

    public void setDynamicFPSAdjustment(boolean enabled) {
        this.dynamicFPSAdjustment = enabled;
    }

    public boolean isDynamicFPSAdjustment() {
        return dynamicFPSAdjustment;
    }

    public void setPredictionFactor(float factor) {
        this.predictionFactor = factor;
    }

    public float getPredictionFactor() {
        return predictionFactor;
    }

    // --- МЕТОДЫ ДЛЯ ОПТИМИЗАЦИИ ---
    
    public void setRenderOptimizationLevel(int level) {
        this.renderOptimizationLevel = level;
        setupOpenGLOptimizations(); // Применяем изменения
    }

    public int getRenderOptimizationLevel() {
        return renderOptimizationLevel;
    }

    // --- МЕТОДЫ ДЛЯ ИНТЕРПОЛЯЦИИ СОДЕРЖИМОГО ---
    
    public boolean isInterpolationActive() {
        return interpolationActive;
    }

    public int getActiveIntermediateFrames() {
        return activeIntermediateFramesCount;
    }

    public float getCurrentInterpolationFactor() {
        return currentInterpolationFactor;
    }

    public boolean isSceneStable() {
        return sceneStableFlag;
    }

    // --- МЕТОДЫ ДЛЯ МОНИТОРИНГА ПРОИЗВОДИТЕЛЬНОСТИ ---
    
    private void updatePerformanceMetrics() {
        if (!performanceMonitoringEnabled) return;
        
        long renderTime = System.nanoTime() - currentFrameTime;
        renderTimes[renderTimeIndex] = renderTime;
        renderTimeIndex = (renderTimeIndex + 1) % renderTimes.length;
        
        // Обновляем FPS каждые 500ms
        long currentTime = System.nanoTime();
        frameCount++;
        
        if (currentTime - lastFPSCheck >= 500_000_000L) { // 500ms
            currentFPS = frameCount * 1_000_000_000.0f / (currentTime - lastFPSCheck);
            frameCount = 0;
            lastFPSCheck = currentTime;
        }
        
        // Обновляем среднее время кадра
        long totalRenderTime = 0;
        int validCount = 0;
        minFrameTime = Float.MAX_VALUE;
        maxFrameTime = 0;
        
        for (long time : renderTimes) {
            if (time > 0) {
                float frameTimeMs = time / 1_000_000.0f;
                totalRenderTime += time;
                validCount++;
                
                if (frameTimeMs < minFrameTime) minFrameTime = frameTimeMs;
                if (frameTimeMs > maxFrameTime) maxFrameTime = frameTimeMs;
            }
        }
        
        if (validCount > 0) {
            avgFrameTime = (totalRenderTime / validCount) / 1_000_000.0f;
        }
    }
    
    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(currentFPS, avgFrameTime, minFrameTime, maxFrameTime);
    }
    
    public void setPerformanceMonitoringEnabled(boolean enabled) {
        this.performanceMonitoringEnabled = enabled;
        if (!enabled) {
            // Сбрасываем статистику
            for (int i = 0; i < renderTimes.length; i++) {
                renderTimes[i] = 0;
            }
            currentFPS = 0;
            avgFrameTime = 0;
            minFrameTime = Float.MAX_VALUE;
            maxFrameTime = 0;
            frameCount = 0;
        }
    }
    
    public static class PerformanceStats {
        public final float fps;
        public final float avgFrameTime;
        public final float minFrameTime;
        public final float maxFrameTime;
        
        public PerformanceStats(float fps, float avgFrameTime, float minFrameTime, float maxFrameTime) {
            this.fps = fps;
            this.avgFrameTime = avgFrameTime;
            this.minFrameTime = minFrameTime;
            this.maxFrameTime = maxFrameTime;
        }
        
        @Override
        public String toString() {
            return String.format("FPS: %.1f, Avg: %.2fms, Min: %.2fms, Max: %.2fms", 
                               fps, avgFrameTime, minFrameTime, maxFrameTime);
        }
    }
    
    // --- МЕТОДЫ ДЛЯ ОБНОВЛЕНИЯ СЦЕНЫ ---
    
    private void updateScene() {
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
            renderableWindows.clear();
            collectRenderableWindows(xServer.windowManager.rootWindow, xServer.windowManager.rootWindow.getX(), xServer.windowManager.rootWindow.getY());
        }
    }

    private void collectRenderableWindows(Window window, int x, int y) {
        if (!window.attributes.isMapped()) return;
        if (window != xServer.windowManager.rootWindow) {
            boolean viewable = true;

            if (unviewableWMClasses != null) {
                String wmClass = window.getClassName();
                for (String unviewableWMClass : unviewableWMClasses) {
                    if (wmClass.contains(unviewableWMClass)) {
                        if (window.attributes.isEnabled()) window.disableAllDescendants();
                        viewable = false;
                        break;
                    }
                }
            }

            if (viewable)
                renderableWindows.add(new RenderableWindow(window.getContent(), x, y));
        }

        for (Window child : window.getChildren()) {
            collectRenderableWindows(child, child.getX() + x, child.getY() + y);
        }
    }

    private void updateWindowPosition(Window window) {
        for (RenderableWindow renderableWindow : renderableWindows) {
            if (renderableWindow.content == window.getContent()) {
                renderableWindow.rootX = window.getRootX();
                renderableWindow.rootY = window.getRootY();
                break;
            }
        }
    }
    
    // Внутренний класс RenderableWindow
    private static class RenderableWindow {
        public final Drawable content;
        public int rootX;
        public int rootY;

        public RenderableWindow(Drawable content, int rootX, int rootY) {
            this.content = content;
            this.rootX = rootX;
            this.rootY = rootY;
        }
    }
    
    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---
    
    private Drawable createRootCursorDrawable() {
        Context context = xServerView.getContext();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.cursor, options);
        return Drawable.fromBitmap(bitmap);
    }
}