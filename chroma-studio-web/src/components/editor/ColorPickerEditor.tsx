'use client';

import React, { useRef, useState, useEffect, useCallback } from 'react';
import { ColorStop, GradientLayer, LayerType } from '@/types/models';
import { ColorUtils } from '@/utils/colorUtils';
import { MagicWand, Plus, X, List } from '@phosphor-icons/react';

interface ColorPickerEditorProps {
  layer: GradientLayer;
  onUpdateLayer: (updates: Partial<GradientLayer>) => void;
}

export default function ColorPickerEditor({ layer, onUpdateLayer }: ColorPickerEditorProps) {
  const [activeStopIdx, setActiveStopIdx] = useState<number>(0);
  const activeStop = layer.stops[activeStopIdx] || layer.stops[0] || { id: '0', color: '#4F46E5', position: 0 };

  // Parse color into RGB
  const getRgb = useCallback((colStr: string) => {
    if (colStr.startsWith('rgba')) {
      const parts = colStr.match(/[\d.]+/g);
      if (parts && parts.length >= 4) {
        return { r: Number(parts[0]), g: Number(parts[1]), b: Number(parts[2]), a: Number(parts[3]) };
      }
    }
    const [r, g, b] = ColorUtils.hex2rgb(colStr);
    return { r, g, b, a: 1 };
  }, []);

  const rgb = getRgb(activeStop.color);
  const [h, s, v] = ColorUtils.rgb2hsv(rgb.r, rgb.g, rgb.b);

  // Maintain an internal hue state to prevent jumping to red when saturation goes to 0
  const [internalHue, setInternalHue] = useState<number>(h);
  const prevStopIdRef = useRef<string>(activeStop.id);

  useEffect(() => {
    if (prevStopIdRef.current !== activeStop.id) {
      setInternalHue(h);
      prevStopIdRef.current = activeStop.id;
    } else if (s > 0) {
      setInternalHue(h);
    }
  }, [activeStop.id, h, s]);

  // Local state for Hex input to improve UX
  const [localHex, setLocalHex] = useState(ColorUtils.rgb2hex(rgb.r, rgb.g, rgb.b));
  useEffect(() => {
    setLocalHex(ColorUtils.rgb2hex(rgb.r, rgb.g, rgb.b));
  }, [rgb.r, rgb.g, rgb.b]);

  // Keep latest references for drag listeners to avoid stale closures
  const stateRef = useRef({ layer, activeStopIdx, internalHue, rgb, s, v });
  useEffect(() => {
    stateRef.current = { layer, activeStopIdx, internalHue, rgb, s, v };
  }, [layer, activeStopIdx, internalHue, rgb, s, v]);

  const svRef = useRef<HTMLDivElement>(null);
  const hueRef = useRef<HTMLDivElement>(null);
  const alphaRef = useRef<HTMLDivElement>(null);
  const trackRef = useRef<HTMLDivElement>(null);

  // Update active color helper (uses latest state)
  const updateActiveColor = useCallback((newRgb: { r: number; g: number; b: number; a: number }) => {
    const { layer, activeStopIdx } = stateRef.current;
    const hex = ColorUtils.rgb2hex(newRgb.r, newRgb.g, newRgb.b);
    const colorStr = newRgb.a < 1 ? `rgba(${newRgb.r},${newRgb.g},${newRgb.b},${newRgb.a})` : `#${hex}`;

    const newStops = layer.stops.map((st, idx) =>
      idx === activeStopIdx ? { ...st, color: colorStr } : st
    );
    onUpdateLayer({ stops: newStops });
  }, [onUpdateLayer]);

  // Stop Handle Drag
  const handleStopMove = useCallback((clientX: number, startStopIdx: number, initialStops: ColorStop[]) => {
    if (!trackRef.current) return;
    const rect = trackRef.current.getBoundingClientRect();
    const pos = Math.round(ColorUtils.clamp(((clientX - rect.left) / rect.width) * 100, 0, 100));

    const updatedStops = initialStops.map((st, i) => i === startStopIdx ? { ...st, position: pos } : st);
    onUpdateLayer({ stops: updatedStops });
  }, [onUpdateLayer]);

  const onStopDragStart = (e: React.MouseEvent | React.TouchEvent, stopIdx: number) => {
    e.stopPropagation();
    setActiveStopIdx(stopIdx);
    const initialStops = stateRef.current.layer.stops;
    
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    handleStopMove(clientX, stopIdx, initialStops);

    const onMove = (me: MouseEvent | TouchEvent) => {
      const cx = 'touches' in me ? me.touches[0].clientX : (me as MouseEvent).clientX;
      handleStopMove(cx, stopIdx, initialStops);
    };
    
    const onEnd = () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onEnd);
      window.removeEventListener('touchmove', onMove);
      window.removeEventListener('touchend', onEnd);
    };

    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onEnd);
    window.addEventListener('touchmove', onMove);
    window.addEventListener('touchend', onEnd);
  };

  // SV Picker Drag
  const handleSvMove = useCallback((clientX: number, clientY: number) => {
    if (!svRef.current) return;
    const { internalHue, rgb } = stateRef.current;
    const rect = svRef.current.getBoundingClientRect();
    const sat = ColorUtils.clamp((clientX - rect.left) / rect.width, 0, 1);
    const val = ColorUtils.clamp(1 - (clientY - rect.top) / rect.height, 0, 1);

    const [r, g, b] = ColorUtils.hsv2rgb(internalHue, sat, val);
    updateActiveColor({ r, g, b, a: rgb.a });
  }, [updateActiveColor]);

  const onSvStart = (e: React.MouseEvent | React.TouchEvent) => {
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    handleSvMove(clientX, clientY);

    const onMove = (me: MouseEvent | TouchEvent) => {
      const cx = 'touches' in me ? me.touches[0].clientX : (me as MouseEvent).clientX;
      const cy = 'touches' in me ? me.touches[0].clientY : (me as MouseEvent).clientY;
      handleSvMove(cx, cy);
    };
    const onEnd = () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onEnd);
      window.removeEventListener('touchmove', onMove);
      window.removeEventListener('touchend', onEnd);
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onEnd);
    window.addEventListener('touchmove', onMove);
    window.addEventListener('touchend', onEnd);
  };

  // Hue Slider Drag
  const handleHueMove = useCallback((clientX: number) => {
    if (!hueRef.current) return;
    const { s, v, rgb } = stateRef.current;
    const rect = hueRef.current.getBoundingClientRect();
    const newHue = ColorUtils.clamp((clientX - rect.left) / rect.width, 0, 1);
    
    setInternalHue(newHue);
    
    const [r, g, b] = ColorUtils.hsv2rgb(newHue, s, v);
    updateActiveColor({ r, g, b, a: rgb.a });
  }, [updateActiveColor]);

  const onHueStart = (e: React.MouseEvent | React.TouchEvent) => {
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    handleHueMove(clientX);

    const onMove = (me: MouseEvent | TouchEvent) => {
      const cx = 'touches' in me ? me.touches[0].clientX : (me as MouseEvent).clientX;
      handleHueMove(cx);
    };
    const onEnd = () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onEnd);
      window.removeEventListener('touchmove', onMove);
      window.removeEventListener('touchend', onEnd);
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onEnd);
    window.addEventListener('touchmove', onMove);
    window.addEventListener('touchend', onEnd);
  };

  // Alpha Slider Drag
  const handleAlphaMove = useCallback((clientX: number) => {
    if (!alphaRef.current) return;
    const { rgb } = stateRef.current;
    const rect = alphaRef.current.getBoundingClientRect();
    const newAlpha = Math.round(ColorUtils.clamp((clientX - rect.left) / rect.width, 0, 1) * 100) / 100;
    updateActiveColor({ ...rgb, a: newAlpha });
  }, [updateActiveColor]);

  const onAlphaStart = (e: React.MouseEvent | React.TouchEvent) => {
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    handleAlphaMove(clientX);

    const onMove = (me: MouseEvent | TouchEvent) => {
      const cx = 'touches' in me ? me.touches[0].clientX : (me as MouseEvent).clientX;
      handleAlphaMove(cx);
    };
    const onEnd = () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onEnd);
      window.removeEventListener('touchmove', onMove);
      window.removeEventListener('touchend', onEnd);
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onEnd);
    window.addEventListener('touchmove', onMove);
    window.addEventListener('touchend', onEnd);
  };

  // Stop Track Operations
  const addStopAtPosition = (pos: number) => {
    const newStop: ColorStop = {
      id: crypto.randomUUID(),
      color: activeStop.color,
      position: Math.round(pos)
    };
    const updated = [...layer.stops, newStop].sort((a, b) => a.position - b.position);
    onUpdateLayer({ stops: updated });
    setActiveStopIdx(updated.findIndex(st => st.id === newStop.id));
  };

  const removeStop = (index: number) => {
    if (layer.stops.length <= 1) return;
    const updated = layer.stops.filter((_, i) => i !== index);
    onUpdateLayer({ stops: updated });
    setActiveStopIdx(Math.max(0, index - 1));
  };

  const handleTrackClick = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const pos = ColorUtils.clamp(((e.clientX - rect.left) / rect.width) * 100, 0, 100);
    addStopAtPosition(pos);
  };

  const autoHarmony = () => {
    const baseH = Math.random();
    const count = Math.max(4, layer.stops.length);
    
    let newStops = [...layer.stops];
    if (newStops.length < count) {
      for (let i = newStops.length; i < count; i++) {
        newStops.push({
          id: crypto.randomUUID(),
          color: '#ffffff',
          position: Math.round((i / (count - 1)) * 100)
        });
      }
    }
    
    newStops = newStops.map((st, idx) => {
      const stopH = (baseH + (idx * 0.25)) % 1;
      const [r, g, b] = ColorUtils.hsv2rgb(stopH, 0.8, 0.9);
      const hex = ColorUtils.rgb2hex(r, g, b);
      return { ...st, color: `#${hex}` };
    });
    
    onUpdateLayer({ stops: newStops });
  };

  const pureHueRgb = ColorUtils.hsv2rgb(internalHue, 1, 1);

  return (
    <div className="flex flex-col gap-3">
      {layer.type !== LayerType.BLOB && layer.type !== LayerType.LIQUID && (
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <label className="field-label mb-0">Color Stops ({layer.stops.length})</label>
            <span className="text-[9px] uppercase tracking-wider text-[var(--text-muted)] font-bold">
              Tap track or + to add
            </span>
          </div>

          <div
            ref={trackRef}
            className="stops-track-container relative h-8 rounded-xl overflow-hidden border border-[var(--glass-border)] cursor-pointer"
            onClick={handleTrackClick}
          >
            <div
              className="absolute inset-0"
              style={{
                background: `linear-gradient(to right, ${layer.stops
                  .slice()
                  .sort((a, b) => a.position - b.position)
                  .map(s => `${s.color} ${s.position}%`)
                  .join(', ')})`
              }}
            />
            {layer.stops.map((stop, idx) => (
              <div
                key={stop.id || idx}
                onMouseDown={(e) => onStopDragStart(e, idx)}
                onTouchStart={(e) => onStopDragStart(e, idx)}
                className={`stop-thumb absolute top-0 bottom-0 w-5 -ml-2.5 flex items-center justify-center cursor-grab active:cursor-grabbing ${idx === activeStopIdx ? 'active z-10' : 'z-0'}`}
                style={{ left: `${stop.position}%` }}
              >
                <div
                  className={`stop-thumb-dot w-4 h-4 rounded-full border border-white/50 ${idx === activeStopIdx ? 'scale-125 border-white' : ''}`}
                  style={{ background: stop.color }}
                />
              </div>
            ))}
          </div>

          <div className="flex flex-col gap-1.5 mt-2">
            {layer.stops.map((stop, idx) => (
              <div
                key={stop.id || idx}
                draggable
                onDragStart={(e) => { 
                  e.dataTransfer.setData('text/plain', idx.toString()); 
                  e.dataTransfer.effectAllowed = 'move'; 
                }}
                onDragOver={(e) => { 
                  e.preventDefault(); 
                  e.dataTransfer.dropEffect = 'move'; 
                }}
                onDrop={(e) => {
                  e.preventDefault();
                  const fromIdx = Number(e.dataTransfer.getData('text/plain'));
                  if (fromIdx !== idx && !isNaN(fromIdx)) {
                    const newStops = [...layer.stops];
                    const [moved] = newStops.splice(fromIdx, 1);
                    newStops.splice(idx, 0, moved);
                    onUpdateLayer({ stops: newStops });
                    if (activeStopIdx === fromIdx) {
                      setActiveStopIdx(idx);
                    } else if (activeStopIdx === idx) {
                      setActiveStopIdx(fromIdx);
                    }
                  }
                }}
                onClick={() => setActiveStopIdx(idx)}
                className={`flex items-center gap-2 px-2 py-1.5 rounded-xl border text-xs cursor-pointer ${
                  idx === activeStopIdx
                    ? 'border-white/40 bg-white/20 font-bold'
                    : 'border-[var(--glass-border)] bg-[var(--glass-bg)] opacity-70'
                }`}
              >
                <div className="cursor-grab active:cursor-grabbing text-[var(--text-muted)] opacity-50">
                  <List size={14} weight="bold" />
                </div>
                
                <div
                  className="w-4 h-4 rounded-full border border-white/50 shrink-0"
                  style={{ backgroundColor: stop.color }}
                />
                
                <div className="flex-1 flex items-center gap-2 min-w-0 px-1">
                  <input 
                    type="range"
                    className="range flex-1 opacity-70"
                    min="0" max="100" step="1"
                    value={stop.position}
                    draggable
                    onDragStart={(e) => { e.preventDefault(); e.stopPropagation(); }}
                    onMouseDown={(e) => e.stopPropagation()}
                    onClick={(e) => e.stopPropagation()}
                    onChange={(e) => {
                      const newPos = ColorUtils.clamp(Number(e.target.value), 0, 100);
                      const updatedStops = layer.stops.map((st, i) => i === idx ? { ...st, position: newPos } : st);
                      onUpdateLayer({ stops: updatedStops });
                    }}
                  />
                  <div className="flex items-center">
                    <input
                      type="number"
                      min="0"
                      max="100"
                      value={stop.position}
                      onClick={(e) => e.stopPropagation()}
                      onChange={(e) => {
                        const newPos = ColorUtils.clamp(Number(e.target.value), 0, 100);
                        const updatedStops = layer.stops.map((st, i) => i === idx ? { ...st, position: newPos } : st);
                        onUpdateLayer({ stops: updatedStops });
                      }}
                      className="w-8 bg-transparent text-[10px] font-mono text-[var(--text-main)] border-b border-[var(--glass-border)] text-center outline-none focus:border-white/50"
                    />
                    <span className="text-[9px] text-[var(--text-muted)] font-mono">%</span>
                  </div>
                </div>

                {layer.stops.length > 1 && (
                  <button
                    className="text-[var(--text-muted)] p-1 ml-1 rounded"
                    onClick={(e) => {
                      e.stopPropagation();
                      removeStop(idx);
                    }}
                    title="Remove Stop"
                  >
                    <X size={12} weight="bold" />
                  </button>
                )}
              </div>
            ))}

            <button
              className="btn btn-ghost text-[10px] px-2 py-1.5 flex items-center justify-center gap-1 opacity-70 mt-1 border-dashed w-full"
              onClick={() => addStopAtPosition(50)}
              title="Add Color Stop"
            >
              <Plus size={12} weight="bold" /> Add Stop
            </button>
          </div>
        </div>
      )}

      {/* 2D Color Picker Box */}
      <div className="glass-panel border border-[var(--glass-border)] rounded-2xl p-3 flex flex-col gap-3">
        {/* Saturation/Value Box */}
        <div
          ref={svRef}
          onMouseDown={onSvStart}
          onTouchStart={onSvStart}
          className="sat-val-area relative h-40 rounded-xl overflow-hidden cursor-crosshair"
          style={{ backgroundColor: `rgb(${pureHueRgb[0]}, ${pureHueRgb[1]}, ${pureHueRgb[2]})` }}
        >
          <div
            className="absolute inset-0 pointer-events-none"
            style={{ background: 'linear-gradient(to right, #ffffff, transparent)' }}
          />
          <div
            className="absolute inset-0 pointer-events-none"
            style={{ background: 'linear-gradient(to top, #000000, transparent)' }}
          />
          <div
            className="sv-thumb absolute w-5 h-5 border-2 border-white rounded-full -translate-x-1/2 -translate-y-1/2 pointer-events-none active:scale-90"
            style={{
              left: `${s * 100}%`,
              top: `${(1 - v) * 100}%`,
              backgroundColor: activeStop.color
            }}
          />
        </div>

        {/* Hue Track */}
        <div
          ref={hueRef}
          onMouseDown={onHueStart}
          onTouchStart={onHueStart}
          className="slider-track hue-track relative h-5 rounded-full cursor-pointer border border-white/10"
          style={{
            background: 'linear-gradient(to right, #ff0000 0%, #ffff00 17%, #00ff00 33%, #00ffff 50%, #0000ff 67%, #ff00ff 83%, #ff0000 100%)'
          }}
        >
          <div
            className="slider-thumb absolute w-6 h-6 border-2 border-white rounded-full -translate-x-1/2 -translate-y-1/2 top-1/2 pointer-events-none"
            style={{ left: `${internalHue * 100}%`, backgroundColor: `rgb(${pureHueRgb[0]}, ${pureHueRgb[1]}, ${pureHueRgb[2]})` }}
          />
        </div>

        {/* Alpha Track */}
        <div
          ref={alphaRef}
          onMouseDown={onAlphaStart}
          onTouchStart={onAlphaStart}
          className="alpha-track-bg relative h-5 rounded-full cursor-pointer overflow-hidden border border-white/10"
          style={{
            backgroundImage: 'linear-gradient(45deg, rgba(0,0,0,0.1) 25%, transparent 25%), linear-gradient(-45deg, rgba(0,0,0,0.1) 25%, transparent 25%), linear-gradient(45deg, transparent 75%, rgba(0,0,0,0.1) 75%), linear-gradient(-45deg, transparent 75%, rgba(0,0,0,0.1) 75%)',
            backgroundSize: '10px 10px',
            backgroundPosition: '0 0, 0 5px, 5px -5px, -5px 0px',
            backgroundColor: 'rgba(255,255,255,0.5)'
          }}
        >
          <div
            className="slider-track relative h-full rounded-full"
            style={{ background: `linear-gradient(to right, transparent, rgb(${rgb.r},${rgb.g},${rgb.b}))` }}
          >
            <div
              className="slider-thumb absolute w-6 h-6 border-2 border-white rounded-full -translate-x-1/2 -translate-y-1/2 top-1/2 pointer-events-none"
              style={{ left: `${rgb.a * 100}%`, backgroundColor: activeStop.color }}
            />
          </div>
        </div>

        {/* Hex and Alpha numerical inputs */}
        <div className="grid grid-cols-[2fr_1fr] gap-2 mt-2">
          <div className="flex items-center bg-[var(--glass-bg)] border border-[var(--glass-border)] rounded-lg px-3 h-8">
            <span className="text-[var(--text-muted)] font-mono text-xs mr-1 font-bold opacity-50">#</span>
            <input
              type="text"
              value={localHex}
              onChange={(e) => {
                setLocalHex(e.target.value);
                const hex = e.target.value.trim();
                if (/^[0-9A-Fa-f]{6}$/.test(hex)) {
                  const [r, g, b] = ColorUtils.hex2rgb(hex);
                  updateActiveColor({ r, g, b, a: rgb.a });
                }
              }}
              onBlur={() => {
                setLocalHex(ColorUtils.rgb2hex(rgb.r, rgb.g, rgb.b));
              }}
              className="bg-transparent text-[var(--text-main)] font-mono text-xs w-full outline-none uppercase font-bold"
            />
          </div>

          <div className="flex items-center bg-[var(--glass-bg)] border border-[var(--glass-border)] rounded-lg px-3 h-8">
            <input
              type="number"
              min="0"
              max="100"
              value={Math.round(rgb.a * 100)}
              onChange={(e) => {
                const val = Math.min(100, Math.max(0, Number(e.target.value)));
                updateActiveColor({ ...rgb, a: val / 100 });
              }}
              className="bg-transparent text-[var(--text-main)] font-mono text-xs w-full outline-none text-right font-bold"
            />
            <span className="text-[var(--text-muted)] font-mono text-xs ml-1 font-bold opacity-50">%</span>
          </div>
        </div>

        <button
          className="btn btn-ghost w-full mt-1 text-xs py-2 transition-all hover:bg-white/10"
          onClick={autoHarmony}
        >
          <MagicWand size={14} weight="bold" /> Auto-Harmony
        </button>
      </div>
    </div>
  );
}
