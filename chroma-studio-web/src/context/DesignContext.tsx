'use client';

import React, { createContext, useContext, useState, useRef, ReactNode, useEffect } from 'react';

export type ToolType = 'select' | 'draw' | 'proto' | 'pan' | 'artboard' | 'rect' | 'circle' | 'triangle' | 'star' | 'text';

interface DesignState {
  appMode: 'design' | 'prototype';
  setAppMode: (mode: 'design' | 'prototype') => void;
  canvas: any;
  setCanvas: (canvas: any) => void;
  protoLinesCanvas: any;
  setProtoLinesCanvas: (ctx: any) => void;
  
  currentTool: ToolType;
  setTool: (tool: ToolType) => void;
  
  selectedObject: any;
  setSelectedObject: (obj: any) => void;
  
  historyIndex: number;
  historyLength: number;
  undo: () => void;
  redo: () => void;
  
  enableSmartGuides: boolean;
  toggleSmartGuides: () => void;
  snapToGrid: boolean;
  toggleGridSnap: () => void;
  
  zoomLevel: number;
  zoomIn: () => void;
  zoomOut: () => void;
  zoomToFit: () => void;

  isPlayingPrototype: boolean;
  setIsPlayingPrototype: (playing: boolean) => void;
  protoStartScreenId: string | null;

  addShape: (type: 'rect' | 'circle' | 'triangle' | 'star') => void;
  addText: () => void;
  addArtboard: () => void;
  addUIKit: (type: string, dropPos?: {x: number, y: number}) => void;
  
  deleteSelected: () => void;
  copySelected: () => void;
  pasteSelected: () => void;
  duplicateSelected: () => void;
  groupSelected: () => void;
  ungroupSelected: () => void;
  bringForward: () => void;
  sendBackward: () => void;
  maskSelection: () => void;
  clearWorkspace: () => void;
}

const defaultState: DesignState = {
  appMode: 'design',
  setAppMode: () => {},
  canvas: null,
  setCanvas: () => {},
  protoLinesCanvas: null,
  setProtoLinesCanvas: () => {},
  currentTool: 'select',
  setTool: () => {},
  selectedObject: null,
  setSelectedObject: () => {},
  historyIndex: 0,
  historyLength: 0,
  undo: () => {},
  redo: () => {},
  enableSmartGuides: true,
  toggleSmartGuides: () => {},
  snapToGrid: false,
  toggleGridSnap: () => {},
  zoomLevel: 1,
  zoomIn: () => {},
  zoomOut: () => {},
  zoomToFit: () => {},
  isPlayingPrototype: false,
  setIsPlayingPrototype: () => {},
  protoStartScreenId: null,

  addShape: () => {},
  addText: () => {},
  addArtboard: () => {},
  addUIKit: () => {},
  
  deleteSelected: () => {},
  copySelected: () => {},
  pasteSelected: () => {},
  duplicateSelected: () => {},
  groupSelected: () => {},
  ungroupSelected: () => {},
  bringForward: () => {},
  sendBackward: () => {},
  maskSelection: () => {},
  clearWorkspace: () => {},
};

const DesignContext = createContext<DesignState>(defaultState);

export const useDesign = () => useContext(DesignContext);

export const DesignProvider = ({ children }: { children: ReactNode }) => {
  const [appMode, setAppMode] = useState<'design' | 'prototype'>('design');
  const [canvas, setCanvas] = useState<any>(null);
  const [protoLinesCanvas, setProtoLinesCanvas] = useState<any>(null);
  const [currentTool, setCurrentTool] = useState<ToolType>('select');
  const [selectedObject, setSelectedObject] = useState<any>(null);
  const [historyIndex, setHistoryIndex] = useState(0);
  const [historyLength, setHistoryLength] = useState(0);
  const [enableSmartGuides, setEnableSmartGuides] = useState(true);
  const [snapToGrid, setSnapToGrid] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1);
  const [isPlayingPrototype, setIsPlayingPrototype] = useState(false);
  const [protoStartScreenId, setProtoStartScreenId] = useState<string | null>(null);

  const commandsRef = useRef<any>({});

  const executeCommand = (cmd: string, args?: any) => {
    if (commandsRef.current[cmd]) {
      commandsRef.current[cmd](args);
    }
  };

  const value: DesignState = {
    appMode,
    setAppMode,
    canvas, setCanvas,
    protoLinesCanvas, setProtoLinesCanvas,
    currentTool, setTool: (t) => { setCurrentTool(t); executeCommand('setTool', t); },
    selectedObject, setSelectedObject,
    historyIndex, historyLength,
    undo: () => executeCommand('undo'),
    redo: () => executeCommand('redo'),
    enableSmartGuides, toggleSmartGuides: () => { setEnableSmartGuides(!enableSmartGuides); executeCommand('toggleSmartGuides', !enableSmartGuides); },
    snapToGrid, toggleGridSnap: () => { setSnapToGrid(!snapToGrid); executeCommand('toggleGridSnap', !snapToGrid); },
    zoomLevel,
    zoomIn: () => executeCommand('zoom', 1.1),
    zoomOut: () => executeCommand('zoom', 0.9),
    zoomToFit: () => executeCommand('zoomToFit'),
    isPlayingPrototype, setIsPlayingPrototype,
    protoStartScreenId,

    addShape: (type) => executeCommand('addShape', type),
    addText: () => executeCommand('addText'),
    addArtboard: () => executeCommand('addArtboard'),
    addUIKit: (type, dropPos) => executeCommand('addUIKit', { type, dropPos }),
    
    deleteSelected: () => executeCommand('deleteSelected'),
    copySelected: () => executeCommand('copySelected'),
    pasteSelected: () => executeCommand('pasteSelected'),
    duplicateSelected: () => executeCommand('duplicateSelected'),
    groupSelected: () => executeCommand('groupSelected'),
    ungroupSelected: () => executeCommand('ungroupSelected'),
    bringForward: () => executeCommand('bringForward'),
    sendBackward: () => executeCommand('sendBackward'),
    maskSelection: () => executeCommand('maskSelection'),
    clearWorkspace: () => executeCommand('clearWorkspace'),
  };

  useEffect(() => {
    (window as any).__designCommandsRef = commandsRef;
  }, []);

  return <DesignContext.Provider value={value}>{children}</DesignContext.Provider>;
};
