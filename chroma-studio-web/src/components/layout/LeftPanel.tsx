'use client';

import React, { useState, useRef } from 'react';
import { Sparkle, MagicWand, Image as ImageIcon, UploadSimple, Stop, Play, Pause, Sliders } from '@phosphor-icons/react';
import { useStudio } from '@/context/StudioContext';
import { PostProcessingFx, AnimStyle, LayerType } from '@/types/models';
import { ColorUtils } from '@/utils/colorUtils';
import { useDesign } from '@/context/DesignContext';
import clsx from 'clsx';

export default function LeftPanel() {
  const {
    postFx, setPostFx,
    fxIntensity, setFxIntensity,
    mouseReact, setMouseReact,
    animStatus, setAnimStatus,
    animStyle, setAnimStyle,
    animSpeed, setAnimSpeed,
    animIntensity, setAnimIntensity,
    globalAdjustments, setGlobalAdjustments,
    applyPreset,
    addLayer
  } = useStudio();
  const { appMode, addUIKit, canvas } = useDesign();

  const [activeTab, setActiveTab] = useState<'gradients' | 'uikit' | 'layers'>('gradients');
  const [aiPrompt, setAiPrompt] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  React.useEffect(() => {
    setActiveTab(appMode === 'design' ? 'gradients' : 'uikit');
  }, [appMode]);

  const generateFromPrompt = () => {
    const prompt = aiPrompt.trim().toLowerCase();
    if (!prompt) return;

    let colors = ['#4F46E5', '#818CF8'];
    let type = LayerType.LINEAR;

    if (prompt.includes('cyberpunk') || prompt.includes('neon') || prompt.includes('synth')) {
      colors = ['#FF00EA', '#00E1FF', '#4300FF'];
      type = LayerType.MESH;
    } else if (prompt.includes('ocean') || prompt.includes('sea') || prompt.includes('water')) {
      colors = ['#0077BE', '#00A3CC', '#00C1D4'];
      type = LayerType.LIQUID;
    } else if (prompt.includes('forest') || prompt.includes('nature') || prompt.includes('green')) {
      colors = ['#134E5E', '#71B280', '#228B22'];
      type = LayerType.AURORA;
    } else if (prompt.includes('sunset') || prompt.includes('dusk') || prompt.includes('fire')) {
      colors = ['#FF512F', '#DD2476', '#F12711'];
      type = LayerType.BLOB;
    } else {
      colors = [
        `#${ColorUtils.rgb2hex(...ColorUtils.hsv2rgb(Math.random(), 0.8, 0.9))}`,
        `#${ColorUtils.rgb2hex(...ColorUtils.hsv2rgb(Math.random(), 0.8, 0.9))}`,
        `#${ColorUtils.rgb2hex(...ColorUtils.hsv2rgb(Math.random(), 0.8, 0.9))}`
      ];
      type = LayerType.LINEAR;
    }

    addLayer(type);
  };

  const extractFromImage = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const img = new Image();
    const reader = new FileReader();

    reader.onload = (event) => {
      img.src = event.target?.result as string;
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        canvas.width = 64;
        canvas.height = 64;
        if (ctx) {
          ctx.drawImage(img, 0, 0, 64, 64);
          const data = ctx.getImageData(0, 0, 64, 64).data;
          const colors: string[] = [];
          for (let i = 0; i < 3; i++) {
            const idx = Math.floor(Math.random() * (64 * 64)) * 4;
            const r = data[idx], g = data[idx + 1], b = data[idx + 2];
            colors.push(`#${ColorUtils.rgb2hex(r, g, b)}`);
          }
          addLayer(LayerType.MESH);
        }
      };
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  return (
    <aside className="left-panel glass-panel w-[300px] shrink-0 flex flex-col z-10 transition-transform duration-400 ease-in-out h-full border-y-0 border-l-0">
      <div className="flex p-2 gap-1 border-b border-[var(--glass-border)] shrink-0">
        <button 
          className={clsx("flex-1 py-1.5 px-2 text-[11px] font-semibold rounded-lg transition", activeTab === 'gradients' ? 'bg-white/20 text-white' : 'text-gray-400 hover:bg-white/10')} 
          onClick={() => setActiveTab('gradients')}
        >
          Gradients
        </button>
        <button 
          className={clsx("flex-1 py-1.5 px-2 text-[11px] font-semibold rounded-lg transition", activeTab === 'uikit' ? 'bg-white/20 text-white' : 'text-gray-400 hover:bg-white/10')} 
          onClick={() => setActiveTab('uikit')}
        >
          UI Kit
        </button>
      </div>
      
      <div className="flex-1 overflow-y-auto min-h-0 relative p-4 flex flex-col gap-6">
        {activeTab === 'gradients' && (
          <>
            {/* AI & EXTRACT */}
            <div className="flex flex-col gap-3 shrink-0">
          <label className="field-label mb-0 flex items-center gap-1">
            <Sparkle weight="regular" className="text-[var(--primary)]" /> AI Prompt-to-Palette
          </label>
          <div className="flex gap-2">
            <input 
              type="text" 
              className="flat-input text-xs" 
              placeholder="e.g. Cyberpunk neon city..."
              value={aiPrompt}
              onChange={(e) => setAiPrompt(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && generateFromPrompt()}
            />
            <button className="btn btn-primary px-3 focus-ring" onClick={generateFromPrompt}>
              <MagicWand weight="regular" />
            </button>
          </div>
          
          <label className="field-label mt-2 mb-0 flex items-center gap-1">
            <ImageIcon weight="regular" className="text-[var(--primary)]" /> Extract from Image
          </label>
          <button 
            className="btn btn-ghost text-xs w-full border-dashed focus-ring"
            onClick={() => fileInputRef.current?.click()}
          >
            <UploadSimple weight="regular" /> Upload Reference Image
          </button>
          <input 
            type="file" 
            ref={fileInputRef} 
            accept="image/*" 
            className="hidden" 
            onChange={extractFromImage}
          />
        </div>

        <div className="w-full h-px bg-[var(--glass-border)] shrink-0"></div>

        {/* IMAGE ADJUSTMENTS PANEL */}
        <div className="flex flex-col gap-3 shrink-0">
          <label className="field-label mb-0 flex items-center gap-1">
            <Sliders weight="regular" className="text-[var(--primary)]" /> Global Adjustments
          </label>
          
          <div className="grid grid-cols-2 gap-3">
            <div className="field mb-0">
              <label className="field-label flex justify-between items-center text-[9px]">
                Brightness <span className="font-mono text-[var(--text-main)] normal-case">{globalAdjustments.brightness}%</span>
              </label>
              <div className="range-wrap">
                <input
                  type="range"
                  className="range"
                  min="50"
                  max="150"
                  value={globalAdjustments.brightness}
                  onChange={(e) => setGlobalAdjustments({ brightness: Number(e.target.value) })}
                />
              </div>
            </div>

            <div className="field mb-0">
              <label className="field-label flex justify-between items-center text-[9px]">
                Contrast <span className="font-mono text-[var(--text-main)] normal-case">{globalAdjustments.contrast}%</span>
              </label>
              <div className="range-wrap">
                <input
                  type="range"
                  className="range"
                  min="50"
                  max="150"
                  value={globalAdjustments.contrast}
                  onChange={(e) => setGlobalAdjustments({ contrast: Number(e.target.value) })}
                />
              </div>
            </div>

            <div className="field mb-0">
              <label className="field-label flex justify-between items-center text-[9px]">
                Saturation <span className="font-mono text-[var(--text-main)] normal-case">{globalAdjustments.saturation}%</span>
              </label>
              <div className="range-wrap">
                <input
                  type="range"
                  className="range"
                  min="0"
                  max="200"
                  value={globalAdjustments.saturation}
                  onChange={(e) => setGlobalAdjustments({ saturation: Number(e.target.value) })}
                />
              </div>
            </div>

            <div className="field mb-0">
              <label className="field-label flex justify-between items-center text-[9px]">
                Hue Shift <span className="font-mono text-[var(--text-main)] normal-case">{globalAdjustments.hueShift}°</span>
              </label>
              <div className="range-wrap">
                <input
                  type="range"
                  className="range"
                  min="0"
                  max="360"
                  value={globalAdjustments.hueShift}
                  onChange={(e) => setGlobalAdjustments({ hueShift: Number(e.target.value) })}
                />
              </div>
            </div>
          </div>

          <div className="field mb-0 mt-1">
            <label className="field-label flex justify-between items-center text-[9px]">
              Blur <span className="font-mono text-[var(--text-main)] normal-case">{globalAdjustments.blur}px</span>
            </label>
            <div className="range-wrap">
              <input
                type="range"
                className="range"
                min="0"
                max="20"
                value={globalAdjustments.blur}
                onChange={(e) => setGlobalAdjustments({ blur: Number(e.target.value) })}
              />
            </div>
          </div>
        </div>

        <div className="w-full h-px bg-[var(--glass-border)] shrink-0"></div>

        {/* POST PROCESSING */}
        <div className="flex flex-col gap-3 shrink-0">
          <label className="field-label mb-0">Post-Processing FX</label>
          <select 
            className="flat-select text-xs py-1.5"
            value={postFx}
            onChange={(e) => setPostFx(e.target.value as PostProcessingFx)}
          >
            <option value={PostProcessingFx.NONE}>None (Clean)</option>
            <option value={PostProcessingFx.GRAIN}>Film Grain</option>
            <option value={PostProcessingFx.HALFTONE}>Retro Halftone</option>
            <option value={PostProcessingFx.DITHER}>Brutalist Dither</option>
          </select>

          {postFx !== PostProcessingFx.NONE && (
            <div className="range-wrap mt-1">
              <input 
                type="range" 
                className="range" 
                min="0" max="100" 
                value={fxIntensity} 
                onChange={(e) => setFxIntensity(Number(e.target.value))} 
              />
            </div>
          )}
        </div>

        <div className="w-full h-px bg-[var(--glass-border)] shrink-0"></div>

        {/* INTERACTIVITY */}
        <div className="flex flex-col gap-2 shrink-0">
          <div className="switch-container py-0">
            <div>
              <span className="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-widest block">Mouse Reactivity</span>
              <span className="text-[9px] text-[var(--text-muted)] block mt-0.5">Parallax effect on cursor move</span>
            </div>
            <label className="switch">
              <input type="checkbox" checked={mouseReact} onChange={(e) => setMouseReact(e.target.checked)} />
              <span className="slider"></span>
            </label>
          </div>
        </div>

        <div className="w-full h-px bg-[var(--glass-border)] shrink-0"></div>

        {/* CSS ANIMATION */}
        <div className="flex flex-col gap-3 shrink-0">
          <div className="flex-between flex items-center justify-between">
            <label className="field-label mb-0">CSS Animation</label>
            <div className="flex bg-[var(--glass-bg)] border border-[var(--glass-border)] rounded-md overflow-hidden">
              <button 
                className={`px-2.5 py-1 transition-colors border-r border-[var(--glass-border)] ${animStatus === 'stopped' ? 'bg-[var(--primary)] text-white' : 'text-[var(--text-muted)] hover:bg-[var(--primary)] hover:text-white'}`}
                onClick={() => setAnimStatus('stopped')} title="Stop"
              ><Stop weight="fill" className="text-xs" /></button>
              <button 
                className={`px-2.5 py-1 transition-colors border-r border-[var(--glass-border)] ${animStatus === 'playing' ? 'bg-[var(--primary)] text-white' : 'text-[var(--text-muted)] hover:bg-[var(--primary)] hover:text-white'}`}
                onClick={() => setAnimStatus('playing')} title="Play"
              ><Play weight="fill" className="text-xs" /></button>
              <button 
                className={`px-2.5 py-1 transition-colors ${animStatus === 'paused' ? 'bg-[var(--primary)] text-white' : 'text-[var(--text-muted)] hover:bg-[var(--primary)] hover:text-white'}`}
                onClick={() => setAnimStatus('paused')} title="Pause"
              ><Pause weight="fill" className="text-xs" /></button>
            </div>
          </div>
          <div className="field mb-0">
            <select 
              className="flat-select py-1.5 text-xs"
              value={animStyle}
              onChange={(e) => setAnimStyle(e.target.value as AnimStyle)}
            >
              <option value={AnimStyle.DRIFT}>Drift (Pan Position)</option>
              <option value={AnimStyle.PULSE}>Pulse (Scale & Opacity)</option>
              <option value={AnimStyle.HUE}>Hue Shift (Color Rotation)</option>
              <option value={AnimStyle.BREATHE}>Breathe (Size Expansion)</option>
            </select>
          </div>
          <div className="grid grid-cols-2 gap-4 mt-1">
            <div className="field mb-0">
              <label className="field-label flex justify-between items-center text-[9px]">Speed <span className="font-mono text-[var(--text-main)] normal-case">{animSpeed}</span></label>
              <div className="range-wrap"><input type="range" className="range" min="1" max="100" value={animSpeed} onChange={(e) => setAnimSpeed(Number(e.target.value))} /></div>
            </div>
            <div className="field mb-0">
              <label className="field-label flex justify-between items-center text-[9px]">Amount <span className="font-mono text-[var(--text-main)] normal-case">{animIntensity}</span></label>
              <div className="range-wrap"><input type="range" className="range" min="0" max="100" value={animIntensity} onChange={(e) => setAnimIntensity(Number(e.target.value))} /></div>
            </div>
          </div>
        </div>

        <div className="w-full h-px bg-[var(--glass-border)] shrink-0"></div>

        {/* PRESETS */}
        <div className="flex flex-col gap-2 shrink-0 pb-6">
          <label className="field-label mb-0">Apple Style Presets</label>
          <div className="grid grid-cols-4 gap-2">
            {[
              { name: 'Sunset', colors: ['#FFFF7E5F', '#FFFEB47B'], type: LayerType.LINEAR },
              { name: 'Ocean', colors: ['#FF2E3192', '#FF1BFFFF'], type: LayerType.LINEAR },
              { name: 'Candy', colors: ['#FFFC466B', '#FF3F5EFB'], type: LayerType.LINEAR },
              { name: 'Mint', colors: ['#FF00B09B', '#FF96C93D'], type: LayerType.LINEAR },
              { name: 'Peach', colors: ['#FFFFECD2', '#FFFCB69F'], type: LayerType.RADIAL },
              { name: 'Aurora', colors: ['#FF00C9FF', '#FF92FE9D'], type: LayerType.AURORA },
              { name: 'Grape', colors: ['#FF7F00FF', '#FFE100FF'], type: LayerType.CONIC },
              { name: 'Fire', colors: ['#FFF83600', '#FFF9D423'], type: LayerType.LINEAR }
            ].map((preset, idx) => (
              <button 
                key={preset.name}
                onClick={() => applyPreset(idx)}
                title={preset.name}
                className="w-full aspect-square rounded-xl focus-ring border border-white/40 cursor-pointer"
                style={{
                  background: `linear-gradient(135deg, ${preset.colors.join(', ')})`
                }}
              />
            ))}
          </div>
        </div>
        </>
        )}

        {activeTab === 'uikit' && (
          <div className="flex flex-col gap-4">
            <div className="text-[11px] text-[var(--text-muted)] mb-2 italic">Drag components or click to add.</div>
            
            <div className="uikit-cat-block">
              <div className="text-[10px] font-bold text-blue-400 uppercase tracking-wider mb-2 flex items-center justify-between">
                  <span>Cards & Containers</span>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div 
                  className="uikit-card group bg-black/30 border border-white/5 rounded-lg p-2.5 cursor-pointer transition-colors"
                  onClick={() => addUIKit('card-basic')}
                >
                    <span className="text-[10px] font-semibold text-white mb-1.5 block">Standard Card</span>
                    <div className="w-full h-12 bg-zinc-800 border border-white/5 rounded-md p-1.5 flex flex-col gap-1 group-hover:border-blue-500/50 transition-colors">
                        <div className="w-1/2 h-1.5 bg-white/20 rounded"></div>
                        <div className="w-full h-1 bg-white/10 rounded"></div>
                        <div className="w-3/4 h-1 bg-white/10 rounded"></div>
                    </div>
                </div>
                <div 
                  className="uikit-card group bg-black/30 border border-white/5 rounded-lg p-2.5 cursor-pointer transition-colors"
                  onClick={() => addUIKit('card-blue')}
                >
                    <span className="text-[10px] font-semibold text-white mb-1.5 block">Blue Card</span>
                    <div className="w-full h-12 bg-blue-900 border border-blue-500/20 rounded-md p-1.5 flex flex-col gap-1 group-hover:border-blue-400 transition-colors">
                        <div className="w-1/2 h-1.5 bg-white/30 rounded"></div>
                        <div className="w-full h-1 bg-white/20 rounded"></div>
                        <div className="w-3/4 h-1 bg-white/20 rounded"></div>
                    </div>
                </div>
                <div 
                  className="uikit-card group bg-black/30 border border-white/5 rounded-lg p-2.5 cursor-pointer transition-colors"
                  onClick={() => addUIKit('card-rose')}
                >
                    <span className="text-[10px] font-semibold text-white mb-1.5 block">Rose Card</span>
                    <div className="w-full h-12 bg-rose-900 border border-rose-500/20 rounded-md p-1.5 flex flex-col gap-1 group-hover:border-rose-400 transition-colors">
                        <div className="w-1/2 h-1.5 bg-white/30 rounded"></div>
                        <div className="w-full h-1 bg-white/20 rounded"></div>
                        <div className="w-3/4 h-1 bg-white/20 rounded"></div>
                    </div>
                </div>
                <div 
                  className="uikit-card group bg-black/30 border border-white/5 rounded-lg p-2.5 cursor-pointer transition-colors"
                  onClick={() => addUIKit('section-dark')}
                >
                    <span className="text-[10px] font-semibold text-white mb-1.5 block">Dark Section</span>
                    <div className="w-full h-12 bg-black border border-white/10 rounded-md p-1.5 flex flex-col gap-1 items-center justify-center group-hover:border-white/30 transition-colors">
                        <div className="w-2/3 h-2 bg-white/30 rounded"></div>
                        <div className="w-1/2 h-1 bg-white/20 rounded mt-1"></div>
                    </div>
                </div>
              </div>
            </div>

            <div className="uikit-cat-block mt-4">
              <div className="text-[10px] font-bold text-emerald-400 uppercase tracking-wider mb-2 flex items-center justify-between">
                  <span>Forms & Inputs</span>
              </div>
              <div className="grid grid-cols-2 gap-2">
                  <div 
                    className="uikit-card group bg-black/30 border border-white/5 rounded-lg p-2.5 cursor-pointer transition-colors flex flex-col items-center" 
                    onClick={() => addUIKit('button-primary')}
                  >
                      <div className="w-full h-7 bg-indigo-600 rounded-md flex items-center justify-center mb-1"><span className="text-[8px] font-bold text-white">ACTION</span></div>
                      <span className="text-[9px] text-gray-300 text-center">Primary Btn</span>
                  </div>
                  <div 
                    className="uikit-card group bg-black/30 border border-white/5 rounded-lg p-2.5 cursor-pointer transition-colors flex flex-col items-center" 
                    onClick={() => addUIKit('button-outline')}
                  >
                      <div className="w-full h-7 border border-indigo-500 rounded-md flex items-center justify-center mb-1"><span className="text-[8px] font-bold text-indigo-400">SECONDARY</span></div>
                      <span className="text-[9px] text-gray-300 text-center">Outline Btn</span>
                  </div>
              </div>
            </div>
          </div>
        )}

      </div>
    </aside>
  );
}
