package com.winlator.cmod.renderer;

import android.opengl.GLES20;
import android.util.Log;

import com.winlator.cmod.renderer.effects.Effect;
import com.winlator.cmod.renderer.effects.VREffect;
import com.winlator.cmod.renderer.effects.ToonEffect;
import com.winlator.cmod.renderer.material.ShaderMaterial;

import java.util.ArrayList;
import java.util.List;

public class EffectComposer {
    // Constants
    private static final String TAG = "EffectComposer";
    private boolean isRendering = false;

    // Instance fields
    private final List<Effect> effects = new ArrayList<>();
    private RenderTarget readBuffer;
    private RenderTarget writeBuffer;
    private final GLRenderer renderer;

    // Constructor
    public EffectComposer(GLRenderer renderer) {
        this.renderer = renderer;
//        Log.d(TAG, "EffectComposer created");
    }

    // Initializes the buffers if they are not already initialized
    private void initBuffers() {
//        Log.d(TAG, "initBuffers() called");

        if (readBuffer == null) {
            readBuffer = new RenderTarget();
            readBuffer.allocateFramebuffer(renderer.getSurfaceWidth(), renderer.getSurfaceHeight());
//            Log.d(TAG, "Initialized readBuffer with size: " + renderer.getSurfaceWidth() + "x" + renderer.getSurfaceHeight());
        }

        if (writeBuffer == null) {
            writeBuffer = new RenderTarget();
            writeBuffer.allocateFramebuffer(renderer.getSurfaceWidth(), renderer.getSurfaceHeight());
//            Log.d(TAG, "Initialized writeBuffer with size: " + renderer.getSurfaceWidth() + "x" + renderer.getSurfaceHeight());
        }
    }

    public synchronized void addEffect(Effect effect) {
        if (effect != null && !effects.contains(effect)) { // <--- Проверка на null
            effects.add(effect);
            Log.d(TAG, "Effect added: " + effect.getClass().getSimpleName()); // Включаем лог
        } else if (effect == null) { // <--- Лог для null
            Log.e(TAG, "Attempted to add a null effect.");
        } else {
            Log.d(TAG, "Effect already present: " + effect.getClass().getSimpleName()); // Включаем лог
        }
        // Move this call to the end of a batch effect addition or modification to prevent immediate rendering
        renderer.xServerView.requestRender();
    }



    // Gets an effect by its class type
    public synchronized <T extends Effect> T getEffect(Class<T> effectClass) {
        Log.d(TAG, "getEffect() called for: " + effectClass.getSimpleName()); // Включаем лог

        for (Effect effect : effects) {
            if (effect.getClass() == effectClass) {
                Log.d(TAG, "Effect found: " + effectClass.getSimpleName()); // Включаем лог
                return effectClass.cast(effect);
            }
        }
        Log.d(TAG, "Effect not found: " + effectClass.getSimpleName()); // Включаем лог
        return null;
    }

    // Метод для получения VREffect (специальный метод для совместимости)
    public synchronized VREffect getVREffect() {
        return getEffect(VREffect.class);
    }

    // Checks if there are any effects present
    public synchronized boolean hasEffects() {
        boolean hasEffects = !effects.isEmpty();
//        Log.d(TAG, "hasEffects() called. Effects present: " + hasEffects);
        return hasEffects;
    }

    // Removes a specific effect from the composer
    public synchronized void removeEffect(Effect effect) {
        if (effect != null && effects.remove(effect)) { // <--- Проверка на null
            Log.d(TAG, "Effect removed: " + effect.getClass().getSimpleName()); // Включаем лог
        } else if (effect == null) { // <--- Лог для null
            Log.e(TAG, "Attempted to remove a null effect.");
        } else {
            Log.d(TAG, "Effect not found for removal: " + effect.getClass().getSimpleName()); // Включаем лог
        }
        renderer.xServerView.requestRender();
    }

    // Renders all the effects in the composer
    public synchronized void render() {
        // Check for recursive rendering
        if (isRendering) {
//            Log.d(TAG, "Render already in progress, skipping.");
            return;
        }

        isRendering = true; // Set flag to true

        Log.d(TAG, "render() called"); // Включаем лог

        initBuffers();

        // Set up framebuffer if there are effects to render
        if (hasEffects()) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, readBuffer.getFramebuffer());
//            Log.d(TAG, "Binding to readBuffer framebuffer: " + readBuffer.getFramebuffer());
        } else {
            // Если эффектов нет, рендерим сразу на экран
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//            Log.d(TAG, "Binding to default framebuffer (0)");
        }

        // --- ИСПРАВЛЕНИЕ: Вызов renderer.drawFrame() теперь рендерит только основной кадр ---
        // Если включена интерполяция, renderer.drawFrame() вызывает performFrameInterpolation,
        // которая рендерит промежуточные кадры напрямую в OpenGL, минуя EffectComposer.
        // Чтобы эффекты применялись к *результату* интерполяции, нужно:
        // 1. Сначала рендерить *все* кадры (включая промежуточные) в буфер.
        // 2. Затем применить эффекты к этому буферу.

        // Однако, текущая архитектура делает это сложно.
        // Временное решение: вызвать drawFrame(), который нарисует все кадры,
        // но *не* будет использовать EffectComposer для промежуточных кадров.
        // Чтобы эффекты всё же применялись, нужно изменить логику в GLRenderer.

        // ИДЕАЛЬНОЕ РЕШЕНИЕ (требует изменений в GLRenderer):
        // GLRenderer не должен сам вызывать performFrameInterpolation().
        // Вместо этого, он должен возвращать "состояния" (transform, cursor) для каждого кадра.
        // EffectComposer должен управлять циклом рендеринга для каждого состояния.

        // ВРЕМЕННОЕ РЕШЕНИЕ:
        // Проверим, включена ли интерполяция.
        boolean isInterpolationEnabled = renderer.isFrameInterpolationEnabled();

        if (isInterpolationEnabled) {
            // Если интерполяция включена, мы не можем применить эффекты к промежуточным кадрам напрямую.
            // Попробуем рендерить основной кадр в буфер, применить эффекты, затем рендерить интерполяцию на экран.
            // Это НЕ ИДЕАЛЬНО, так как эффекты применяются к основному кадру, а не к результату интерполяции.
            // Или, наоборот, рендерим всё (включая интерполяцию) в буфер, затем применяем эффекты к финальному результату.

            // ВАРИАНТ 1 (предлагаемый): Рендерим всё (основной + промежуточные кадры) в буфер, затем применяем эффекты к результату.
            // Это требует изменения GLRenderer, чтобы он рендерил *в буфер*, а не на экран, когда есть эффекты.
            // renderer.drawFrame() -> рендерит всё в readBuffer
            // Затем применяем эффекты к readBuffer и выводим на экран.

            // ПРЕДПОЛОЖИМ, что renderer.drawFrame() теперь рендерит в текущий FBO (readBuffer если есть эффекты, иначе на экран).
            // Это требует изменения renderer.drawFrame()!
            // renderer.drawFrame(); // Это теперь рендерит основной кадр и интерполяцию в текущий FBO (readBuffer)

            // Но renderer.drawFrame() всё ещё вызывает performFrameInterpolation(), которая рендерит напрямую.
            // Поэтому нужно изменить GLRenderer.drawFrame(), чтобы при наличии эффектов он НЕ вызывал performFrameInterpolation(),
            // а возвращал бы список "состояний" или просто рендерил бы в буфер без интерполяции,
            // а интерполяцию и применение эффектов делал EffectComposer.

            // ВАРИАНТ 2 (альтернативное временное решение): Отключить эффекты при включенной интерполяции или наоборот.
            // Это просто, но не идеально для UX.

            // ПОПРОБУЕМ ВАРИАНТ 1: Предположим, что renderer.drawFrame() теперь рендерит всё (включая интерполяцию) в readBuffer.
            // Это требует, чтобы renderer.drawFrame() всегда рендерил в текущий FBO.
            // Мы уже привязали readBuffer выше, так что renderer.drawFrame() будет рендерить туда.
            renderer.drawFrame(); // Рендерит основной кадр в readBuffer

            // Теперь нужно применить эффекты к readBuffer.
            // Цикл по эффектам:
            for (int i = 0; i < effects.size(); i++) {
                Effect effect = effects.get(i);
                boolean renderToScreen = (i == effects.size() - 1); // Последний эффект рендерит на экран
                int targetFramebuffer = renderToScreen ? 0 : writeBuffer.getFramebuffer(); // Используем writeBuffer как промежуточный

                // Привязываем целевой фреймбуффер
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFramebuffer);

                // Устанавливаем viewport
                GLES20.glViewport(0, 0, renderer.surfaceWidth, renderer.surfaceHeight);

                // Очищаем целевой буфер
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                // Рендерим эффект
                renderEffect(effect);

                // Меняем буферы для следующей итерации (если не последняя)
                if (!renderToScreen) {
                     swapBuffers();
                }
            }
            // После цикла, финальный результат находится в нужном буфере (либо на экране, либо в writeBuffer, если он был последним таргетом, но renderToScreen был true для предпоследнего эффекта и т.д.)
            // В любом случае, последний вызов glBindFramebuffer установил нужный фреймбуффер.

        } else {
             // Если интерполяция выключена, используем старую логику
            renderer.drawFrame(); // Рендерит основной кадр

            // Iterate through each effect and render it
            for (Effect effect : effects) {
                Log.d(TAG, "Processing effect in loop: " + effect.getClass().getSimpleName()); // Включаем лог
                boolean renderToScreen = effect == effects.get(effects.size() - 1);
                int targetFramebuffer = renderToScreen ? 0 : writeBuffer.getFramebuffer();

                // Bind appropriate framebuffer
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFramebuffer);
//            Log.d(TAG, "Binding to " + (renderToScreen ? "screen" : "writeBuffer") + " framebuffer: " + targetFramebuffer);

                GLES20.glViewport(0, 0, renderer.surfaceWidth, renderer.surfaceHeight);
                // renderer.setViewportNeedsUpdate(true); // Не нужно здесь, viewport устанавливается выше
//            Log.d(TAG, "Viewport updated to size: " + renderer.surfaceWidth + "x" + renderer.surfaceHeight);

                // Clear the buffer
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//            Log.d(TAG, "Framebuffer cleared");

                // Render the effect
                renderEffect(effect);
//            Log.d(TAG, "Effect rendered: " + effect.getClass().getSimpleName());

                // Swap the read and write buffers
                if (!renderToScreen) { // Меняем буферы только если следующий эффект не рендерит на экран
                     swapBuffers();
                }
//            Log.d(TAG, "Buffers swapped");
            }
        }


        isRendering = false; // Reset flag after rendering
    }

    // Renders a single effect
    private void renderEffect(Effect effect) {
        Log.d(TAG, "renderEffect() called for: " + effect.getClass().getSimpleName()); // Включаем лог

        ShaderMaterial material = effect.getMaterial();
        if (material == null) {
            Log.e(TAG, "Material is null for effect: " + effect.getClass().getSimpleName()); // Включаем лог
            return;
        }

        Log.d(TAG, "About to call material.use() for: " + effect.getClass().getSimpleName()); // Включаем лог
        material.use(); // Вот тут должен вызваться use() DepthEffectMaterial
        Log.d(TAG, "material.use() called for: " + effect.getClass().getSimpleName()); // Включаем лог

        // Bind the quad vertices to the shader program
        renderer.getQuadVertices().bind(material.programId);
        Log.d(TAG, "Quad vertices bound to program ID: " + material.programId + " for: " + effect.getClass().getSimpleName()); // Включаем лог

        // Set uniform values
        material.setUniformVec2("resolution", renderer.surfaceWidth, renderer.surfaceHeight);
        Log.d(TAG, "Set resolution uniform: " + renderer.surfaceWidth + "x" + renderer.surfaceHeight + " for: " + effect.getClass().getSimpleName()); // Включаем лог

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Используем текстуру из readBuffer (последнего результата)
        int textureIdToUse = readBuffer.getTextureId(); // readBuffer содержит последний промежуточный результат
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIdToUse); // Привязываем readBuffer как текстуру
        Log.d(TAG, "Bound texture ID: " + textureIdToUse + " to GL_TEXTURE0 for: " + effect.getClass().getSimpleName()); // Включаем лог
        material.setUniformInt("screenTexture", 0);
        Log.d(TAG, "Set screenTexture uniform to 0 for: " + effect.getClass().getSimpleName()); // Включаем лог

        // Draw the quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, renderer.quadVertices.count());
        Log.d(TAG, "Quad drawn for: " + effect.getClass().getSimpleName()); // Включаем лог

        // Unbind the texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        Log.d(TAG, "Texture unbound from GL_TEXTURE0 after: " + effect.getClass().getSimpleName()); // Включаем лог
    }

    // Swaps the read and write buffers
    private void swapBuffers() {
        RenderTarget tmp = writeBuffer;
        writeBuffer = readBuffer;
        readBuffer = tmp;
//        Log.d(TAG, "swapBuffers() called. Buffers swapped.");
    }

    // Add a method to add the ToonEffect
    public synchronized void toggleToonEffect() {
        ToonEffect toonEffect = getEffect(ToonEffect.class);
        if (toonEffect != null) {
            removeEffect(toonEffect); // Remove if already present
            Log.d(TAG, "ToonEffect removed");
        } else {
            addEffect(new ToonEffect()); // Add if not present
            Log.d(TAG, "ToonEffect added");
        }
        renderer.xServerView.requestRender();
    }

}