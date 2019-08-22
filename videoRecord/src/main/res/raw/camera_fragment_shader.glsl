#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 ftPosition;
uniform samplerExternalOES vTexture;

void main(){
    vec4 nColor = texture2D(vTexture, ftPosition);
    gl_FragColor = nColor;
}