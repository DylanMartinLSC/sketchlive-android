package dev.sketchlive.android;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import dev.sketchlive.core.ModelProfile;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FastModeDeviceTest {
  private Object field(Object target, String name) {
    try {
      Field f = target.getClass().getDeclaredField(name);
      f.setAccessible(true);
      return f.get(target);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private String waitIdle(ActivityScenario<MainActivity> activity) throws Exception {
    AtomicReference<Boolean> busy = new AtomicReference<>(true);
    AtomicReference<String> status = new AtomicReference<>("");
    long end = System.currentTimeMillis() + 180000;
    do {
      Thread.sleep(100);
      activity.onActivity(
          a -> {
            busy.set((Boolean) field(a, "busy"));
            status.set(((TextView) field(a, "status")).getText().toString());
          });
    } while (busy.get() && System.currentTimeMillis() < end);
    assertFalse("Inference timed out", busy.get());
    return status.get();
  }

  @Test
  public void fastAndQualitySwitchAndCancelOffline() throws Exception {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Assume.assumeTrue(
        "Install Fast and Quality packs",
        new ModelStore(context, ModelProfile.FAST).ready() && new ModelStore(context).ready());
    File report = new File(context.getExternalFilesDir(null), "fast-validation");
    assertTrue(report.exists() || report.mkdirs());
    StringBuilder timings = new StringBuilder();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            a.selectProfile(ModelProfile.FAST);
            assertFalse(((Button) field(a, "enhance")).isEnabled());
            ((EditText) field(a, "prompt"))
                .setText(
                    "a small friendly orange robot standing, two glowing cyan eyes, rounded metal"
                        + " body, detailed 3d render, studio lighting");
            ((EditText) field(a, "seedInput")).setText("7");
            ((Spinner) field(a, "style")).setSelection(4);
            ((SeekBar) field(a, "creativity")).setProgress(65);
            Bitmap input =
                BitmapFactory.decodeFile(new File(a.getFilesDir(), "fixture-robot.png").toString());
            assertNotNull(input);
            ((SketchView) field(a, "sketch")).place(input);
            input.recycle();
            ((Button) field(a, "generate")).performClick();
            assertFalse(((Spinner) field(a, "mode")).isEnabled());
            a.selectProfile(ModelProfile.QUALITY);
            assertEquals(ModelProfile.FAST, ((ModelStore) field(a, "store")).profile);
          });
      String cold = waitIdle(scenario);
      assertTrue(cold, cold.contains("generated on this phone"));
      timings.append("FAST_COLD: ").append(cold).append('\n');
      scenario.onActivity(
          a -> {
            Bitmap output = (Bitmap) field(a, "latest");
            assertEquals(256, output.getWidth());
            save(output, new File(report, "fast-robot.png"));
            ((Button) field(a, "generate")).performClick();
          });
      String warm = waitIdle(scenario);
      assertTrue(warm, warm.contains("generated on this phone"));
      timings.append("FAST_WARM: ").append(warm).append('\n');
      scenario.onActivity(
          a -> {
            ((Button) field(a, "generate")).performClick();
            ((Button) field(a, "stop")).performClick();
          });
      assertEquals("Generation stopped", waitIdle(scenario));
      scenario.onActivity(
          a -> {
            a.selectProfile(ModelProfile.QUALITY);
            assertTrue(((Button) field(a, "enhance")).isEnabled());
            ((SeekBar) field(a, "creativity")).setProgress(65);
            ((Button) field(a, "generate")).performClick();
          });
      String quality = waitIdle(scenario);
      assertTrue(quality, quality.contains("generated on this phone"));
      timings.append("QUALITY_COLD: ").append(quality).append('\n');
      scenario.onActivity(
          a -> {
            assertEquals(384, ((Bitmap) field(a, "latest")).getWidth());
            ((Button) field(a, "generate")).performClick();
          });
      String qualityWarm = waitIdle(scenario);
      assertTrue(qualityWarm, qualityWarm.contains("generated on this phone"));
      timings.append("QUALITY_WARM: ").append(qualityWarm).append('\n');
      scenario.onActivity(
          a -> {
            ((SeekBar) field(a, "creativity")).setProgress(45);
            a.selectProfile(ModelProfile.FAST);
            assertFalse(((Button) field(a, "enhance")).isEnabled());
            assertEquals(65, ((SeekBar) field(a, "creativity")).getProgress());
            a.selectProfile(ModelProfile.QUALITY);
            assertEquals(45, ((SeekBar) field(a, "creativity")).getProgress());
            a.selectProfile(ModelProfile.FAST);
          });
    }
    try (Writer out = new FileWriter(new File(report, "timings.txt"))) {
      out.write("Android 15 x86_64 emulator, desktop host CPU; not phone FPS\n" + timings);
    }
  }

  private void save(Bitmap bitmap, File target) {
    try (OutputStream out = new FileOutputStream(target)) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
