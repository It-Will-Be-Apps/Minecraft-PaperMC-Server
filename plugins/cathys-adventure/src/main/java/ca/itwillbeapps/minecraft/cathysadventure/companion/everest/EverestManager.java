
package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.companion.LocationFinder;
import ca.itwillbeapps.minecraft.cathysadventure.config.Configurations;
import ca.itwillbeapps.minecraft.cathysadventure.items.cheese.CheeseItem;
import ca.itwillbeapps.minecraft.cathysadventure.items.summoninghorn.SummoningHornInteractionListener;
import ca.itwillbeapps.minecraft.cathysadventure.items.summoninghorn.SummoningHornItem;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;
import net.kyori.adventure.text.Component;

public class EverestManager {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;
    private final CheeseItem cheeseItem;

    private SummoningHornItem summoningHornItem;
    private EverestEntity everestEntity;
    private EverestMovement everestMovement;
    private EverestBehavior everestBehavior;
    private EverestCombat everestCombat;
    private EverestChunkLoader everestChunkLoader;
    private EverestTargetListener everestTargetListener;
    private EverestLoadedListener everestLoadedListener;
    private EverestDamageListener everestDamageListener;
    private EverestEnrageListener everestEnrageListener;
    private EverestFeedingListener everestFeedingListener;
    private EverestBreedingListener everestBreedingListener;
    private EverestNamingListener everestNamingListener;
    private EverestSittingListener everestSittingListener;
    private SummoningHornInteractionListener summoningHornInteractionListener;

    private Wolf everest;
    private UUID everestUuid;

    public EverestManager(CathysAdventure plugin, PlayerManager playerManager, CheeseItem cheeseItem) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.cheeseItem = cheeseItem;
    }

    /**
     * Initializes the everest entity, behaviors and listeners and spawns
     * Everest in a proper location if required.
     */
    public void initialize() {
        everestEntity = new EverestEntity(plugin);
        ensureEverestExists();

        summoningHornItem = new SummoningHornItem(plugin);

        everestMovement = new EverestMovement();

        everestCombat = new EverestCombat(plugin, this);
        everestCombat.start();

        everestBehavior = new EverestBehavior(plugin, playerManager, cheeseItem, this, everestMovement, everestCombat);
        everestBehavior.start();

        everestChunkLoader = new EverestChunkLoader(plugin, this);
        everestChunkLoader.start();

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        everestLoadedListener = new EverestLoadedListener(this, everestEntity);
        pluginManager.registerEvents(everestLoadedListener, plugin);

        everestTargetListener = new EverestTargetListener(everestEntity);
        pluginManager.registerEvents(everestTargetListener, plugin);

        everestDamageListener = new EverestDamageListener(everestEntity);
        pluginManager.registerEvents(everestDamageListener, plugin);

        everestEnrageListener = new EverestEnrageListener(plugin, playerManager, this, everestEntity, everestCombat);
        pluginManager.registerEvents(everestEnrageListener, plugin);

        everestFeedingListener = new EverestFeedingListener(playerManager, cheeseItem, summoningHornItem, everestEntity);
        pluginManager.registerEvents(everestFeedingListener, plugin);

        everestBreedingListener = new EverestBreedingListener(playerManager, everestEntity);
        pluginManager.registerEvents(everestBreedingListener, plugin);

        everestNamingListener = new EverestNamingListener(everestEntity);
        pluginManager.registerEvents(everestNamingListener, plugin);
        
        everestSittingListener = new EverestSittingListener(everestEntity);
        pluginManager.registerEvents(everestSittingListener, plugin);

        summoningHornInteractionListener = new SummoningHornInteractionListener(playerManager, this, everestMovement, summoningHornItem);
        pluginManager.registerEvents(summoningHornInteractionListener, plugin);

        plugin.getLogger().info("Everest manager initialized");
    }

    /**
     * Ensures that Everest exists in the world, by finding her or spawning her
     * if she cannot be found.
     */
    private void ensureEverestExists() {
        UUID savedUuid = getSavedEverestUuid();

        if (savedUuid != null) {
            everestUuid = savedUuid;
            Entity savedEntity = Bukkit.getEntity(savedUuid);

            if (savedEntity == null) {
                plugin.getLogger().info("Everest is not currently loaded, waiting for her saved entity to load");
                return;
            }

            if (savedEntity instanceof Wolf wolf) {
                everest = wolf;
                everestEntity.markAsEverest(wolf);
                plugin.getLogger().info("Found existing Everest");
                return;
            }

            plugin.getLogger().severe("Everest's saved UUID belongs to a non-wolf entity, no replacement was spawned");
            return;
        }

        spawnEverest();
    }

    /**
     * Retrieves Everest's saved UUID from the plugin configuration, if it
     * exists.
     *
     * @return Everest's UUID, or null if it doesn't exist
     */
    private UUID getSavedEverestUuid() {
        String uuid = plugin.getConfig().getString(Configurations.EVEREST_UUID);

        if (uuid == null || uuid.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().info("Everest's saved UUID is missing or invalid");
            return null;
        }
    }

    /**
     * Spawns Everest in the world and saves her UUID in the plugin data.
     */
    private void spawnEverest() {
        if (Bukkit.getWorlds().isEmpty()) {
            plugin.getLogger().severe("Cannot spawn Everest, no worlds are loaded");
            return;
        }

        World world = Bukkit.getWorlds().get(0);
        Location location = LocationFinder.nearLocation(world, world.getSpawnLocation());

        if (location == null) {
            location = world.getSpawnLocation().clone().add(0.5, 1, 0.5);
        }

        everest = world.spawn(location, Wolf.class);
        everest.setVariant(Wolf.Variant.SNOWY);
        everest.customName(Component.text("Mysterious Dog"));
        everest.setCustomNameVisible(false);
        everest.setPersistent(true);
        everest.setRemoveWhenFarAway(false);

        everestEntity.markAsEverest(everest);
        saveEverestUuid(everest.getUniqueId());

        plugin.getLogger().info("Spawned Everest at location: " + location.toString());
    }

    /**
     * Saves the given UUID as Everest's UUID.
     *
     * @param uuid The UUID to save for Everest
     */
    private void saveEverestUuid(UUID uuid) {
        everestUuid = uuid;
        plugin.getConfig().set(Configurations.EVEREST_UUID, uuid.toString());
        plugin.saveConfig();
    }

    /**
     * Verifies if the given entity is Everest.
     *
     * @param entity The entity to verify
     * @return Whether the entity is Everest
     */
    public boolean isEverest(Entity entity) {
        return everestEntity.isEverest(entity);
    }

    /**
     * Returns Everest if her entity is currently loaded.
     *
     * @return Everest, or null if she isn't loaded or can't be found
     */
    public Wolf getEverest() {
        return everest;
    }

    /**
     * Sets the Everest entity.
     *
     * @param everest The Everest entity, or null if it isn't loaded
     */
    public void setEverest(Wolf everest) {
        this.everest = everest;
    }

    /**
     * Returns Everest's UUID if known.
     *
     * @return Everest's UUID, or null if it isn't known
     */
    public UUID getEverestUuid() {
        return everestUuid;
    }

    /**
     * Cleans up the Everest behaviors and listeners.
     */
    public void cleanUp() {
        everestBehavior.stop();
        everestCombat.stop();
        everestChunkLoader.stop();

        HandlerList.unregisterAll(everestLoadedListener);
        HandlerList.unregisterAll(everestTargetListener);
        HandlerList.unregisterAll(everestDamageListener);
        HandlerList.unregisterAll(everestEnrageListener);
        HandlerList.unregisterAll(everestFeedingListener);
        HandlerList.unregisterAll(everestBreedingListener);
        HandlerList.unregisterAll(everestNamingListener);
        HandlerList.unregisterAll(everestSittingListener);
        HandlerList.unregisterAll(summoningHornInteractionListener);

        plugin.getLogger().info("Everest manager disabled");
    }
}
