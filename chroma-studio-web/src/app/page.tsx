'use client';

import React, { useState } from 'react';
import AppBar from '@/components/layout/AppBar';
import LeftPanel from '@/components/layout/LeftPanel';
import RightPanel from '@/components/layout/RightPanel';
import RenderCanvas from '@/components/canvas/RenderCanvas';
import PrototypeCanvas from '@/components/canvas/PrototypeCanvas';
import Toast from '@/components/layout/Toast';
import { StudioProvider } from '@/context/StudioContext';
import { DesignProvider } from '@/context/DesignContext';

export default function StudioPage() {
  React.useEffect(() => {
    document.body.classList.remove('dark');
  }, []);

  return (
    <StudioProvider>
      <DesignProvider>
        <div className="flex flex-col h-screen w-full overflow-hidden relative">
          {/* SVG Filters for color blindness */}
          <svg className="svg-filters h-0 w-0 absolute">
            <defs>
              <filter id="protanopia"><feColorMatrix type="matrix" values="0.567, 0.433, 0, 0, 0  0.558, 0.442, 0, 0, 0  0, 0.242, 0.758, 0, 0  0, 0, 0, 1, 0"/></filter>
              <filter id="deuteranopia"><feColorMatrix type="matrix" values="0.625, 0.375, 0, 0, 0  0.7, 0.3, 0, 0, 0  0, 0.3, 0.7, 0, 0  0, 0, 0, 1, 0"/></filter>
              <filter id="tritanopia"><feColorMatrix type="matrix" values="0.95, 0.05, 0, 0, 0  0, 0.433, 0.567, 0, 0  0, 0.475, 0.525, 0, 0  0, 0, 0, 1, 0"/></filter>
            </defs>
          </svg>

          <div className="grid-background"></div>

          {/* Background Gradient Layer */}
          <RenderCanvas />
          
          {/* Design Prototype Layer */}
          <PrototypeCanvas />

          {/* Floating UI Layer */}
          <div className="absolute inset-0 pointer-events-none flex flex-col z-10">
            <div className="pointer-events-auto">
              <AppBar />
            </div>

            <div className="workspace-area flex-1 flex justify-between overflow-hidden min-h-0 pointer-events-none">
              <div className="pointer-events-auto h-full">
                <LeftPanel />
              </div>
              <div className="pointer-events-auto h-full">
                <RightPanel />
              </div>
            </div>
          </div>

          <Toast />
        </div>
      </DesignProvider>
    </StudioProvider>
  );
}
