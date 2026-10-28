package dev.sketchlive.core;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.*;

public class FastModeTest {
  private ClipTokenizer tokenizer() throws Exception {
    File assets = new File(System.getProperty("assetsDir"));
    try (Reader v =
            new InputStreamReader(
                new FileInputStream(new File(assets, "sdxs-vocab.json")),
                java.nio.charset.StandardCharsets.UTF_8);
        Reader m =
            new InputStreamReader(
                new FileInputStream(new File(assets, "sdxs-merges.txt")),
                java.nio.charset.StandardCharsets.UTF_8)) {
      return new ClipTokenizer(v, m, 49407);
    }
  }

  @Test
  public void sd1PaddingAndPunctuation() throws Exception {
    long[] ids = tokenizer().encode("Hello, world!");
    assertArrayEquals(new long[] {49406, 3306, 267, 1002, 256, 49407}, Arrays.copyOf(ids, 6));
    for (int i = 6; i < 77; i++) assertEquals(49407, ids[i]);
    assertEquals(49407, tokenizer().encode("")[1]);
  }

  @Test
  public void lineAndColorConditioning() {
    int size = 8, n = size * size;
    float[] white = new float[3 * n];
    Arrays.fill(white, 1);
    assertArrayEquals(new float[3 * n], SketchCondition.create(white, size), 0);
    for (int c = 0; c < 3; c++) white[c * n + 4 * size + 4] = -1;
    float[] ink = SketchCondition.create(white, size);
    assertEquals(1, ink[4 * size + 4], 0);
    assertEquals(0, ink[0], 0);
    float[] color = new float[3 * n];
    Arrays.fill(color, -1);
    for (int y = 0; y < size; y++) for (int x = size / 2; x < size; x++) color[y * size + x] = 1;
    float[] edges = SketchCondition.create(color, size);
    assertEquals(0, edges[size + 1], 0);
    assertTrue(edges[size + 3] > .9);
    assertTrue(edges[size + 4] > .9);
    assertEquals(edges[size + 3], edges[n + size + 3], 0);
  }

  @Test
  public void fastControlNetRealInference() throws Exception {
    String directory = System.getProperty("fastModelDir", "");
    Assume.assumeFalse("Pass -DfastModelDir for real SDXS inference", directory.isEmpty());
    int size = ModelProfile.FAST.size, n = size * size;
    BufferedImage original = ImageIO.read(new File("../docs/examples/robot-sketch.png"));
    BufferedImage sketch = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D graphics = sketch.createGraphics();
    graphics.drawImage(original, 0, 0, size, size, null);
    graphics.dispose();
    float[] rgb = new float[3 * n];
    for (int i = 0; i < n; i++) {
      int p = sketch.getRGB(i % size, i / size);
      rgb[i] = ((p >> 16) & 255) / 127.5f - 1;
      rgb[i + n] = ((p >> 8) & 255) / 127.5f - 1;
      rgb[i + 2 * n] = (p & 255) / 127.5f - 1;
    }
    try (DiffusionEngine engine =
        new DiffusionEngine(new File(directory), tokenizer(), 4, ModelProfile.FAST)) {
      long started = System.nanoTime();
      float[] result =
          engine.generate(
              rgb,
              "a small friendly orange robot standing, two glowing cyan eyes, detailed 3d render",
              .85f,
              7,
              false,
              System.out::println);
      System.out.println("FAST_COLD_SECONDS=" + (System.nanoTime() - started) / 1e9);
      assertEquals(rgb.length, result.length);
      double diff = 0;
      for (int i = 0; i < result.length; i++) {
        assertTrue(Float.isFinite(result[i]));
        diff += Math.abs(rgb[i] - result[i]);
      }
      assertTrue(diff / result.length > .1);
      save(result, size, "fast-robot.png");
      started = System.nanoTime();
      float[] repeat =
          engine.generate(
              rgb,
              "a small friendly orange robot standing, two glowing cyan eyes, detailed 3d render",
              .85f,
              7,
              false,
              s -> {});
      System.out.println("FAST_WARM_SECONDS=" + (System.nanoTime() - started) / 1e9);
      assertArrayEquals(result, repeat, 0.0001f);
      float[] blank = new float[rgb.length];
      Arrays.fill(blank, 1);
      float[] noSketch =
          engine.generate(
              blank,
              "a small friendly orange robot standing, two glowing cyan eyes, detailed 3d render",
              .85f,
              7,
              false,
              s -> {});
      double change = 0;
      for (int i = 0; i < result.length; i++) change += Math.abs(result[i] - noSketch[i]);
      assertTrue("Sketch must change the result", change / result.length > .03);
      save(noSketch, size, "fast-without-sketch.png");
      try {
        engine.generate(rgb, "test", .85f, 7, true, s -> {});
        fail("SDXS must reject unsupported two-step enhancement");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  private void save(float[] rgb, int size, String name) throws Exception {
    int n = size * size;
    BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    for (int i = 0; i < n; i++) {
      int r = channel(rgb[i]), g = channel(rgb[i + n]), b = channel(rgb[i + 2 * n]);
      out.setRGB(i % size, i / size, (r << 16) | (g << 8) | b);
    }
    File folder = new File("build/reports");
    folder.mkdirs();
    ImageIO.write(out, "png", new File(folder, name));
  }

  private int channel(float v) {
    return Math.min(255, Math.max(0, Math.round((v + 1) * 127.5f)));
  }
}
