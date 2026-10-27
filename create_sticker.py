#!/usr/bin/env python3
"""
ChromaStudio - Auto Background Remover & 5px White Border Sticker Generator
=============================================================================
Creates die-cut stickers for Stories, Chats, and Messages:
1. Automatically removes image backgrounds using AI segmentation (rembg) or
   built-in morphological color-distance segmentation.
2. Automatically applies a crisp 5px white die-cut border with smooth anti-aliasing.
3. Supports optional drop shadow, batch directory processing, and custom tolerances.

Usage:
    python create_sticker.py input.jpg output.png
    python create_sticker.py photo.png --border 5 --shadow
    python create_sticker.py --dir ./my_photos --out ./stickers
"""

import sys
import os
import argparse
from typing import Tuple, Optional
from PIL import Image, ImageFilter, ImageOps
import numpy as np

# Try importing rembg for high-fidelity AI background removal
try:
    import rembg
    HAS_REMBG = True
except ImportError:
    HAS_REMBG = False


def remove_background_ai(image: Image.Image) -> Image.Image:
    """Removes the background using rembg (U2-Net / deep learning segmentation)."""
    if not HAS_REMBG:
        raise RuntimeError("rembg is not installed. Please install rembg or use fallback.")
    return rembg.remove(image)


def remove_background_fallback(
    image: Image.Image,
    tolerance: int = 32,
    bg_color: Optional[Tuple[int, int, int]] = None
) -> Image.Image:
    """
    Fallback background removal using luminance/color distance analysis,
    corner sampling, and morphological alpha mask cleanup.
    """
    img_rgba = image.convert("RGBA")
    data = np.array(img_rgba, dtype=np.float32)
    rgb = data[:, :, :3]
    h, w = rgb.shape[:2]

    # If background color not specified, sample the 4 corners and borders
    if bg_color is None:
        corners = [
            rgb[0, 0],
            rgb[0, -1],
            rgb[-1, 0],
            rgb[-1, -1],
            rgb[0, w // 2],
            rgb[-1, w // 2],
            rgb[h // 2, 0],
            rgb[h // 2, -1],
        ]
        bg_ref = np.median(corners, axis=0)
    else:
        bg_ref = np.array(bg_color, dtype=np.float32)

    # Calculate Euclidean color distance from background color
    dist = np.linalg.norm(rgb - bg_ref, axis=2)

    # Create soft alpha mask based on tolerance
    lower = max(0, tolerance * 0.8)
    upper = tolerance * 1.5
    alpha = np.clip((dist - lower) / (upper - lower + 1e-5) * 255.0, 0, 255).astype(np.uint8)

    # Morphological cleanup using PIL filters
    mask = Image.fromarray(alpha, mode="L")
    mask = mask.filter(ImageFilter.MedianFilter(3))
    mask = mask.filter(ImageFilter.GaussianBlur(1.0))

    img_rgba.putalpha(mask)
    return img_rgba


def remove_background(
    image: Image.Image,
    force_fallback: bool = False,
    tolerance: int = 35
) -> Image.Image:
    """
    Primary background removal entrypoint:
    Uses rembg AI segmentation if available, with graceful fallback.
    """
    if HAS_REMBG and not force_fallback:
        try:
            return remove_background_ai(image)
        except Exception as e:
            print(f"[Warning] AI background removal failed ({e}), falling back to morphological color segmentation...")
            return remove_background_fallback(image, tolerance=tolerance)
    else:
        return remove_background_fallback(image, tolerance=tolerance)


def add_sticker_border(
    image: Image.Image,
    border_px: int = 5,
    border_color: Tuple[int, int, int, int] = (255, 255, 255, 255),
    add_shadow: bool = False,
    feather_px: float = 0.5
) -> Image.Image:
    """
    Applies an authentic die-cut white border of specified pixel width around
    the transparent subject mask, with anti-aliasing and optional drop shadow.
    """
    rgba = image.convert("RGBA")
    w, h = rgba.size

    # Extra padding to ensure border and shadow do not touch the image boundaries
    pad = border_px + (15 if add_shadow else 6)
    new_w = w + pad * 2
    new_h = h + pad * 2

    # Canvas for padded foreground
    padded_fg = Image.new("RGBA", (new_w, new_h), (0, 0, 0, 0))
    padded_fg.paste(rgba, (pad, pad), rgba)

    # Extract alpha channel
    alpha = padded_fg.split()[3]

    # 1. Dilation: Expand the alpha mask by border_px
    # In PIL, MaxFilter with size (2 * border_px + 1) expands non-zero pixels by border_px
    filter_size = max(3, 2 * border_px + 1)
    dilated_alpha = alpha.filter(ImageFilter.MaxFilter(filter_size))

    # Anti-alias smoothing for the die-cut border
    if feather_px > 0:
        dilated_alpha = dilated_alpha.filter(ImageFilter.GaussianBlur(feather_px))

    # 2. Create the border image
    border_img = Image.new("RGBA", (new_w, new_h), border_color)
    border_img.putalpha(dilated_alpha)

    # 3. Create composite canvas
    result = Image.new("RGBA", (new_w, new_h), (0, 0, 0, 0))

    # 4. Optional subtle sticker drop shadow for contrast
    if add_shadow:
        shadow_filter_size = max(5, filter_size + 4)
        shadow_mask = dilated_alpha.filter(ImageFilter.MaxFilter(shadow_filter_size))
        shadow_mask = shadow_mask.filter(ImageFilter.GaussianBlur(3.5))
        shadow_img = Image.new("RGBA", (new_w, new_h), (0, 0, 0, 90))
        shadow_img.putalpha(shadow_mask)
        # Offset shadow slightly down-right
        result.paste(shadow_img, (2, 3), shadow_img)

    # 5. Composite white border, then foreground subject
    result.paste(border_img, (0, 0), border_img)
    result.paste(padded_fg, (0, 0), padded_fg)

    # 6. Auto-crop transparent bounding box with neat 4px margin
    bbox = result.getbbox()
    if bbox:
        crop_box = (
            max(0, bbox[0] - 4),
            max(0, bbox[1] - 4),
            min(new_w, bbox[2] + 4),
            min(new_h, bbox[3] + 4)
        )
        result = result.crop(crop_box)

    return result


def create_sticker(
    input_path: str,
    output_path: Optional[str] = None,
    border_px: int = 5,
    border_color: Tuple[int, int, int, int] = (255, 255, 255, 255),
    add_shadow: bool = False,
    force_fallback: bool = False,
    tolerance: int = 35
) -> str:
    """
    High-level function: Loads an image, removes background, applies 5px white border,
    and saves the transparent sticker PNG.
    """
    if not os.path.exists(input_path):
        raise FileNotFoundError(f"Input image not found: {input_path}")

    if output_path is None:
        base, _ = os.path.splitext(input_path)
        output_path = f"{base}_sticker.png"

    print(f"[1/3] Loading '{input_path}'...")
    src_image = Image.open(input_path)

    # Fix EXIF orientation if present
    src_image = ImageOps.exif_transpose(src_image)

    print("[2/3] Removing background...")
    cutout = remove_background(src_image, force_fallback=force_fallback, tolerance=tolerance)

    print(f"[3/3] Applying {border_px}px white die-cut border...")
    sticker = add_sticker_border(
        cutout,
        border_px=border_px,
        border_color=border_color,
        add_shadow=add_shadow
    )

    # Ensure output directory exists
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    sticker.save(output_path, format="PNG", optimize=True)
    print(f"[Done] Sticker saved to: '{output_path}' ({sticker.width}x{sticker.height} px)")
    return output_path


def parse_color(color_str: str) -> Tuple[int, int, int, int]:
    """Parses hex (#FFFFFF) or rgba comma string into (R, G, B, A)."""
    color_str = color_str.strip().lstrip('#')
    if len(color_str) == 6:
        r = int(color_str[0:2], 16)
        g = int(color_str[2:4], 16)
        b = int(color_str[4:6], 16)
        return (r, g, b, 255)
    elif len(color_str) == 8:
        r = int(color_str[0:2], 16)
        g = int(color_str[2:4], 16)
        b = int(color_str[4:6], 16)
        a = int(color_str[6:8], 16)
        return (r, g, b, a)
    elif ',' in color_str:
        parts = [int(p.strip()) for p in color_str.split(',')]
        if len(parts) == 3:
            return (parts[0], parts[1], parts[2], 255)
        elif len(parts) == 4:
            return (parts[0], parts[1], parts[2], parts[3])
    # Default white
    return (255, 255, 255, 255)


def main():
    parser = argparse.ArgumentParser(
        description="ChromaStudio - Remove background & create 5px white border stickers."
    )
    parser.add_argument("input", nargs="?", help="Input image file path")
    parser.add_argument("output", nargs="?", help="Output PNG sticker file path")
    parser.add_argument("--dir", "-d", help="Directory of images to batch process into stickers")
    parser.add_argument("--out-dir", help="Output directory for batch processing")
    parser.add_argument("--border", "-b", type=int, default=5, help="Border width in pixels (default: 5)")
    parser.add_argument("--border-color", default="#FFFFFF", help="Border color in hex or R,G,B,A (default: #FFFFFF)")
    parser.add_argument("--shadow", "-s", action="store_true", help="Add subtle drop shadow behind white border")
    parser.add_argument("--tolerance", "-t", type=int, default=35, help="Fallback color segmentation tolerance")
    parser.add_argument("--fallback", action="store_true", help="Force fast fallback color-distance segmentation")

    args = parser.parse_args()
    border_color = parse_color(args.border_color)

    if args.dir:
        in_dir = args.dir
        out_dir = args.out_dir or os.path.join(in_dir, "stickers")
        os.makedirs(out_dir, exist_ok=True)
        valid_exts = {".png", ".jpg", ".jpeg", ".webp", ".bmp"}
        files = [f for f in os.listdir(in_dir) if os.path.splitext(f)[1].lower() in valid_exts]
        print(f"Found {len(files)} images to process in '{in_dir}'.")
        for idx, filename in enumerate(files, 1):
            in_file = os.path.join(in_dir, filename)
            out_file = os.path.join(out_dir, f"{os.path.splitext(filename)[0]}_sticker.png")
            print(f"[{idx}/{len(files)}] Processing {filename}...")
            try:
                create_sticker(
                    input_path=in_file,
                    output_path=out_file,
                    border_px=args.border,
                    border_color=border_color,
                    add_shadow=args.shadow,
                    force_fallback=args.fallback,
                    tolerance=args.tolerance
                )
            except Exception as e:
                print(f"  Error processing {filename}: {e}")
        print("Batch processing complete!")
        return

    if not args.input:
        parser.print_help()
        sys.exit(1)

    create_sticker(
        input_path=args.input,
        output_path=args.output,
        border_px=args.border,
        border_color=border_color,
        add_shadow=args.shadow,
        force_fallback=args.fallback,
        tolerance=args.tolerance
    )


if __name__ == "__main__":
    main()
