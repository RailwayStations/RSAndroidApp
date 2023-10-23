package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge;

import org.mapsforge.map.layer.download.tilesource.OnlineTileSource;

public class DbsTileSource extends OnlineTileSource {

    public DbsTileSource(String name, String baseUrl) {
        super(new String[]{"osm-prod.noncd.db.de"}, 8100);

        setName(name);
        setBaseUrl(baseUrl);
        setName(name).setAlpha(false)
                .setBaseUrl(baseUrl)
                .setParallelRequestsLimit(8).setProtocol("https").setTileSize(256)
                .setZoomLevelMax((byte) 18).setZoomLevelMin((byte) 0);
        setApiKey("P9roW0ePGY9TCRiXh8y5P1T4traxBrWl");
    }

}
