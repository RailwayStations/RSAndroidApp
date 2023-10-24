package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

import org.mapsforge.map.layer.download.tilesource.OnlineTileSource

class DbsTileSource(name: String?, baseUrl: String?) :
    OnlineTileSource(arrayOf("osm-prod.noncd.db.de"), 8100) {
    init {
        setName(name)
        setBaseUrl(baseUrl)
        setName(name).setAlpha(false)
            .setBaseUrl(baseUrl)
            .setParallelRequestsLimit(8).setProtocol("https").setTileSize(256)
            .setZoomLevelMax(18.toByte()).zoomLevelMin = 0.toByte()
        setApiKey("P9roW0ePGY9TCRiXh8y5P1T4traxBrWl")
    }
}