package dev.sketchlive.core;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Prompt safeguard required by Google Play's AI-generated content policy. The models have no
 * safety checker and no negative prompt, so restricted requests are refused before inference. The
 * filter is deliberately conservative; false positives are preferable to generating restricted
 * content. Matching is on normalized whole words, so "Sussex" or "chicken breast" are not blocked.
 */
public final class ContentSafety {
  public enum Category {
    SEXUAL("sexual or nude content"),
    MINOR("content that sexualizes or endangers minors"),
    SEXUAL_VIOLENCE("sexual violence"),
    GORE("graphic violence or gore"),
    SELF_HARM("self-harm"),
    HATE("hateful symbols or extremism"),
    DECEPTIVE("deepfakes or edits of real people"),
    REPORTED("a prompt you reported earlier");
    public final String description;

    Category(String description) {
      this.description = description;
    }
  }

  /** Null when allowed. */
  public static final class Verdict {
    public final Category category;
    public final String term;

    Verdict(Category category, String term) {
      this.category = category;
      this.term = term;
    }

    public String message() {
      return "SketchLive can't create "
          + category.description
          + ". Please change your description.";
    }
  }

  private static final Pattern WORD = Pattern.compile("[a-z]+");
  private static final Map<String, Category> TERMS = new HashMap<>();
  private static final Map<String, Category> PREFIXES = new LinkedHashMap<>();
  private static final Set<String> MINORS =
      Set.of(
          "child", "children", "kid", "kids", "kiddie", "toddler", "toddlers", "infant", "baby",
          "babies", "minor", "minors", "underage", "teen", "teens", "teenage", "teenager",
          "teenagers", "preteen", "tween", "schoolgirl", "schoolgirls", "schoolboy", "schoolboys",
          "loli", "lolita", "shota", "boy", "boys", "girl", "girls", "youth",
          "juvenile", "daughter");
  private static final Set<String> SUGGESTIVE =
      Set.of(
          "bikini", "swimsuit", "swimwear", "underwear", "panties", "panty", "bra", "bras",
          "thong", "lingerie", "bath", "bathing", "bathtub", "shower", "showering", "bedroom",
          "kiss", "kissing", "seductive", "sensual", "provocative", "suggestive", "intimate",
          "lover", "lovers", "skimpy", "revealing", "pinup");

  static {
    add(
        Category.SEXUAL,
        "nude", "nudes", "nudity", "nudist", "naked", "nsfw", "porn", "porno", "pornographic",
        "pornography", "sex", "sexy", "sexual", "sexually", "sexualized", "erotic", "erotica",
        "hentai", "ecchi", "lewd", "xxx", "topless", "bottomless", "nipple", "nipples", "areola",
        "genital", "genitals", "genitalia", "penis", "penises", "vagina", "vaginas", "vulva",
        "boobs", "boobies", "tits", "titties", "breasts", "buttocks", "lingerie",
        "orgasm", "fetish", "bdsm", "bondage", "stripper", "striptease", "onlyfans", "playboy",
        "cum", "blowjob", "intercourse", "unclothed", "unclad", "disrobed", "seminude", "horny",
        "aroused", "milf", "thong", "upskirt", "camgirl", "r34", "hardcore", "softcore");
    addPrefix(Category.SEXUAL, "masturbat", "undress", "pornstar", "orgy", "orgies", "fellat");
    add(Category.MINOR, "loli", "lolicon", "shota", "shotacon", "jailbait", "pedo", "pedophile",
        "paedophile", "cp");
    add(Category.SEXUAL_VIOLENCE, "rape", "raped", "raping", "rapist", "molest", "molested",
        "molestation", "grope", "groped", "groping", "incest");
    add(Category.GORE, "gore", "gory", "gruesome", "beheading", "beheaded", "decapitated",
        "disemboweled", "disembowelled", "entrails", "torture", "tortured",
        "massacre", "lynching", "lynched");
    addPrefix(Category.GORE, "decapitat", "dismember", "mutilat", "disembowel", "eviscerat");
    add(Category.SELF_HARM, "suicide", "suicidal");
    add(Category.HATE, "swastika", "swastikas", "nazi", "nazis", "kkk", "hitler", "neonazi");
    add(Category.DECEPTIVE, "deepfake", "deepfakes", "faceswap");
  }

  private static final String[][] PHRASES = {
    {"no clothes", "SEXUAL"}, {"without clothes", "SEXUAL"}, {"without clothing", "SEXUAL"},
    {"no clothing", "SEXUAL"}, {"remove clothes", "SEXUAL"}, {"remove her clothes", "SEXUAL"},
    {"remove his clothes", "SEXUAL"}, {"take off clothes", "SEXUAL"},
    {"taking off clothes", "SEXUAL"}, {"take off her clothes", "SEXUAL"},
    {"see through", "SEXUAL"}, {"rule 34", "SEXUAL"}, {"sexual assault", "SEXUAL_VIOLENCE"},
    {"self harm", "SELF_HARM"}, {"cut wrists", "SELF_HARM"}, {"cutting wrists", "SELF_HARM"},
    {"slit wrists", "SELF_HARM"}, {"hang himself", "SELF_HARM"}, {"hang herself", "SELF_HARM"},
    {"kill myself", "SELF_HARM"}, {"white power", "HATE"}, {"heil hitler", "HATE"},
    {"sieg heil", "HATE"}, {"face swap", "DECEPTIVE"}, {"school shooting", "GORE"},
    {"mass shooting", "GORE"}, {"blood splatter", "GORE"}, {"severed head", "GORE"},
    {"severed limbs", "GORE"}, {"exposed organs", "GORE"}
  };

  private ContentSafety() {}

  private static void add(Category category, String... words) {
    for (String w : words) TERMS.put(w, category);
  }

  private static void addPrefix(Category category, String... stems) {
    for (String s : stems) PREFIXES.put(s, category);
  }

  /**
   * Lowercase letters separated by single spaces. Removes accents, undoes common digit/symbol
   * substitutions, and rejoins letter-by-letter spelling ("n u d e", "n.u.d.e").
   */
  public static String normalize(String prompt) {
    String s = Normalizer.normalize(prompt == null ? "" : prompt, Normalizer.Form.NFKD);
    StringBuilder out = new StringBuilder(s.length());
    for (char c : s.toLowerCase(Locale.ROOT).toCharArray()) {
      if (Character.getType(c) == Character.NON_SPACING_MARK) continue;
      switch (c) {
        case '0': c = 'o'; break;
        case '1': c = 'i'; break;
        case '3': c = 'e'; break;
        case '4': case '@': c = 'a'; break;
        case '5': case '$': c = 's'; break;
        case '7': c = 't'; break;
        default:
      }
      out.append(c >= 'a' && c <= 'z' ? c : ' ');
    }
    String[] words = out.toString().trim().split(" +");
    StringBuilder joined = new StringBuilder();
    for (int i = 0; i < words.length; i++) {
      if (words[i].isEmpty()) continue;
      if (joined.length() > 0) {
        boolean spelled =
            words[i].length() == 1
                && i > 0
                && words[i - 1].length() == 1
                && !words[i].equals("a")
                && !words[i - 1].equals("a");
        if (!spelled) joined.append(' ');
      }
      joined.append(words[i]);
    }
    return joined.toString();
  }

  /** Returns null when the prompt is allowed. */
  public static Verdict check(String prompt) {
    return check(prompt, Collections.emptySet());
  }

  /**
   * @param reported normalized prompts this user reported; their results are refused again.
   */
  public static Verdict check(String prompt, Set<String> reported) {
    String text = normalize(prompt);
    if (reported.contains(text)) return new Verdict(Category.REPORTED, text);
    String padded = " " + text + " ";
    for (String[] phrase : PHRASES)
      if (padded.contains(" " + phrase[0] + " "))
        return new Verdict(Category.valueOf(phrase[1]), phrase[0]);
    // "4" -> "a" breaks numbers like "r34", so also test the raw lowercase words.
    List<String> words = new ArrayList<>();
    var m = WORD.matcher(text);
    while (m.find()) words.add(m.group());
    for (String raw : (prompt == null ? "" : prompt).toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
      if (!raw.isEmpty()) words.add(raw);
    boolean minor = false, suggestive = false;
    for (String w : words) {
      Category c = TERMS.get(w);
      if (c == null && w.endsWith("s")) c = TERMS.get(w.substring(0, w.length() - 1));
      if (c != null) return new Verdict(c, w);
      for (Map.Entry<String, Category> p : PREFIXES.entrySet())
        if (w.startsWith(p.getKey())) return new Verdict(p.getValue(), w);
      minor |= MINORS.contains(w);
      suggestive |= SUGGESTIVE.contains(w);
    }
    if (minor && suggestive) return new Verdict(Category.MINOR, "minor + suggestive");
    return null;
  }
}
