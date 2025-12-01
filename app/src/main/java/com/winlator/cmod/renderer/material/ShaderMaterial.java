package com.winlator.cmod.renderer.material;

import android.graphics.Color;
import android.opengl.GLES20;
import android.util.Log;

import androidx.collection.ArrayMap;

public class ShaderMaterial {
    public int programId;
    private final ArrayMap<String, Integer> uniforms = new ArrayMap<>();

    public void setUniformNames(String... names) {
        uniforms.clear();
        for (String name : names) {
            uniforms.put(name, -1);
        }
    }

    protected static int compileShaders(String vertexShader, String fragmentShader) {
        int programId = GLES20.glCreateProgram();
        int[] compiled = new int[1];

        // Compile vertex shader
        int vertexShaderId = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShaderId, vertexShader);
        GLES20.glCompileShader(vertexShaderId);

        GLES20.glGetShaderiv(vertexShaderId, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(vertexShaderId);
            GLES20.glDeleteShader(vertexShaderId);
            throw new RuntimeException("Could not compile vertex shader:\n" + log);
        }
        GLES20.glAttachShader(programId, vertexShaderId);

        // Compile fragment shader
        int fragmentShaderId = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShaderId, fragmentShader);
        GLES20.glCompileShader(fragmentShaderId);

        GLES20.glGetShaderiv(fragmentShaderId, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(fragmentShaderId);
            GLES20.glDeleteShader(vertexShaderId);
            GLES20.glDeleteShader(fragmentShaderId);
            GLES20.glDeleteProgram(programId);
            throw new RuntimeException("Could not compile fragment shader:\n" + log);
        }
        GLES20.glAttachShader(programId, fragmentShaderId);

        // Link program
        GLES20.glLinkProgram(programId);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(programId);
            GLES20.glDeleteShader(vertexShaderId);
            GLES20.glDeleteShader(fragmentShaderId);
            GLES20.glDeleteProgram(programId);
            throw new RuntimeException("Could not link shader program:\n" + log);
        }

        // Clean up shaders
        GLES20.glDeleteShader(vertexShaderId);
        GLES20.glDeleteShader(fragmentShaderId);

        return programId;
    }

    protected String getVertexShader() {
        return "";
    }

    protected String getFragmentShader() {
        return "";
    }

    public void use() {
        if (programId == 0) {
            programId = compileShaders(getVertexShader(), getFragmentShader());
            // Cache all uniform locations once
            for (int i = 0; i < uniforms.size(); i++) {
                String name = uniforms.keyAt(i);
                int location = GLES20.glGetUniformLocation(programId, name);
                uniforms.setValueAt(i, location);
            }
        }
        GLES20.glUseProgram(programId);
    }

    public int getUniformLocation(String name) {
        Integer location = uniforms.get(name);
        if (location == null) {
            Log.e("ShaderMaterial", "Uniform '" + name + "' is not registered in setUniformNames().");
            return -1;
        }
        if (location == -1) {
            Log.w("ShaderMaterial", "Uniform '" + name + "' is not active in the shader (location = -1).");
        }
        return location;
    }

    public void destroy() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId);
            programId = 0;
        }
    }

    public void setUniformVec2(String uniformName, float x, float y) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            GLES20.glUniform2f(location, x, y);
        }
    }

    public void setUniformVec3(String uniformName, float x, float y, float z) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            GLES20.glUniform3f(location, x, y, z);
        }
    }

    public void setUniformInt(String uniformName, int value) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            GLES20.glUniform1i(location, value);
        }
    }

    public void setUniformFloat(String uniformName, float value) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            GLES20.glUniform1f(location, value);
        }
    }

    public void setUniformFloatArray(String uniformName, float[] values) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            GLES20.glUniform1fv(location, values.length, values, 0);
        }
    }

    public void setUniformColor(String uniformName, int color) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            float r = Color.red(color) / 255.0f;
            float g = Color.green(color) / 255.0f;
            float b = Color.blue(color) / 255.0f;
            GLES20.glUniform3f(location, r, g, b);
        }
    }
}