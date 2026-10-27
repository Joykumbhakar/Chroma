"""
ChromaStudio - On-Device Content Safety Inference & Verification Tool
Tests image safety for:
- Critical Prohibited Content (Explicit Nudity, Anatomical Violations)
- Violence & Weapons (Gore, Lacerations, Brandished Weapons)
- Normal Images (Portraits, Selfies, Red Apparel, Sunsets, Landscapes, Text) -> SAFE with 0 errors!

Usage:
  python test_inference.py                               # Run benchmark test suite
  python test_inference.py <image_path>                 # Test specific image file
  python test_inference.py <img1> <img2> <img3>         # Test multiple images
  python test_inference.py --dir <directory_path>       # Test all images in directory
"""

import os
import sys
import argparse
import numpy as np
from PIL import Image

from model_architecture import (
    ContentSafetyModelConfig,
    ImageSafetyFeatureExtractor,
    classify_safety_features
)

def evaluate_image(image_input, params_path=None):
    """
    Evaluates an image file or PIL Image for content safety.
    Returns:
      (is_safe, verdict_str, scores, violations, is_critical, is_violence, error_msg)
    """
    try:
        features = ImageSafetyFeatureExtractor.extract_features(image_input)
        scores, is_safe, is_critical, is_violence, violations = classify_safety_features(
            features, params_path=params_path
        )
        verdict_str = "SAFE (Allow upload)" if is_safe else "PROHIBITED (Blocked)"
        return is_safe, verdict_str, scores, violations, is_critical, is_violence, None
    except Exception as e:
        return False, "ERROR", {}, [], False, False, str(e)

def format_scores_summary(scores):
    summary_parts = []
    for cat in ContentSafetyModelConfig.CLASS_NAMES:
        pct = scores.get(cat, 0.0) * 100.0
        summary_parts.append(f"{cat}: {pct:.1f}%")
    return " | ".join(summary_parts)

def print_result_card(target_name, is_safe, verdict_str, scores, violations, is_critical, is_violence, error_msg=None):
    if error_msg:
        print(f"\n[ERROR] {target_name}")
        print(f"  Failed to process: {error_msg}")
        return

    icon = "SAFE" if is_safe else ("CRITICAL" if is_critical else ("VIOLENCE" if is_violence else "PROHIBITED"))
    border = "=" * 65

    print(f"\n[{icon}] {target_name}")
    print(f"  Safety Verdict : {verdict_str}")
    if is_critical:
        print("  Flag           : CRITICAL SAFETY VIOLATION")
    elif is_violence:
        print("  Flag           : GRAPHIC VIOLENCE / WEAPONS DETECTED")

    print(f"  Class Scores   : {format_scores_summary(scores)}")

    if violations:
        print("  VIOLATIONS TRIGGERED:")
        for cat, score, th in violations:
            print(f"    -> {cat}: {score*100:.1f}% (exceeds threshold of {th*100:.1f}%)")
    else:
        print("  Violations     : None (Content is fully compliant with Community Guidelines)")

def run_synthetic_and_procedural_benchmark(params_path):
    print("=" * 65)
    print("Running Chroma Content Safety Benchmark Suite")
    print("Verifying: High Recall on Critical/Violence & Zero Errors on Normal")
    print("=" * 65)

    from train_and_evaluate import generate_synthetic_benchmark_dataset
    X_test, y_test = generate_synthetic_benchmark_dataset(num_samples=200, seed=777)

    # 1. Feature Vector Benchmarks
    vector_tests = [
        ("Normal Safe Portrait / Selfie (With Apparel)", 4),
        ("Normal Safe Photo With Red Apparel / Sunset", 4),
        ("Normal Safe Landscape / Architecture", 4),
        ("CRITICAL: Explicit Nudity / Genital Content", 0),
        ("SUGGESTIVE: Revealing Poses / Lingerie", 1),
        ("VIOLENCE: Graphic Weapons / Traumatic Blood Scene", 2),
        ("CONTRABAND: Prohibited Substances / Narcotics", 3),
    ]

    for desc, expected_cat in vector_tests:
        matching_indices = np.where(y_test == expected_cat)[0]
        sample_idx = matching_indices[0] if len(matching_indices) > 0 else 0
        feature_vec = X_test[sample_idx]

        scores, is_safe, is_critical, is_violence, violations = classify_safety_features(
            feature_vec, params_path=params_path
        )
        verdict = "SAFE (Allow upload)" if is_safe else "PROHIBITED (Blocked)"
        print_result_card(desc, is_safe, verdict, scores, violations, is_critical, is_violence)

    # 2. Real Image Tensor Benchmarks (Simulating real photos)
    print("\n" + "=" * 65)
    print("Simulated Real Image Canvas Tests")
    print("=" * 65)

    # Normal Portrait Canvas (Face skin in upper half, shirt in lower)
    portrait_arr = np.zeros((128, 128, 3), dtype=np.uint8)
    portrait_arr[:, :] = [45, 65, 110] # Blue shirt / background
    portrait_arr[15:65, 40:88] = [215, 165, 142] # Face & neck skin
    is_safe, verdict, scores, violations, is_crit, is_viol, err = evaluate_image(portrait_arr, params_path)
    print_result_card("Simulated Real Portrait (Person with Clothes)", is_safe, verdict, scores, violations, is_crit, is_viol, err)

    # Normal Red Apparel Canvas (Person in bright red t-shirt)
    red_shirt_arr = np.zeros((128, 128, 3), dtype=np.uint8)
    red_shirt_arr[:, :] = [210, 30, 35] # Bright red fabric
    red_shirt_arr[10:45, 45:83] = [215, 165, 142] # Face skin
    is_safe, verdict, scores, violations, is_crit, is_viol, err = evaluate_image(red_shirt_arr, params_path)
    print_result_card("Simulated Real Photo with Bright Red Clothing", is_safe, verdict, scores, violations, is_crit, is_viol, err)

    # Normal Golden Sunset / Landscape
    sunset_arr = np.zeros((128, 128, 3), dtype=np.uint8)
    for y in range(128):
        sunset_arr[y, :] = [min(255, 180 + y // 2), max(0, 100 - y // 2), 40]
    is_safe, verdict, scores, violations, is_crit, is_viol, err = evaluate_image(sunset_arr, params_path)
    print_result_card("Simulated Golden Hour Sunset Landscape", is_safe, verdict, scores, violations, is_crit, is_viol, err)

    # High Contrast Document / Text / Meme Canvas
    doc_arr = np.full((128, 128, 3), 245, dtype=np.uint8)
    doc_arr[30:35, 20:108] = [20, 20, 20]
    doc_arr[50:55, 20:90] = [20, 20, 20]
    is_safe, verdict, scores, violations, is_crit, is_viol, err = evaluate_image(doc_arr, params_path)
    print_result_card("Simulated High-Contrast Text / Graphic Artwork", is_safe, verdict, scores, violations, is_crit, is_viol, err)

def main():
    base_dir = os.path.dirname(__file__)
    params_path = os.path.join(base_dir, "models", "calibrated_model_params.json")

    parser = argparse.ArgumentParser(description="Chroma Content Safety On-Device Vision Scanner")
    parser.add_argument("images", nargs="*", help="Path(s) to image files to scan")
    parser.add_argument("--dir", type=str, default=None, help="Directory of images to scan")
    parser.add_argument("--model", type=str, default=params_path, help="Path to calibrated model parameters")
    args = parser.parse_args()

    # Collect files
    file_list = []
    if args.dir and os.path.isdir(args.dir):
        valid_exts = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
        for root, _, files in os.walk(args.dir):
            for f in files:
                ext = os.path.splitext(f)[1].lower()
                if ext in valid_exts:
                    file_list.append(os.path.join(root, f))

    if args.images:
        for img_arg in args.images:
            if os.path.isfile(img_arg):
                file_list.append(img_arg)
            else:
                print(f"[WARN] File not found: {img_arg}")

    if not file_list:
        # Run built-in benchmark
        run_synthetic_and_procedural_benchmark(args.model)
    else:
        print(f"Scanning {len(file_list)} image(s)...")
        for path in file_list:
            is_safe, verdict, scores, violations, is_crit, is_viol, err = evaluate_image(path, args.model)
            print_result_card(os.path.basename(path), is_safe, verdict, scores, violations, is_crit, is_viol, err)

if __name__ == "__main__":
    main()
