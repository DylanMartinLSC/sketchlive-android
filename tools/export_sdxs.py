"""Export the pinned SDXS + Sketch ControlNet to the Android FP16 / FP32-I/O pack.

Requires torch, diffusers, transformers, accelerate, onnx and a CUDA GPU.
Usage: python tools/export_sdxs.py OUTPUT_DIRECTORY
No training or remote inference is used. Source weights are downloaded from Hugging Face.
"""
import gc
import hashlib
import json
from pathlib import Path
import sys
import shutil
import torch
import onnx
from diffusers import StableDiffusionPipeline, ControlNetModel
from diffusers.models.attention_processor import AttnProcessor
from huggingface_hub import snapshot_download

BASE_REVISION = "76f720262bb051da75666b22c902a78c8e16c763"
CONTROL_REVISION = "cd341678087336ed72c05660ac84d765896b48e7"
SIZE = 256


class Text(torch.nn.Module):
    def __init__(self, module):
        super().__init__(); self.module = module
    def forward(self, input_ids):
        return self.module(input_ids, return_dict=False)[0].float()


class Drawing(torch.nn.Module):
    def __init__(self, unet, control):
        super().__init__(); self.unet = unet; self.control = control
    def forward(self, sample, timestep, encoder_hidden_states, control_image, control_scale):
        sample = sample.half(); context = encoder_hidden_states.half()
        residuals, mid = self.control(sample, timestep, encoder_hidden_states=context,
            controlnet_cond=control_image.half(), conditioning_scale=control_scale.half(), return_dict=False)
        return self.unet(sample, timestep, encoder_hidden_states=context,
            down_block_additional_residuals=residuals, mid_block_additional_residual=mid, return_dict=False)[0].float()


class Decoder(torch.nn.Module):
    def __init__(self, module):
        super().__init__(); self.module = module
    def forward(self, latent):
        return self.module.decode(latent.half(), return_dict=False)[0].float()


def export(module, args, output, weights, inputs, result):
    temporary = output.with_suffix(".export.onnx")
    with torch.no_grad():
        torch.onnx.export(module.eval(), args, str(temporary), input_names=inputs,
            output_names=[result], opset_version=17, dynamo=False, do_constant_folding=True)
    graph = onnx.load(str(temporary))
    onnx.save_model(graph, str(output), save_as_external_data=True, all_tensors_to_one_file=True,
        location=weights, size_threshold=1024, convert_attribute=False)
    onnx.checker.check_model(str(output))
    temporary.unlink(); del graph; gc.collect(); torch.cuda.empty_cache()
    print("Exported", output.name, flush=True)


def main():
    output = Path(sys.argv[1]); output.mkdir(parents=True, exist_ok=True)
    # Refuse to append to existing external-data files: that would silently alter the pack.
    if any(output.glob("*.data")):
        raise SystemExit("Use a new empty output directory for an immutable model pack")
    snapshot = snapshot_download("IDKiro/sdxs-512-dreamshaper", revision=BASE_REVISION,
        allow_patterns=["model_index.json", "scheduler/*", "text_encoder/*", "tokenizer/*", "unet/*", "vae/*", "feature_extractor/*"])
    pipe = StableDiffusionPipeline.from_pretrained(snapshot, torch_dtype=torch.float16,
        safety_checker=None, requires_safety_checker=False).to("cuda")
    control = ControlNetModel.from_pretrained("IDKiro/sdxs-512-dreamshaper-sketch", revision=CONTROL_REVISION,
        torch_dtype=torch.float16, use_safetensors=False).to("cuda")
    pipe.unet.set_attn_processor(AttnProcessor()); control.set_attn_processor(AttnProcessor())
    pipe.text_encoder.set_attn_implementation("eager")
    latent = SIZE // 8
    export(Text(pipe.text_encoder), (torch.zeros((1,77),device="cuda",dtype=torch.int64),),
        output/"text_encoder.onnx", "text_encoder.data", ["input_ids"], "embedding")
    export(Drawing(pipe.unet,control), (
        torch.zeros((1,4,latent,latent),device="cuda"), torch.tensor([999.],device="cuda"),
        torch.zeros((1,77,768),device="cuda"),torch.zeros((1,3,SIZE,SIZE),device="cuda"),torch.tensor([.85],device="cuda")),
        output/f"unet_{SIZE}.onnx","unet.data",
        ["sample","timestep","encoder_hidden_states","control_image","control_scale"],"noise_pred")
    export(Decoder(pipe.vae), (torch.zeros((1,4,latent,latent),device="cuda"),),
        output/f"decoder_{SIZE}.onnx","decoder.data",["latent"],"image")
    notices=Path(__file__).parents[1]/"app/src/main/assets/sdxs-notices"
    for notice in notices.iterdir():shutil.copyfile(notice,output/notice.name)
    files=[]
    for file in sorted(output.iterdir()):
        if file.suffix in [".onnx",".data",".txt",".md"]:
            with file.open("rb") as stream: digest=hashlib.file_digest(stream,"sha256").hexdigest()
            files.append({"name":file.name,"size":file.stat().st_size,"sha256":digest})
    identity=hashlib.sha256(json.dumps(files,sort_keys=True).encode()).hexdigest()
    manifest={"id":"sdxs-sketch-256-v1","schema":1,"revision":identity,
        "base_url":"https://github.com/DylanMartinLSC/sketchlive-android/releases/download/sdxs-sketch-v1/",
        "source_model":"IDKiro/sdxs-512-dreamshaper","source_revision":BASE_REVISION,
        "control_model":"IDKiro/sdxs-512-dreamshaper-sketch","control_revision":CONTROL_REVISION,
        "resolutions":[SIZE],"context_width":768,"pad_token":49407,"timestep":999,
        "sigma":14.614646911621094,"conditioning":"white sketch lines on black, RGB [0,1]",
        "note":"One-step SDXS with fused Sketch ControlNet, FP16 weights / FP32 float I/O. Modified by ONNX conversion; no training. Model weights retain upstream OpenRAIL++ terms.","files":files}
    (output/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
    for name in ["vocab.json","merges.txt"]:shutil.copyfile(Path(snapshot)/"tokenizer"/name,output/name)
    print("Verified pack:",sum(item['size'] for item in files),"bytes",flush=True)


if __name__ == "__main__":
    main()
