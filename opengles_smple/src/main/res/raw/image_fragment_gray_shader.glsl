precision mediump float;

uniform sampler2D vTexture;
varying vec2 aCoordinate;

void main(){
    vec4 dColor = texture2D(vTexture, aCoordinate);
    float c = dColor.r * 0.30 + dColor.g * 0.59 + dColor.b * 0.11;
    gl_FragColor = vec4(c, c, c, dColor.a);
}