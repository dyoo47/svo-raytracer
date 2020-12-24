#version 430 core
uniform sampler2D tex;
in vec2 texcoord;
layout(location = 0) out vec4 color;
void main(void) {
  color = texture2D(tex, texcoord);
}