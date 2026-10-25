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

public class GroguMapItem {
    private static final String ITEM_ID = "GROGU_MAP";

    private final CathysAdventure plugin;
    private final MapsManager mapsManager;

    public GroguMapItem(CathysAdventure plugin, MapsManager mapsManager) {
        this.plugin = plugin;
        this.mapsManager = mapsManager;
    }

    /**
     * Creates a map to Grogu.
     *
     * @param groguUuid Grogu's UUID
     * @return The created map to Grogu
     */
    public ItemStack create(UUID groguUuid) {
        if (groguUuid == null) {
            return new ItemStack(Material.FILLED_MAP);
        }

        Entity grogu = plugin.getServer().getEntity(groguUuid);

        if (grogu == null || grogu.isDead() || !grogu.isValid()) {
            return new ItemStack(Material.FILLED_MAP);
        }

        World world = grogu.getWorld();
        int[] groguJailCoordinates = grogu.getPersistentDataContainer().get(plugin.getKeys().groguJailLocationKey, PersistentDataType.INTEGER_ARRAY);
        Location groguJailLocation = new Location(world, groguJailCoordinates[0], groguJailCoordinates[1], groguJailCoordinates[2]);

        MapView mapView = plugin.getServer().createMap(world);
        mapView.setCenterX(groguJailLocation.getBlockX());
        mapView.setCenterZ(groguJailLocation.getBlockZ());
        mapView.setScale(MapView.Scale.CLOSEST);
        mapView.setTrackingPosition(true);
        mapView.setUnlimitedTracking(true);

        mapsManager.saveCustomMapId(mapView.getId());

        world.getChunkAt(groguJailLocation).load(true);

        mapView.addRenderer(new RedXMapRenderer());

        ItemStack groguMap = ItemStack.of(Material.FILLED_MAP);

        groguMap.editMeta(MapMeta.class, meta -> {
            meta.setMapView(mapView);
            meta.displayName(Component.text("Forest map", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(Component.text("Obélix was really holding on to this map, it must be important...", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(plugin.getKeys().itemIdKey, PersistentDataType.STRING, ITEM_ID);
            meta.getPersistentDataContainer().set(plugin.getKeys().soulboundItemKey, PersistentDataType.BOOLEAN, true);
        });

        return groguMap;
    }

    /**
     * Verifies if the given item is a map to Grogu.
     *
     * @param item The item to verify
     * @return Whether the item is a map to Grogu
     */
    public boolean isMapToGrogu(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        if (item.getType() != Material.FILLED_MAP) {
            return false;
        }

        return ITEM_ID.equals(item.getPersistentDataContainer().get(plugin.getKeys().itemIdKey, PersistentDataType.STRING));
    }
}
