'use client';

import React, { useEffect, useState } from 'react';
import { useDesign } from '@/context/DesignContext';
import { ArrowsOut, CornersIn, CornersOut, TextAa, Drop, LinkSimple, Image as ImageIcon } from '@phosphor-icons/react';
import clsx from 'clsx';

export default function ObjectInspector() {
  const { canvas, selectedObject } = useDesign();
  // We use a dummy state to force re-render when object properties change during drag
  const [, setTick] = useState(0);

  useEffect(() => {
    if (!canvas) return;
    const updateTick = () => setTick(t => t + 1);
    canvas.on('object:modified', updateTick);
    canvas.on('object:moving', updateTick);
    canvas.on('object:scaling', updateTick);
    canvas.on('object:rotating', updateTick);
    return () => {
      canvas.off('object:modified', updateTick);
      canvas.off('object:moving', updateTick);
      canvas.off('object:scaling', updateTick);
      canvas.off('object:rotating', updateTick);
    };
  }, [canvas]);

  if (!selectedObject) return null;

  const updateObj = (prop: string, val: any) => {
    if (prop === 'width') selectedObject.scaleToWidth(val);
    else if (prop === 'height') selectedObject.scaleToHeight(val);
    else {
      // If it's a group and we are changing fill/stroke, target the first child (background)
      if (selectedObject.type === 'group' && (prop === 'fill' || prop === 'stroke' || prop === 'strokeWidth')) {
        const bg = selectedObject.getObjects()[0];
        if (bg) bg.set(prop, val);
      } else {
        selectedObject.set(prop, val);
        if (prop === 'opacity') selectedObject.set('opacity', val / 100);
      }
      selectedObject.setCoords();
    }
    canvas?.requestRenderAll();
    setTick(t => t + 1);
  };

  const getPropValue = (prop: string) => {
    if (selectedObject.type === 'group' && (prop === 'fill' || prop === 'stroke' || prop === 'strokeWidth')) {
      const bg = selectedObject.getObjects()[0];
      return bg ? bg.get(prop) : selectedObject.get(prop);
    }
    return selectedObject.get(prop);
  };

  const isText = selectedObject.type === 'i-text' || selectedObject.type === 'text';
  const isRect = selectedObject.type === 'rect';
  const isImage = selectedObject.type === 'image';

  return (
    <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-4">
      <div className="flex items-center justify-between border-b border-[var(--glass-border)] pb-2">
        <h3 className="text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] m-0">Properties</h3>
        <span className="text-[10px] bg-white/10 px-2 py-0.5 rounded text-white">{selectedObject.type}</span>
      </div>

      <div className="flex flex-col gap-2">
        <h4 className="text-[10px] uppercase font-bold text-[var(--text-muted)]">Transform</h4>
        <div className="grid grid-cols-2 gap-2">
          <div className="field mb-0">
            <label className="field-label text-[9px] mb-1">X Position</label>
            <input type="number" className="flat-input text-xs" value={Math.round(selectedObject.left || 0)} onChange={(e) => updateObj('left', parseFloat(e.target.value) || 0)} />
          </div>
          <div className="field mb-0">
            <label className="field-label text-[9px] mb-1">Y Position</label>
            <input type="number" className="flat-input text-xs" value={Math.round(selectedObject.top || 0)} onChange={(e) => updateObj('top', parseFloat(e.target.value) || 0)} />
          </div>
        </div>
        <div className="grid grid-cols-[1fr_1fr_auto] gap-2 items-end">
          <div className="field mb-0">
            <label className="field-label text-[9px] mb-1">Width</label>
            <input type="number" className="flat-input text-xs" value={Math.round(selectedObject.getScaledWidth() || 0)} onChange={(e) => updateObj('width', parseFloat(e.target.value) || 0)} />
          </div>
          <div className="field mb-0">
            <label className="field-label text-[9px] mb-1">Height</label>
            <input type="number" className="flat-input text-xs" value={Math.round(selectedObject.getScaledHeight() || 0)} onChange={(e) => updateObj('height', parseFloat(e.target.value) || 0)} />
          </div>
          <button className="btn-icon w-8 h-8 text-[var(--text-muted)] mb-0.5">
            <LinkSimple size={14} />
          </button>
        </div>
        <div className="grid grid-cols-2 gap-2 mt-1">
          <div className="field mb-0">
            <label className="field-label text-[9px] mb-1">Rotation (°)</label>
            <input type="number" className="flat-input text-xs" value={Math.round(selectedObject.angle || 0)} onChange={(e) => updateObj('angle', parseFloat(e.target.value) || 0)} />
          </div>
          {(isRect || isImage) && (
            <div className="field mb-0">
              <label className="field-label text-[9px] mb-1">Corner Radius</label>
              <input type="number" className="flat-input text-xs" value={selectedObject.rx || 0} onChange={(e) => { updateObj('rx', parseFloat(e.target.value) || 0); updateObj('ry', parseFloat(e.target.value) || 0); }} />
            </div>
          )}
        </div>
      </div>

      <div className="w-full h-px bg-[var(--glass-border)] my-1"></div>

      {isText && (
        <div className="flex flex-col gap-2">
          <h4 className="text-[10px] uppercase font-bold text-[var(--text-muted)]">Typography</h4>
          <div className="field mb-0">
            <select className="flat-input text-xs" value={selectedObject.fontFamily} onChange={(e) => updateObj('fontFamily', e.target.value)}>
              <option value="Inter">Inter</option>
              <option value="JetBrains Mono">JetBrains Mono</option>
              <option value="Arial">Arial</option>
            </select>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <select className="flat-input text-xs" value={selectedObject.fontWeight} onChange={(e) => updateObj('fontWeight', e.target.value)}>
              <option value="300">Light</option>
              <option value="normal">Regular</option>
              <option value="600">Semi Bold</option>
              <option value="bold">Bold</option>
            </select>
            <input type="number" className="flat-input text-xs" placeholder="Size" value={Math.round(selectedObject.fontSize || 16)} onChange={(e) => updateObj('fontSize', parseFloat(e.target.value) || 16)} />
          </div>
        </div>
      )}

      {selectedObject.type !== 'image' && (
        <>
          <div className="w-full h-px bg-[var(--glass-border)] my-1"></div>
          <div className="flex flex-col gap-2">
            <h4 className="text-[10px] uppercase font-bold text-[var(--text-muted)]">Appearance</h4>
            <div className="field mb-0">
              <label className="field-label text-[9px] mb-1">Fill Color</label>
              <div className="flex items-center gap-2">
                <input type="color" className="w-8 h-8 rounded border-none p-0 cursor-pointer" value={typeof getPropValue('fill') === 'string' ? getPropValue('fill') : '#000000'} onChange={(e) => updateObj('fill', e.target.value)} />
                <input type="text" className="flat-input text-xs font-mono uppercase flex-1" value={typeof getPropValue('fill') === 'string' ? getPropValue('fill') : 'NONE'} onChange={(e) => updateObj('fill', e.target.value)} />
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-2 mt-2">
              <div className="field mb-0">
                <label className="field-label text-[9px] mb-1">Stroke Color</label>
                <input type="color" className="w-full h-8 rounded border-none p-0 cursor-pointer" value={getPropValue('stroke') || '#000000'} onChange={(e) => updateObj('stroke', e.target.value)} />
              </div>
              <div className="field mb-0">
                <label className="field-label text-[9px] mb-1">Stroke Width</label>
                <input type="number" className="flat-input text-xs" value={getPropValue('strokeWidth') || 0} onChange={(e) => updateObj('strokeWidth', parseFloat(e.target.value) || 0)} />
              </div>
            </div>
          </div>
        </>
      )}

      <div className="w-full h-px bg-[var(--glass-border)] my-1"></div>
      
      <div className="flex flex-col gap-2">
        <h4 className="text-[10px] uppercase font-bold text-[var(--text-muted)]">Effects</h4>
        <div className="field mb-0">
          <label className="field-label text-[9px] mb-1">Opacity (%)</label>
          <input type="number" className="flat-input text-xs" min="0" max="100" value={Math.round((selectedObject.opacity || 1) * 100)} onChange={(e) => updateObj('opacity', parseFloat(e.target.value) || 100)} />
        </div>
      </div>

    </div>
  );
}
