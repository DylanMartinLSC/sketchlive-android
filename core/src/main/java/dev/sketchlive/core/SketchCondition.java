package dev.sketchlive.core;

/** SDXS Sketch expects white lines on black, RGB [0,1]. No remote preprocessor is needed. */
public final class SketchCondition {
  private SketchCondition() {}

  public static float[] create(float[] image, int size) {
    int pixels = size * size;
    if (image.length != 3 * pixels) throw new IllegalArgumentException("Invalid sketch shape");
    float[] output = new float[image.length];
    // Ink on white paper: invert the actual strokes, preserving their full width.
    boolean ink = true;
    for (int i = 0; i < pixels; i++)
      if (Math.abs(image[i] - image[i + pixels]) > .04f
          || Math.abs(image[i] - image[i + 2 * pixels]) > .04f) {
        ink = false;
        break;
      }
    float corners = (image[0] + image[size - 1] + image[pixels - size] + image[pixels - 1]) / 4;
    if (ink && corners > .8f) {
      for (int i = 0; i < pixels; i++) {
        float value = Math.min(1, Math.max(0, (1 - image[i]) / 2));
        for (int c = 0; c < 3; c++) output[c * pixels + i] = value;
      }
      return output;
    }
    // Filled/color sketches: per-channel Sobel outlines retain boundaries even at equal luminance.
    for (int y = 0; y < size; y++)
      for (int x = 0; x < size; x++) {
        float edge = 0;
        for (int c = 0; c < 3; c++) {
          float gx = 0, gy = 0;
          for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
              int sx = Math.max(0, Math.min(size - 1, x + dx)),
                  sy = Math.max(0, Math.min(size - 1, y + dy));
              float v = (image[c * pixels + sy * size + sx] + 1) / 2;
              gx += v * dx * (dy == 0 ? 2 : 1);
              gy += v * dy * (dx == 0 ? 2 : 1);
            }
          edge = Math.max(edge, (float) Math.sqrt(gx * gx + gy * gy) * 2);
        }
        edge = Math.min(1, edge);
        for (int c = 0; c < 3; c++) output[c * pixels + y * size + x] = edge;
      }
    return output;
  }
}
