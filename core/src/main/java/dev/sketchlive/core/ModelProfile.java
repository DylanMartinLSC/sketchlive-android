package dev.sketchlive.core;

/**
 * Fixed, independently verified model packs; a profile also defines tokenizer and tensor shapes.
 */
public enum ModelProfile {
  QUALITY("Quality · SD-Turbo", "sd-turbo-v1", "model-manifest.json", "", 384, 1024, 0),
  FAST("Fast · SDXS Sketch", "sdxs-v1", "sdxs-manifest.json", "sdxs-", 256, 768, 49407);
  public final String label, directory, manifest, tokenizerPrefix;
  public final int size, contextWidth, padToken;

  ModelProfile(
      String label,
      String directory,
      String manifest,
      String tokenizerPrefix,
      int size,
      int contextWidth,
      int padToken) {
    this.label = label;
    this.directory = directory;
    this.manifest = manifest;
    this.tokenizerPrefix = tokenizerPrefix;
    this.size = size;
    this.contextWidth = contextWidth;
    this.padToken = padToken;
  }

  public boolean supportsEnhance() {
    return this == QUALITY;
  }
}
