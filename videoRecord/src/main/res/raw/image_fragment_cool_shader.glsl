precision mediump float;

uniform sampler2D vTexture;
varying vec2 aCoordinate;

void fixColor(vec4 deltaColor){
    deltaColor.r = max(min(deltaColor.r, 1.0), 0.0);
    deltaColor.g = max(min(deltaColor.g, 1.0), 0.0);
    deltaColor.b = max(min(deltaColor.b, 1.0), 0.0);
}

void main(){
    vec4 nColor = texture2D(vTexture, aCoordinate);
    vec4 deltaColor = nColor + vec4(0.0, 0.0, 0.2, 0.0);
    fixColor(deltaColor);
    gl_FragColor = deltaColor;
}