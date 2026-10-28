package dev.sketchlive.core;

import static org.junit.Assert.*;

import java.util.Set;
import org.junit.Test;

public class ContentSafetyTest {
  private ContentSafety.Category category(String prompt) {
    ContentSafety.Verdict v = ContentSafety.check(prompt);
    return v == null ? null : v.category;
  }

  @Test
  public void allowsEverydayPrompts() {
    for (String prompt :
        new String[] {
          "a magical red mushroom cottage, glowing windows, mossy forest, fireflies",
          "friendly orange robot with glowing cyan eyes, 3d render",
          "stormy lighthouse on rocky coast, lightning, teal waves",
          "a girl flying a kite in a sunny park",
          "kids building a sandcastle at the beach",
          "chicken breast on a grill, food photography",
          "map of Sussex and Essex, watercolor",
          "a butterfly on a sunflower",
          "knight battling a dragon, epic fantasy",
          "Egyptian goddess statue, 3d render",
          "", "   "
        })
      assertNull(prompt, category(prompt));
  }

  @Test
  public void blocksSexualContentIncludingObfuscation() {
    for (String prompt :
        new String[] {
          "nude woman", "N.U.D.E portrait", "n u d e", "s3xy pose", "p0rn", "NSFW art",
          "photo of her undressed", "woman without clothes", "r34 anime", "Nakéd figure"
        })
      assertEquals(prompt, ContentSafety.Category.SEXUAL, category(prompt));
  }

  @Test
  public void blocksMinorsInSuggestiveContexts() {
    assertEquals(ContentSafety.Category.MINOR, category("teen girl in bikini"));
    assertEquals(ContentSafety.Category.MINOR, category("schoolgirl, seductive look"));
    assertEquals(ContentSafety.Category.MINOR, category("loli character"));
  }

  @Test
  public void blocksOtherRestrictedCategories() {
    assertEquals(ContentSafety.Category.GORE, category("decapitated soldier"));
    assertEquals(ContentSafety.Category.SELF_HARM, category("person cutting wrists"));
    assertEquals(ContentSafety.Category.HATE, category("flag with a swastika"));
    assertEquals(ContentSafety.Category.SEXUAL_VIOLENCE, category("sexual assault scene"));
    assertEquals(ContentSafety.Category.DECEPTIVE, category("face swap my neighbor"));
  }

  @Test
  public void reportedPromptsAreRefused() {
    String prompt = "Ominous   Clown!";
    Set<String> reported = Set.of(ContentSafety.normalize(prompt));
    assertEquals(
        ContentSafety.Category.REPORTED, ContentSafety.check("ominous clown!", reported).category);
    assertNull(ContentSafety.check("ominous clown", Set.of()));
    // Found in emulator testing: punctuation must not change the reported-prompt key.
    Set<String> cottage = Set.of(ContentSafety.normalize("red cottage, mossy forest, fireflies"));
    assertNotNull(ContentSafety.check("Red cottage mossy forest, fireflies!", cottage));
    assertTrue(ContentSafety.check("nude", Set.of()).message().contains("can't create"));
  }
}
