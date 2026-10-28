package dev.sketchlive.android;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.*;
import org.junit.runner.RunWith;

/**
 * Reproducible real-app demos. Run with instrumentation argument: -e demo robot|cottage|lighthouse.
 */
@RunWith(AndroidJUnit4.class)
public class DemoRecorder {
  private ActivityScenario<MainActivity> scenario;
  private File directory;
  private int frame;

  private Object field(Object object, String name) {
    try {
      Field f = object.getClass().getDeclaredField(name);
      f.setAccessible(true);
      return f.get(object);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void save(Bitmap image, String name) throws Exception {
    try (OutputStream out = new FileOutputStream(new File(directory, name))) {
      assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
  }

  private void capture(int repeat) throws Exception {
    for (int i = 0; i < repeat; i++) {
      Thread.sleep(160);
      Bitmap screen =
          InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
      assertNotNull(screen);
      save(screen, String.format(java.util.Locale.ROOT, "%04d.png", frame++));
      screen.recycle();
    }
  }

  private void brush(String color, int size) {
    scenario.onActivity(
        a -> {
          SketchView s = (SketchView) field(a, "sketch");
          s.color = Color.parseColor(color);
          s.brushSize = size * 1.5f;
          s.eraser = false;
        });
  }

  private void stroke(float... points) throws Exception {
    scenario.onActivity(
        a -> {
          SketchView view = (SketchView) field(a, "sketch");
          long start = SystemClock.uptimeMillis();
          for (int i = 0; i < points.length; i += 2) {
            if (i == 0) event(view, start, MotionEvent.ACTION_DOWN, points[i], points[i + 1]);
            else
              for (int j = 1; j <= 8; j++)
                event(
                    view,
                    start,
                    MotionEvent.ACTION_MOVE,
                    points[i - 2] + (points[i] - points[i - 2]) * j / 8,
                    points[i - 1] + (points[i + 1] - points[i - 1]) * j / 8);
          }
          event(
              view,
              start,
              MotionEvent.ACTION_UP,
              points[points.length - 2],
              points[points.length - 1]);
        });
    capture(1);
  }

  private void event(SketchView view, long start, int type, float x, float y) {
    MotionEvent e =
        MotionEvent.obtain(
            start, SystemClock.uptimeMillis(), type, x * view.getWidth(), y * view.getHeight(), 0);
    view.dispatchTouchEvent(e);
    e.recycle();
  }

  private void waitForResult() throws Exception {
    AtomicReference<Boolean> busy = new AtomicReference<>(true);
    long deadline = System.currentTimeMillis() + 240000;
    do {
      capture(1);
      scenario.onActivity(a -> busy.set((Boolean) field(a, "busy")));
    } while (busy.get() && System.currentTimeMillis() < deadline);
    assertFalse("Generation timed out", busy.get());
    scenario.onActivity(
        a ->
            assertTrue(
                ((TextView) field(a, "status"))
                    .getText()
                    .toString()
                    .contains("generated on this phone")));
  }

  @Test
  public void recordExample() throws Exception {
    String name = InstrumentationRegistry.getArguments().getString("demo", "");
    Assume.assumeTrue("Pass -e demo to record an example", !name.isEmpty());
    boolean fast =
        "fast".equals(InstrumentationRegistry.getArguments().getString("mode", "quality"));
    dev.sketchlive.core.ModelProfile profile =
        fast ? dev.sketchlive.core.ModelProfile.FAST : dev.sketchlive.core.ModelProfile.QUALITY;
    assertTrue(
        new ModelStore(InstrumentationRegistry.getInstrumentation().getTargetContext(), profile)
            .ready());
    directory =
        new File(
            InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getExternalFilesDir(null),
            "demos/" + (fast ? "fast-" : "") + name);
    assertTrue(directory.exists() || directory.mkdirs());
    String prompt;
    switch (name) {
      case "robot":
        prompt =
            "a small friendly orange robot standing, two glowing cyan eyes, rounded metal body,"
                + " detailed 3d render, studio lighting";
        break;
      case "lighthouse":
        prompt =
            "a lighthouse on a rocky island, stormy teal ocean, golden beacon, dramatic clouds,"
                + " cinematic detailed painting";
        break;
      case "cottage":
        prompt =
            "a magical red mushroom cottage, glowing windows, mossy forest, fireflies, cinematic"
                + " fantasy art";
        break;
      default:
        throw new IllegalArgumentException(name);
    }
    try (ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
      scenario = activity;
      scenario.onActivity(
          a -> {
            a.selectProfile(profile);
            ((EditText) field(a, "prompt")).setText(prompt);
            ((EditText) field(a, "seedInput")).setText(name.equals("robot") ? "7" : "42");
            ((SeekBar) field(a, "creativity"))
                .setProgress(fast ? 65 : (name.equals("robot") ? 65 : 70));
            ((Spinner) field(a, "style")).setSelection(name.equals("robot") ? 4 : 1);
            ((Switch) field(a, "live")).setChecked(false);
            SketchView sketch = (SketchView) field(a, "sketch");
            sketch.clear();
            ((Button) field(a, "drawTab")).performClick();
            android.view.View stage = (android.view.View) field(a, "stage");
            ((ScrollView) stage.getParent().getParent())
                .scrollTo(
                    0,
                    Math.max(
                        0,
                        stage.getTop()
                            - (int) (180 * a.getResources().getDisplayMetrics().density)));
          });
      if (name.equals("robot")) robot();
      else if (name.equals("lighthouse")) lighthouse();
      else cottage();
      scenario.onActivity(
          a -> {
            try {
              Bitmap sketch = ((SketchView) field(a, "sketch")).snapshot();
              save(sketch, "sketch.png");
              sketch.recycle();
            } catch (Exception e) {
              throw new AssertionError(e);
            }
          });
      capture(3);
      scenario.onActivity(a -> ((Button) field(a, "generate")).performClick());
      waitForResult();
      capture(6);
      scenario.onActivity(
          a -> {
            try {
              save((Bitmap) field(a, "latest"), "preview.png");
            } catch (Exception e) {
              throw new AssertionError(e);
            }
            ((Button) field(a, "drawTab")).performClick();
          });
      capture(3);
      if (!fast) {
        scenario.onActivity(a -> ((Button) field(a, "enhance")).performClick());
        waitForResult();
        capture(8);
        scenario.onActivity(
            a -> {
              try {
                save((Bitmap) field(a, "latest"), "enhanced.png");
              } catch (Exception e) {
                throw new AssertionError(e);
              }
            });
      } else capture(8);
      try (Writer out = new FileWriter(new File(directory, "recording.txt"))) {
        out.write(
            "frames="
                + frame
                + "\nprompt="
                + prompt
                + "\nmode="
                + (fast ? "Fast SDXS Sketch" : "Quality SD-Turbo")
                + "\nstyle="
                + (name.equals("robot") ? "3D render" : "Cinematic")
                + "\ncreativity="
                + (fast ? "0.85" : (name.equals("robot") ? "0.85" : "0.90"))
                + "\nseed="
                + (name.equals("robot") ? "7" : "42")
                + "\nAndroid 15 x86_64 emulator; CPU inference; airplane mode\n");
      }
    }
  }

  private void cottage() throws Exception {
    brush("#142b25", 14);
    scenario.onActivity(a -> ((SketchView) field(a, "sketch")).fill());
    capture(1);
    brush("#365b32", 100);
    stroke(.05f, .92f, .32f, .85f, .64f, .89f, .95f, .90f);
    brush("#d9bd88", 110);
    stroke(.42f, .72f, .42f, .47f, .58f, .47f, .58f, .72f);
    brush("#b52d35", 95);
    stroke(.23f, .40f, .36f, .24f, .50f, .18f, .64f, .24f, .77f, .40f, .23f, .40f);
    stroke(.38f, .32f, .62f, .32f);
    brush("#ffedb5", 20);
    stroke(.36f, .32f, .361f, .321f);
    stroke(.52f, .24f, .521f, .241f);
    stroke(.65f, .34f, .651f, .341f);
    brush("#542b28", 40);
    stroke(.50f, .77f, .50f, .63f);
    brush("#ffb829", 25);
    stroke(.37f, .55f, .371f, .551f);
    stroke(.63f, .55f, .631f, .551f);
  }

  private void robot() throws Exception {
    brush("#173442", 14);
    scenario.onActivity(a -> ((SketchView) field(a, "sketch")).fill());
    capture(1);
    brush("#bd672d", 45);
    stroke(.38f, .78f, .36f, .88f);
    stroke(.62f, .78f, .64f, .88f);
    brush("#df8436", 110);
    stroke(.43f, .69f, .43f, .51f, .57f, .51f, .57f, .69f);
    brush("#dc7a2c", 110);
    stroke(.37f, .30f, .63f, .30f);
    brush("#e38c3e", 35);
    stroke(.30f, .52f, .23f, .65f);
    stroke(.70f, .52f, .77f, .65f);
    brush("#24394c", 40);
    stroke(.38f, .31f, .62f, .31f);
    brush("#68e8fb", 21);
    stroke(.40f, .31f, .401f, .311f);
    stroke(.60f, .31f, .601f, .311f);
    brush("#8fd4dc", 12);
    stroke(.50f, .18f, .50f, .10f);
    brush("#fff1b0", 20);
    stroke(.50f, .10f, .501f, .101f);
    brush("#3c4c54", 12);
    stroke(.43f, .62f, .57f, .62f);
  }

  private void lighthouse() throws Exception {
    brush("#294a60", 14);
    scenario.onActivity(a -> ((SketchView) field(a, "sketch")).fill());
    capture(1);
    brush("#146574", 100);
    stroke(.05f, .87f, .35f, .82f, .65f, .88f, .95f, .80f);
    brush("#363f40", 90);
    stroke(.18f, .84f, .40f, .73f, .65f, .76f, .81f, .89f);
    brush("#e5d5b4", 80);
    stroke(.48f, .69f, .48f, .34f, .53f, .34f, .53f, .69f);
    brush("#a34737", 30);
    stroke(.44f, .48f, .56f, .48f);
    stroke(.44f, .62f, .56f, .62f);
    brush("#25343c", 55);
    stroke(.43f, .28f, .57f, .28f);
    brush("#ffdc77", 24);
    stroke(.46f, .28f, .54f, .28f);
    brush("#9a3f32", 27);
    stroke(.41f, .23f, .50f, .17f, .59f, .23f);
    brush("#bce6dd", 9);
    stroke(.07f, .78f, .23f, .77f);
    stroke(.73f, .94f, .93f, .93f);
  }
}
