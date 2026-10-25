package ca.itwillbeapps.minecraft.cathysadventure.items.maps;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class RedXMapRenderer extends MapRenderer {
    public RedXMapRenderer() {
        super(false); 
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        MapCursorCollection cursors = canvas.getCursors();

        if (cursors.size() == 0) {
            cursors.addCursor(new MapCursor((byte) 0, (byte) 0, (byte) 8, MapCursor.Type.RED_X, true));
            canvas.setCursors(cursors);
        }
    }
}
