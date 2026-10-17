'use client';

import React, { useState, useEffect } from 'react';
import { useStudio } from '@/context/StudioContext';
import { FloppyDisk, Trash, Folder } from '@phosphor-icons/react';

interface SavedWorkspace {
  name: string;
  date: number;
  state: any;
}

export default function WorkspaceDropdown() {
  const { layers, globalAdjustments, postFx, animStatus, animStyle, animSpeed, animIntensity, showToast, addLayer } = useStudio();
  const [isOpen, setIsOpen] = useState(false);
  const [wsName, setWsName] = useState('');
  const [savedSpaces, setSavedSpaces] = useState<Record<string, SavedWorkspace>>({});

  useEffect(() => {
    try {
      const stored = localStorage.getItem('chroma_workspaces');
      if (stored) {
        setSavedSpaces(JSON.parse(stored));
      }
    } catch (e) {}
  }, []);

  const saveWorkspace = () => {
    const name = wsName.trim() || `Workspace ${Object.keys(savedSpaces).length + 1}`;
    const newState = {
      layers,
      globalAdjustments,
      postFx,
      animStatus,
      animStyle,
      animSpeed,
      animIntensity
    };

    const updated = {
      ...savedSpaces,
      [name]: {
        name,
        date: Date.now(),
        state: newState
      }
    };

    setSavedSpaces(updated);
    try {
      localStorage.setItem('chroma_workspaces', JSON.stringify(updated));
    } catch (e) {}

    setWsName('');
    showToast(`Saved workspace "${name}"`);
  };

  const deleteWorkspace = (name: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const updated = { ...savedSpaces };
    delete updated[name];
    setSavedSpaces(updated);
    try {
      localStorage.setItem('chroma_workspaces', JSON.stringify(updated));
    } catch (e) {}
    showToast(`Deleted workspace "${name}"`);
  };

  return (
    <div className="relative">
      <button 
        className="btn btn-ghost text-xs" 
        onClick={() => setIsOpen(!isOpen)}
      >
        <Folder weight="bold" /> <span className="hidden sm:inline">Workspaces</span>
      </button>

      {isOpen && (
        <div className="workspace-dropdown show absolute top-full right-0 mt-2 bg-[var(--glass-bg)] backdrop-blur-xl border border-[var(--glass-border)] rounded-xl p-3 shadow-2xl min-w-[240px] z-[250] flex flex-col gap-2">
          <div className="flex gap-2">
            <input
              type="text"
              value={wsName}
              onChange={(e) => setWsName(e.target.value)}
              placeholder="Name workspace..."
              className="flat-input text-xs py-1.5 flex-1"
              onKeyDown={(e) => e.key === 'Enter' && saveWorkspace()}
            />
          </div>

          <button
            className="btn btn-primary text-xs w-full py-1.5 flex items-center justify-center gap-1.5"
            onClick={saveWorkspace}
          >
            <FloppyDisk size={14} /> Save Current
          </button>

          <div className="w-full h-px bg-[var(--glass-border)] my-1" />

          <div className="flex flex-col gap-1 max-h-[180px] overflow-y-auto">
            {Object.keys(savedSpaces).length === 0 ? (
              <span className="text-[10px] text-[var(--text-muted)] text-center py-2">
                No saved workspaces
              </span>
            ) : (
              Object.values(savedSpaces).map((ws) => (
                <div
                  key={ws.name}
                  className="ws-item p-2 rounded-lg hover:bg-[var(--glass-bg-hover)] text-xs flex justify-between items-center cursor-pointer"
                  onClick={() => {
                    showToast(`Restored ${ws.name}`);
                    setIsOpen(false);
                  }}
                >
                  <span className="font-semibold text-[var(--text-main)] truncate max-w-[150px]">
                    {ws.name}
                  </span>
                  <button
                    className="text-[var(--error)] hover:scale-110 p-1"
                    onClick={(e) => deleteWorkspace(ws.name, e)}
                  >
                    <Trash size={12} />
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
