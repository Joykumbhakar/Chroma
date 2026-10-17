package com.chroma.studio.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

object ThreeDIconHtml {
    val PHOTOS_ICON_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>3D Photos Icon</title>
    
    
    <style>
        html, body {
            margin: 0;
            padding: 0;
            background: transparent !important;
            background-color: transparent !important;
            width: 100%;
            height: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
            overflow: hidden;
            -webkit-user-select: none;
            user-select: none;
        }

        .pointer-events-none { pointer-events: none; }

        .scene {
            perspective: 1200px;
            width: 100%;
            height: 100%;
            max-width: 100%;
            max-height: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
        }
    
        

        /* 3D Scene setup */
        
        
        .icon-3d {
            width: 100%;
            height: 100%;
            position: relative;
            transform-style: preserve-3d;
            /* Continuous wobble to showcase 3D depth without hovering */
            animation: wobble 12s cubic-bezier(0.4, 0, 0.2, 1) infinite;
        }

        /* Layer positioning */
        .layer {
            position: absolute;
            inset: 0;
            width: 100%;
            height: 100%;
            transform-style: preserve-3d;
        }

        /* Z-Axis Depth Translations for Parallax Effect */
        .l-shadow { transform: translateZ(-40px); filter: blur(28px); opacity: 0.5; }
        .l-base   { transform: translateZ(0px); }
        .l-sun    { transform: translateZ(35px); }
        .l-mount1 { transform: translateZ(70px); }
        .l-mount2 { transform: translateZ(105px); }
        .l-border { transform: translateZ(140px); }

        /* Smooth continuous 3D rotation */
        @keyframes wobble {
            0%, 100% { transform: rotateX(14deg) rotateY(-20deg) rotateZ(3deg); }
            33% { transform: rotateX(-10deg) rotateY(-8deg) rotateZ(-2deg); }
            66% { transform: rotateX(-14deg) rotateY(18deg) rotateZ(-3deg); }
        }

        /* Ambient floating animations for inner elements */
        .float-slow {
            animation: float 6s ease-in-out infinite alternate;
        }
        .pulse-slow {
            animation: pulse 4s ease-in-out infinite alternate;
        }

        @keyframes float {
            0% { transform: translateY(0px); }
            100% { transform: translateY(-4px); }
        }
        @keyframes pulse {
            0% { transform: scale(0.95); opacity: 0.8; }
            100% { transform: scale(1.05); opacity: 1; }
        }
    </style>
</head>
<body>

    <svg class="absolute w-0 h-0 overflow-hidden" aria-hidden="true">
        <defs>
            <!-- Background Sky Gradient (Vibrant Blue Daytime vibe) -->
            <linearGradient id="sky-grad" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#1E40AF" /> <!-- Deep Blue -->
                <stop offset="50%" stop-color="#3B82F6" /> <!-- Vibrant Blue -->
                <stop offset="100%" stop-color="#93C5FD" /> <!-- Soft Light Blue -->
            </linearGradient>

            <!-- Sun Gradient (Golden hour contrast) -->
            <linearGradient id="sun-grad" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#FEF08A" />
                <stop offset="100%" stop-color="#F59E0B" />
            </linearGradient>

            <!-- Back Mountain Gradient (Slate/Indigo for depth) -->
            <linearGradient id="mount-back-grad" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#64748B" stop-opacity="0.95" />
                <stop offset="100%" stop-color="#334155" stop-opacity="0.95" />
            </linearGradient>

            <!-- Front Mountain Gradient (Rich Emerald) -->
            <linearGradient id="mount-front-grad" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#10B981" stop-opacity="0.95" />
                <stop offset="100%" stop-color="#047857" stop-opacity="0.95" />
            </linearGradient>

            <!-- Glass Reflection Highlight -->
            <linearGradient id="glass-shine" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.8" />
                <stop offset="30%" stop-color="#FFFFFF" stop-opacity="0.1" />
                <stop offset="50%" stop-color="#FFFFFF" stop-opacity="0" />
                <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.3" />
            </linearGradient>

            <!-- Clip path to keep parallax elements inside the frame visually -->
            <clipPath id="frame-clip">
                <!-- Matches the frame geometry: 24x24 scaled up -> 196x196 centered -->
                <rect x="30" y="30" width="196" height="196" rx="42" />
            </clipPath>
        </defs>
    </svg>

    <div class="scene">
        <div class="icon-3d">
            
            <!-- Layer 1: Ambient Drop Shadow -->
            <svg class="layer l-shadow" viewBox="0 0 256 256">
                <rect x="34" y="44" width="188" height="188" rx="42" fill="#000000" />
            </svg>

            <!-- Layer 2: Base Frame & Sky -->
            <svg class="layer l-base" viewBox="0 0 256 256">
                <!-- Main Sky Background -->
                <rect x="30" y="30" width="196" height="196" rx="42" fill="url(#sky-grad)" />
                <!-- Inner glow/shadow for depth -->
                <rect x="30" y="30" width="196" height="196" rx="42" fill="none" stroke="#000000" stroke-width="2" stroke-opacity="0.1" />
            </svg>

            <!-- Layer 3: Sun/Moon -->
            <svg class="layer l-sun" viewBox="0 0 256 256">
                <g clip-path="url(#frame-clip)">
                    <g class="pulse-slow" style="transform-origin: 90px 90px;">
                        <!-- Scaled placement matching Lucide cx=9, cy=9 -->
                        <circle cx="95" cy="95" r="28" fill="url(#sun-grad)" />
                        <circle cx="95" cy="95" r="40" fill="#FEF08A" opacity="0.3" filter="blur(8px)" />
                    </g>
                </g>
            </svg>

            <!-- Layer 4: Background Mountain -->
            <svg class="layer l-mount1" viewBox="0 0 256 256">
                <g clip-path="url(#frame-clip)" class="float-slow" style="animation-delay: -3s;">
                    <!-- Right-leaning peak -->
                    <path d="M 90 260 L 180 110 Q 190 95 200 110 L 290 240 L 290 260 Z" fill="url(#mount-back-grad)" />
                    <!-- Highlight edge -->
                    <path d="M 90 260 L 180 110 Q 190 95 200 110" fill="none" stroke="#CBD5E1" stroke-width="3" stroke-linecap="round" opacity="0.6" />
                </g>
            </svg>

            <!-- Layer 5: Foreground Mountain -->
            <svg class="layer l-mount2" viewBox="0 0 256 256">
                <g clip-path="url(#frame-clip)" class="float-slow">
                    <!-- Left-leaning peak based on the lucide path -->
                    <path d="M -20 260 L 90 130 Q 100 115 110 130 L 220 260 Z" fill="url(#mount-front-grad)" />
                    <!-- Highlight edge -->
                    <path d="M -20 260 L 90 130 Q 100 115 110 130" fill="none" stroke="#6EE7B7" stroke-width="3" stroke-linecap="round" opacity="0.8" />
                </g>
            </svg>

            <!-- Layer 6: Front Floating Border Frame & Glass Shine -->
            <svg class="layer l-border pointer-events-none" viewBox="0 0 256 256">
                <!-- Thick floating border defining the original outline -->
                <rect x="30" y="30" width="196" height="196" rx="42" fill="none" stroke="#FFFFFF" stroke-width="10" opacity="0.95" />
                <!-- Subtle internal glass highlight -->
                <rect x="35" y="35" width="186" height="186" rx="37" fill="none" stroke="url(#glass-shine)" stroke-width="2" opacity="0.9" />
                <!-- Full face diagonal glass reflection -->
                <rect x="30" y="30" width="196" height="196" rx="42" fill="url(#glass-shine)" clip-path="url(#frame-clip)" />
            </svg>

        </div>
    </div>

</body>
</html>"""

    val ZAP_ICON_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>3D Zap Energy Icon</title>
    
    
    <style>
        html, body {
            margin: 0;
            padding: 0;
            background: transparent !important;
            background-color: transparent !important;
            width: 100%;
            height: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
            overflow: hidden;
            -webkit-user-select: none;
            user-select: none;
        }

        .pointer-events-none { pointer-events: none; }

        .scene {
            perspective: 1200px;
            width: 100%;
            height: 100%;
            max-width: 100%;
            max-height: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
        }
    
        

        /* 3D Scene setup */
        
        
        .icon-3d {
            width: 100%;
            height: 100%;
            position: relative;
            transform-style: preserve-3d;
            /* Continuous wobble to showcase 3D depth without hovering */
            animation: wobble 12s cubic-bezier(0.4, 0, 0.2, 1) infinite;
        }

        .layer {
            position: absolute;
            inset: 0;
            width: 100%;
            height: 100%;
            transform-style: preserve-3d;
        }

        /* Z-Axis Depth Translations for Parallax Effect */
        .l-shadow     { transform: translateZ(-40px); filter: blur(28px); opacity: 0.5; }
        .l-base       { transform: translateZ(0px); }
        .l-glow       { transform: translateZ(40px); }
        .l-bolt-back  { transform: translateZ(75px); }
        .l-bolt-front { transform: translateZ(115px); }
        .l-sparks     { transform: translateZ(135px); }
        .l-border     { transform: translateZ(150px); }

        /* Smooth continuous 3D rotation */
        @keyframes wobble {
            0%, 100% { transform: rotateX(14deg) rotateY(-20deg) rotateZ(3deg); }
            33% { transform: rotateX(-10deg) rotateY(-8deg) rotateZ(-2deg); }
            66% { transform: rotateX(-14deg) rotateY(18deg) rotateZ(-3deg); }
        }

        /* Ambient floating and energetic pulsing for inner elements */
        .float-fast {
            animation: float 4s ease-in-out infinite alternate;
        }
        .float-slow {
            animation: float 6s ease-in-out infinite alternate;
        }
        .pulse-energy {
            animation: pulseEnergy 2s ease-in-out infinite alternate;
        }
        .sparkle-blink {
            animation: blink 3s cubic-bezier(0.4, 0, 0.2, 1) infinite alternate;
        }

        @keyframes float {
            0% { transform: translateY(0px) scale(1); }
            100% { transform: translateY(-8px) scale(1.02); }
        }
        @keyframes pulseEnergy {
            0% { filter: drop-shadow(0 0 10px rgba(253,224,71,0.4)); opacity: 0.9; }
            100% { filter: drop-shadow(0 0 25px rgba(253,224,71,0.8)); opacity: 1; }
        }
        @keyframes blink {
            0%, 20% { opacity: 0.2; transform: scale(0.8); }
            50% { opacity: 1; transform: scale(1.2); }
            80%, 100% { opacity: 0.2; transform: scale(0.8); }
        }
    </style>
</head>
<body>

    <svg class="absolute w-0 h-0 overflow-hidden" aria-hidden="true">
        <defs>
            <!-- Background Frame Gradient (Deep Indigo / Space vibe for contrast) -->
            <linearGradient id="bg-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#2E1065" /> <!-- Deep Purple -->
                <stop offset="50%" stop-color="#1E1B4B" /> <!-- Dark Indigo -->
                <stop offset="100%" stop-color="#0F172A" /> <!-- Slate Black -->
            </linearGradient>

            <!-- Front Lightning Bolt Gradient (Electric Yellow) -->
            <linearGradient id="bolt-front-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#FEF08A" />
                <stop offset="30%" stop-color="#FDE047" />
                <stop offset="100%" stop-color="#F59E0B" />
            </linearGradient>

            <!-- Back Lightning Bolt Gradient (Cyan/Blue Chromatic Shift) -->
            <linearGradient id="bolt-back-grad" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#38BDF8" stop-opacity="0.9" />
                <stop offset="100%" stop-color="#0284C7" stop-opacity="0.9" />
            </linearGradient>

            <!-- Radial Background Glow behind the bolt -->
            <radialGradient id="aura-glow" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stop-color="#8B5CF6" stop-opacity="0.6" />
                <stop offset="100%" stop-color="#8B5CF6" stop-opacity="0" />
            </radialGradient>

            <!-- Glass Reflection Highlight for the frame -->
            <linearGradient id="glass-shine" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.7" />
                <stop offset="30%" stop-color="#FFFFFF" stop-opacity="0.1" />
                <stop offset="50%" stop-color="#FFFFFF" stop-opacity="0" />
                <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.2" />
            </linearGradient>

            <!-- Frame clipping boundary -->
            <clipPath id="frame-clip">
                <rect x="30" y="30" width="196" height="196" rx="42" />
            </clipPath>
        </defs>
    </svg>

    <div class="scene">
        <div class="icon-3d">
            
            <!-- Layer 1: Ambient Drop Shadow -->
            <svg class="layer l-shadow" viewBox="0 0 256 256">
                <rect x="34" y="44" width="188" height="188" rx="42" fill="#000000" />
            </svg>

            <!-- Layer 2: Base Dark Frame -->
            <svg class="layer l-base" viewBox="0 0 256 256">
                <rect x="30" y="30" width="196" height="196" rx="42" fill="url(#bg-grad)" />
                <!-- Inner glow/shadow for depth framing -->
                <rect x="30" y="30" width="196" height="196" rx="42" fill="none" stroke="#6D28D9" stroke-width="2" stroke-opacity="0.3" />
            </svg>

            <!-- Layer 3: Central Ambient Glow/Aura -->
            <svg class="layer l-glow" viewBox="0 0 256 256">
                <g clip-path="url(#frame-clip)">
                    <circle cx="128" cy="128" r="90" fill="url(#aura-glow)" filter="blur(16px)" class="pulse-energy" style="animation-delay: -1s;" />
                </g>
            </svg>

            <!-- Layer 4: Background Cyan Lightning (Chromatic Separation) -->
            <svg class="layer l-bolt-back pointer-events-none" viewBox="0 0 256 256">
                <g class="float-slow">
                    <!-- Translate and scale the 24x24 viewBox path to center it in 256x256 -->
                    <!-- Scale roughly 5.5x, offset to center -->
                    <g transform="translate(56, 56) scale(5.5)">
                        <path d="M15.914 4a1.5 1.5 0 00-2.474-1.561l-9 9A1.5 1.5 0 005.5 14h4.002a.5.5 0 01.471.666L8.086 20a1.5 1.5 0 002.475 1.56l9-9A1.5 1.5 0 0018.5 10h-3.997a.5.5 0 01-.472-.667z" 
                              fill="url(#bolt-back-grad)" />
                    </g>
                </g>
            </svg>

            <!-- Layer 5: Foreground Main Yellow Lightning -->
            <svg class="layer l-bolt-front pointer-events-none" viewBox="0 0 256 256">
                <g class="float-fast pulse-energy">
                    <!-- Shifted slightly upward and left compared to back bolt to exaggerate depth -->
                    <g transform="translate(62, 60) scale(5.5)">
                        <path d="M15.914 4a1.5 1.5 0 00-2.474-1.561l-9 9A1.5 1.5 0 005.5 14h4.002a.5.5 0 01.471.666L8.086 20a1.5 1.5 0 002.475 1.56l9-9A1.5 1.5 0 0018.5 10h-3.997a.5.5 0 01-.472-.667z" 
                              fill="url(#bolt-front-grad)" />
                        <!-- Highlighting edge to make it look sharp and glassy -->
                        <path d="M15.914 4a1.5 1.5 0 00-2.474-1.561l-9 9A1.5 1.5 0 005.5 14h4.002a.5.5 0 01.471.666L8.086 20a1.5 1.5 0 002.475 1.56l9-9A1.5 1.5 0 0018.5 10h-3.997a.5.5 0 01-.472-.667z" 
                              fill="none" stroke="#FFFFFF" stroke-width="0.3" stroke-opacity="0.8" />
                    </g>
                </g>
            </svg>

            <!-- Layer 6: Dynamic Floating Sparks -->
            <svg class="layer l-sparks pointer-events-none" viewBox="0 0 256 256">
                <g clip-path="url(#frame-clip)">
                    <g transform="translate(62, 60) scale(5.5)">
                        <circle cx="5" cy="8" r="0.6" fill="#FDE047" class="sparkle-blink" style="animation-delay: 0.1s;" />
                        <circle cx="19" cy="15" r="0.8" fill="#FDE047" class="sparkle-blink" style="animation-delay: 1.2s;" />
                        <circle cx="17" cy="5" r="0.4" fill="#38BDF8" class="sparkle-blink" style="animation-delay: 0.7s;" />
                        <circle cx="8" cy="19" r="0.7" fill="#38BDF8" class="sparkle-blink" style="animation-delay: 2.1s;" />
                        <circle cx="5" cy="8" r="0.4" fill="#FDE047" class="sparkle-blink" style="animation-delay: 0.1s;" opacity="0.6" />
                    </g>
                </g>
            </svg>

            <!-- Layer 7: Front Floating Border Frame & Glass Shine -->
            <svg class="layer l-border pointer-events-none" viewBox="0 0 256 256">
                <!-- Floating border defining the original outline shape -->
                <rect x="30" y="30" width="196" height="196" rx="42" fill="none" stroke="#8B5CF6" stroke-width="6" opacity="0.4" />
                <rect x="30" y="30" width="196" height="196" rx="42" fill="none" stroke="#FFFFFF" stroke-width="2" opacity="0.6" />
                
                <!-- Subtle internal glass highlight -->
                <rect x="35" y="35" width="186" height="186" rx="37" fill="none" stroke="url(#glass-shine)" stroke-width="2" opacity="0.9" />
                
                <!-- Full face diagonal glass reflection acting as screen/casing -->
                <rect x="30" y="30" width="196" height="196" rx="42" fill="url(#glass-shine)" clip-path="url(#frame-clip)" />
            </svg>

        </div>
    </div>

</body>
</html>"""

    val HEART_HANDSHAKE_ICON_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>3D Heart Handshake Icon</title>
    
    
    <style>
        html, body {
            margin: 0;
            padding: 0;
            background: transparent !important;
            background-color: transparent !important;
            width: 100%;
            height: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
            overflow: hidden;
            -webkit-user-select: none;
            user-select: none;
        }

        .pointer-events-none { pointer-events: none; }

        .scene {
            perspective: 1200px;
            width: 100%;
            height: 100%;
            max-width: 100%;
            max-height: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
        }
    
        

        
        
        .icon-3d {
            width: 100%;
            height: 100%;
            position: relative;
            transform-style: preserve-3d;
            /* Continuous 3D wobble animation without requiring hover */
            animation: wobble 12s cubic-bezier(0.4, 0, 0.2, 1) infinite;
        }

        .layer {
            position: absolute;
            inset: 0;
            width: 100%;
            height: 100%;
            transform-style: preserve-3d;
        }

        .l-shadow     { transform: translateZ(-40px); filter: blur(28px); opacity: 0.5; }
        .l-base       { transform: translateZ(0px); }
        .l-glow       { transform: translateZ(35px); }
        .l-icon-back  { transform: translateZ(75px); }
        .l-icon-front { transform: translateZ(115px); }
        .l-border     { transform: translateZ(140px); }

        @keyframes wobble {
            0%, 100% { transform: rotateX(12deg) rotateY(-22deg) rotateZ(3deg); }
            33% { transform: rotateX(-12deg) rotateY(-10deg) rotateZ(-2deg); }
            66% { transform: rotateX(-8deg) rotateY(18deg) rotateZ(-4deg); }
        }

        /* Ambient floating and pulsing for inner elements */
        .float-subtle {
            animation: float 4s ease-in-out infinite alternate;
        }
        .pulse-energy {
            animation: pulseEnergy 2s ease-in-out infinite alternate;
        }

        @keyframes float {
            0% { transform: translateY(0px) scale(1); }
            100% { transform: translateY(-6px) scale(1.02); }
        }
        
        @keyframes pulseEnergy {
            0% { filter: drop-shadow(0 0 12px rgba(244, 63, 94, 0.4)); opacity: 0.9; }
            100% { filter: drop-shadow(0 0 25px rgba(244, 63, 94, 0.8)); opacity: 1; }
        }
    </style>
</head>
<body>

    <svg class="absolute w-0 h-0 overflow-hidden" aria-hidden="true">
        <defs>
            <!-- Background Frame Gradient (Deep Rose/Burgundy to Violet) -->
            <linearGradient id="bg-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#831843" /> <!-- Pink 900 -->
                <stop offset="50%" stop-color="#4C0519" /> <!-- Rose 950 -->
                <stop offset="100%" stop-color="#2E1065" /> <!-- Violet 900 -->
            </linearGradient>

            <!-- Front Icon Gradient (Vibrant Pink to Golden Yellow for trust/warmth) -->
            <linearGradient id="icon-front-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#FDA4AF" /> <!-- Rose 300 -->
                <stop offset="40%" stop-color="#F43F5E" /> <!-- Rose 500 -->
                <stop offset="100%" stop-color="#FBBF24" /> <!-- Amber 400 -->
            </linearGradient>

            <!-- Back Icon Offset Gradient (Deep Magenta shadow) -->
            <linearGradient id="icon-back-grad" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#E11D48" stop-opacity="0.9" /> <!-- Rose 600 -->
                <stop offset="100%" stop-color="#9F1239" stop-opacity="0.7" /> <!-- Rose 800 -->
            </linearGradient>

            <!-- Radial Background Glow -->
            <radialGradient id="aura-glow" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stop-color="#F43F5E" stop-opacity="0.5" />
                <stop offset="100%" stop-color="#F43F5E" stop-opacity="0" />
            </radialGradient>

            <!-- Glass Reflection Highlight -->
            <linearGradient id="glass-shine" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.6" />
                <stop offset="30%" stop-color="#FFFFFF" stop-opacity="0.05" />
                <stop offset="50%" stop-color="#FFFFFF" stop-opacity="0" />
                <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.15" />
            </linearGradient>

            <!-- Frame clipping boundary -->
            <clipPath id="frame-clip">
                <rect x="30" y="30" width="196" height="196" rx="42" />
            </clipPath>
        </defs>
    </svg>

    <div class="scene">
        <div class="icon-3d">
            
            <!-- Layer 1: Ambient Drop Shadow -->
            <svg class="layer l-shadow" viewBox="0 0 256 256">
                <rect x="34" y="44" width="188" height="188" rx="42" fill="#000000" />
            </svg>

            <!-- Layer 2: Base Dark Frame -->
            <svg class="layer l-base" viewBox="0 0 256 256">
                <rect x="30" y="30" width="196" height="196" rx="42" fill="url(#bg-grad)" />
                <!-- Inner glow/shadow for depth framing -->
                <rect x="30" y="30" width="196" height="196" rx="42" fill="none" stroke="#BE185D" stroke-width="2" stroke-opacity="0.4" />
            </svg>

            <!-- Layer 3: Central Ambient Glow/Aura -->
            <svg class="layer l-glow" viewBox="0 0 256 256">
                <g clip-path="url(#frame-clip)">
                    <circle cx="128" cy="128" r="95" fill="url(#aura-glow)" filter="blur(16px)" class="pulse-energy" style="animation-delay: -1s;" />
                </g>
            </svg>

            <!-- Layer 4: Background Depth Path -->
            <svg class="layer l-icon-back pointer-events-none" viewBox="0 0 256 256">
                <g class="float-subtle">
                    <!-- Translate and scale to center (24x24 up to 110x110 approx) -->
                    <!-- scale(5): 24 * 5 = 120. Center is 256/2 = 128. Offset = 128 - (12*5) = 68 -->
                    <g transform="translate(68, 68) scale(5)">
                        <path d="M19.414 14.414C21 12.828 22 11.5 22 9.5a5.5 5.5 0 0 0-9.591-3.676.6.6 0 0 1-.818.001A5.5 5.5 0 0 0 2 9.5c0 2.3 1.5 4 3 5.5l5.535 5.362a2 2 0 0 0 2.879.052 2.12 2.12 0 0 0-.004-3 2.124 2.124 0 1 0 3-3 2.124 2.124 0 0 0 3.004 0 2 2 0 0 0 0-2.828l-1.881-1.882a2.41 2.41 0 0 0-3.409 0l-1.71 1.71a2 2 0 0 1-2.828 0 2 2 0 0 1 0-2.828l2.823-2.762" 
                              fill="none" stroke="url(#icon-back-grad)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
                    </g>
                </g>
            </svg>

            <!-- Layer 5: Foreground Main Vibrant Path -->
            <svg class="layer l-icon-front pointer-events-none" viewBox="0 0 256 256">
                <g class="float-subtle pulse-energy">
                    <!-- Shifted slightly up and left to exaggerate holographic depth -->
                    <g transform="translate(66, 66) scale(5)">
                        <path d="M19.414 14.414C21 12.828 22 11.5 22 9.5a5.5 5.5 0 0 0-9.591-3.676.6.6 0 0 1-.818.001A5.5 5.5 0 0 0 2 9.5c0 2.3 1.5 4 3 5.5l5.535 5.362a2 2 0 0 0 2.879.052 2.12 2.12 0 0 0-.004-3 2.124 2.124 0 1 0 3-3 2.124 2.124 0 0 0 3.004 0 2 2 0 0 0 0-2.828l-1.881-1.882a2.41 2.41 0 0 0-3.409 0l-1.71 1.71a2 2 0 0 1-2.828 0 2 2 0 0 1 0-2.828l2.823-2.762" 
                              fill="none" stroke="url(#icon-front-grad)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                        
                        <!-- Extra white specular highlight for a glowing glass tube effect -->
                        <path d="M19.414 14.414C21 12.828 22 11.5 22 9.5a5.5 5.5 0 0 0-9.591-3.676.6.6 0 0 1-.818.001A5.5 5.5 0 0 0 2 9.5c0 2.3 1.5 4 3 5.5l5.535 5.362a2 2 0 0 0 2.879.052 2.12 2.12 0 0 0-.004-3 2.124 2.124 0 1 0 3-3 2.124 2.124 0 0 0 3.004 0 2 2 0 0 0 0-2.828l-1.881-1.882a2.41 2.41 0 0 0-3.409 0l-1.71 1.71a2 2 0 0 1-2.828 0 2 2 0 0 1 0-2.828l2.823-2.762" 
                              fill="none" stroke="#FFFFFF" stroke-width="0.4" stroke-opacity="0.8" stroke-linecap="round" stroke-linejoin="round" />
                    </g>
                </g>
            </svg>

            <!-- Layer 6: Front Floating Border Frame & Glass Shine -->
            <svg class="layer l-border pointer-events-none" viewBox="0 0 256 256">
                <!-- Floating border defining the original outline shape -->
                <rect x="30" y="30" width="196" height="196" rx="42" fill="none" stroke="#F43F5E" stroke-width="4" opacity="0.3" />
                <rect x="30" y="30" width="196" height="196" rx="42" fill="none" stroke="#FFFFFF" stroke-width="1.5" opacity="0.5" />
                
                <!-- Subtle internal glass highlight -->
                <rect x="35" y="35" width="186" height="186" rx="37" fill="none" stroke="url(#glass-shine)" stroke-width="2" opacity="0.9" />
                
                <!-- Full face diagonal glass reflection acting as screen/casing -->
                <rect x="30" y="30" width="196" height="196" rx="42" fill="url(#glass-shine)" clip-path="url(#frame-clip)" />
            </svg>

        </div>
    </div>

</body>
</html>"""
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Html3DIcon(
    html: String,
    modifier: Modifier = Modifier.size(130.dp)
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(0) // Transparent
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setOnTouchListener { _, _ -> true }
                loadDataWithBaseURL("https://localhost", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost", html, "text/html", "UTF-8", null)
        }
    )
}
