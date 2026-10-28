---
license: openrail++
tags:
- text-to-image
- stable-diffusion
library_name: diffusers
inference: false
---

# SDXS-512-DreamShaper-Sketch

SDXS is a model that can generate high-resolution images in real-time based on prompt texts, trained using score distillation and feature matching. 
For more information, please refer to our research paper: [SDXS: Real-Time One-Step Latent Diffusion Models with Image Conditions](https://arxiv.org/abs/2403.16627). 
We open-source the model as part of the research.

SDXS-512-DreamShaper-Sketch is the sketch-to-image **ControlNet** for [SDXS-512-DreamShaper](https://huggingface.co/IDKiro/sdxs-512-dreamshaper).
Watch [our repo](https://github.com/IDKiro/sdxs) for any updates. 
See our [Demo Code](https://huggingface.co/spaces/IDKiro/SDXS-512-DreamShaper-Sketch/tree/main) to use the model.

<p align="left" >
<img src="sketch.gif" width="800" />
</p>

## Cite Our Work

```
@article{song2024sdxs,
  author    = {Yuda Song, Zehao Sun, Xuanwu Yin},
  title     = {SDXS: Real-Time One-Step Latent Diffusion Models with Image Conditions},
  journal   = {arxiv},
  year      = {2024},
}
```
