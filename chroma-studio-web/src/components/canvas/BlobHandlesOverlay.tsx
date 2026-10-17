'use client';

import React, { useRef } from 'react';
import { useStudio } from '@/context/StudioContext';
import { LayerType } from '@/types/models';

export default function BlobHandlesOverlay() {
  const { layers, updateLayer } = useStudio();
  const overlayRef = useRef<HTMLDivElement>(null);

  // Active layer check
  const activeLayer = layers.find(l => l.expanded && l.visible);

  if (!activeLayer) return null;

  const isBlobOrLiquid = activeLayer.type === LayerType.BLOB || activeLayer.type === LayerType.LIQUID;
  const isMesh = activeLayer.type === LayerType.MESH;

  if (!isBlobOrLiquid && !isMesh) return null;

  const handlePointerDown = (index: number, e: React.PointerEvent) => {
    e.stopPropagation();
    if (!overlayRef.current) return;
    const rect = overlayRef.current.getBoundingClientRect();

    const onPointerMove = (pe: PointerEvent) => {
      const x = Math.min(100, Math.max(0, ((pe.clientX - rect.left) / rect.width) * 100));
      const y = Math.min(100, Math.max(0, ((pe.clientY - rect.top) / rect.height) * 100));

      if (isBlobOrLiquid) {
        const updatedBlobs = activeLayer.blobs.map((b, i) =>
          i === index ? { ...b, x, y } : b
        );
        updateLayer(activeLayer.id, { blobs: updatedBlobs, activeBlobIdx: index });
      } else if (isMesh) {
        const updatedPoints = activeLayer.meshPoints.map((pt, i) =>
          i === index ? { x, y } : pt
        );
        updateLayer(activeLayer.id, { meshPoints: updatedPoints });
      }
    };

    const onPointerUp = () => {
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerup', onPointerUp);
    };

    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);
  };

  return (
    <div ref={overlayRef} className="absolute inset-0 w-full h-full pointer-events-none z-[160]">
      {isBlobOrLiquid &&
        activeLayer.blobs.map((blob, idx) => {
          const isActive = activeLayer.activeBlobIdx === idx;
          const stopColor = activeLayer.stops[idx]?.color || activeLayer.stops[0]?.color || '#4F46E5';

          return (
            <React.Fragment key={idx}>
              {/* Outer feather ring */}
              <div
                className="blob-ring-node absolute rounded-full border border-dashed border-white/40 -translate-x-1/2 -translate-y-1/2 pointer-events-none"
                style={{
                  left: `${blob.x}%`,
                  top: `${blob.y}%`,
                  width: `${blob.width}%`,
                  height: `${blob.height}%`
                }}
              />

              {/* Draggable Handle */}
              <div
                onPointerDown={(e) => handlePointerDown(idx, e)}
                className={`blob-handle-node absolute w-6 h-6 rounded-full border-2 border-white cursor-grab active:cursor-grabbing pointer-events-auto -translate-x-1/2 -translate-y-1/2 transition-transform hover:scale-125 ${
                  isActive ? 'ring-4 ring-[var(--primary)] scale-110 z-20' : 'z-10'
                }`}
                style={{
                  left: `${blob.x}%`,
                  top: `${blob.y}%`,
                  backgroundColor: stopColor
                }}
              />
            </React.Fragment>
          );
        })}

      {isMesh &&
        activeLayer.meshPoints.map((pt, idx) => {
          const stopColor = activeLayer.stops[idx]?.color || '#818CF8';
          return (
            <div
              key={idx}
              onPointerDown={(e) => handlePointerDown(idx, e)}
              className="blob-handle-node absolute w-5 h-5 rounded-full border-2 border-white cursor-grab active:cursor-grabbing pointer-events-auto -translate-x-1/2 -translate-y-1/2 hover:scale-125 z-20"
              style={{
                left: `${pt.x}%`,
                top: `${pt.y}%`,
                backgroundColor: stopColor
              }}
            />
          );
        })}
    </div>
  );
}
