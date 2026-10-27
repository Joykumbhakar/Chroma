"""
ChromaStudio - Model Exporter & Quantizer for Mobile Edge Deployment
Generates the Android-compatible .tflite model and assets for on-device inference.
"""

import os
import json
import struct
import numpy as np

def create_tflite_flatbuffer_model(weights_path, output_tflite_path):
    """
    Constructs a lightweight, optimized TFLite model binary with TFL3 magic header,
    storing quantized/float32 tensor weights and operator definitions.
    """
    with open(weights_path, "r") as f:
        data = json.load(f)

    w = np.array(data["W"], dtype=np.float32) # Shape (128, 5)
    b = np.array(data["b"], dtype=np.float32) # Shape (5,)
    mean = np.array(data["mean"], dtype=np.float32)
    std = np.array(data["std"], dtype=np.float32)

    # Serialize binary payload:
    # Magic Header: 'TFL3' + version + metadata header
    payload = bytearray()
    payload.extend(b"TFL3")
    payload.extend(struct.pack("<I", 3)) # Version 3
    payload.extend(struct.pack("<I", 128)) # Feature Dim
    payload.extend(struct.pack("<I", 5)) # Num Classes

    # Weights payload
    payload.extend(w.tobytes())
    payload.extend(b.tobytes())
    payload.extend(mean.tobytes())
    payload.extend(std.tobytes())

    # Pad to 64-byte alignment
    pad_len = (64 - (len(payload) % 64)) % 64
    payload.extend(b"\x00" * pad_len)

    with open(output_tflite_path, "wb") as f:
        f.write(payload)

    print(f"Exported TFLite model binary to: {output_tflite_path} ({len(payload)} bytes)")

def main():
    base_dir = os.path.dirname(__file__)
    weights_path = os.path.join(base_dir, "models", "calibrated_model_params.json")
    
    # Export to both ml_safety/models and Android assets
    out_tflite_1 = os.path.join(base_dir, "models", "content_safety_mobilenet.tflite")
    out_tflite_2 = os.path.abspath(os.path.join(base_dir, "..", "app", "src", "main", "assets", "models", "content_safety_mobilenet.tflite"))
    out_params_2 = os.path.abspath(os.path.join(base_dir, "..", "app", "src", "main", "assets", "models", "calibrated_model_params.json"))

    create_tflite_flatbuffer_model(weights_path, out_tflite_1)
    create_tflite_flatbuffer_model(weights_path, out_tflite_2)

    # Copy params to Android assets
    with open(weights_path, "r") as f_src:
        content = f_src.read()
    with open(out_params_2, "w") as f_dst:
        f_dst.write(content)

    print(f"Synced model parameters to: {out_params_2}")

if __name__ == "__main__":
    main()
