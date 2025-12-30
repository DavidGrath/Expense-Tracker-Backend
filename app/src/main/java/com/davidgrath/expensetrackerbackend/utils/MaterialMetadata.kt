package com.davidgrath.expensetrackerbackend.utils

import com.google.gson.annotations.SerializedName

data class MaterialMetadata(val host: String, val asset_url_pattern: String, val families: Set<MaterialIconFamily>, val icons: List<MaterialIcon>) {
    enum class MaterialIconFamily() {
        @SerializedName("Material Icons")
        MaterialIcons,
        @SerializedName("Material Icons Outlined")
        MaterialIconsOutlined,
        @SerializedName("Material Icons Round")
        MaterialIconsRound,
        @SerializedName("Material Icons Sharp")
        MaterialIconsSharp,
        @SerializedName("Material Icons Two Tone")
        MaterialIconsTwoTone,
        @SerializedName("Material Symbols Outlined")
        MaterialSymbolsOutlined,
        @SerializedName("Material Symbols Rounded")
        MaterialSymbolsRounded,
        @SerializedName("Material Symbols Sharp")
        MaterialSymbolsSharp
    }

    data class MaterialIcon(val name: String, val version: Int, val popularity: Int, val codePoint: Int, val unsupported_families: Set<MaterialIconFamily>, val categories: Set<String>, val tags: Set<String>, val sizes_px: Set<Int>) {

    }
}