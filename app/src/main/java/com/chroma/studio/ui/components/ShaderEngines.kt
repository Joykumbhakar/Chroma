package com.chroma.studio.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

object ShaderEngines {

    const val POST_FX_SHADER = """
        uniform float2 resolution;
        uniform float time;
        uniform int mode; // 1 = Grain, 2 = Halftone, 3 = Dither
        uniform shader content;
        
        float random(float2 p) {
            return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 fragCoord) {
            half4 c = content.eval(fragCoord);
            float2 uv = fragCoord / resolution;
            
            if (mode == 1) {
                // Grain
                float noise = random(fragCoord + time) * 2.0 - 1.0;
                c.rgb += noise * 0.08;
            } else if (mode == 2) {
                // Halftone
                float angle = 0.785398; // 45 degrees
                float scale = 12.0;
                float s = sin(angle);
                float c_cos = cos(angle);
                float2x2 rot = float2x2(c_cos, -s, s, c_cos);
                float2 p = (fragCoord - resolution * 0.5) * rot;
                float pattern = sin(p.x / scale) * sin(p.y / scale);
                float lum = dot(c.rgb, float3(0.299, 0.587, 0.114));
                float threshold = pattern * 0.5 + 0.5;
                if (lum < threshold) {
                    c.rgb = float3(0.1);
                } else {
                    c.rgb = float3(0.95);
                }
            } else if (mode == 3) {
                // Dither
                float r = random(fragCoord + time);
                c.rgb = step(r, c.rgb);
            }
            return c;
        }
    """

    const val MESH_SHADER = """
        uniform float2 resolution;
        uniform int count;
        // AGSL supports up to 16-element arrays for colors/positions
        uniform half4 colors[16];
        uniform float2 positions[16];

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float4 sumColor = float4(0.0);
            float sumWeight = 0.0;
            
            for (int i = 0; i < 16; ++i) {
                if (i >= count) break;
                float d = distance(uv, positions[i]);
                // Inverse distance weighting, power of 3 looks smooth and organic
                float w = 1.0 / (pow(d, 3.0) + 0.001); 
                sumColor += float4(colors[i]) * w;
                sumWeight += w;
            }
            return half4(sumColor / sumWeight);
        }
    """

    const val AURORA_SHADER = """
        uniform float2 resolution;
        uniform float time;
        uniform float speed;
        uniform float complexity;
        uniform half4 color1;
        uniform half4 color2;
        uniform half4 color3;
        uniform half4 color4;

        // Smooth sweeping aurora beams
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            
            // Adjust time and speed
            float t = time * speed * 0.05;
            
            // Generate multiple sine waves with different frequencies and phases
            float wave1 = sin(uv.x * 3.0 + t * 1.5 + sin(uv.y * 2.0));
            float wave2 = sin(uv.x * 4.0 - t * 1.2 + cos(uv.y * 3.0 + t));
            float wave3 = sin(uv.x * 2.5 + t * 2.0 + sin(uv.y * 1.5 - t));
            float wave4 = sin(uv.x * 5.0 - t * 0.8 + cos(uv.y * 2.5 + t * 1.1));
            
            // Map waves to [0, 1]
            wave1 = wave1 * 0.5 + 0.5;
            wave2 = wave2 * 0.5 + 0.5;
            wave3 = wave3 * 0.5 + 0.5;
            wave4 = wave4 * 0.5 + 0.5;
            
            // Combine waves into smooth fluid flow
            float flow = (wave1 + wave2 + wave3 + wave4) / 4.0;
            
            // Apply complexity scaling
            flow = pow(flow, 2.0 - complexity * 0.2);
            
            // Interpolate colors based on flow and Y position
            float3 col = mix(float3(color1.rgb), float3(color2.rgb), smoothstep(0.0, 0.4, flow));
            col = mix(col, float3(color3.rgb), smoothstep(0.3, 0.7, flow));
            col = mix(col, float3(color4.rgb), smoothstep(0.6, 1.0, flow));
            
            // Add a vertical fade so it looks like it's sweeping up
            float verticalFade = smoothstep(0.0, 1.0, 1.0 - uv.y);
            col *= (0.5 + 0.5 * verticalFade);
            
            // Enhance contrast and glow
            col = col * col * 1.5 + col * 0.5;
            
            return half4(clamp(col, 0.0, 1.0), 1.0);
        }
    """
}
