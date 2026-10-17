'use client';

import React, { useEffect, useRef } from 'react';
import { useDesign } from '@/context/DesignContext';
import clsx from 'clsx';

// Include the entire NexGen logic inside the component. We use require('fabric') since we are in a client component.
export default function PrototypeCanvas() {
  const { appMode, setCanvas, setProtoLinesCanvas, currentTool, setTool, setSelectedObject } = useDesign();
  const wrapperRef = useRef<HTMLDivElement>(null);
  const mainCanvasRef = useRef<HTMLCanvasElement>(null);
  const protoCanvasRef = useRef<HTMLCanvasElement>(null);
  
  // Fabric instance
  const fCanvas = useRef<any>(null);
  
  useEffect(() => {
    if (typeof window === 'undefined') return;
    
    // Dynamically import fabric to avoid SSR issues
    import('fabric').then((fabricModule) => {
      const fabric = fabricModule.fabric || fabricModule;

      if (!wrapperRef.current || !mainCanvasRef.current || !protoCanvasRef.current) return;
      
      const wrapper = wrapperRef.current;
      
      // Initialize Fabric Canvas
      const canvas = new fabric.Canvas(mainCanvasRef.current, {
        width: wrapper.clientWidth,
        height: wrapper.clientHeight,
        preserveObjectStacking: true,
        selection: true,
        fireRightClick: true,
        stopContextMenu: true
      });
      
      fCanvas.current = canvas;
      setCanvas(canvas);

      const plCanvasEl = protoCanvasRef.current;
      plCanvasEl.width = wrapper.clientWidth;
      plCanvasEl.height = wrapper.clientHeight;
      setProtoLinesCanvas(plCanvasEl.getContext('2d'));

      // Configure default object properties
      fabric.Object.prototype.transparentCorners = false;
      fabric.Object.prototype.cornerColor = '#FFFFFF';
      fabric.Object.prototype.cornerStrokeColor = '#6366f1';
      fabric.Object.prototype.borderColor = '#6366f1';
      fabric.Object.prototype.cornerSize = 8;
      fabric.Object.prototype.strokeUniform = true; 

      // Port over the custom render functions for blur/shadows/corners
      // Note: We are ignoring the shadow/glow requests per the user's rule for the UI,
      // but the *canvas objects* the user draws can have shadows if they want.
      
      let isPanning = false;
      let lastPosX = 0;
      let lastPosY = 0;

      canvas.on('mouse:down', function(opt: any) {
        // @ts-ignore
        if (window.__spacePressed || opt.e.button === 1 || opt.e.code === 'Space') {
          isPanning = true;
          canvas.selection = false;
          lastPosX = opt.e.clientX || (opt.e.touches && opt.e.touches[0].clientX); 
          lastPosY = opt.e.clientY || (opt.e.touches && opt.e.touches[0].clientY);
        }
      });

      canvas.on('mouse:move', function(opt: any) {
        if (isPanning && opt.e) {
          let clientX = opt.e.clientX || (opt.e.touches && opt.e.touches[0].clientX);
          let clientY = opt.e.clientY || (opt.e.touches && opt.e.touches[0].clientY);
          let vpt = canvas.viewportTransform!.slice();
          vpt[4] += clientX - lastPosX; vpt[5] += clientY - lastPosY;
          canvas.setViewportTransform(vpt);
          wrapper.style.backgroundPosition = `${vpt[4]}px ${vpt[5]}px`;
          lastPosX = clientX; lastPosY = clientY;
        }
      });

      canvas.on('mouse:up', function() {
        isPanning = false;
        canvas.selection = true;
      });

      canvas.on('mouse:wheel', function(opt: any) {
        if(opt.e.ctrlKey || opt.e.metaKey) {
          let zoom = canvas.getZoom() * (0.999 ** opt.e.deltaY);
          zoom = Math.max(0.05, Math.min(20, zoom));
          canvas.zoomToPoint({ x: opt.e.offsetX, y: opt.e.offsetY }, zoom);
        } else {
          let vpt = canvas.viewportTransform!.slice();
          vpt[4] -= opt.e.deltaX; vpt[5] -= opt.e.deltaY;
          canvas.setViewportTransform(vpt);
        }
        wrapper.style.backgroundPosition = `${canvas.viewportTransform![4]}px ${canvas.viewportTransform![5]}px`;
        opt.e.preventDefault();
        opt.e.stopPropagation();
      });

      canvas.on('selection:created', () => setSelectedObject(canvas.getActiveObject()));
      canvas.on('selection:updated', () => setSelectedObject(canvas.getActiveObject()));
      canvas.on('selection:cleared', () => setSelectedObject(null));

      // Hook up commands
      const commandsRef = (window as any).__designCommandsRef;
      if (commandsRef) {
        let objCounter = 2;
        commandsRef.current.addShape = (type: string) => {
          let vpt = canvas.viewportTransform!;
          let zoom = canvas.getZoom();
          let center = { x: -vpt[4]/zoom + (canvas.getWidth()/zoom)/2, y: -vpt[5]/zoom + (canvas.getHeight()/zoom)/2 };
          let opts: any = { left: center.x-50, top: center.y-50, width: 100, height: 100, fill: '#6366f1', id: 'obj_'+(objCounter++), name: type, rx: 8, ry: 8 };
          let obj;
          if (type === 'rect') obj = new fabric.Rect(opts);
          else if (type === 'circle') { opts.radius=50; delete opts.width; delete opts.height; delete opts.rx; delete opts.ry; obj = new fabric.Circle(opts); }
          else if (type === 'triangle') { opts.width = 100; opts.height = 100; delete opts.rx; delete opts.ry; obj = new fabric.Triangle(opts); }
          if(obj) { canvas.add(obj); canvas.setActiveObject(obj); }
        };
        
        commandsRef.current.addArtboard = () => {
          let artboards = canvas.getObjects().filter((o:any) => o.id?.startsWith('artboard'));
          let lastAB = artboards[artboards.length - 1];
          let left = lastAB ? lastAB.left! + (lastAB.width! * lastAB.scaleX!) + 100 : (wrapper.clientWidth - 393) / 2;
          let top = lastAB ? lastAB.top : (wrapper.clientHeight - 852) / 2;
          let newAB = new fabric.Rect({
              left, top, width: 393, height: 852, fill: 'rgba(24, 24, 27, 0.85)', selectable: true, evented: true,
              id: 'artboard_' + (++objCounter), name: 'Screen ' + objCounter, rx: 24, ry: 24
          } as any);
          canvas.add(newAB); canvas.sendToBack(newAB); canvas.renderAll();
        };

        commandsRef.current.deleteSelected = () => {
          let a = canvas.getActiveObjects();
          if (a.length) {
            a.forEach(o => canvas.remove(o));
            canvas.discardActiveObject();
          }
        };

        commandsRef.current.zoomToFit = () => {
            let artboards = canvas.getObjects().filter((o:any) => o.id?.startsWith('artboard'));
            if(artboards.length === 0) return;
            
            let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
            artboards.forEach(ab => {
                let rect = ab.getBoundingRect();
                if (rect.left < minX) minX = rect.left;
                if (rect.top < minY) minY = rect.top;
                if (rect.left + rect.width > maxX) maxX = rect.left + rect.width;
                if (rect.top + rect.height > maxY) maxY = rect.top + rect.height;
            });
            
            let w = maxX - minX; let h = maxY - minY; let padding = 150;
            let vpw = canvas.getWidth(); let vph = canvas.getHeight();
            let zoomX = vpw / (w + padding); let zoomY = vph / (h + padding);
            let zoom = Math.min(zoomX, zoomY); if (zoom > 2) zoom = 2; 
            
            canvas.setZoom(zoom);
            let vpt = canvas.viewportTransform!;
            vpt[4] = (vpw - w * zoom) / 2 - minX * zoom;
            vpt[5] = (vph - h * zoom) / 2 - minY * zoom;
            
            wrapper.style.backgroundPosition = `${vpt[4]}px ${vpt[5]}px`;
            canvas.requestRenderAll();
        };

        commandsRef.current.addUIKit = ({type, dropPos}: any) => {
          let vpt = canvas.viewportTransform!;
          let zoom = canvas.getZoom();
          let center = dropPos || { x: -vpt[4]/zoom + (canvas.getWidth()/zoom)/2, y: -vpt[5]/zoom + (canvas.getHeight()/zoom)/2 };
          let objs: any[] = [];
          let groupName = "UI Component";
          let pOpts = { fill: 'transparent', stroke: '#a1a1aa', strokeWidth: 2, strokeLineCap: 'round', strokeLineJoin: 'round' };

          if (type === 'card-basic') {
            let bg = new fabric.Rect({ width: 280, height: 160, fill: '#18181b', stroke: '#27272a', strokeWidth: 1, rx: 16, ry: 16 });
            let title = new fabric.IText('Card Title', { fontSize: 16, fontFamily: 'Inter', fontWeight: 600, fill: '#ffffff', left: 20, top: 20 });
            let desc = new fabric.IText('This is a basic card component. You can add more text or items inside it.', { fontSize: 12, fontFamily: 'Inter', fill: '#a1a1aa', left: 20, top: 50, width: 240 });
            objs = [bg, title, desc]; groupName = "Card Component";
          } else if (type === 'card-blue') {
            let bg = new fabric.Rect({ width: 280, height: 160, fill: '#1e3a8a', stroke: '#3b82f6', strokeWidth: 1, rx: 16, ry: 16 });
            let title = new fabric.IText('Blue Card', { fontSize: 16, fontFamily: 'Inter', fontWeight: 600, fill: '#ffffff', left: 20, top: 20 });
            let desc = new fabric.IText('A vibrant blue card component.', { fontSize: 12, fontFamily: 'Inter', fill: '#bfdbfe', left: 20, top: 50, width: 240 });
            objs = [bg, title, desc]; groupName = "Blue Card";
          } else if (type === 'card-rose') {
            let bg = new fabric.Rect({ width: 280, height: 160, fill: '#881337', stroke: '#f43f5e', strokeWidth: 1, rx: 16, ry: 16 });
            let title = new fabric.IText('Rose Card', { fontSize: 16, fontFamily: 'Inter', fontWeight: 600, fill: '#ffffff', left: 20, top: 20 });
            let desc = new fabric.IText('A vibrant rose card component.', { fontSize: 12, fontFamily: 'Inter', fill: '#fecdd3', left: 20, top: 50, width: 240 });
            objs = [bg, title, desc]; groupName = "Rose Card";
          } else if (type === 'section-dark') {
            let bg = new fabric.Rect({ width: 600, height: 300, fill: '#09090b', stroke: '#27272a', strokeWidth: 1, rx: 0, ry: 0 });
            let title = new fabric.IText('Section Title', { fontSize: 24, fontFamily: 'Inter', fontWeight: 800, fill: '#ffffff', left: 40, top: 40 });
            let desc = new fabric.IText('This is a large section container for page layouts.', { fontSize: 14, fontFamily: 'Inter', fill: '#a1a1aa', left: 40, top: 80, width: 500 });
            objs = [bg, title, desc]; groupName = "Dark Section";
          } else if (type === 'button-primary') {
            let rect = new fabric.Rect({ width: 140, height: 44, fill: '#6366f1', rx: 10, ry: 10 });
            let text = new fabric.IText('Primary Action', { fontSize: 13, fontFamily: 'Inter', fontWeight: 600, fill: '#ffffff', left: 24, top: 14 });
            objs = [rect, text]; groupName = "Primary Button";
          } else if (type === 'button-outline') {
            let rect = new fabric.Rect({ width: 140, height: 44, fill: 'transparent', stroke: '#6366f1', strokeWidth: 2, rx: 10, ry: 10 });
            let text = new fabric.IText('Secondary', { fontSize: 13, fontFamily: 'Inter', fontWeight: 600, fill: '#6366f1', left: 35, top: 14 });
            objs = [rect, text]; groupName = "Outline Button";
          }

          if (objs.length > 0) {
            let group = new fabric.Group(objs, {
                left: center.x - (objs[0].width / 2), top: center.y - (objs[0].height / 2),
                id: 'obj_' + (objCounter++), name: groupName, subTargetCheck: true
            } as any);
            canvas.add(group); canvas.setActiveObject(group);
          }
        };
      }

      const handleKeyDown = (e: KeyboardEvent) => {
        if (e.key === 'Delete' || e.key === 'Backspace') {
           // ensure we aren't in an input field
           if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement).tagName)) return;
           const commandsRef = (window as any).__designCommandsRef;
           if (commandsRef && commandsRef.current.deleteSelected) {
             commandsRef.current.deleteSelected();
           }
        }
      };
      window.addEventListener('keydown', handleKeyDown);

      const handleResize = () => {
        canvas.setWidth(wrapper.clientWidth);
        canvas.setHeight(wrapper.clientHeight);
        plCanvasEl.width = wrapper.clientWidth;
        plCanvasEl.height = wrapper.clientHeight;
        canvas.renderAll();
      };
      window.addEventListener('resize', handleResize);

      // Cleanup
      return () => {
        window.removeEventListener('resize', handleResize);
        window.removeEventListener('keydown', handleKeyDown);
        canvas.dispose();
      };
    });
  }, []);

  return (
    <div 
      ref={wrapperRef} 
      className={clsx(
        "absolute inset-0 z-10 transition-all duration-300",
        appMode === 'prototype' ? "pointer-events-auto" : "pointer-events-none"
      )}
      style={{
        backgroundImage: appMode === 'prototype' ? 'radial-gradient(rgba(255,255,255,0.08) 1px, transparent 1px)' : 'none',
        backgroundSize: '24px 24px',
        backgroundPosition: 'center center'
      }}
    >
      <canvas ref={mainCanvasRef} />
      <canvas ref={protoCanvasRef} className="absolute inset-0 pointer-events-none z-20" />
    </div>
  );
}
