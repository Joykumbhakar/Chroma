package com.chroma.studio.utils

import com.chroma.studio.model.GradientLayer
import com.chroma.studio.model.LayerType
import com.chroma.studio.model.AnimStyle
import kotlin.math.roundToInt

object ExportEngine {

    private fun rgb2hex(r: Float, g: Float, b: Float): String {
        val ir = (r * 255).roundToInt().coerceIn(0, 255)
        val ig = (g * 255).roundToInt().coerceIn(0, 255)
        val ib = (b * 255).roundToInt().coerceIn(0, 255)
        return String.format("%02x%02x%02x", ir, ig, ib).uppercase()
    }

    private fun getCSS(layer: GradientLayer): String {
        val stops = layer.stops.sortedBy { it.position }
        val stopString = stops.joinToString(", ") { s ->
            "rgba(${(s.color.red * 255).toInt()},${(s.color.green * 255).toInt()},${(s.color.blue * 255).toInt()},${s.color.alpha}) ${s.position}%"
        }
        return when (layer.type) {
            LayerType.LINEAR -> "linear-gradient(${layer.angle}deg, $stopString)"
            LayerType.RADIAL -> "radial-gradient(circle at ${layer.centerX}% ${layer.centerY}%, $stopString)"
            LayerType.CONIC -> "conic-gradient(from ${layer.angle}deg at ${layer.centerX}% ${layer.centerY}%, $stopString)"
            LayerType.BLOB -> {
                val bStops = layer.stops
                val bg = if (layer.hasBaseBackground) {
                    val bc = layer.blobBgColor
                    "rgba(${(bc.red*255).toInt()},${(bc.green*255).toInt()},${(bc.blue*255).toInt()},${bc.alpha}), "
                } else ""
                val bgs = layer.blobs.mapIndexedNotNull { i, blob ->
                    val s = bStops.getOrNull(i) ?: bStops.firstOrNull() ?: return@mapIndexedNotNull null
                    val c = s.color.copy(alpha = s.color.alpha * blob.opacity)
                    val r = (c.red * 255).toInt()
                    val g = (c.green * 255).toInt()
                    val b = (c.blue * 255).toInt()
                    val feather = (100f - blob.feather).coerceIn(0f, 99f)
                    "radial-gradient(${blob.width}% ${blob.height}% at ${blob.x}% ${blob.y}%, rgba($r,$g,$b,${c.alpha}) 0%, rgba($r,$g,$b,${c.alpha}) $feather%, rgba($r,$g,$b,0) 100%)"
                }.joinToString(", ")
                if (bg.isNotEmpty()) "$bgs, linear-gradient(${bg}transparent)" else bgs
            }
            LayerType.LIQUID -> "linear-gradient(180deg, rgba(0,0,0,0), rgba(0,0,0,0)) /* Liquid uses Blob internals */"
            LayerType.AURORA -> "linear-gradient(180deg, rgba(0,0,0,0), rgba(0,0,0,0)) /* Aurora needs Canvas JS */"
            LayerType.MESH -> "linear-gradient(180deg, rgba(0,0,0,0), rgba(0,0,0,0)) /* Mesh not supported in raw CSS */"
        }
    }

    private data class ExportStrings(
        val bg: String,
        val cssBlock: String,
        val reactColors: String,
        val swiftUIColors: String,
        val rnColors: String,
        val composeColors: String,
        val flutterColors: String,
        val xmlColors: String,
        val kotlinColors: String,
        val javaColors: String
    )

    private fun getExportStrings(l: GradientLayer, i: Int, hasAnim: Boolean, aStyle: AnimStyle, aSpeed: Float, intensity: Int): ExportStrings {
        val bg = getCSS(l)
        val blendMap = l.blendMode.name.lowercase().replace("_", "-")
        var cssBlock = "  background: $bg;\n  mix-blend-mode: $blendMap;\n  opacity: ${l.opacity};\n"
        
        if (hasAnim && l.type != LayerType.BLOB && l.type != LayerType.LIQUID && l.type != LayerType.MESH && l.type != LayerType.AURORA) {
            val spd = aSpeed + i * 2
            when (aStyle) {
                AnimStyle.DRIFT -> cssBlock += "  background-size: ${120 + intensity}% ${120 + intensity}%;\n  animation: anim-drift ${spd}s ease infinite alternate;\n"
                AnimStyle.PULSE -> cssBlock += "  animation: anim-pulse ${spd}s ease-in-out infinite alternate;\n"
                AnimStyle.HUE -> cssBlock += "  animation: anim-hue ${spd}s linear infinite alternate;\n"
                AnimStyle.BREATHE -> cssBlock += "  background-size: 100% 100%; background-position: center;\n  animation: anim-breathe ${spd}s ease-in-out infinite alternate;\n"
            }
        }

        val stops = l.stops.sortedBy { it.position }
        val reactColors = stops.joinToString(", ") { s -> "'rgba(${(s.color.red * 255).toInt()},${(s.color.green * 255).toInt()},${(s.color.blue * 255).toInt()},${s.color.alpha})'" }
        val swiftUIColors = stops.joinToString(", ") { s -> "Color(red: ${s.color.red}, green: ${s.color.green}, blue: ${s.color.blue}, opacity: ${s.color.alpha})" }
        val composeColors = stops.joinToString(", ") { s -> 
            val alphaHex = (s.color.alpha * 255).roundToInt().toString(16).padStart(2, '0').uppercase()
            "Color(0x$alphaHex${rgb2hex(s.color.red, s.color.green, s.color.blue)})" 
        }
        val flutterColors = composeColors

        var xmlColors = if (stops.size >= 2) {
            val first = stops.first()
            val last = stops.last()
            val fAlpha = (first.color.alpha * 255).roundToInt().toString(16).padStart(2, '0').uppercase()
            val lAlpha = (last.color.alpha * 255).roundToInt().toString(16).padStart(2, '0').uppercase()
            "android:startColor=\"#$fAlpha${rgb2hex(first.color.red, first.color.green, first.color.blue)}\"\n        android:endColor=\"#$lAlpha${rgb2hex(last.color.red, last.color.green, last.color.blue)}\""
        } else ""

        if (stops.size > 2) {
            val mid = stops[stops.size / 2]
            val mAlpha = (mid.color.alpha * 255).roundToInt().toString(16).padStart(2, '0').uppercase()
            xmlColors += "\n        android:centerColor=\"#$mAlpha${rgb2hex(mid.color.red, mid.color.green, mid.color.blue)}\""
        }

        val kotlinColors = "intArrayOf(${stops.joinToString(", ") { s -> 
            val a = (s.color.alpha * 255).roundToInt().toString(16).padStart(2, '0').uppercase()
            "Color.parseColor(\"#$a${rgb2hex(s.color.red, s.color.green, s.color.blue)}\")" 
        }})"
        val javaColors = "new int[]{${stops.joinToString(", ") { s -> 
            val a = (s.color.alpha * 255).roundToInt().toString(16).padStart(2, '0').uppercase()
            "Color.parseColor(\"#$a${rgb2hex(s.color.red, s.color.green, s.color.blue)}\")" 
        }}}"

        return ExportStrings(bg, cssBlock, reactColors, swiftUIColors, reactColors, composeColors, flutterColors, xmlColors, kotlinColors, javaColors)
    }

    fun generateCode(
        type: String, 
        layersRaw: List<GradientLayer>, 
        isDarkTheme: Boolean = false, 
        hasAnim: Boolean = false, 
        aStyle: AnimStyle = AnimStyle.DRIFT, 
        aSpeed: Float = 50f, 
        intensity: Int = 50,
        shape: String = "rounded",
        textContent: String = "CHROMA"
    ): String {
        var code = ""
        val layers = layersRaw.reversed().filter { it.visible }
        val bgColor = if (isDarkTheme) "#0d1117" else "#f8fafc"
        val hasLiquid = layers.any { it.type == LayerType.LIQUID }

        when (type) {
            "CSS / SCSS" -> {
                val shapeCss = when(shape) {
                    "circle" -> "  border-radius: 50%; overflow: hidden;\n"
                    "rounded" -> "  border-radius: 24px; overflow: hidden;\n"
                    "text" -> "  background-clip: text; -webkit-background-clip: text; color: transparent; font-size: 15vw; font-weight: 900; text-align: center; line-height: 100vh;\n"
                    else -> ""
                }
                code = ".chroma-bg {\n  width: 100%; height: 100vh;\n  position: relative;\n  background-color: $bgColor;\n$shapeCss}\n"
                if (shape == "text") {
                    code += "/* HTML Usage: <div class=\"chroma-bg\">$textContent</div> */\n\n"
                }
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    val liquidStr = if (l.type == LayerType.LIQUID) "  filter: url('#liquid-filter');\n" else ""
                    code += ".chroma-bg::before /* Layer: ${l.name} */ {\n  content: \"\"; position: absolute; inset: 0;\n$liquidStr${exp.cssBlock}  z-index: -1;\n}\n"
                }
                if (hasAnim) {
                    code += "\n/* Animations */\n@keyframes anim-drift { 0% { background-position: 0% 50%; } 100% { background-position: 100% 50%; } }\n"
                    code += "@keyframes anim-pulse { 0% { transform: scale(1); opacity: 1; } 100% { transform: scale(${1 + intensity / 100f}); opacity: ${(1 - intensity / 100f).coerceAtLeast(0.1f)}; } }\n"
                    code += "@keyframes anim-hue { 0% { filter: hue-rotate(0deg); } 100% { filter: hue-rotate(${intensity * 3.6}deg); } }\n"
                    code += "@keyframes anim-breathe { 0% { background-size: 100% 100%; } 100% { background-size: ${100 + intensity}% ${100 + intensity}%; } }\n"
                }
                if (hasLiquid) {
                    code += "\n/* Note: Liquid filter requires SVG definition in HTML */\n"
                }
            }
            "Tailwind" -> {
                val bgs = layers.mapIndexed { i, l -> "\"chroma-\$i\": \"${getExportStrings(l, i, false, aStyle, aSpeed, intensity).bg.replace("\n", " ")}\"" }.joinToString(",\n      ")
                code = "// tailwind.config.js\nmodule.exports = {\n  theme: {\n    extend: {\n      backgroundImage: {\n      $bgs\n      }\n    }\n  }\n}"
                val shapeClasses = when(shape) {
                    "circle" -> " rounded-full overflow-hidden"
                    "rounded" -> " rounded-3xl overflow-hidden"
                    "text" -> " bg-clip-text text-transparent text-[15vw] font-black text-center leading-[100vh]"
                    else -> ""
                }
                code = "// tailwind.config.js\nmodule.exports = {\n  theme: {\n    extend: {\n      backgroundImage: {\n      $bgs\n      }\n    }\n  }\n}"
                code += "\n\n// HTML Usage:\n<div class=\"relative w-full h-screen ${if (isDarkTheme) "bg-gray-950" else "bg-slate-50"}$shapeClasses\">\n"
                if (shape == "text") {
                    code += "  $textContent\n"
                }
                layers.forEachIndexed { i, l ->
                    val liquidClass = if (l.type == LayerType.LIQUID) "style=\"filter: url('#liquid-filter');\" " else ""
                    code += "  <div class=\"absolute inset-0 bg-chroma-$i mix-blend-${l.blendMode.name.lowercase()} opacity-[${l.opacity}] -z-10\" $liquidClass></div>\n"
                }
                code += "</div>"
            }
            "React / Next.js" -> {
                val shapeStyle = when(shape) {
                    "circle" -> "borderRadius: '50%', overflow: 'hidden', "
                    "rounded" -> "borderRadius: '24px', overflow: 'hidden', "
                    "text" -> "WebkitBackgroundClip: 'text', backgroundClip: 'text', color: 'transparent', fontSize: '15vw', fontWeight: 900, textAlign: 'center', lineHeight: '100vh', "
                    else -> ""
                }
                code = "import React from 'react';\n\nexport const ChromaBackground = () => {\n  return (\n    <div style={{ position: 'relative', width: '100%', height: '100vh', ${shapeStyle}backgroundColor: '$bgColor' }}>\n"
                if (shape == "text") {
                    code += "      $textContent\n"
                }
                if (hasLiquid) {
                    code += "      <svg width=\"0\" height=\"0\" style={{position:'absolute'}}>\n        <filter id=\"liquid-filter\">\n          <feGaussianBlur in=\"SourceGraphic\" stdDeviation=\"10\" result=\"blur\" />\n          <feColorMatrix in=\"blur\" mode=\"matrix\" values=\"1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 18 -7\" result=\"liquid\" />\n          <feBlend in=\"SourceGraphic\" in2=\"liquid\" />\n        </filter>\n      </svg>\n"
                }
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    val liquidStyle = if (l.type == LayerType.LIQUID) "filter: 'url(#liquid-filter)', " else ""
                    code += "      <div style={{ position: 'absolute', inset: 0, mixBlendMode: '${l.blendMode.name.lowercase()}', opacity: ${l.opacity}, ${liquidStyle}background: '${exp.bg.replace("\n", " ")}', zIndex: -1 }} />\n"
                }
                code += "    </div>\n  );\n};"
            }
            "Vue" -> {
                code = "<template>\n  <div class=\"chroma-wrap\">\n"
                if (hasLiquid) code += "    <svg width=\"0\" height=\"0\" style=\"position:absolute;\"><filter id=\"liquid-filter\"><feGaussianBlur in=\"SourceGraphic\" stdDeviation=\"10\" result=\"blur\" /><feColorMatrix in=\"blur\" mode=\"matrix\" values=\"1 0 0 0 0 0 1 0 0 0 0 0 1 0 0 0 0 0 18 -7\" result=\"liquid\" /><feBlend in=\"SourceGraphic\" in2=\"liquid\" /></filter></svg>\n"
                layers.forEachIndexed { i, _ -> code += "    <div class=\"chroma-layer layer-$i\"></div>\n" }
                code += "  </div>\n</template>\n\n<style scoped>\n.chroma-wrap { position: relative; width: 100%; height: 100vh; background-color: $bgColor; }\n.chroma-layer { position: absolute; inset: 0; }\n"
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    val liq = if (l.type == LayerType.LIQUID) "filter: url(\"#liquid-filter\"); " else ""
                    code += ".layer-$i { ${liq}background: ${exp.bg.replace("\n", " ")}; mix-blend-mode: ${l.blendMode.name.lowercase()}; opacity: ${l.opacity}; }\n"
                }
                code += "</style>"
            }
            "Svelte" -> {
                code = "<div class=\"chroma-wrap\">\n"
                if (hasLiquid) code += "  <svg width=\"0\" height=\"0\" style=\"position:absolute;\"><filter id=\"liquid-filter\"><feGaussianBlur in=\"SourceGraphic\" stdDeviation=\"10\" result=\"blur\" /><feColorMatrix in=\"blur\" mode=\"matrix\" values=\"1 0 0 0 0 0 1 0 0 0 0 0 1 0 0 0 0 0 18 -7\" result=\"liquid\" /><feBlend in=\"SourceGraphic\" in2=\"liquid\" /></filter></svg>\n"
                layers.forEachIndexed { i, _ -> code += "  <div class=\"chroma-layer layer-$i\"></div>\n" }
                code += "</div>\n\n<style>\n  .chroma-wrap { position: relative; width: 100%; height: 100vh; background-color: $bgColor; }\n  .chroma-layer { position: absolute; inset: 0; }\n"
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    val liq = if (l.type == LayerType.LIQUID) "filter: url(\"#liquid-filter\"); " else ""
                    code += "  .layer-$i { ${liq}background: ${exp.bg.replace("\n", " ")}; mix-blend-mode: ${l.blendMode.name.lowercase()}; opacity: ${l.opacity}; }\n"
                }
                code += "</style>"
            }
            "Angular" -> {
                code = "<!-- component.html -->\n<div class=\"chroma-wrap\">\n"
                if (hasLiquid) code += "  <svg width=\"0\" height=\"0\" style=\"position:absolute;\"><filter id=\"liquid-filter\"><feGaussianBlur in=\"SourceGraphic\" stdDeviation=\"10\" result=\"blur\" /><feColorMatrix in=\"blur\" mode=\"matrix\" values=\"1 0 0 0 0 0 1 0 0 0 0 0 1 0 0 0 0 0 18 -7\" result=\"liquid\" /><feBlend in=\"SourceGraphic\" in2=\"liquid\" /></filter></svg>\n"
                layers.forEachIndexed { i, _ -> code += "  <div class=\"chroma-layer layer-$i\"></div>\n" }
                code += "</div>\n\n/* component.css */\n.chroma-wrap { position: relative; width: 100%; height: 100vh; background-color: $bgColor; }\n.chroma-layer { position: absolute; inset: 0; }\n"
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    val liq = if (l.type == LayerType.LIQUID) "filter: url(\"#liquid-filter\"); " else ""
                    code += ".layer-$i { ${liq}background: ${exp.bg.replace("\n", " ")}; mix-blend-mode: ${l.blendMode.name.lowercase()}; opacity: ${l.opacity}; }\n"
                }
            }
            "SwiftUI" -> {
                val shapeModifier = when(shape) {
                    "circle" -> ".clipShape(Circle())"
                    "rounded" -> ".clipShape(RoundedRectangle(cornerRadius: 24))"
                    "text" -> ".mask(Text(\"$textContent\").font(.system(size: 100, weight: .black)).multilineTextAlignment(.center))"
                    else -> ""
                }
                code = "import SwiftUI\n\nstruct ChromaBackground: View {\n    var body: some View {\n        ZStack {\n            Color(red: 0.97, green: 0.98, blue: 0.99).ignoresSafeArea()\n\n"
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    var gradientType = when (l.type) {
                        LayerType.RADIAL -> "RadialGradient(gradient: Gradient(colors: [${exp.swiftUIColors}]), center: .center, startRadius: 0, endRadius: 300)"
                        LayerType.CONIC -> "AngularGradient(gradient: Gradient(colors: [${exp.swiftUIColors}]), center: .center, angle: .degrees(${l.angle}))"
                        else -> "LinearGradient(gradient: Gradient(colors: [${exp.swiftUIColors}]), startPoint: .topLeading, endPoint: .bottomTrailing)"
                    }
                    if (l.type == LayerType.BLOB || l.type == LayerType.LIQUID || l.type == LayerType.AURORA || l.type == LayerType.MESH) {
                        gradientType = "// Complex gradient requires custom drawing Canvas\n            Color.clear"
                    }
                    code += "            $gradientType\n                .blendMode(.${l.blendMode.name.lowercase().replace("_", "")})\n                .opacity(${l.opacity})\n                .ignoresSafeArea()\n\n"
                }
                code += "        }\n        $shapeModifier\n    }\n}"
            }
            "Jetpack Compose" -> {
                val clipModifier = when(shape) {
                    "circle" -> ".clip(CircleShape)"
                    "rounded" -> ".clip(RoundedCornerShape(24.dp))"
                    else -> ""
                }
                code = "import androidx.compose.foundation.background\nimport androidx.compose.foundation.layout.*\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.graphics.*\nimport androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.foundation.shape.*\nimport androidx.compose.ui.draw.clip\nimport androidx.compose.runtime.Composable\n\n@Composable\nfun ChromaBackground() {\n"
                if (shape == "text") {
                    code += "    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)), contentAlignment = androidx.compose.ui.Alignment.Center) {\n"
                    code += "        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {\n"
                    code += "            // Draw Text with BlendMode.DstIn to mask the gradient!\n"
                    code += "            // (Implement standard Compose drawText mask logic here)\n"
                    code += "        }\n"
                } else {
                    code += "    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))$clipModifier) {\n"
                }
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    var brush = when (l.type) {
                        LayerType.RADIAL -> "Brush.radialGradient(colors = listOf(${exp.composeColors}))"
                        LayerType.CONIC -> "Brush.sweepGradient(colors = listOf(${exp.composeColors}))"
                        else -> "Brush.linearGradient(colors = listOf(${exp.composeColors}))"
                    }
                    if (l.type == LayerType.BLOB || l.type == LayerType.LIQUID || l.type == LayerType.AURORA || l.type == LayerType.MESH) {
                        brush = "Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent)) /* Complex gradients require Canvas draw phase */"
                    }
                    code += "        Box(\n            modifier = Modifier.fillMaxSize().background($brush, alpha = ${l.opacity}f)\n        )\n"
                }
                code += "    }\n}"
            }
            "Flutter" -> {
                val flutterBg = if (bgColor == "#0d1117") "0xFF0d1117" else "0xFFF8FAFC"
                val clipStart = when(shape) {
                    "circle" -> "ClipOval(\n      child: "
                    "rounded" -> "ClipRRect(\n      borderRadius: BorderRadius.circular(24.0),\n      child: "
                    "text" -> "ShaderMask(\n      blendMode: BlendMode.srcIn,\n      shaderCallback: (bounds) => const LinearGradient(colors: [Colors.white, Colors.white]).createShader(bounds),\n      // The actual masking requires complex setup in Flutter, here's a rough approximation:\n      child: "
                    else -> ""
                }
                val clipEnd = when(shape) {
                    "circle", "rounded", "text" -> ",\n    )"
                    else -> ""
                }
                code = "import 'package:flutter/material.dart';\n\nclass ChromaBackground extends StatelessWidget {\n  @override\n  Widget build(BuildContext context) {\n    return $clipStart Stack(\n      children: [\n        Container(color: Color($flutterBg)),\n"
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    var grad = when (l.type) {
                        LayerType.RADIAL -> "RadialGradient(colors: [${exp.flutterColors}], center: Alignment.center, radius: 1.0)"
                        LayerType.CONIC -> "SweepGradient(colors: [${exp.flutterColors}], center: Alignment.center)"
                        else -> "LinearGradient(colors: [${exp.flutterColors}], begin: Alignment.topLeft, end: Alignment.bottomRight)"
                    }
                    if (l.type == LayerType.BLOB || l.type == LayerType.LIQUID || l.type == LayerType.AURORA || l.type == LayerType.MESH) {
                        grad = "LinearGradient(colors: [Color(0x00000000), Color(0x00000000)]) /* Complex gradients require CustomPaint Canvas API */"
                    }
                    code += "        Opacity(\n          opacity: ${l.opacity},\n          child: Container(\n            decoration: BoxDecoration(gradient: $grad),\n          ),\n        ),\n"
                }
                if (shape == "text") {
                    code += "        Center(child: Text('$textContent', style: TextStyle(fontSize: 100, fontWeight: FontWeight.w900))),\n"
                }
                code += "      ],\n    )$clipEnd;\n  }\n}"
            }
            "React Native" -> {
                code = "import React from 'react';\nimport { View, Text, StyleSheet } from 'react-native';\nimport LinearGradient from 'react-native-linear-gradient';\nimport MaskedView from '@react-native-masked-view/masked-view';\n\nexport const ChromaBackground = () => {\n"
                if (shape == "text") {
                    code += "  return (\n    <MaskedView style={{ flex: 1 }} maskElement={<View style={styles.maskContainer}><Text style={styles.maskText}>$textContent</Text></View>}>\n      <View style={styles.container}>\n"
                } else {
                    val radius = when(shape) { "circle" -> "borderRadius: 999, overflow: 'hidden', "; "rounded" -> "borderRadius: 24, overflow: 'hidden', "; else -> "" }
                    code += "  return (\n    <View style={[styles.container, { $radius }]}>\n"
                }
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    code += "      <LinearGradient colors={[${exp.rnColors}]} style={[StyleSheet.absoluteFill, { opacity: ${l.opacity} }]} />\n"
                }
                if (shape == "text") {
                    code += "      </View>\n    </MaskedView>\n  );\n};\n\nconst styles = StyleSheet.create({\n  container: { flex: 1, backgroundColor: '$bgColor' },\n  maskContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },\n  maskText: { fontSize: 100, fontWeight: '900', color: 'black' }\n});"
                } else {
                    code += "    </View>\n  );\n};\n\nconst styles = StyleSheet.create({\n  container: { flex: 1, backgroundColor: '$bgColor' },\n});"
                }
            }
            "XML Drawable" -> {
                code = "<!-- res/drawable/chroma_bg.xml -->\n<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<layer-list xmlns:android=\"http://schemas.android.com/apk/res/android\">\n  <item>\n    <shape android:shape=\"rectangle\">\n      <solid android:color=\"$bgColor\"/>\n    </shape>\n  </item>\n"
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    val typeMap = when (l.type) {
                        LayerType.RADIAL -> "radial"
                        LayerType.CONIC -> "sweep"
                        else -> "linear"
                    }
                    if (l.type == LayerType.BLOB || l.type == LayerType.LIQUID || l.type == LayerType.AURORA || l.type == LayerType.MESH) return@forEachIndexed
                    val extraAttr = when (l.type) {
                        LayerType.LINEAR -> "android:angle=\"${l.angle}\"\n        "
                        LayerType.RADIAL -> "android:gradientRadius=\"300dp\"\n        "
                        else -> ""
                    }
                    code += "  <item>\n    <shape android:shape=\"rectangle\">\n      <gradient\n        android:type=\"$typeMap\"\n        $extraAttr${exp.xmlColors} />\n    </shape>\n  </item>\n"
                }
                code += "</layer-list>"
            }
            "Kotlin" -> {
                code = "// Android Kotlin Programmatic Gradient\nval backgroundLayer = GradientDrawable()\nbackgroundLayer.setColor(Color.parseColor(\"$bgColor\"))\n\nval layers = mutableListOf<Drawable>(backgroundLayer)\n\n"
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    val shape = when (l.type) {
                        LayerType.RADIAL -> "GradientDrawable.RADIAL_GRADIENT"
                        LayerType.CONIC -> "GradientDrawable.SWEEP_GRADIENT"
                        else -> "GradientDrawable.LINEAR_GRADIENT"
                    }
                    if (l.type == LayerType.BLOB || l.type == LayerType.LIQUID || l.type == LayerType.AURORA || l.type == LayerType.MESH) return@forEachIndexed
                    code += "val layer$i = GradientDrawable(GradientDrawable.Orientation.TL_BR, ${exp.kotlinColors})\nlayer$i.gradientType = $shape\nlayer$i.alpha = ${(l.opacity * 255).toInt()}\nlayers.add(layer$i)\n\n"
                }
                code += "val layerDrawable = LayerDrawable(layers.toTypedArray())\nview.background = layerDrawable"
            }
            "Java" -> {
                code = "// Android Java Programmatic Gradient\nGradientDrawable backgroundLayer = new GradientDrawable();\nbackgroundLayer.setColor(Color.parseColor(\"$bgColor\"));\n\nDrawable[] layers = new Drawable[${layers.size + 1}];\nlayers[0] = backgroundLayer;\n\n"
                layers.forEachIndexed { i, l ->
                    val exp = getExportStrings(l, i, hasAnim, aStyle, aSpeed, intensity)
                    val shape = when (l.type) {
                        LayerType.RADIAL -> "GradientDrawable.RADIAL_GRADIENT"
                        LayerType.CONIC -> "GradientDrawable.SWEEP_GRADIENT"
                        else -> "GradientDrawable.LINEAR_GRADIENT"
                    }
                    if (l.type == LayerType.BLOB || l.type == LayerType.LIQUID || l.type == LayerType.AURORA || l.type == LayerType.MESH) return@forEachIndexed
                    code += "GradientDrawable layer$i = new GradientDrawable(GradientDrawable.Orientation.TL_BR, ${exp.javaColors});\nlayer$i.setGradientType($shape);\nlayer$i.setAlpha(${(l.opacity * 255).toInt()});\nlayers[${i+1}] = layer$i;\n\n"
                }
                code += "LayerDrawable layerDrawable = new LayerDrawable(layers);\nview.setBackground(layerDrawable);"
            }
            "Canvas JS" -> {
                code = "// Canvas JS rendering for accurate Aurora & Complex effects\nconst canvas = document.getElementById(\"chromaCanvas\");\nconst ctx = canvas.getContext(\"2d\");\nlet time = 0;\n\nfunction render() {\n  ctx.clearRect(0, 0, canvas.width, canvas.height);\n"
                code += "  ctx.fillStyle = \"$bgColor\";\n  ctx.fillRect(0, 0, canvas.width, canvas.height);\n"
                layers.forEachIndexed { _, l ->
                    if (l.type == LayerType.AURORA) {
                        code += "  // Aurora Math\n  const nW = Math.min(4, ${l.stops.size});\n  ctx.globalCompositeOperation = \"screen\";\n  ctx.globalAlpha = ${l.opacity};\n"
                        code += "  for(let wi=0; wi<nW; wi++) {\n    const phase = wi*(Math.PI*2/nW) + time*(0.5+wi*0.3);\n    const yBase = canvas.height*(0.15+wi*0.14);\n    ctx.beginPath(); ctx.moveTo(0, canvas.height);\n    for(let x=0; x<=canvas.width; x+=3) {\n      let y = yBase;\n      for(let k=1; k<=5; k++) {\n        y += Math.sin((x/canvas.width)*Math.PI*2*k + phase + k*0.5)*canvas.height*(0.08+0.04/k);\n      }\n      ctx.lineTo(x, y);\n    }\n    ctx.lineTo(canvas.width, canvas.height); ctx.closePath();\n    ctx.fillStyle = \"rgba(255,255,255,0.2)\"; // Set stops here based on layer.stops\n    ctx.fill();\n  }\n"
                    }
                }
                code += "  ctx.globalAlpha = 1.0;\n  ctx.globalCompositeOperation = \"source-over\";\n  time += 0.005;\n  requestAnimationFrame(render);\n}\nrender();"
            }
            else -> {
                code = "/* Code generation for $type is not yet implemented. */\n// Check back soon!"
            }
        }
        return code
    }
}
