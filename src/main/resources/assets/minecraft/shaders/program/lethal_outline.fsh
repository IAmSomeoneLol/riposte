#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    // Calculate the size of a single pixel on the screen
    vec2 texel = 1.0 / OutSize;

    // Get the color of the current pixel
    vec4 center = texture(DiffuseSampler, texCoord);

    // Look at the pixels directly above, below, left, and right
    vec4 up    = texture(DiffuseSampler, texCoord + vec2(0.0, texel.y));
    vec4 down  = texture(DiffuseSampler, texCoord + vec2(0.0, -texel.y));
    vec4 left  = texture(DiffuseSampler, texCoord + vec2(-texel.x, 0.0));
    vec4 right = texture(DiffuseSampler, texCoord + vec2(texel.x, 0.0));

    // Calculate how much the colors change between the center and its neighbors
    float diff = length(abs(center - up) + abs(center - down) + abs(center - left) + abs(center - right));

    // Convert the center pixel to grayscale using standard luminosity math
    float luma = dot(center.rgb, vec3(0.299, 0.587, 0.114));
    vec3 finalColor = vec3(luma);

    // If the color difference is sharp enough (an edge), paint it pitch black!
    // You can lower 0.3 to make MORE outlines, or raise it to make LESS outlines.
    if (diff > 0.3) {
        finalColor = vec3(0.0);
    }

    fragColor = vec4(finalColor, center.a);
}