precision mediump float;

uniform sampler2D vTexture;
varying vec2 aCoordinate;
varying vec4 aPosition;

void main(){
    vec4 nColor = texture2D(vTexture, aCoordinate);
    float dis = distance(vec2(aPosition.x, aPosition.y / 0.5), vec2(0.0, 0.0));
    if(dis < 0.4){
        nColor = texture2D(vTexture, vec2(aCoordinate.x / 2.0 + 0.25, aCoordinate.y / 2.0 + 0.25));
    }
    gl_FragColor = nColor;
}