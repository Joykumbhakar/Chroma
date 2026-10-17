'use client';

import React from 'react';
import { LayerType, LayerTypeDetails, ChromaBlendMode, BlendModeLabels } from '@/types/models';
import { useStudio } from '@/context/StudioContext';
import { useDesign } from '@/context/DesignContext';
import { Plus, List, CaretDown, CaretUp, Trash, Eye, EyeSlash, DotsSixVertical, Shuffle, ArrowUpRight, Circle, Triangle, GridFour, Drop, Sparkle, Waves, CopySimple, ArrowsLeftRight, Copy } from '@phosphor-icons/react';
import ColorPickerEditor from '@/components/editor/ColorPickerEditor';
import LayerInspector from '@/components/editor/LayerInspector';
import ObjectInspector from '@/components/editor/ObjectInspector';
import clsx from 'clsx';

export default function RightPanel() {
  const { layers, addLayer, updateLayer, deleteLayer, duplicateLayer, invertLayerColors, copyLayerCss, moveLayerUp, moveLayerDown, randomize } = useStudio();
  const { selectedObject } = useDesign();

  if (selectedObject) {
    return (
      <aside className="right-panel glass-panel w-[360px] shrink-0 flex flex-col z-50 h-full border-y-0 border-r-0">
        <ObjectInspector />
      </aside>
    );
  }

  return (
    <aside className="right-panel glass-panel w-[360px] shrink-0 flex flex-col z-50 h-full border-y-0 border-r-0">
      <div className="panel-header flex items-center justify-between p-4 cursor-default border-b border-[var(--glass-border)] shrink-0">
        <h3 className="text-[10px] uppercase tracking-widest font-bold text-[var(--text-muted)] m-0">Background Gradients</h3>
        <div className="flex gap-1.5">
          <button 
            className="btn-icon w-8 h-8 text-sm bg-[var(--primary)] text-white border-none" 
            onClick={() => addLayer(LayerType.LINEAR)}
            title="Add Layer"
          >
            <Plus weight="regular" />
          </button>
          <button 
            className="btn-icon w-8 h-8 text-sm bg-[var(--glass-bg)] border border-[var(--glass-border)]" 
            onClick={randomize}
            title="Randomize All"
          >
            <Shuffle weight="regular" />
          </button>
        </div>
      </div>

      <div className="panel-body flex-1 overflow-y-auto min-h-0 p-4 flex flex-col gap-3">
        {layers.length === 0 ? (
          <div className="flex flex-col items-center justify-center p-8 text-[var(--text-muted)] text-center h-full opacity-60">
            <List size={48} weight="thin" className="mb-2" />
            <p className="text-xs font-semibold">No Layers</p>
            <p className="text-[10px]">Click + to add a new gradient layer</p>
          </div>
        ) : (
          layers.map((layer) => (
            <div 
              key={layer.id} 
              className={clsx(
                "layer-card rounded-2xl border overflow-hidden relative",
                layer.expanded ? "border-white/40 bg-white/40 z-10 backdrop-blur-md" : "border-white/20 bg-white/10"
              )}
            >
              {/* Layer Header */}
              <div 
                className="flex items-center p-3 gap-2 cursor-pointer transition-colors"
                onClick={() => updateLayer(layer.id, { expanded: !layer.expanded })}
              >
                {/* Active Indicator Line */}
                {layer.expanded && (
                  <div className="absolute left-0 top-0 bottom-0 w-1 bg-gradient-to-b from-indigo-500 to-cyan-400" />
                )}
                
                <div className="layer-drag-handle text-[var(--text-muted)] cursor-grab ml-1">
                  <DotsSixVertical weight="regular" />
                </div>
                
                <button 
                  className="btn-icon w-6 h-6 text-[var(--text-muted)] shrink-0"
                  onClick={(e) => { e.stopPropagation(); updateLayer(layer.id, { visible: !layer.visible }); }}
                >
                  {layer.visible ? <Eye weight="regular" /> : <EyeSlash weight="regular" />}
                </button>

                <div className="flex-1 min-w-0" onClick={(e) => e.stopPropagation()}>
                  <input 
                    type="text" 
                    value={layer.name}
                    placeholder="Layer Name"
                    onChange={(e) => updateLayer(layer.id, { name: e.target.value })}
                    className="bg-transparent border-none text-xs font-bold text-[var(--text-main)] w-full focus-ring rounded px-1 placeholder:font-normal placeholder:text-[var(--text-muted)] placeholder:opacity-50"
                  />
                  <div className="text-[9px] text-[var(--text-muted)] uppercase tracking-wider px-1 mt-0.5 font-mono">
                    {LayerTypeDetails[layer.type].label}
                  </div>
                </div>

                <div className="flex items-center gap-0.5 shrink-0">
                  <button 
                    className="btn-icon w-6 h-6 text-xs text-[var(--text-muted)]" 
                    onClick={(e) => { e.stopPropagation(); duplicateLayer(layer.id); }}
                    title="Duplicate Layer"
                  >
                    <CopySimple weight="regular" />
                  </button>
                  <button 
                    className="btn-icon w-6 h-6 text-xs text-[var(--text-muted)]" 
                    onClick={(e) => { e.stopPropagation(); invertLayerColors(layer.id); }}
                    title="Invert Colors"
                  >
                    <ArrowsLeftRight weight="regular" />
                  </button>
                  <button 
                    className="btn-icon w-6 h-6 text-xs text-[var(--text-muted)]" 
                    onClick={(e) => { e.stopPropagation(); copyLayerCss(layer.id); }}
                    title="Copy Layer CSS"
                  >
                    <Copy weight="regular" />
                  </button>
                  <button 
                    className="btn-icon w-6 h-6 text-xs" 
                    onClick={(e) => { e.stopPropagation(); moveLayerUp(layer.id); }}
                    title="Move Up"
                  >
                    <CaretUp weight="regular" />
                  </button>
                  <button 
                    className="btn-icon w-6 h-6 text-xs" 
                    onClick={(e) => { e.stopPropagation(); moveLayerDown(layer.id); }}
                    title="Move Down"
                  >
                    <CaretDown weight="regular" />
                  </button>
                  <button 
                    className="btn-icon w-6 h-6 text-xs text-[var(--mac-red)]"
                    onClick={(e) => { e.stopPropagation(); deleteLayer(layer.id); }}
                    title="Delete Layer"
                  >
                    <Trash weight="regular" />
                  </button>
                </div>
              </div>

              {/* Layer Inspector & Color Picker */}
              {layer.expanded && (
                <div className="p-4 flex flex-col gap-4 border-t border-[var(--glass-border)] bg-[var(--glass-bg)] max-h-[440px] overflow-y-auto">
                  {/* Engine Grid Selector */}
                  <div className="flex flex-col gap-1.5">
                    <label className="field-label mb-0">Engine Selection</label>
                    <div className="type-grid grid grid-cols-3 gap-1.5">
                      {Object.values(LayerType).map(type => {
                        const details = LayerTypeDetails[type];
                        const isActive = layer.type === type;
                        return (
                          <button 
                            key={type}
                            onClick={() => updateLayer(layer.id, { type })}
                            className={clsx(
                              "type-btn py-1.5 px-1 flex flex-col items-center gap-1 rounded-xl text-[10px] font-semibold border",
                              isActive 
                                ? "bg-white/80 text-[var(--primary)] border-white/60" 
                                : "bg-white/10 border-white/10 text-[var(--text-muted)]"
                            )}
                          >
                            <span className="text-sm">
                              {type === 'LINEAR' && <ArrowUpRight weight="regular" />}
                              {type === 'RADIAL' && <Circle weight="regular" />}
                              {type === 'CONIC' && <Triangle weight="regular" />}
                              {type === 'MESH' && <GridFour weight="regular" />}
                              {type === 'BLOB' && <Drop weight="regular" />}
                              {type === 'AURORA' && <Sparkle weight="regular" />}
                              {type === 'LIQUID' && <Waves weight="regular" />}
                            </span>
                            {details.label}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* Blend mode & Opacity / Blur */}
                  <div className="grid grid-cols-2 gap-3">
                    <div className="field mb-0">
                      <label className="field-label mb-1">Blend Mode</label>
                      <select 
                        className="flat-select text-xs py-1.5"
                        value={layer.blendMode}
                        onChange={(e) => updateLayer(layer.id, { blendMode: e.target.value as ChromaBlendMode })}
                      >
                        {Object.values(ChromaBlendMode).map(bm => (
                          <option key={bm} value={bm}>{BlendModeLabels[bm]}</option>
                        ))}
                      </select>
                    </div>

                    <div className="field mb-0">
                      <label className="field-label flex justify-between items-center text-[9px] mb-1">
                        Opacity <span className="font-mono text-[var(--text-main)] normal-case">{Math.round(layer.opacity * 100)}%</span>
                      </label>
                      <div className="range-wrap">
                        <input 
                          type="range" 
                          className="range" 
                          min="0" 
                          max="1" 
                          step="0.01" 
                          value={layer.opacity} 
                          onChange={(e) => updateLayer(layer.id, { opacity: Number(e.target.value) })} 
                        />
                      </div>
                    </div>
                  </div>

                  <div className="field mb-0 mt-2">
                    <label className="field-label flex justify-between items-center text-[9px] mb-1">
                      Layer Blur <span className="font-mono text-[var(--text-main)] normal-case">{layer.blur || 0}px</span>
                    </label>
                    <div className="range-wrap">
                      <input 
                        type="range" 
                        className="range" 
                        min="0" 
                        max="50" 
                        step="1" 
                        value={layer.blur || 0} 
                        onChange={(e) => updateLayer(layer.id, { blur: Number(e.target.value) })} 
                      />
                    </div>
                  </div>

                  {/* Dynamic Layer Parameters */}
                  <LayerInspector 
                    layer={layer} 
                    onUpdateLayer={(updates) => updateLayer(layer.id, updates)} 
                  />

                  <div className="w-full h-px bg-[var(--glass-border)] my-1" />

                  {/* Color Picker & Stops Track */}
                  <ColorPickerEditor 
                    layer={layer} 
                    onUpdateLayer={(updates) => updateLayer(layer.id, updates)} 
                  />
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </aside>
  );
}
