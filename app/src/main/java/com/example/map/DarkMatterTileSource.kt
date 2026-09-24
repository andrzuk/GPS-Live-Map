package com.example.map

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

/**
 * CartoDB Dark Matter tile source.
 * Provides a modern dark mode map displaying high contrast streets and street names
 * dynamically scaled by zoom level, with zero commercial POI clutter.
 */
class DarkMatterTileSource(private val apiKey: String) : OnlineTileSourceBase(
    "CartoDBDarkMatter",
    0,
    20,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/dark_all/",
        "https://b.basemaps.cartocdn.com/rastertiles/dark_all/",
        "https://c.basemaps.cartocdn.com/rastertiles/dark_all/",
        "https://d.basemaps.cartocdn.com/rastertiles/dark_all/"
    ),
    "© OpenStreetMap contributors, © CARTO"
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$baseUrl$zoom/$x/$y$mImageFilenameEnding?api_key=$apiKey"
    }
}
