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

}
