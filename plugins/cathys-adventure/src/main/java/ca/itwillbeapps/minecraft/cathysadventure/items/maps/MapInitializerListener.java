package ca.itwillbeapps.minecraft.cathysadventure.items.maps;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapView;

public class MapInitializerListener implements Listener {
    private final MapsManager mapsManager;

    public MapInitializerListener(MapsManager mapsManager) {
        this.mapsManager = mapsManager;
    }

    /**
     * Listens to map initialization events to add the red X in the center.
     *
     * @param event The map initialization event
     */
    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        MapView mapView = event.getMap();

        if (mapsManager.isCustomMap(mapView.getId())) {
            mapView.addRenderer(new RedXMapRenderer());
        }
    }
}
