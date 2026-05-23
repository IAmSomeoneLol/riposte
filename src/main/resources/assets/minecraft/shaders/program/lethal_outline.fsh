#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 texel = 1.0 / OutSize;

    // Sample
    vec4 center = texture(DiffuseSampler, texCoord);
    vec4 up    = texture(DiffuseSampler, texCoord + vec2(0.0, 2.0 * texel.y));
    vec4 down  = texture(DiffuseSampler, texCoord + vec2(0.0, -2.0 * texel.y));
    vec4 left  = texture(DiffuseSampler, texCoord + vec2(-2.0 * texel.x, 0.0));
    vec4 right = texture(DiffuseSampler, texCoord + vec2(2.0 * texel.x, 0.0));

    // Calculate Luminance for Grayscale
    float lumaCenter = dot(center.rgb, vec3(0.299, 0.587, 0.114));
    float lumaUp     = dot(up.rgb, vec3(0.299, 0.587, 0.114));
    float lumaDown   = dot(down.rgb, vec3(0.299, 0.587, 0.114));
    float lumaLeft   = dot(left.rgb, vec3(0.299, 0.587, 0.114));
    float lumaRight  = dot(right.rgb, vec3(0.299, 0.587, 0.114));

    // Detect edges
    float edge = abs(lumaCenter - lumaUp) + abs(lumaCenter - lumaDown) +
                 abs(lumaCenter - lumaLeft) + abs(lumaCenter - lumaRight);

    // If an edge is found, draw black; otherwise, draw the grayscale pixel
    if (edge > 0.08) {
        fragColor = vec4(0.0, 0.0, 0.0, center.a);
    } else {
        fragColor = vec4(vec3(lumaCenter), center.a);
    }
}