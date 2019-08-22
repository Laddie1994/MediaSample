attribute vec4 vPosition;
attribute vec2 fPosition;
varying vec2 ftPosition;
uniform mat4 mMatrix;
uniform mat4 vCoordMatrix;

void main(){
    //(vCoordMatrix * vec4(fPosition, 0, 1)).xy
    ftPosition = fPosition;
    gl_Position = mMatrix * vPosition;
}