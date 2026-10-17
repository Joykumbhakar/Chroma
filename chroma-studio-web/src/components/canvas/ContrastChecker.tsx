'use client';

import React from 'react';
import { useStudio } from '@/context/StudioContext';
import { ColorUtils } from '@/utils/colorUtils';
import { Eyeglasses, MagicWand, CheckCircle, Warning } from '@phosphor-icons/react';

export default function ContrastChecker() {
  const { layers, canvasShape, colorBlindMode, setColorBlindMode, updateLayer } = useStudio();

  // Calculate contrast for base layer color vs text
  const primaryLayer = layers[0];
  const firstStop = primaryLayer?.stops[0] || { color: '#4F46E5' };

  let rgb = { r: 79, g: 70, b: 229 };
  if (firstStop.color.startsWith('rgba')) {
    const parts = firstStop.color.match(/[\d.]+/g);
    if (parts && parts.length >= 3) {
      rgb = { r: Number(parts[0]), g: Number(parts[1]), b: Number(parts[2]) };
    }
  } else {
    const [r, g, b] = ColorUtils.hex2rgb(firstStop.color);
    rgb = { r, g, b };
  }

  const lumBG = ColorUtils.getLuminance(rgb.r, rgb.g, rgb.b);
  const lumText = ColorUtils.getLuminance(255, 255, 255); // White text
  const ratio = Math.round(ColorUtils.getContrastRatio(lumBG, lumText) * 10) / 10;

  const isPassAAA = ratio >= 7;
  const isPassAA = ratio >= 4.5;

  const autoFixContrast = () => {
    if (!primaryLayer || !primaryLayer.stops.length) return;
    let [h, sat, val] = ColorUtils.rgb2hsv(rgb.r, rgb.g, rgb.b);

    // Adjust value / saturation to hit >= 4.5 ratio
    let currVal = val;
    let currSat = sat;
    let newRatio = ratio;

    for (let i = 0; i < 20; i++) {
      if (newRatio >= 4.5) break;
      currVal = Math.max(0.1, currVal - 0.05);
      currSat = Math.min(1.0, currSat + 0.05);
      const [r, g, b] = ColorUtils.hsv2rgb(h, currSat, currVal);
      const lum = ColorUtils.getLuminance(r, g, b);
      newRatio = ColorUtils.getContrastRatio(lum, lumText);
    }

    const [r, g, b] = ColorUtils.hsv2rgb(h, currSat, currVal);
    const hex = ColorUtils.rgb2hex(r, g, b);
    const newStops = primaryLayer.stops.map((st, i) => i === 0 ? { ...st, color: `#${hex}` } : st);
    updateLayer(primaryLayer.id, { stops: newStops });
  };

  return (
    <div className="absolute bottom-4 left-4 right-4 z-50 flex flex-wrap items-center justify-between gap-3 pointer-events-none">
      {/* Color Blindness Simulator selector */}
      <div className="glass-panel px-3 py-1.5 rounded-full shadow-lg flex items-center gap-2 pointer-events-auto">
        <Eyeglasses size={16} className="text-[var(--text-muted)]" />
        <select
          value={colorBlindMode}
          onChange={(e) => setColorBlindMode(e.target.value as any)}
          className="bg-transparent text-[10px] font-semibold text-[var(--text-main)] uppercase tracking-widest outline-none cursor-pointer"
        >
          <option value="none">Normal Vision</option>
          <option value="protanopia">Protanopia (Red-Blind)</option>
          <option value="deuteranopia">Deuteranopia (Green-Blind)</option>
          <option value="tritanopia">Tritanopia (Blue-Blind)</option>
        </select>
      </div>

      {/* Contrast Ratio Indicator */}
      {canvasShape === 'text' && (
        <div className="glass-panel px-3 py-1.5 rounded-full shadow-lg flex items-center gap-2 pointer-events-auto text-[11px] font-bold">
          {isPassAA ? (
            <span className="text-emerald-500 flex items-center gap-1">
              <CheckCircle weight="fill" size={14} /> WCAG {isPassAAA ? 'AAA' : 'AA'} ({ratio}:1)
            </span>
          ) : (
            <span className="text-amber-500 flex items-center gap-1">
              <Warning weight="fill" size={14} /> Low Contrast ({ratio}:1)
            </span>
          )}

          {!isPassAA && (
            <button
              onClick={autoFixContrast}
              className="ml-2 btn btn-primary text-[10px] py-0.5 px-2 rounded-full"
            >
              <MagicWand size={12} /> Auto-Fix
            </button>
          )}
        </div>
      )}
    </div>
  );
}
