'use client';

import React, { useEffect, useRef } from 'react';
import { useStudio } from '@/context/StudioContext';
import { LayerType, PostProcessingFx, GradientLayer } from '@/types/models';
import BlobHandlesOverlay from '@/components/canvas/BlobHandlesOverlay';
import ContrastChecker from '@/components/canvas/ContrastChecker';
import { ColorUtils } from '@/utils/colorUtils';
import clsx from 'clsx';

// Exact GLSL Fragment Shader ported 1:1 from Kotlin's AURORA_SHADER in ShaderEngines.kt
const AURORA_FRAGMENT_SHADER = `
precision highp float;
uniform vec2 u_resolution;
uniform float u_time;
uniform float u_speed;
uniform float u_complexity;
uniform vec4 u_color1;
uniform vec4 u_color2;
uniform vec4 u_color3;
uniform vec4 u_color4;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    // Flip Y for WebGL vs Android AGSL coordinates
    uv.y = 1.0 - uv.y;
    
    float t = u_time * u_speed * 0.5;
    
    // Wave calculations matching AGSL exactly
    float wave1 = sin(uv.x * 3.0 + t * 1.5 + sin(uv.y * 2.0));
    float wave2 = sin(uv.x * 4.0 - t * 1.2 + cos(uv.y * 3.0 + t));
    float wave3 = sin(uv.x * 2.5 + t * 2.0 + sin(uv.y * 1.5 - t));
    float wave4 = sin(uv.x * 5.0 - t * 0.8 + cos(uv.y * 2.5 + t * 1.1));
    
    wave1 = wave1 * 0.5 + 0.5;
    wave2 = wave2 * 0.5 + 0.5;
    wave3 = wave3 * 0.5 + 0.5;
    wave4 = wave4 * 0.5 + 0.5;
    
    float flow = (wave1 + wave2 + wave3 + wave4) / 4.0;
    flow = pow(flow, max(0.1, 2.0 - u_complexity * 0.2));
    
    vec3 col = mix(u_color1.rgb, u_color2.rgb, smoothstep(0.0, 0.4, flow));
    col = mix(col, u_color3.rgb, smoothstep(0.3, 0.7, flow));
    col = mix(col, u_color4.rgb, smoothstep(0.6, 1.0, flow));
    
    float verticalFade = smoothstep(0.0, 1.0, 1.0 - uv.y);
    col *= (0.5 + 0.5 * verticalFade);
    
    col = col * col * 1.5 + col * 0.5;
    
    gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
`;

const VERTEX_SHADER = `
attribute vec2 a_position;
void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
}
`;

// WebGL Canvas component executing the exact Kotlin ShaderEngines.kt Aurora shader
function AuroraCanvasLayer({ layer }: { layer: GradientLayer }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const { animStatus, animSpeed, animStyle } = useStudio();
  const timeRef = useRef<number>(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !layer.visible) return;

    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl') as WebGLRenderingContext | null;
    if (!gl) return;

    // Handle high-DPI scaling dynamically
    const updateSize = () => {
      const rect = canvas.getBoundingClientRect();
      const dpr = window.devicePixelRatio || 1;
      canvas.width = rect.width * dpr;
      canvas.height = rect.height * dpr;
      gl.viewport(0, 0, canvas.width, canvas.height);
    };
    
    const observer = new ResizeObserver(updateSize);
    observer.observe(canvas);
    updateSize();

    // Create shaders
    const createShader = (glCtx: WebGLRenderingContext, type: number, source: string) => {
      const shader = glCtx.createShader(type);
      if (!shader) return null;
      glCtx.shaderSource(shader, source);
      glCtx.compileShader(shader);
      return shader;
    };

    const vs = createShader(gl, gl.VERTEX_SHADER, VERTEX_SHADER);
    const fs = createShader(gl, gl.FRAGMENT_SHADER, AURORA_FRAGMENT_SHADER);
    if (!vs || !fs) return;

    const program = gl.createProgram();
    if (!program) return;
    gl.attachShader(program, vs);
    gl.attachShader(program, fs);
    gl.linkProgram(program);
    gl.useProgram(program);

    // Quad geometry
    const positionBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
    gl.bufferData(
      gl.ARRAY_BUFFER,
      new Float32Array([
        -1, -1,
         1, -1,
        -1,  1,
        -1,  1,
         1, -1,
         1,  1,
      ]),
      gl.STATIC_DRAW
    );

    const positionLocation = gl.getAttribLocation(program, 'a_position');
    gl.enableVertexAttribArray(positionLocation);
    gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0);

    // Uniform locations
    const uRes = gl.getUniformLocation(program, 'u_resolution');
    const uTime = gl.getUniformLocation(program, 'u_time');
    const uSpeed = gl.getUniformLocation(program, 'u_speed');
    const uComplexity = gl.getUniformLocation(program, 'u_complexity');
    const uColor1 = gl.getUniformLocation(program, 'u_color1');
    const uColor2 = gl.getUniformLocation(program, 'u_color2');
    const uColor3 = gl.getUniformLocation(program, 'u_color3');
    const uColor4 = gl.getUniformLocation(program, 'u_color4');

    const getColorGl = (colorStr: string) => {
      if (colorStr.startsWith('rgba')) {
        const parts = colorStr.match(/[\d.]+/g);
        if (parts) return [Number(parts[0]) / 255, Number(parts[1]) / 255, Number(parts[2]) / 255, Number(parts[3])];
      }
      const [r, g, b] = ColorUtils.hex2rgb(colorStr);
      return [r / 255, g / 255, b / 255, 1.0];
    };

    const c1 = getColorGl(layer.stops[0]?.color || '#FF2C55');
    const c2 = getColorGl(layer.stops[1]?.color || '#007BFF');
    const c3 = getColorGl(layer.stops[2]?.color || '#FFD60A');
    const c4 = getColorGl(layer.stops[3]?.color || layer.stops[0]?.color || '#FF2C55');

    let animId: number;

    const renderFrame = () => {
      if (animStatus === 'playing') {
        timeRef.current = performance.now() / 1000 * (animSpeed / 50);
      }

      gl.uniform2f(uRes, canvas.width, canvas.height);
      gl.uniform1f(uTime, timeRef.current);
      gl.uniform1f(uSpeed, (layer.animSpeed || 50) / 50);
      gl.uniform1f(uComplexity, ((layer.complexity || 50) / 10));

      gl.uniform4f(uColor1, c1[0], c1[1], c1[2], c1[3]);
      gl.uniform4f(uColor2, c2[0], c2[1], c2[2], c2[3]);
      gl.uniform4f(uColor3, c3[0], c3[1], c3[2], c3[3]);
      gl.uniform4f(uColor4, c4[0], c4[1], c4[2], c4[3]);

      gl.drawArrays(gl.TRIANGLES, 0, 6);

      if (animStatus === 'playing') {
        animId = requestAnimationFrame(renderFrame);
      }
    };

    renderFrame();

    return () => {
      cancelAnimationFrame(animId);
      observer.disconnect();
    };
  }, [layer, animStatus, animSpeed]);

  return (
    <canvas
      ref={canvasRef}
      width={600}
      height={400}
      className="absolute inset-0 w-full h-full pointer-events-none transition-opacity duration-400"
      style={{
        opacity: layer.visible ? layer.opacity : 0,
        mixBlendMode: layer.blendMode === 'normal' ? 'normal' : (layer.blendMode as any),
        filter: `brightness(${layer.brightness || 100}%) contrast(${layer.contrast || 100}%) saturate(${layer.saturation || 100}%) hue-rotate(${layer.hueShift || 0}deg) blur(${layer.blur || 0}px)`,
        animation: animStatus === 'playing' ? `${animStyle}-anim ${30 - (animSpeed / 100) * 28}s infinite alternate ease-in-out` : 'none'
      }}
    />
  );
}

export default function RenderCanvas() {
  const { layers, canvasShape, colorBlindMode, postFx, fxIntensity, animStatus, animStyle, animSpeed, animIntensity, mouseReact, globalAdjustments } = useStudio();
  const containerRef = useRef<HTMLDivElement>(null);
  const parallaxRef = useRef<HTMLDivElement>(null);

  // Mouse Reactivity Effect
  useEffect(() => {
    if (!mouseReact) return;
    const handleMouseMove = (e: MouseEvent) => {
      if (!parallaxRef.current) return;
      const x = e.clientX - (window.innerWidth / 2);
      const y = e.clientY - (window.innerHeight / 2);
      const moveX = (x / window.innerWidth) * -30;
      const moveY = (y / window.innerHeight) * -30;
      parallaxRef.current.style.transform = `translate(${moveX}px, ${moveY}px) scale(1.05)`;
    };
    window.addEventListener('mousemove', handleMouseMove);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      if (parallaxRef.current) parallaxRef.current.style.transform = 'none';
    };
  }, [mouseReact]);

  const shapeClass = {
    'rounded': 'aspect-[3/2] max-w-[600px] rounded-2xl w-full',
    'circle': 'aspect-square max-w-[400px] rounded-full w-full',
    'full': 'w-full h-full rounded-none max-w-full',
    'text': 'aspect-[3/2] max-w-[600px] rounded-2xl w-full shape-text'
  }[canvasShape];

  // Combine ColorBlind filter with Global Adjustments CSS filters
  const globalFilterStyle = [
    colorBlindMode !== 'none' ? `url(#${colorBlindMode})` : '',
    `brightness(${globalAdjustments.brightness}%)`,
    `contrast(${globalAdjustments.contrast}%)`,
    `saturate(${globalAdjustments.saturation}%)`,
    `hue-rotate(${globalAdjustments.hueShift}deg)`,
    globalAdjustments.blur > 0 ? `blur(${globalAdjustments.blur}px)` : ''
  ].filter(Boolean).join(' ') || 'none';

  // Helper to construct CSS background from a layer
  const renderLayerStyle = (layer: GradientLayer) => {
    if (!layer.visible) return {};
    
    let background = '';
    const colorStops = layer.stops
      .slice()
      .sort((a, b) => a.position - b.position)
      .map(stop => `${stop.color} ${stop.position}%`)
      .join(', ');

    switch (layer.type) {
      case LayerType.LINEAR:
        background = `${layer.repeatPattern ? 'repeating-' : ''}linear-gradient(${layer.angle}deg, ${colorStops})`;
        break;
      case LayerType.RADIAL:
        background = `${layer.repeatPattern ? 'repeating-' : ''}radial-gradient(ellipse ${layer.width}% ${layer.height}% at ${layer.centerX}% ${layer.centerY}%, ${colorStops})`;
        break;
      case LayerType.CONIC:
        background = `conic-gradient(from ${layer.angle}deg at ${layer.centerX}% ${layer.centerY}%, ${colorStops})`;
        break;
      case LayerType.MESH:
        background = layer.meshPoints.map((pt, i) => {
          const stop = layer.stops[i % layer.stops.length] || layer.stops[0];
          return `radial-gradient(circle at ${pt.x}% ${pt.y}%, ${stop.color} 0%, transparent 60%)`;
        }).join(', ');
        break;
      case LayerType.BLOB:
      case LayerType.LIQUID:
        let blobStr = layer.blobs.map((b, i) => {
          const s = layer.stops[i] || layer.stops[0];
          const hardStop = 100 - (b.feather || 100);
          return `radial-gradient(ellipse ${b.width || 40}% ${b.height || 40}% at ${b.x}% ${b.y}%, ${s?.color || '#4F46E5'} 0%, ${s?.color || '#4F46E5'} ${hardStop}%, transparent 100%)`;
        }).join(', ');
        if (layer.hasBaseBackground) {
          blobStr = blobStr ? `${blobStr}, ${layer.blobBgColor}` : layer.blobBgColor;
        }
        background = blobStr || `radial-gradient(circle at 50% 50%, ${layer.stops[0]?.color || '#4F46E5'} 0%, transparent 70%)`;
        break;
    }

    return {
      background,
      opacity: layer.opacity,
      mixBlendMode: layer.blendMode === 'normal' ? 'normal' : (layer.blendMode as any),
      backgroundSize: animStatus === 'playing' && animStyle === 'drift' ? '200% 200%' : '100% 100%',
      filter: `brightness(${layer.brightness || 100}%) contrast(${layer.contrast || 100}%) saturate(${layer.saturation || 100}%) hue-rotate(${layer.hueShift || 0}deg) blur(${layer.blur || 0}px)`,
      animation: animStatus === 'playing' ? `${animStyle}-anim ${30 - (animSpeed / 100) * 28}s infinite alternate ease-in-out` : 'none'
    };
  };

  return (
    <main 
      className="absolute inset-0 flex flex-col z-0 overflow-hidden"
      style={{
        paddingLeft: canvasShape === 'full' ? '0' : '352px',
        paddingRight: canvasShape === 'full' ? '0' : '392px',
        paddingTop: canvasShape === 'full' ? '0' : '96px',
        paddingBottom: canvasShape === 'full' ? '0' : '32px'
      }}
    >
      <div className="flex-1 relative flex items-center justify-center overflow-hidden w-full h-full">
        <div 
          ref={containerRef}
          className={clsx(
            "relative transition-all duration-500 ease-[cubic-bezier(0.25,0.8,0.25,1)] overflow-hidden",
            shapeClass
          )}
          style={{ filter: globalFilterStyle }}
        >
          {/* SVG liquid filter */}
          <svg width="0" height="0" className="absolute pointer-events-none">
            <filter id="liquid-filter">
              <feGaussianBlur in="SourceGraphic" stdDeviation="10" result="blur" />
              <feColorMatrix in="blur" mode="matrix" values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 18 -7" result="liquid" />
              <feBlend in="SourceGraphic" in2="liquid" />
            </filter>
          </svg>

          {/* Parallax Wrapper */}
          <div ref={parallaxRef} className="absolute -inset-8 pointer-events-none transition-transform duration-75 ease-out">
             {/* Render stack */}
             <div className="absolute inset-0 w-full h-full">
             {[...layers].reverse().map(layer => {
               if (layer.type === LayerType.AURORA) {
                 return <AuroraCanvasLayer key={layer.id} layer={layer} />;
               }
               return (
                <div 
                  key={layer.id} 
                  className="absolute inset-0 w-full h-full pointer-events-none transition-opacity duration-400"
                  style={{
                    ...renderLayerStyle(layer),
                    filter: layer.type === LayerType.LIQUID 
                      ? `${renderLayerStyle(layer).filter} url(#liquid-filter)` 
                      : renderLayerStyle(layer).filter
                  }}
                />
               );
             })}
             </div>

             {/* Canvas Interactive Handles Overlay */}
             <BlobHandlesOverlay />
          </div>

          {/* Post Processing FX Overlays */}
          {postFx === PostProcessingFx.GRAIN && (
             <div 
               className="absolute inset-0 w-full h-full pointer-events-none mix-blend-overlay opacity-50 z-[100]" 
               style={{ backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E")`, opacity: fxIntensity / 100 }} 
             />
          )}
          
          {postFx === PostProcessingFx.HALFTONE && (
             <div 
               className="absolute inset-0 w-full h-full pointer-events-none mix-blend-overlay z-[101]"
               style={{ background: 'radial-gradient(circle, #000 2px, transparent 2.5px)', backgroundSize: '8px 8px', opacity: (fxIntensity / 100) * 0.3 }} 
             />
          )}
          
          {postFx === PostProcessingFx.DITHER && (
             <div 
               className="absolute inset-0 w-full h-full pointer-events-none mix-blend-multiply z-[101]"
               style={{ backgroundImage: 'repeating-linear-gradient(45deg, rgba(0,0,0,0.1) 25%, transparent 25%, transparent 75%, rgba(0,0,0,0.1) 75%, rgba(0,0,0,0.1)), repeating-linear-gradient(45deg, rgba(0,0,0,0.1) 25%, transparent 25%, transparent 75%, rgba(0,0,0,0.1) 75%, rgba(0,0,0,0.1))', backgroundPosition: '0 0, 2px 2px', backgroundSize: '4px 4px', opacity: (fxIntensity / 100) * 0.5 }} 
             />
          )}

          {canvasShape === 'text' && (
             <div 
               className="text-mask-preview focus:outline-none" 
               contentEditable 
               spellCheck="false"
               suppressContentEditableWarning
             >
               CHROMA
             </div>
          )}
        </div>
      </div>
      
      {/* Contrast Ratio Indicator */}
      <ContrastChecker />

      {/* Dynamic Keyframes */}
      <style dangerouslySetInnerHTML={{__html: `
        @keyframes drift-anim { 0% { background-position: 0% 50%; } 100% { background-position: 100% 50%; } }
        @keyframes pulse-anim { 0% { transform: scale(1); opacity: 1; } 100% { transform: scale(${1 + (animIntensity / 500)}); opacity: ${Math.max(0.1, 1 - (animIntensity / 200))}; } }
        @keyframes hue-anim { 0% { filter: hue-rotate(0deg); } 100% { filter: hue-rotate(${animIntensity * 3.6}deg); } }
        @keyframes breathe-anim { 0% { border-radius: 16px; transform: scale(1); } 50% { border-radius: 24px; transform: scale(${1 + (animIntensity / 1000)}); } 100% { border-radius: 16px; transform: scale(1); } }
      `}} />
    </main>
  );
}
