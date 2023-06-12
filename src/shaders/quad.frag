#version 430 core
uniform sampler2D tex;
in vec2 texcoord;
layout(location = 0) out vec4 color;
void main(void) {
  vec4 r = texture(tex, texcoord);
  color = vec4(r.x, r.y, r.z, 1.0f);
}