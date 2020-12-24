#version 430 core
out vec2 texcoord;
void main(void) {
  vec2 vertex = vec2((gl_VertexID & 1) << 2, (gl_VertexID & 2) << 1) - vec2(1.0);
  gl_Position = vec4(vertex, 0.0, 1.0);
  texcoord = vertex * 0.5 + vec2(0.5, 0.5);
}