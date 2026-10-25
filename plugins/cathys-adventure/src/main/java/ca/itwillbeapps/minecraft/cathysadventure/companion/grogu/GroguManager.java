
package ca.itwillbeapps.minecraft.cathysadventure.companion.grogu;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LeashHitch;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.config.Configurations;
import ca.itwillbeapps.minecraft.cathysadventure.items.metealbead.MetalBeadItem;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;
import ca.itwillbeapps.minecraft.cathysadventure.util.LocationFinder;
import net.kyori.adventure.text.Component;

public class GroguManager {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;

    private GroguEntity groguEntity;
    private GroguChunkLoader groguChunkLoader;
    private GroguLoadedListener groguLoadedListener;

    private UUID groguUuid;
    private Allay grogu;
    private MetalBeadItem metalBeadItem;

    public GroguManager(CathysAdventure plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    /**
     * Initializes the Grogu entity, behaviors and listeners and spawns
     * Grogu in a proper location if required.
     */
    public void initialize() {
        groguEntity = new GroguEntity(plugin);
        ensureGroguExists();

        metalBeadItem = new MetalBeadItem(plugin);

        groguChunkLoader = new GroguChunkLoader(plugin, this);
        groguChunkLoader.start();

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        groguLoadedListener = new GroguLoadedListener(this, groguEntity);
        pluginManager.registerEvents(groguLoadedListener, plugin);

        plugin.getLogger().info("Grogu manager initialized");
    }

    /**
     * Ensures that Grogu exists in the world, by finding him or spawning him
     * if he cannot be found.
     */
    private void ensureGroguExists() {
        UUID savedUuid = getSavedGroguUuid();

        if (savedUuid != null) {
            groguUuid = savedUuid;
            Entity savedEntity = plugin.getServer().getEntity(savedUuid);

            if (savedEntity == null) {
                plugin.getLogger().info("Grogu is not currently loaded, waiting for his saved entity to load");
                return;
            }

            if (savedEntity instanceof Allay allay) {
                grogu = allay;
                groguEntity.markAsGrogu(allay);
                plugin.getLogger().info("Found existing Grogu");
                return;
            }

            plugin.getLogger().severe("Grogu's saved UUID belongs to a non-allay entity, no replacement was spawned");
            return;
        }

        spawnGrogu();
    }

    /**
     * Retrieves Grogu's saved UUID from the plugin configuration, if it
     * exists.
     *
     * @return Grogu's UUID, or null if it doesn't exist
     */
    private UUID getSavedGroguUuid() {
        String uuid = plugin.getConfig().getString(Configurations.GROGU_UUID);

        if (uuid == null || uuid.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().info("Grogu's saved UUID is missing or invalid");
            return null;
        }
    }

    /**
     * Spawns Grogu in the world and saves his UUID in the plugin data.
     */
    private void spawnGrogu() {
        if (plugin.getServer().getWorlds().isEmpty()) {
            plugin.getLogger().severe("Cannot spawn Grogu, no worlds are loaded");
            return;
        }

        World world = plugin.getServer().getWorlds().get(0);
        Location location = LocationFinder.inWoodlandMansionJail(world.getSpawnLocation());

        if (location == null) {
            location = world.getSpawnLocation().clone().add(0.5, 1, 0.5);
        }
        
        Bukkit.getLogger().info("Post location start");
        Location postLocation = LocationFinder.nearLocation(location.clone().add(0.0, -1.0, 0.0), 1, 1);
        Bukkit.getLogger().info("Post location end: " + postLocation.toString());
        postLocation.getBlock().setType(Material.OAK_FENCE);

        LeashHitch leashHitch = world.spawn(postLocation, LeashHitch.class);

        grogu = world.spawn(location, Allay.class);
        grogu.setLeashHolder(leashHitch);
        grogu.customName(Component.text("Grogu"));
        grogu.setCustomNameVisible(false);
        grogu.setPersistent(true);
        grogu.setRemoveWhenFarAway(false);
        grogu.getPersistentDataContainer().set(plugin.getKeys().groguJailLocationKey, PersistentDataType.INTEGER_ARRAY,
            new int[] { location.getBlockX(), location.getBlockY(), location.getBlockZ() });

        groguEntity.markAsGrogu(grogu);
        saveGroguUuid(grogu.getUniqueId());

        plugin.getLogger().info("Spawned Grogu at location: " + location.toString());
    }

    /**
     * Saves the given UUID as Grogu's UUID.
     *
     * @param uuid The UUID to save for Grogu
     */
    private void saveGroguUuid(UUID uuid) {
        plugin.getLogger().info("Saving Grogu UUID : " + uuid.toString());
        groguUuid = uuid;
        plugin.getConfig().set(Configurations.GROGU_UUID, uuid.toString());
        plugin.saveConfig();
    }

    /**
     * Returns Grogu's UUID if known.
     *
     * @return Grogu's UUID, or null if it isn't known
     */
    public UUID getGroguUuid() {
        plugin.getLogger().info("Getting Grogu UUID : " + groguUuid.toString());
        return groguUuid;
    }

    /**
     * Returns the Grogu entity if he is loaded.
     *
     * @return The Grogu entity if he is currently loaded
     */
    public Allay getGrogu() {
        return grogu;
    }

    /**
     * Sets the Grogu entity.
     *
     * @param allay The Grogu entity
     */
    public void setGrogu(Allay grogu) {
        this.grogu = grogu;
    }

    /**
     * Returns the metal bead item definition.
     *
     * @return The metal bead item definition
     */
    public MetalBeadItem getMetalBeadItem() {
        return metalBeadItem;
    }

    /**
     * Cleans up the Grogu behaviors and listeners.
     */
    public void cleanUp() {
        groguChunkLoader.stop();

        HandlerList.unregisterAll(groguLoadedListener);

        plugin.getLogger().info("Grogu manager disabled");
    }
}
