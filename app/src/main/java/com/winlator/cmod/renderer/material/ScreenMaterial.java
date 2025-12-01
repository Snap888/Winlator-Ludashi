package com.winlator.cmod.renderer.material;

public class ScreenMaterial extends ShaderMaterial {
    private static final String VERTEX_SHADER = String.join("\n",
        "attribute vec2 position;",
        "varying vec2 vUV;",
        "void main() {",
        "    // Input 'position' is in UV space [0,1] for both x and y",
        "    vUV = position;",
        "    // Convert UV [0,1] to NDC [-1,1]",
        "    gl_Position = vec4(position * 2.0 - 1.0, 0.0, 1.0);",
        "}"
    );

    public ScreenMaterial() {
        super();
        // Only include truly universal uniforms.
        setUniformNames("screenTexture");
    }

    @Override
    protected String getVertexShader() {
        return VERTEX_SHADER;
    }
}