package ca.itwillbeapps.minecraft.cathysadventure.items.maps;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ObelixMapItem {
    private static final String ITEM_ID = "OBELIX_MAP";

    private final CathysAdventure plugin;
    private final MapsManager mapsManager;

    public ObelixMapItem(CathysAdventure plugin, MapsManager mapsManager) {
        this.plugin = plugin;
        this.mapsManager = mapsManager;
    }

    /**
     * Creates a map to Obélix.
     *
     * @param obelixUuid Obélix's UUID
     * @return The created map to Obélix
     */
    public ItemStack create(UUID obelixUuid) {
        if (obelixUuid == null) {
            return new ItemStack(Material.FILLED_MAP);
        }

        Entity obelix = plugin.getServer().getEntity(obelixUuid);

        if (obelix == null || obelix.isDead() || !obelix.isValid()) {
            return new ItemStack(Material.FILLED_MAP);
        }

        World world = obelix.getWorld();
        int[] obelixHomeCoordinates = obelix.getPersistentDataContainer().get(plugin.getKeys().obelixHomeLocationKey, PersistentDataType.INTEGER_ARRAY);
        Location obelixHomeLocation = new Location(world, obelixHomeCoordinates[0], obelixHomeCoordinates[1], obelixHomeCoordinates[2]);

        MapView mapView = plugin.getServer().createMap(world);
        mapView.setCenterX(obelixHomeLocation.getBlockX());
        mapView.setCenterZ(obelixHomeLocation.getBlockZ());
        mapView.setScale(MapView.Scale.FARTHEST);
        mapView.setTrackingPosition(true);
        mapView.setUnlimitedTracking(true);

        mapsManager.saveCustomMapId(mapView.getId());

        world.getChunkAt(obelixHomeLocation).load(true);

        mapView.addRenderer(new RedXMapRenderer());

        ItemStack obelixMap = ItemStack.of(Material.FILLED_MAP);

        obelixMap.editMeta(MapMeta.class, meta -> {
            meta.setMapView(mapView);
            meta.displayName(Component.text("Snowy map", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(Component.text("Everest was really holding on to this map, it must be important...", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(plugin.getKeys().itemIdKey, PersistentDataType.STRING, ITEM_ID);
            meta.getPersistentDataContainer().set(plugin.getKeys().soulboundItemKey, PersistentDataType.BOOLEAN, true);
        });

        return obelixMap;
    }

    /**
     * Verifies if the given item is a map to Obélix.
     *
     * @param item The item to verify
     * @return Whether the item is a map to Obélix
     */
    public boolean isMapToObelix(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        if (item.getType() != Material.FILLED_MAP) {
            return false;
        }

        return ITEM_ID.equals(item.getPersistentDataContainer().get(plugin.getKeys().itemIdKey, PersistentDataType.STRING));
    }
}
