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
    nColor += texture2D(vTexture, vec2(aCoordinate.x - 0.006,aCoordinate.y - 0.006));
    nColor += texture2D(vTexture, vec2(aCoordinate.x - 0.006,aCoordinate.y + 0.006));
    nColor += texture2D(vTexture, vec2(aCoordinate.x + 0.006,aCoordinate.y - 0.006));
    nColor += texture2D(vTexture, vec2(aCoordinate.x + 0.006,aCoordinate.y + 0.006));
    nColor += texture2D(vTexture, vec2(aCoordinate.x - 0.004,aCoordinate.y - 0.004));
    nColor += texture2D(vTexture, vec2(aCoordinate.x - 0.004,aCoordinate.y + 0.004));
    nColor += texture2D(vTexture, vec2(aCoordinate.x + 0.004,aCoordinate.y - 0.004));
    nColor += texture2D(vTexture, vec2(aCoordinate.x + 0.004,aCoordinate.y + 0.004));
    nColor += texture2D(vTexture, vec2(aCoordinate.x - 0.002,aCoordinate.y - 0.002));
    nColor += texture2D(vTexture, vec2(aCoordinate.x - 0.002,aCoordinate.y + 0.002));
    nColor += texture2D(vTexture, vec2(aCoordinate.x + 0.002,aCoordinate.y - 0.002));
    nColor += texture2D(vTexture, vec2(aCoordinate.x + 0.002,aCoordinate.y + 0.002));
    nColor /= 13.0;
    fixColor(nColor);
    gl_FragColor=nColor;
}