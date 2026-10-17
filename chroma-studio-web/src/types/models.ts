export enum LayerType {
  LINEAR = 'LINEAR',
  RADIAL = 'RADIAL',
  CONIC = 'CONIC',
  MESH = 'MESH',
  BLOB = 'BLOB',
  AURORA = 'AURORA',
  LIQUID = 'LIQUID',
}

export const LayerTypeDetails: Record<LayerType, { label: string; icon: string }> = {
  [LayerType.LINEAR]: { label: 'Linear', icon: 'ph-arrow-up-right' },
  [LayerType.RADIAL]: { label: 'Radial', icon: 'ph-circle' },
  [LayerType.CONIC]: { label: 'Conic', icon: 'ph-cone' },
  [LayerType.MESH]: { label: 'Mesh', icon: 'ph-grid-four' },
  [LayerType.BLOB]: { label: 'Blob', icon: 'ph-drop' },
  [LayerType.AURORA]: { label: 'Aurora', icon: 'ph-sparkle' },
  [LayerType.LIQUID]: { label: 'Liquid', icon: 'ph-drop' },
};

export enum ChromaBlendMode {
  NORMAL = 'normal',
  MULTIPLY = 'multiply',
  SCREEN = 'screen',
  OVERLAY = 'overlay',
  DARKEN = 'darken',
  LIGHTEN = 'lighten',
  COLOR_DODGE = 'color-dodge',
  DIFFERENCE = 'difference',
}

export const BlendModeLabels: Record<ChromaBlendMode, string> = {
  [ChromaBlendMode.NORMAL]: 'Normal',
  [ChromaBlendMode.MULTIPLY]: 'Multiply',
  [ChromaBlendMode.SCREEN]: 'Screen',
  [ChromaBlendMode.OVERLAY]: 'Overlay',
  [ChromaBlendMode.DARKEN]: 'Darken',
  [ChromaBlendMode.LIGHTEN]: 'Lighten',
  [ChromaBlendMode.COLOR_DODGE]: 'Color Dodge',
  [ChromaBlendMode.DIFFERENCE]: 'Difference',
};

export enum ColorBlindMode {
  NONE = 'none',
  PROTANOPIA = 'protanopia',
  DEUTERANOPIA = 'deuteranopia',
  TRITANOPIA = 'tritanopia',
}

export enum PostProcessingFx {
  NONE = 'none',
  GRAIN = 'grain',
  HALFTONE = 'halftone',
  DITHER = 'dither',
}

export enum AnimStyle {
  DRIFT = 'drift',
  PULSE = 'pulse',
  HUE = 'hue',
  BREATHE = 'breathe',
}

export type AnimStatus = 'stopped' | 'playing' | 'paused';

export interface ColorStop {
  id: string;
  color: string;
  position: number;
}

export interface BlobPoint {
  x: number;
  y: number;
  width: number;
  height: number;
  feather: number;
  opacity: number;
  rotation: number;
}

export interface MeshPoint {
  x: number;
  y: number;
}

export interface GradientLayer {
  id: string;
  name: string;
  type: LayerType;
  blendMode: ChromaBlendMode;
  opacity: number;
  angle: number;
  centerX: number;
  centerY: number;
  visible: boolean;
  expanded: boolean;
  animated: boolean;
  animSpeed: number;
  animIntensity: number;
  repeatPattern: boolean;
  width: number;
  height: number;
  feather: number;
  waveSpeed: number;
  complexity: number;
  brightness: number; // 0..200%
  contrast: number;   // 0..200%
  saturation: number; // 0..200%
  hueShift: number;   // 0..360 deg
  blur: number;       // 0..50 px
  columns: number;
  rows: number;
  hasBaseBackground: boolean;
  blobBgColor: string;
  activeBlobIdx: number;
  blobs: BlobPoint[];
  meshPoints: MeshPoint[];
  stops: ColorStop[];
}

export interface GlobalAdjustments {
  brightness: number; // 50..150
  contrast: number;   // 50..150
  saturation: number; // 0..200
  hueShift: number;   // 0..360
  blur: number;       // 0..20
}

export interface GradientPreset {
  name: string;
  colors: string[];
  type: LayerType;
}

export const ApplePresets: GradientPreset[] = [
  { name: 'Sunset', colors: ['#FFFF7E5F', '#FFFEB47B'], type: LayerType.LINEAR },
  { name: 'Ocean', colors: ['#FF2E3192', '#FF1BFFFF'], type: LayerType.LINEAR },
  { name: 'Candy', colors: ['#FFFC466B', '#FF3F5EFB'], type: LayerType.LINEAR },
  { name: 'Mint', colors: ['#FF00B09B', '#FF96C93D'], type: LayerType.LINEAR },
  { name: 'Peach', colors: ['#FFFFECD2', '#FFFCB69F'], type: LayerType.RADIAL },
  { name: 'Aurora', colors: ['#FF00C9FF', '#FF92FE9D'], type: LayerType.AURORA },
  { name: 'Grape', colors: ['#FF7F00FF', '#FFE100FF'], type: LayerType.CONIC },
  { name: 'Fire', colors: ['#FFF83600', '#FFF9D423'], type: LayerType.LINEAR },
];
