'use client';

import React, { useState } from 'react';
import { Palette, ArrowUUpLeft, ArrowUUpRight, Code, Moon, Sun } from '@phosphor-icons/react';
import { useStudio } from '@/context/StudioContext';
import { useDesign, ToolType } from '@/context/DesignContext';
import { CursorClick, Selection, Square, Circle, Triangle, Star, PencilSimple, TextT, HandGrabbing, Ruler, Play, Layout } from '@phosphor-icons/react';
import clsx from 'clsx';
import HandoffModal from '@/components/modals/HandoffModal';
import WorkspaceDropdown from '@/components/modals/WorkspaceDropdown';

export default function AppBar() {
  const { canvasShape, setCanvasShape, globalAdjustments } = useStudio();
  const { appMode, setAppMode, currentTool, setTool, addArtboard, addShape, addText, toggleSmartGuides, enableSmartGuides, setIsPlayingPrototype } = useDesign();
  const [isHandoffOpen, setIsHandoffOpen] = useState(false);

  const handleTool = (t: ToolType) => setTool(t);

  const ToolBtn = ({ tool, icon, onClick, isActive, className }: any) => (
    <button 
      onClick={onClick || (() => handleTool(tool))}
      className={clsx(
        "w-8 h-8 rounded-lg flex items-center justify-center transition-colors cursor-pointer",
        isActive || currentTool === tool 
          ? "bg-white/20 text-white" 
          : "text-[var(--text-muted)] hover:text-white"
      )}
      title={tool}
    >
      {icon}
    </button>
  );

  return (
    <>
      <header className="app-bar glass-panel flex items-center justify-between px-6 h-16 shrink-0 relative z-[150] rounded-b-2xl border-t-0 border-x-0">
        <div className="flex items-center gap-4">
          {/* macOS Window Traffic Lights */}
          <div className="flex items-center gap-1.5">
            <div className="w-3 h-3 rounded-full bg-[var(--mac-red)] border border-black/10" />
            <div className="w-3 h-3 rounded-full bg-[var(--mac-yellow)] border border-black/10" />
            <div className="w-3 h-3 rounded-full bg-[var(--mac-green)] border border-black/10" />
          </div>

          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-[var(--primary)] flex items-center justify-center text-white">
              <Palette weight="regular" size={18} />
            </div>
            <div className="flex flex-col leading-none">
              <h1 className="text-sm font-bold m-0 tracking-tight text-[var(--text-main)]">
                Chroma<span className="text-[var(--primary)]">Studio</span>
              </h1>
            </div>
          </div>
        </div>

        <div className="hidden lg:flex flex-1 justify-center items-center gap-4 min-w-0">
          
          {/* Central Mode Switcher */}
          <div className="flex p-1 bg-white/5 border border-[var(--glass-border)] rounded-full backdrop-blur-xl shadow-inner mx-4">
            <button
              onClick={() => setAppMode('design')}
              className={clsx(
                "px-4 py-1.5 text-xs font-semibold rounded-full transition-all duration-300 flex items-center gap-2",
                appMode === 'design' ? "bg-[var(--primary)] text-white shadow-md" : "text-[var(--text-muted)] hover:text-white"
              )}
            >
              <Palette size={16} weight={appMode === 'design' ? "fill" : "regular"} />
              Design
            </button>
            <button
              onClick={() => setAppMode('prototype')}
              className={clsx(
                "px-4 py-1.5 text-xs font-semibold rounded-full transition-all duration-300 flex items-center gap-2",
                appMode === 'prototype' ? "bg-blue-600 text-white shadow-md" : "text-[var(--text-muted)] hover:text-white"
              )}
            >
              <Layout size={16} weight={appMode === 'prototype' ? "fill" : "regular"} />
              Prototype
            </button>
          </div>

          {appMode === 'prototype' ? (
            <div className="flex p-1 bg-white/10 border border-white/20 rounded-full backdrop-blur-md items-center gap-1 shrink-0 animate-in fade-in zoom-in-95 duration-300">
              <ToolBtn tool="select" icon={<CursorClick size={18} />} />
              <ToolBtn tool="proto" icon={<Selection size={18} className="text-blue-400" />} />
              <div className="w-px h-5 bg-white/10 mx-1"></div>
              <ToolBtn tool="artboard" icon={<Layout size={18} />} onClick={() => addArtboard()} />
              <ToolBtn tool="rect" icon={<Square size={18} />} onClick={() => addShape('rect')} />
              <ToolBtn tool="circle" icon={<Circle size={18} />} onClick={() => addShape('circle')} />
              <ToolBtn tool="triangle" icon={<Triangle size={18} />} onClick={() => addShape('triangle')} />
              <ToolBtn tool="star" icon={<Star size={18} />} onClick={() => addShape('star')} />
              <div className="w-px h-5 bg-white/10 mx-1"></div>
              <ToolBtn tool="draw" icon={<PencilSimple size={18} />} />
              <ToolBtn tool="text" icon={<TextT size={18} />} onClick={() => addText()} />
              <ToolBtn tool="pan" icon={<HandGrabbing size={18} />} />
              <ToolBtn tool="guides" icon={<Ruler size={18} className={enableSmartGuides ? "text-pink-400" : ""} />} onClick={() => toggleSmartGuides()} isActive={enableSmartGuides} />
            </div>
          ) : (
            <div className="flex p-1 bg-white/10 border border-white/20 rounded-full backdrop-blur-md shrink-0 animate-in fade-in zoom-in-95 duration-300">
              <button
                className={`hc-pill ${canvasShape === 'rounded' ? 'active' : ''}`}
                onClick={() => setCanvasShape('rounded')}
              >
                Card
              </button>
              <button
                className={`hc-pill ${canvasShape === 'circle' ? 'active' : ''}`}
                onClick={() => setCanvasShape('circle')}
              >
                Circle
              </button>
              <button
                className={`hc-pill ${canvasShape === 'full' ? 'active' : ''}`}
                onClick={() => setCanvasShape('full')}
              >
                Full
              </button>
              <button
                className={`hc-pill ${canvasShape === 'text' ? 'active' : ''}`}
                onClick={() => setCanvasShape('text')}
              >
                Text
              </button>
            </div>
          )}
        </div>

        <div className="flex items-center gap-1 lg:gap-3">
          <WorkspaceDropdown />

          <button className="btn-icon focus-ring" disabled title="Undo (Ctrl+Z)">
            <ArrowUUpLeft weight="regular" />
          </button>
          <button className="btn-icon focus-ring" disabled title="Redo (Ctrl+Y)">
            <ArrowUUpRight weight="regular" />
          </button>

          <div className="w-px h-5 bg-[var(--glass-border)] mx-1 hidden lg:block"></div>

          <div className="flex gap-2 items-center">
          <button 
            className="btn px-4 bg-blue-600 hover:bg-blue-500 text-white rounded-full text-xs font-semibold shadow-lg border-none flex items-center justify-center gap-1 cursor-pointer"
            onClick={() => setIsPlayingPrototype(true)}
          >
            <Play size={14} weight="fill" /> Play
          </button>

          <button 
            className="btn btn-primary px-4 cursor-pointer"
            onClick={() => setIsHandoffOpen(true)}
          >
            <span className="font-mono mr-1.5 opacity-70">{"</>"}</span>
            Handoff
          </button>
        </div>
        </div>
      </header>

      <HandoffModal isOpen={isHandoffOpen} onClose={() => setIsHandoffOpen(false)} />
    </>
  );
}
