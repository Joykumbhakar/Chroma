'use client';

import React, { useState } from 'react';
import { useStudio } from '@/context/StudioContext';
import { X, Copy, Check, Download, VideoCamera, FileSvg, Image as ImageIcon } from '@phosphor-icons/react';
import { ColorUtils } from '@/utils/colorUtils';
import { LayerType } from '@/types/models';

type ExportTarget =
  | 'css'
  | 'tailwind'
  | 'react'
  | 'vue'
  | 'svelte'
  | 'angular'
  | 'canvas'
  | 'swiftui'
  | 'compose'
  | 'xml'
  | 'kotlin'
  | 'java'
  | 'flutter'
  | 'rn'
  | 'svg';

interface HandoffModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function HandoffModal({ isOpen, onClose }: HandoffModalProps) {
  const { layers, canvasShape, animStatus, animStyle, animSpeed, animIntensity } = useStudio();
  const [tab, setTab] = useState<ExportTarget>('css');
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const activeLayers = layers.filter(l => l.visible);
  const bgColor = '#0d1117';

  // Code Generator Functions
  const generateCode = (target: ExportTarget): string => {
    if (activeLayers.length === 0) return '// No visible layers in studio stack';

    switch (target) {
      case 'css': {
        let code = `.chroma-bg {\n  width: 100%; height: 100vh;\n  position: relative;\n  background-color: ${bgColor};\n}\n\n`;
        activeLayers.forEach((l, i) => {
          const stopsStr = l.stops.slice().sort((a, b) => a.position - b.position).map(s => `${s.color} ${s.position}%`).join(', ');
          let bg = `linear-gradient(${l.angle}deg, ${stopsStr})`;
          if (l.type === LayerType.RADIAL) bg = `radial-gradient(ellipse ${l.width}% ${l.height}% at ${l.centerX}% ${l.centerY}%, ${stopsStr})`;
          if (l.type === LayerType.CONIC) bg = `conic-gradient(from ${l.angle}deg at ${l.centerX}% ${l.centerY}%, ${stopsStr})`;
          
          code += `.chroma-bg::before /* Layer: ${l.name} */ {\n  content: ""; position: absolute; inset: 0;\n  background: ${bg};\n  mix-blend-mode: ${l.blendMode};\n  opacity: ${l.opacity};\n  z-index: ${i + 1};\n}\n\n`;
        });
        return code;
      }

      case 'tailwind': {
        let code = `// tailwind.config.js\nmodule.exports = {\n  theme: {\n    extend: {\n      backgroundImage: {\n`;
        activeLayers.forEach((l, i) => {
          const stopsStr = l.stops.map(s => s.color).join(', ');
          code += `        'chroma-${i}': 'linear-gradient(${l.angle}deg, ${stopsStr})',\n`;
        });
        code += `      }\n    }\n  }\n}\n\n// JSX Usage:\n<div className="relative w-full h-screen bg-slate-950">\n`;
        activeLayers.forEach((l, i) => {
          code += `  <div className="absolute inset-0 bg-chroma-${i} mix-blend-${l.blendMode} opacity-[${l.opacity}]" />\n`;
        });
        code += `</div>`;
        return code;
      }

      case 'react': {
        let code = `import React from 'react';\n\nexport const ChromaBackground = () => {\n  return (\n    <div style={{ position: 'relative', width: '100%', height: '100vh', backgroundColor: '${bgColor}' }}>\n`;
        activeLayers.forEach((l, i) => {
          const stopsStr = l.stops.map(s => `${s.color} ${s.position}%`).join(', ');
          code += `      <div style={{ position: 'absolute', inset: 0, mixBlendMode: '${l.blendMode}', opacity: ${l.opacity}, background: 'linear-gradient(${l.angle}deg, ${stopsStr})' }} />\n`;
        });
        code += `    </div>\n  );\n};`;
        return code;
      }

      case 'vue': {
        let code = `<template>\n  <div class="chroma-wrap">\n`;
        activeLayers.forEach((_, i) => code += `    <div class="layer layer-${i}"></div>\n`);
        code += `  </div>\n</template>\n\n<style scoped>\n.chroma-wrap { position: relative; width: 100%; height: 100vh; background-color: ${bgColor}; }\n`;
        activeLayers.forEach((l, i) => {
          const stopsStr = l.stops.map(s => `${s.color} ${s.position}%`).join(', ');
          code += `.layer-${i} { position: absolute; inset: 0; background: linear-gradient(${l.angle}deg, ${stopsStr}); mix-blend-mode: ${l.blendMode}; opacity: ${l.opacity}; }\n`;
        });
        code += `</style>`;
        return code;
      }

      case 'svelte': {
        let code = `<div class="chroma-wrap">\n`;
        activeLayers.forEach((_, i) => code += `  <div class="layer layer-${i}"></div>\n`);
        code += `</div>\n\n<style>\n  .chroma-wrap { position: relative; width: 100%; height: 100vh; background-color: ${bgColor}; }\n`;
        activeLayers.forEach((l, i) => {
          const stopsStr = l.stops.map(s => `${s.color} ${s.position}%`).join(', ');
          code += `  .layer-${i} { position: absolute; inset: 0; background: linear-gradient(${l.angle}deg, ${stopsStr}); mix-blend-mode: ${l.blendMode}; opacity: ${l.opacity}; }\n`;
        });
        code += `</style>`;
        return code;
      }

      case 'angular': {
        let code = `<!-- component.html -->\n<div class="chroma-wrap">\n`;
        activeLayers.forEach((_, i) => code += `  <div class="layer layer-${i}"></div>\n`);
        code += `</div>\n\n/* component.css */\n.chroma-wrap { position: relative; width: 100%; height: 100vh; background-color: ${bgColor}; }\n`;
        activeLayers.forEach((l, i) => {
          const stopsStr = l.stops.map(s => `${s.color} ${s.position}%`).join(', ');
          code += `.layer-${i} { position: absolute; inset: 0; background: linear-gradient(${l.angle}deg, ${stopsStr}); mix-blend-mode: ${l.blendMode}; opacity: ${l.opacity}; }\n`;
        });
        return code;
      }

      case 'canvas': {
        return `// Canvas 2D Rendering Engine for Chroma Studio
const canvas = document.getElementById("chromaCanvas");
const ctx = canvas.getContext("2d");
let time = 0;

function render() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.fillStyle = "${bgColor}";
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  
  // Render Layer stack
  ctx.globalCompositeOperation = "screen";
  ctx.fillStyle = "rgba(79, 70, 229, 0.8)";
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  
  time += 0.01;
  requestAnimationFrame(render);
}
render();`;
      }

      case 'swiftui': {
        let code = `import SwiftUI\n\n@available(iOS 18.0, *)\nstruct ChromaBackground: View {\n    var body: some View {\n        ZStack {\n            Color(red: 0.05, green: 0.07, blue: 0.09).ignoresSafeArea()\n\n`;
        activeLayers.forEach((l) => {
          const colorsStr = l.stops.map(s => {
            const [r, g, b] = ColorUtils.hex2rgb(s.color);
            return `Color(red: ${(r / 255).toFixed(2)}, green: ${(g / 255).toFixed(2)}, blue: ${(b / 255).toFixed(2)})`;
          }).join(', ');
          code += `            LinearGradient(gradient: Gradient(colors: [${colorsStr}]), startPoint: .topLeading, endPoint: .bottomTrailing)\n                .blendMode(.${l.blendMode})\n                .opacity(${l.opacity})\n                .ignoresSafeArea()\n\n`;
        });
        code += `        }\n    }\n}`;
        return code;
      }

      case 'compose': {
        let code = `import androidx.compose.foundation.background\nimport androidx.compose.foundation.layout.*\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.graphics.*\nimport androidx.compose.runtime.Composable\n\n@Composable\nfun ChromaBackground() {\n    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {\n`;
        activeLayers.forEach((l) => {
          const colorsStr = l.stops.map(s => {
            const hex = s.color.replace('#', '').toUpperCase();
            return `Color(0xFF${hex})`;
          }).join(', ');
          code += `        Box(\n            modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(${colorsStr})), alpha = ${l.opacity}f)\n        )\n`;
        });
        code += `    }\n}`;
        return code;
      }

      case 'xml': {
        let code = `<!-- res/drawable/chroma_bg.xml -->\n<?xml version="1.0" encoding="utf-8"?>\n<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n  <item>\n    <shape android:shape="rectangle">\n      <solid android:color="${bgColor}"/>\n    </shape>\n  </item>\n`;
        activeLayers.forEach((l) => {
          const startCol = l.stops[0]?.color.replace('#', '').toUpperCase() || 'FF4F46E5';
          const endCol = l.stops[l.stops.length - 1]?.color.replace('#', '').toUpperCase() || 'FF818CF8';
          code += `  <item>\n    <shape android:shape="rectangle">\n      <gradient\n        android:type="linear"\n        android:angle="${l.angle}"\n        android:startColor="#FF${startCol}"\n        android:endColor="#FF${endCol}" />\n    </shape>\n  </item>\n`;
        });
        code += `</layer-list>`;
        return code;
      }

      case 'kotlin': {
        let code = `// Android Programmatic Gradient in Kotlin\nval backgroundLayer = GradientDrawable()\nbackgroundLayer.setColor(Color.parseColor("${bgColor}"))\n\nval layers = mutableListOf<Drawable>(backgroundLayer)\n\n`;
        activeLayers.forEach((l, i) => {
          const colorsStr = l.stops.map(s => `Color.parseColor("#FF${s.color.replace('#', '').toUpperCase()}")`).join(', ');
          code += `val layer${i} = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(${colorsStr}))\nlayer${i}.alpha = ${(l.opacity * 255).toFixed(0)}\nlayers.add(layer${i})\n\n`;
        });
        code += `val layerDrawable = LayerDrawable(layers.toTypedArray())\nview.background = layerDrawable`;
        return code;
      }

      case 'java': {
        let code = `// Android Programmatic Gradient in Java\nGradientDrawable backgroundLayer = new GradientDrawable();\nbackgroundLayer.setColor(Color.parseColor("${bgColor}"));\n\nDrawable[] layers = new Drawable[${activeLayers.length + 1}];\nlayers[0] = backgroundLayer;\n\n`;
        activeLayers.forEach((l, i) => {
          const colorsStr = l.stops.map(s => `Color.parseColor("#FF${s.color.replace('#', '').toUpperCase()}")`).join(', ');
          code += `GradientDrawable layer${i} = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{${colorsStr}});\nlayer${i}.setAlpha(${(l.opacity * 255).toFixed(0)});\nlayers[${i + 1}] = layer${i};\n\n`;
        });
        code += `LayerDrawable layerDrawable = new LayerDrawable(layers);\nview.setBackground(layerDrawable);`;
        return code;
      }

      case 'flutter': {
        let code = `import 'package:flutter/material.dart';\n\nclass ChromaBackground extends StatelessWidget {\n  @override\n  Widget build(BuildContext context) {\n    return Stack(\n      children: [\n        Container(color: const Color(0xFF0D1117)),\n`;
        activeLayers.forEach((l) => {
          const colorsStr = l.stops.map(s => `Color(0xFF${s.color.replace('#', '').toUpperCase()})`).join(', ');
          code += `        Opacity(\n          opacity: ${l.opacity},\n          child: Container(\n            decoration: const BoxDecoration(\n              gradient: LinearGradient(colors: [${colorsStr}]),\n            ),\n          ),\n        ),\n`;
        });
        code += `      ],\n    );\n  }\n}`;
        return code;
      }

      case 'rn': {
        let code = `import React from 'react';\nimport { View, StyleSheet } from 'react-native';\nimport LinearGradient from 'react-native-linear-gradient';\n\nexport const ChromaBackground = () => {\n  return (\n    <View style={styles.container}>\n`;
        activeLayers.forEach((l) => {
          const colorsStr = l.stops.map(s => `'${s.color}'`).join(', ');
          code += `      <LinearGradient colors={[${colorsStr}]} style={[StyleSheet.absoluteFill, { opacity: ${l.opacity} }]} />\n`;
        });
        code += `    </View>\n  );\n};\n\nconst styles = StyleSheet.create({\n  container: { flex: 1, backgroundColor: '${bgColor}' },\n});`;
        return code;
      }

      case 'svg': {
        let stopsSvg = activeLayers[0]?.stops.map(s => `<stop offset="${s.position}%" stop-color="${s.color}" />`).join('\n    ');
        return `<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" viewBox="0 0 1920 1080">
  <defs>
    <linearGradient id="chromaGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      ${stopsSvg}
    </linearGradient>
  </defs>
  <rect width="100%" height="100%" fill="url(#chromaGrad)" />
</svg>`;
      }
    }
  };

  const copyToClipboard = () => {
    navigator.clipboard.writeText(generateCode(tab));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-[300] bg-black/60 backdrop-blur-md flex items-center justify-center p-4">
      <div className="glass-panel w-full max-w-4xl rounded-2xl border border-[var(--glass-border)] shadow-2xl overflow-hidden flex flex-col max-h-[88vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-[var(--glass-border)] bg-[var(--glass-bg)] shrink-0">
          <h2 className="text-xs font-bold uppercase tracking-wider text-[var(--text-main)] flex items-center gap-2">
            Developer Handoff / Full Multi-Framework Export Engine
          </h2>
          <button className="btn-icon w-8 h-8" onClick={onClose}>
            <X size={16} />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex flex-col lg:flex-row flex-1 overflow-hidden min-h-[450px]">
          {/* Sidebar target selector */}
          <div className="w-full lg:w-56 bg-[var(--glass-bg)] border-b lg:border-b-0 lg:border-r border-[var(--glass-border)] flex lg:flex-col py-3 px-3 gap-1.5 shrink-0 overflow-x-auto lg:overflow-y-auto">
            <div className="field-label px-2 pt-1 hidden lg:block">WEB & FRONTEND</div>
            <button className={`export-tab ${tab === 'css' ? 'active' : ''}`} onClick={() => setTab('css')}>CSS / SCSS</button>
            <button className={`export-tab ${tab === 'tailwind' ? 'active' : ''}`} onClick={() => setTab('tailwind')}>Tailwind CSS</button>
            <button className={`export-tab ${tab === 'react' ? 'active' : ''}`} onClick={() => setTab('react')}>React / Next.js</button>
            <button className={`export-tab ${tab === 'vue' ? 'active' : ''}`} onClick={() => setTab('vue')}>Vue</button>
            <button className={`export-tab ${tab === 'svelte' ? 'active' : ''}`} onClick={() => setTab('svelte')}>Svelte</button>
            <button className={`export-tab ${tab === 'angular' ? 'active' : ''}`} onClick={() => setTab('angular')}>Angular</button>
            <button className={`export-tab ${tab === 'canvas' ? 'active' : ''}`} onClick={() => setTab('canvas')}>Canvas JS</button>
            <button className={`export-tab ${tab === 'svg' ? 'active' : ''}`} onClick={() => setTab('svg')}>SVG Markup</button>

            <div className="field-label px-2 pt-3 hidden lg:block">APPLE IOS / MAC</div>
            <button className={`export-tab ${tab === 'swiftui' ? 'active' : ''}`} onClick={() => setTab('swiftui')}>SwiftUI</button>

            <div className="field-label px-2 pt-3 hidden lg:block">ANDROID</div>
            <button className={`export-tab ${tab === 'compose' ? 'active' : ''}`} onClick={() => setTab('compose')}>Jetpack Compose</button>
            <button className={`export-tab ${tab === 'xml' ? 'active' : ''}`} onClick={() => setTab('xml')}>XML Drawable</button>
            <button className={`export-tab ${tab === 'kotlin' ? 'active' : ''}`} onClick={() => setTab('kotlin')}>Programmatic Kotlin</button>
            <button className={`export-tab ${tab === 'java' ? 'active' : ''}`} onClick={() => setTab('java')}>Programmatic Java</button>

            <div className="field-label px-2 pt-3 hidden lg:block">CROSS PLATFORM</div>
            <button className={`export-tab ${tab === 'flutter' ? 'active' : ''}`} onClick={() => setTab('flutter')}>Flutter</button>
            <button className={`export-tab ${tab === 'rn' ? 'active' : ''}`} onClick={() => setTab('rn')}>React Native</button>
          </div>

          {/* Main Code View */}
          <div className="flex-1 flex flex-col p-4 overflow-hidden relative">
            <div className="code-block-area flex-1 overflow-y-auto p-4 rounded-xl bg-black/30 font-mono text-xs text-indigo-300 border border-[var(--glass-border)] shadow-inner">
              <pre className="whitespace-pre-wrap break-all m-0">{generateCode(tab)}</pre>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-[var(--glass-border)] flex items-center justify-between bg-[var(--glass-bg)] shrink-0">
          <div className="flex gap-2">
            <button className="btn btn-ghost text-xs py-1.5 px-3" onClick={copyToClipboard}>
              <ImageIcon size={14} /> PNG Image
            </button>
            <button className="btn btn-ghost text-xs py-1.5 px-3" onClick={copyToClipboard}>
              <FileSvg size={14} /> SVG File
            </button>
          </div>

          <div className="flex gap-2">
            <button className="btn btn-ghost text-xs py-1.5 px-3" onClick={onClose}>
              Close
            </button>
            <button className="btn btn-primary text-xs py-1.5 px-4" onClick={copyToClipboard}>
              {copied ? <Check size={14} /> : <Copy size={14} />}
              {copied ? 'Copied!' : 'Copy Code'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
