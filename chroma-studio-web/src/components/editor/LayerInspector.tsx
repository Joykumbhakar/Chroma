'use client';

import React from 'react';
import { GradientLayer, LayerType, BlobPoint } from '@/types/models';
import { Plus, X, CursorClick } from '@phosphor-icons/react';

interface LayerInspectorProps {
  layer: GradientLayer;
  onUpdateLayer: (updates: Partial<GradientLayer>) => void;
}

export default function LayerInspector({ layer, onUpdateLayer }: LayerInspectorProps) {
  const activeBlobIdx = layer.activeBlobIdx || 0;
  const activeBlob = layer.blobs[activeBlobIdx] || layer.blobs[0] || { x: 50, y: 50, width: 40, height: 40, feather: 100, opacity: 1, rotation: 0 };

  const updateActiveBlob = (updates: Partial<BlobPoint>) => {
    const updatedBlobs = layer.blobs.map((b, i) => i === activeBlobIdx ? { ...b, ...updates } : b);
    onUpdateLayer({ blobs: updatedBlobs });
  };

  const addBlobElement = () => {
    const newBlob: BlobPoint = {
      x: 30 + Math.random() * 40,
      y: 30 + Math.random() * 40,
      width: 40,
      height: 40,
      feather: 100,
      opacity: 1,
      rotation: 0
    };
    const updatedBlobs = [...layer.blobs, newBlob];
    onUpdateLayer({ blobs: updatedBlobs, activeBlobIdx: updatedBlobs.length - 1 });
  };

  const removeBlobElement = (index: number) => {
    if (layer.blobs.length <= 1) return;
    const updatedBlobs = layer.blobs.filter((_, i) => i !== index);
    onUpdateLayer({ blobs: updatedBlobs, activeBlobIdx: Math.max(0, activeBlobIdx - 1) });
  };

  const regenerateMesh = () => {
    const points = layer.stops.map(() => ({
      x: Math.round(10 + Math.random() * 80),
      y: Math.round(10 + Math.random() * 80)
    }));
    onUpdateLayer({ meshPoints: points });
  };

  return (
    <div className="flex flex-col gap-3">
      {/* LINEAR CONTROLS */}
      {layer.type === LayerType.LINEAR && (
        <div className="flex flex-col gap-3">
          <div className="field mb-0">
            <label className="field-label flex justify-between items-center text-[9px]">
              Angle <span className="font-mono text-[var(--text-main)] normal-case">{layer.angle}°</span>
            </label>
            <div className="range-wrap">
              <input
                type="range"
                className="range"
                min="0"
                max="360"
                value={layer.angle}
                onChange={(e) => onUpdateLayer({ angle: Number(e.target.value) })}
              />
            </div>
          </div>

          <div className="switch-container py-0">
            <span className="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-widest">Repeat Pattern</span>
            <label className="switch">
              <input
                type="checkbox"
                checked={layer.repeatPattern}
                onChange={(e) => onUpdateLayer({ repeatPattern: e.target.checked })}
              />
              <span className="slider"></span>
            </label>
          </div>
        </div>
      )}

      {/* RADIAL CONTROLS */}
      {layer.type === LayerType.RADIAL && (
        <div className="grid grid-cols-2 gap-3">
          <div className="field mb-0">
            <label className="field-label flex justify-between items-center text-[9px]">
              Width <span className="font-mono text-[var(--text-main)] normal-case">{layer.width}%</span>
            </label>
            <div className="range-wrap">
              <input
                type="range"
                className="range"
                min="10"
                max="200"
                value={layer.width}
                onChange={(e) => onUpdateLayer({ width: Number(e.target.value) })}
              />
            </div>
          </div>

          <div className="field mb-0">
            <label className="field-label flex justify-between items-center text-[9px]">
              Height <span className="font-mono text-[var(--text-main)] normal-case">{layer.height}%</span>
            </label>
            <div className="range-wrap">
              <input
                type="range"
                className="range"
                min="10"
                max="200"
                value={layer.height}
                onChange={(e) => onUpdateLayer({ height: Number(e.target.value) })}
              />
            </div>
          </div>

          <div className="field mb-0">
            <label className="field-label flex justify-between items-center text-[9px]">
              Center X <span className="font-mono text-[var(--text-main)] normal-case">{layer.centerX}%</span>
            </label>
            <div className="range-wrap">
              <input
                type="range"
                className="range"
                min="0"
                max="100"
                value={layer.centerX}
                onChange={(e) => onUpdateLayer({ centerX: Number(e.target.value) })}
              />
            </div>
          </div>

          <div className="field mb-0">
            <label className="field-label flex justify-between items-center text-[9px]">
              Center Y <span className="font-mono text-[var(--text-main)] normal-case">{layer.centerY}%</span>
            </label>
            <div className="range-wrap">
              <input
                type="range"
                className="range"
                min="0"
                max="100"
                value={layer.centerY}
                onChange={(e) => onUpdateLayer({ centerY: Number(e.target.value) })}
              />
            </div>
          </div>
        </div>
      )}

      {/* CONIC & AURORA CONTROLS */}
      {(layer.type === LayerType.CONIC || layer.type === LayerType.AURORA) && (
        <div className="flex flex-col gap-3">
          <div className="field mb-0">
            <label className="field-label flex justify-between items-center text-[9px]">
              Angle <span className="font-mono text-[var(--text-main)] normal-case">{layer.angle}°</span>
            </label>
            <div className="range-wrap">
              <input
                type="range"
                className="range"
                min="0"
                max="360"
                value={layer.angle}
                onChange={(e) => onUpdateLayer({ angle: Number(e.target.value) })}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="field mb-0">
              <label className="field-label flex justify-between items-center text-[9px]">
                Center X <span className="font-mono text-[var(--text-main)] normal-case">{layer.centerX}%</span>
              </label>
              <div className="range-wrap">
                <input
                  type="range"
                  className="range"
                  min="0"
                  max="100"
                  value={layer.centerX}
                  onChange={(e) => onUpdateLayer({ centerX: Number(e.target.value) })}
                />
              </div>
            </div>

            <div className="field mb-0">
              <label className="field-label flex justify-between items-center text-[9px]">
                Center Y <span className="font-mono text-[var(--text-main)] normal-case">{layer.centerY}%</span>
              </label>
              <div className="range-wrap">
                <input
                  type="range"
                  className="range"
                  min="0"
                  max="100"
                  value={layer.centerY}
                  onChange={(e) => onUpdateLayer({ centerY: Number(e.target.value) })}
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MESH CONTROLS */}
      {layer.type === LayerType.MESH && (
        <div className="flex flex-col gap-3">
          <div className="grid grid-cols-2 gap-3">
            <div className="field mb-0">
              <label className="field-label flex justify-between items-center text-[9px]">
                Columns <span className="font-mono text-[var(--text-main)] normal-case">{layer.columns || 3}</span>
              </label>
              <div className="range-wrap">
                <input
                  type="range"
                  className="range"
                  min="2"
                  max="6"
                  value={layer.columns || 3}
                  onChange={(e) => {
                    const cols = Number(e.target.value);
                    const count = cols * (layer.rows || 3);
                    const newPoints = Array.from({ length: count }).map((_, i) => ({
                      x: Math.round(((i % cols) / (cols - 1 || 1)) * 80 + 10),
                      y: Math.round((Math.floor(i / cols) / ((layer.rows || 3) - 1 || 1)) * 80 + 10)
                    }));
                    onUpdateLayer({ columns: cols, meshPoints: newPoints });
                  }}
                />
              </div>
            </div>

            <div className="field mb-0">
              <label className="field-label flex justify-between items-center text-[9px]">
                Rows <span className="font-mono text-[var(--text-main)] normal-case">{layer.rows || 3}</span>
              </label>
              <div className="range-wrap">
                <input
                  type="range"
                  className="range"
                  min="2"
                  max="6"
                  value={layer.rows || 3}
                  onChange={(e) => {
                    const rows = Number(e.target.value);
                    const cols = layer.columns || 3;
                    const count = cols * rows;
                    const newPoints = Array.from({ length: count }).map((_, i) => ({
                      x: Math.round(((i % cols) / (cols - 1 || 1)) * 80 + 10),
                      y: Math.round((Math.floor(i / cols) / (rows - 1 || 1)) * 80 + 10)
                    }));
                    onUpdateLayer({ rows, meshPoints: newPoints });
                  }}
                />
              </div>
            </div>
          </div>

          <button
            className="btn btn-ghost text-xs border-dashed hover:border-solid hover:border-[var(--primary)]"
            onClick={regenerateMesh}
          >
            Randomize Mesh Layout
          </button>
        </div>
      )}

      {/* BLOB & LIQUID CONTROLS */}
      {(layer.type === LayerType.BLOB || layer.type === LayerType.LIQUID) && (
        <div className="flex flex-col gap-3">
          <div className="bg-[rgba(79,70,229,0.08)] border border-[rgba(79,70,229,0.2)] rounded-lg p-2.5 flex gap-2 items-center text-[10px] text-[var(--text-muted)] leading-snug">
            <CursorClick size={20} className="text-[var(--primary)] shrink-0" />
            <p><b>Canvas active:</b> Drag handles on the preview canvas to position elements.</p>
          </div>

          {/* Active Blob controls */}
          <div className="glass-panel border border-[var(--glass-border)] rounded-xl p-3 flex flex-col gap-2">
            <div className="grid grid-cols-2 gap-3">
              <div className="field mb-0">
                <label className="field-label flex justify-between items-center text-[9px]">
                  Position X <span className="font-mono text-[var(--text-main)] normal-case">{Math.round(activeBlob.x)}%</span>
                </label>
                <div className="range-wrap">
                  <input
                    type="range"
                    className="range"
                    min="0"
                    max="100"
                    value={activeBlob.x}
                    onChange={(e) => updateActiveBlob({ x: Number(e.target.value) })}
                  />
                </div>
              </div>

              <div className="field mb-0">
                <label className="field-label flex justify-between items-center text-[9px]">
                  Position Y <span className="font-mono text-[var(--text-main)] normal-case">{Math.round(activeBlob.y)}%</span>
                </label>
                <div className="range-wrap">
                  <input
                    type="range"
                    className="range"
                    min="0"
                    max="100"
                    value={activeBlob.y}
                    onChange={(e) => updateActiveBlob({ y: Number(e.target.value) })}
                  />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 mt-1">
              <div className="field mb-0">
                <label className="field-label flex justify-between items-center text-[9px]">
                  Width <span className="font-mono text-[var(--text-main)] normal-case">{Math.round(activeBlob.width)}%</span>
                </label>
                <div className="range-wrap">
                  <input
                    type="range"
                    className="range"
                    min="10"
                    max="200"
                    value={activeBlob.width}
                    onChange={(e) => updateActiveBlob({ width: Number(e.target.value) })}
                  />
                </div>
              </div>

              <div className="field mb-0">
                <label className="field-label flex justify-between items-center text-[9px]">
                  Height <span className="font-mono text-[var(--text-main)] normal-case">{Math.round(activeBlob.height)}%</span>
                </label>
                <div className="range-wrap">
                  <input
                    type="range"
                    className="range"
                    min="10"
                    max="200"
                    value={activeBlob.height}
                    onChange={(e) => updateActiveBlob({ height: Number(e.target.value) })}
                  />
                </div>
              </div>
            </div>

            <div className="field mb-0 mt-1">
              <label className="field-label flex justify-between items-center text-[9px]">
                Feather <span className="font-mono text-[var(--text-main)] normal-case">{Math.round(activeBlob.feather)}%</span>
              </label>
              <div className="range-wrap">
                <input
                  type="range"
                  className="range"
                  min="0"
                  max="100"
                  value={activeBlob.feather}
                  onChange={(e) => updateActiveBlob({ feather: Number(e.target.value) })}
                />
              </div>
            </div>
          </div>

          {/* Elements list */}
          <div className="flex flex-col gap-1.5">
            <label className="field-label mb-0">Blob Elements ({layer.blobs.length})</label>
            <div className="flex flex-col gap-1">
              {layer.blobs.map((blob, idx) => (
                <div
                  key={idx}
                  onClick={() => onUpdateLayer({ activeBlobIdx: idx })}
                  className={`flex items-center gap-2 p-2 rounded-lg border text-xs cursor-pointer transition-colors ${
                    idx === activeBlobIdx
                      ? 'border-[var(--primary)] bg-[var(--primary)]/10'
                      : 'border-[var(--glass-border)] bg-[var(--glass-bg)] hover:bg-[var(--glass-bg-hover)]'
                  }`}
                >
                  <div
                    className="w-4 h-4 rounded-full border border-white shadow-sm shrink-0"
                    style={{ backgroundColor: layer.stops[idx]?.color || layer.stops[0]?.color || '#4F46E5' }}
                  />
                  <span className="flex-1 font-mono text-[10px] text-[var(--text-main)] font-bold">
                    Blob #{idx + 1} ({Math.round(blob.x)}%, {Math.round(blob.y)}%)
                  </span>
                  {layer.blobs.length > 1 && (
                    <button
                      className="text-[var(--text-muted)] hover:text-red-500 p-0.5"
                      onClick={(e) => { e.stopPropagation(); removeBlobElement(idx); }}
                    >
                      <X size={12} />
                    </button>
                  )}
                </div>
              ))}
            </div>

            <button
              className="btn btn-ghost text-xs w-full py-1.5 border-dashed hover:border-solid hover:border-[var(--primary)] mt-1"
              onClick={addBlobElement}
            >
              <Plus size={12} /> Add Blob Element
            </button>
          </div>

          {/* Base Background setting */}
          <div className="glass-panel border border-[var(--glass-border)] rounded-xl p-3 flex flex-col gap-2 shadow-inner">
            <div className="switch-container py-0">
              <span className="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-widest">Base Background</span>
              <label className="switch">
                <input
                  type="checkbox"
                  checked={layer.hasBaseBackground}
                  onChange={(e) => onUpdateLayer({ hasBaseBackground: e.target.checked })}
                />
                <span className="slider"></span>
              </label>
            </div>

            {layer.hasBaseBackground && (
              <div className="flex items-center gap-3 mt-2">
                <input
                  type="color"
                  value={layer.blobBgColor}
                  onChange={(e) => onUpdateLayer({ blobBgColor: e.target.value })}
                  className="w-8 h-8 rounded border border-[var(--glass-border)] cursor-pointer bg-transparent"
                />
                <span className="font-mono text-xs font-bold text-[var(--text-main)] uppercase">
                  {layer.blobBgColor}
                </span>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
