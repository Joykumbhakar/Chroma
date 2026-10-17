'use client';

import React, { createContext, useContext, useState, ReactNode } from 'react';
import { GradientLayer, LayerType, ChromaBlendMode, ColorBlindMode, PostProcessingFx, AnimStyle, AnimStatus, ApplePresets, GlobalAdjustments } from '@/types/models';

interface ToastState {
  message: string;
  visible: boolean;
}

interface StudioState {
  layers: GradientLayer[];
  canvasShape: 'rounded' | 'circle' | 'full' | 'text';
  colorBlindMode: ColorBlindMode;
  postFx: PostProcessingFx;
  fxIntensity: number;
  mouseReact: boolean;
  animStatus: AnimStatus;
  animStyle: AnimStyle;
  animSpeed: number;
  animIntensity: number;
  globalAdjustments: GlobalAdjustments;
  toast: ToastState;
  
  // Actions
  addLayer: (type?: LayerType) => void;
  updateLayer: (id: string, updates: Partial<GradientLayer>) => void;
  deleteLayer: (id: string) => void;
  duplicateLayer: (id: string) => void;
  invertLayerColors: (id: string) => void;
  copyLayerCss: (id: string) => void;
  moveLayerUp: (id: string) => void;
  moveLayerDown: (id: string) => void;
  reorderLayers: (startIndex: number, endIndex: number) => void;
  setCanvasShape: (shape: 'rounded' | 'circle' | 'full' | 'text') => void;
  setColorBlindMode: (mode: ColorBlindMode) => void;
  setPostFx: (fx: PostProcessingFx) => void;
  setFxIntensity: (intensity: number) => void;
  setMouseReact: (active: boolean) => void;
  setAnimStatus: (status: AnimStatus) => void;
  setAnimStyle: (style: AnimStyle) => void;
  setAnimSpeed: (speed: number) => void;
  setAnimIntensity: (intensity: number) => void;
  setGlobalAdjustments: (updates: Partial<GlobalAdjustments>) => void;
  applyPreset: (presetIndex: number) => void;
  randomize: () => void;
  showToast: (msg: string) => void;
}

const defaultAdjustments: GlobalAdjustments = {
  brightness: 100,
  contrast: 100,
  saturation: 100,
  hueShift: 0,
  blur: 0
};

const defaultState: StudioState = {
  layers: [],
  canvasShape: 'rounded',
  colorBlindMode: ColorBlindMode.NONE,
  postFx: PostProcessingFx.NONE,
  fxIntensity: 50,
  mouseReact: false,
  animStatus: 'playing',
  animStyle: AnimStyle.DRIFT,
  animSpeed: 50,
  animIntensity: 50,
  globalAdjustments: defaultAdjustments,
  toast: { message: '', visible: false },
  
  addLayer: () => {},
  updateLayer: () => {},
  deleteLayer: () => {},
  duplicateLayer: () => {},
  invertLayerColors: () => {},
  copyLayerCss: () => {},
  moveLayerUp: () => {},
  moveLayerDown: () => {},
  reorderLayers: () => {},
  setCanvasShape: () => {},
  setColorBlindMode: () => {},
  setPostFx: () => {},
  setFxIntensity: () => {},
  setMouseReact: () => {},
  setAnimStatus: () => {},
  setAnimStyle: () => {},
  setAnimSpeed: () => {},
  setAnimIntensity: () => {},
  setGlobalAdjustments: () => {},
  applyPreset: () => {},
  randomize: () => {},
  showToast: () => {},
};

const StudioContext = createContext<StudioState>(defaultState);

export const useStudio = () => useContext(StudioContext);

export const StudioProvider = ({ children }: { children: ReactNode }) => {
  const [layers, setLayers] = useState<GradientLayer[]>([
    {
      id: crypto.randomUUID(),
      name: 'Layer 1',
      type: LayerType.AURORA,
      blendMode: ChromaBlendMode.NORMAL,
      opacity: 1,
      angle: 135,
      centerX: 50,
      centerY: 50,
      visible: true,
      expanded: true,
      animated: false,
      animSpeed: 50,
      animIntensity: 50,
      repeatPattern: false,
      width: 70,
      height: 70,
      feather: 100,
      waveSpeed: 50,
      complexity: 50,
      brightness: 100,
      contrast: 100,
      saturation: 100,
      hueShift: 0,
      blur: 0,
      columns: 3,
      rows: 3,
      hasBaseBackground: false,
      blobBgColor: '#000000',
      activeBlobIdx: 0,
      blobs: [
        { x: 30, y: 40, width: 40, height: 40, feather: 100, opacity: 1, rotation: 0 },
        { x: 70, y: 60, width: 50, height: 50, feather: 100, opacity: 1, rotation: 0 }
      ],
      meshPoints: [
        { x: 25, y: 40 },
        { x: 75, y: 60 }
      ],
      stops: [
        { id: crypto.randomUUID(), color: '#FF2C55', position: 0 },
        { id: crypto.randomUUID(), color: '#007BFF', position: 50 },
        { id: crypto.randomUUID(), color: '#FFD60A', position: 100 }
      ]
    }
  ]);

  const [canvasShape, setCanvasShape] = useState<'rounded' | 'circle' | 'full' | 'text'>('rounded');
  const [colorBlindMode, setColorBlindMode] = useState<ColorBlindMode>(ColorBlindMode.NONE);
  const [postFx, setPostFx] = useState<PostProcessingFx>(PostProcessingFx.NONE);
  const [fxIntensity, setFxIntensity] = useState(50);
  const [mouseReact, setMouseReact] = useState(false);
  const [animStatus, setAnimStatus] = useState<AnimStatus>('playing');
  const [animStyle, setAnimStyle] = useState<AnimStyle>(AnimStyle.DRIFT);
  const [animSpeed, setAnimSpeed] = useState(50);
  const [animIntensity, setAnimIntensity] = useState(50);
  const [globalAdjustments, setGlobalAdjustmentsState] = useState<GlobalAdjustments>(defaultAdjustments);
  const [toast, setToast] = useState<ToastState>({ message: '', visible: false });

  const setGlobalAdjustments = (updates: Partial<GlobalAdjustments>) => {
    setGlobalAdjustmentsState(prev => ({ ...prev, ...updates }));
  };

  const showToast = (msg: string) => {
    setToast({ message: msg, visible: true });
    setTimeout(() => {
      setToast({ message: '', visible: false });
    }, 2500);
  };

  const addLayer = (type: LayerType = LayerType.LINEAR) => {
    const newLayer: GradientLayer = {
      id: crypto.randomUUID(),
      name: `Layer ${layers.length + 1}`,
      type,
      blendMode: ChromaBlendMode.NORMAL,
      opacity: 1,
      angle: 135,
      centerX: 50,
      centerY: 50,
      visible: true,
      expanded: true,
      animated: false,
      animSpeed: 50,
      animIntensity: 50,
      repeatPattern: false,
      width: 70,
      height: 70,
      feather: 100,
      waveSpeed: 50,
      complexity: 50,
      brightness: 100,
      contrast: 100,
      saturation: 100,
      hueShift: 0,
      blur: 0,
      columns: 3,
      rows: 3,
      hasBaseBackground: false,
      blobBgColor: '#000000',
      activeBlobIdx: 0,
      blobs: [
        { x: 30, y: 40, width: 40, height: 40, feather: 100, opacity: 1, rotation: 0 },
        { x: 70, y: 60, width: 50, height: 50, feather: 100, opacity: 1, rotation: 0 }
      ],
      meshPoints: [
        { x: 25, y: 40 },
        { x: 75, y: 60 }
      ],
      stops: [
        { id: crypto.randomUUID(), color: '#4F46E5', position: 0 },
        { id: crypto.randomUUID(), color: '#00C8E8', position: 100 }
      ]
    };
    setLayers([newLayer, ...layers]);
    showToast('Layer added');
  };

  const updateLayer = (id: string, updates: Partial<GradientLayer>) => {
    setLayers(layers.map(l => l.id === id ? { ...l, ...updates } : l));
  };

  const deleteLayer = (id: string) => {
    if (layers.length <= 1) {
      showToast('Need at least 1 layer');
      return;
    }
    setLayers(layers.filter(l => l.id !== id));
    showToast('Layer deleted');
  };

  const duplicateLayer = (id: string) => {
    const idx = layers.findIndex(l => l.id === id);
    if (idx === -1) return;
    const target = layers[idx];
    const cloned: GradientLayer = {
      ...JSON.parse(JSON.stringify(target)),
      id: crypto.randomUUID(),
      name: `${target.name} (Copy)`,
      expanded: true
    };
    const updated = [...layers];
    updated.splice(idx, 0, cloned);
    setLayers(updated);
    showToast('Layer duplicated');
  };

  const invertLayerColors = (id: string) => {
    setLayers(layers.map(l => {
      if (l.id !== id) return l;
      const reversedStops = l.stops.map(st => ({
        ...st,
        position: 100 - st.position
      })).reverse();
      return { ...l, stops: reversedStops };
    }));
    showToast('Inverted layer colors');
  };

  const copyLayerCss = (id: string) => {
    const layer = layers.find(l => l.id === id);
    if (!layer) return;
    const colorStops = layer.stops
      .slice()
      .sort((a, b) => a.position - b.position)
      .map(stop => `${stop.color} ${stop.position}%`)
      .join(', ');
    
    let css = '';
    switch (layer.type) {
      case LayerType.LINEAR:
        css = `background: linear-gradient(${layer.angle}deg, ${colorStops});`;
        break;
      case LayerType.RADIAL:
        css = `background: radial-gradient(ellipse ${layer.width}% ${layer.height}% at ${layer.centerX}% ${layer.centerY}%, ${colorStops});`;
        break;
      case LayerType.CONIC:
        css = `background: conic-gradient(from ${layer.angle}deg at ${layer.centerX}% ${layer.centerY}%, ${colorStops});`;
        break;
      default:
        css = `/* ${layer.name} (${layer.type}) */\nopacity: ${layer.opacity};`;
        break;
    }
    
    navigator.clipboard.writeText(css);
    showToast('Copied Layer CSS to clipboard!');
  };

  const moveLayerUp = (id: string) => {
    const idx = layers.findIndex(l => l.id === id);
    if (idx > 0) {
      const copy = [...layers];
      const temp = copy[idx];
      copy[idx] = copy[idx - 1];
      copy[idx - 1] = temp;
      setLayers(copy);
    }
  };

  const moveLayerDown = (id: string) => {
    const idx = layers.findIndex(l => l.id === id);
    if (idx < layers.length - 1) {
      const copy = [...layers];
      const temp = copy[idx];
      copy[idx] = copy[idx + 1];
      copy[idx + 1] = temp;
      setLayers(copy);
    }
  };

  const reorderLayers = (startIndex: number, endIndex: number) => {
    const result = Array.from(layers);
    const [removed] = result.splice(startIndex, 1);
    result.splice(endIndex, 0, removed);
    setLayers(result);
  };

  const applyPreset = (presetIndex: number) => {
    const preset = ApplePresets[presetIndex];
    if (preset) {
      const newLayer: GradientLayer = {
        id: crypto.randomUUID(),
        name: preset.name,
        type: preset.type,
        blendMode: preset.type === LayerType.AURORA ? ChromaBlendMode.SCREEN : ChromaBlendMode.NORMAL,
        opacity: 1,
        angle: 135,
        centerX: 50,
        centerY: 50,
        visible: true,
        expanded: true,
        animated: false,
        animSpeed: 50,
        animIntensity: 50,
        repeatPattern: false,
        width: 70,
        height: 70,
        feather: 100,
        waveSpeed: 50,
        complexity: 50,
        brightness: 100,
        contrast: 100,
        saturation: 100,
        hueShift: 0,
        blur: 0,
        columns: 3,
        rows: 3,
        hasBaseBackground: false,
        blobBgColor: '#000000',
        activeBlobIdx: 0,
        blobs: [
          { x: 32, y: 31, width: 40, height: 40, feather: 100, opacity: 1, rotation: 0 },
          { x: 66, y: 62, width: 50, height: 50, feather: 100, opacity: 1, rotation: 0 }
        ],
        meshPoints: [
          { x: 25, y: 40 },
          { x: 75, y: 60 }
        ],
        stops: preset.colors.map((c, i) => ({
          id: crypto.randomUUID(),
          color: c,
          position: Math.round((i / (preset.colors.length - 1 || 1)) * 100)
        }))
      };
      setLayers([newLayer]);
      showToast(`Applied ${preset.name}`);
    }
  };

  const randomize = () => {
    const types = [LayerType.LINEAR, LayerType.RADIAL, LayerType.MESH, LayerType.BLOB, LayerType.CONIC, LayerType.AURORA];
    const blends = [ChromaBlendMode.NORMAL, ChromaBlendMode.MULTIPLY, ChromaBlendMode.SCREEN, ChromaBlendMode.OVERLAY];
    const count = 2 + Math.floor(Math.random() * 2);

    const newLayers: GradientLayer[] = [];
    for (let i = 0; i < count; i++) {
      const type = types[Math.floor(Math.random() * types.length)];
      const stopsCount = type === LayerType.MESH ? 9 : 3;
      const stops = Array.from({ length: stopsCount }).map((_, j) => {
        const r = Math.floor(Math.random() * 256);
        const g = Math.floor(Math.random() * 256);
        const b = Math.floor(Math.random() * 256);
        const hex = ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1).toUpperCase();
        return {
          id: crypto.randomUUID(),
          color: `#${hex}`,
          position: Math.round((j / (stopsCount - 1)) * 100)
        };
      });

      newLayers.push({
        id: crypto.randomUUID(),
        name: `Mix ${i + 1}`,
        type,
        blendMode: i === 0 ? ChromaBlendMode.NORMAL : blends[Math.floor(Math.random() * blends.length)],
        opacity: 1,
        angle: Math.floor(Math.random() * 360),
        centerX: 50,
        centerY: 50,
        visible: true,
        expanded: i === 0,
        animated: false,
        animSpeed: 50,
        animIntensity: 50,
        repeatPattern: false,
        width: 70,
        height: 70,
        feather: 100,
        waveSpeed: 50,
        complexity: 50,
        brightness: 100,
        contrast: 100,
        saturation: 100,
        hueShift: 0,
        blur: 0,
        columns: 3,
        rows: 3,
        hasBaseBackground: false,
        blobBgColor: '#000000',
        activeBlobIdx: 0,
        blobs: [
          { x: 20 + Math.random() * 60, y: 20 + Math.random() * 60, width: 30 + Math.random() * 50, height: 30 + Math.random() * 50, feather: 100, opacity: 1, rotation: 0 }
        ],
        meshPoints: [
          { x: 25, y: 40 },
          { x: 75, y: 60 }
        ],
        stops
      });
    }

    setLayers(newLayers);
    showToast('Randomized Studio');
  };

  return (
    <StudioContext.Provider
      value={{
        layers,
        canvasShape,
        colorBlindMode,
        postFx,
        fxIntensity,
        mouseReact,
        animStatus,
        animStyle,
        animSpeed,
        animIntensity,
        globalAdjustments,
        toast,
        addLayer,
        updateLayer,
        deleteLayer,
        duplicateLayer,
        invertLayerColors,
        copyLayerCss,
        moveLayerUp,
        moveLayerDown,
        reorderLayers,
        setCanvasShape,
        setColorBlindMode,
        setPostFx,
        setFxIntensity,
        setMouseReact,
        setAnimStatus,
        setAnimStyle,
        setAnimSpeed,
        setAnimIntensity,
        setGlobalAdjustments,
        applyPreset,
        randomize,
        showToast
      }}
    >
      {children}
    </StudioContext.Provider>
  );
};
