
package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.everest.EverestEntity;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.obelix.ObelixEntity;
import ca.itwillbeapps.minecraft.cathysadventure.config.Configurations;
import ca.itwillbeapps.minecraft.cathysadventure.items.cheese.CheeseItem;
import ca.itwillbeapps.minecraft.cathysadventure.items.maps.ObelixMapItem;
import ca.itwillbeapps.minecraft.cathysadventure.items.summoninghorn.SummoningHornInteractionListener;
import ca.itwillbeapps.minecraft.cathysadventure.items.summoninghorn.SummoningHornItem;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;
import ca.itwillbeapps.minecraft.cathysadventure.util.LocationFinder;
import net.kyori.adventure.text.Component;

public class GreatPyreneesManager {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;
    private final CheeseItem cheeseItem;

    private ObelixMapItem obelixMapItem;
    private SummoningHornItem summoningHornItem;
    private EverestEntity everestEntity;
    private ObelixEntity obelixEntity;
    private GreatPyreneesMovement greatPyreneesMovement;
    private GreatPyreneesBehavior greatPyreneesBehavior;
    private GreatPyreneesCombat greatPyreneesCombat;
    private GreatPyreneesChunkLoader greatPyreneesChunkLoader;
    private GreatPyreneesLoadedListener greatPyreneesLoadedListener;
    private EverestTargetListener everestTargetListener;
    private GreatPyreneesDamageListener greatPyreneesDamageListener;
    private GreatPyreneesEnrageListener greatPyreneesEnrageListener;
    private GreatPyreneesFeedingListener greatPyreneesFeedingListener;
    private GreatPyreneesBreedingListener greatPyreneesBreedingListener;
    private GreatPyreneesNamingListener greatPyreneesNamingListener;
    private GreatPyreneesSittingListener greatPyreneesSittingListener;
    private SummoningHornInteractionListener summoningHornInteractionListener;

    private UUID everestUuid;
    private UUID obelixUuid;
    private List<UUID> puppiesUuids;

    private Map<UUID, Wolf> greatPyrenees;

    public GreatPyreneesManager(CathysAdventure plugin, PlayerManager playerManager, CheeseItem cheeseItem) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.cheeseItem = cheeseItem;
    }

    /**
     * Initializes the everest entity, behaviors and listeners and spawns
     * Everest in a proper location if required.
     */
    public void initialize() {
        greatPyrenees = new HashMap<>();

        everestEntity = new EverestEntity(plugin);
        ensureEverestExists();

        obelixEntity = new ObelixEntity(plugin);
        ensureObelixExists();

        obelixMapItem = new ObelixMapItem(plugin, this);

        summoningHornItem = new SummoningHornItem(plugin);

        greatPyreneesMovement = new GreatPyreneesMovement();

        greatPyreneesCombat = new GreatPyreneesCombat(plugin, this);
        greatPyreneesCombat.start();

        greatPyreneesBehavior = new GreatPyreneesBehavior(plugin, playerManager, cheeseItem, everestEntity, obelixEntity, this, greatPyreneesMovement, greatPyreneesCombat);
        greatPyreneesBehavior.start();

        greatPyreneesChunkLoader = new GreatPyreneesChunkLoader(plugin, this);
        greatPyreneesChunkLoader.start();

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        greatPyreneesLoadedListener = new GreatPyreneesLoadedListener(this, everestEntity, obelixEntity);
        pluginManager.registerEvents(greatPyreneesLoadedListener, plugin);

        greatPyreneesDamageListener = new GreatPyreneesDamageListener(everestEntity, obelixEntity);
        pluginManager.registerEvents(greatPyreneesDamageListener, plugin);

        greatPyreneesEnrageListener = new GreatPyreneesEnrageListener(plugin, playerManager, greatPyreneesCombat);
        pluginManager.registerEvents(greatPyreneesEnrageListener, plugin);
        
        greatPyreneesFeedingListener = new GreatPyreneesFeedingListener(playerManager, cheeseItem, summoningHornItem, obelixMapItem, everestEntity, obelixEntity);
        pluginManager.registerEvents(greatPyreneesFeedingListener, plugin);
        
        greatPyreneesBreedingListener = new GreatPyreneesBreedingListener(playerManager, everestEntity, obelixEntity);
        pluginManager.registerEvents(greatPyreneesBreedingListener, plugin);
        
        greatPyreneesNamingListener = new GreatPyreneesNamingListener(everestEntity, obelixEntity);
        pluginManager.registerEvents(greatPyreneesNamingListener, plugin);

        greatPyreneesSittingListener = new GreatPyreneesSittingListener(everestEntity, obelixEntity);
        pluginManager.registerEvents(greatPyreneesSittingListener, plugin);

        everestTargetListener = new EverestTargetListener(everestEntity);
        pluginManager.registerEvents(everestTargetListener, plugin);

        summoningHornInteractionListener = new SummoningHornInteractionListener(playerManager, this, greatPyreneesMovement, summoningHornItem);
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
            Entity savedEntity = plugin.getServer().getEntity(savedUuid);

            if (savedEntity == null) {
                plugin.getLogger().info("Everest is not currently loaded, waiting for her saved entity to load");
                return;
            }

            if (savedEntity instanceof Wolf wolf) {
                greatPyrenees.put(everestUuid, wolf);
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
        if (plugin.getServer().getWorlds().isEmpty()) {
            plugin.getLogger().severe("Cannot spawn Everest, no worlds are loaded");
            return;
        }

        World world = plugin.getServer().getWorlds().get(0);
        Location location = LocationFinder.nearLocation(world.getSpawnLocation());

        if (location == null) {
            location = world.getSpawnLocation().clone().add(0.5, 1, 0.5);
        }

        Wolf everest = world.spawn(location, Wolf.class);
        everest.setVariant(Wolf.Variant.SNOWY);
        everest.customName(Component.text("Mysterious Dog"));
        everest.setCustomNameVisible(false);
        everest.setPersistent(true);
        everest.setRemoveWhenFarAway(false);

        everestEntity.markAsEverest(everest);
        saveEverestUuid(everest.getUniqueId());
        greatPyrenees.put(everestUuid, everest);

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
     * Returns Everest's UUID if known.
     *
     * @return Everest's UUID, or null if it isn't known
     */
    public UUID getEverestUuid() {
        return everestUuid;
    }

    /**
     * Ensures that Obélix exists in the world, by finding him or spawning him
     * if he cannot be found.
     */
    private void ensureObelixExists() {
        UUID savedUuid = getSavedObelixUuid();

        if (savedUuid != null) {
            obelixUuid = savedUuid;
            Entity savedEntity = plugin.getServer().getEntity(savedUuid);

            if (savedEntity == null) {
                plugin.getLogger().info("Obélix is not currently loaded, waiting for his saved entity to load");
                return;
            }

            if (savedEntity instanceof Wolf wolf) {
                greatPyrenees.put(obelixUuid, wolf);
                obelixEntity.markAsObelix(wolf);
                plugin.getLogger().info("Found existing Obélix");
                return;
            }

            plugin.getLogger().severe("Obélix's saved UUID belongs to a non-wolf entity, no replacement was spawned");
            return;
        }

        spawnObelix();
    }

    /**
     * Retrieves Obélix's saved UUID from the plugin configuration, if it
     * exists.
     *
     * @return Obélix's UUID, or null if it doesn't exist
     */
    private UUID getSavedObelixUuid() {
        String uuid = plugin.getConfig().getString(Configurations.OBELIX_UUID);

        if (uuid == null || uuid.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().info("Obélix's saved UUID is missing or invalid");
            return null;
        }
    }

    /**
     * Spawns Obélix in the world and saves his UUID in the plugin data.
     */
    private void spawnObelix() {
        if (plugin.getServer().getWorlds().isEmpty()) {
            plugin.getLogger().severe("Cannot spawn Obélix, no worlds are loaded");
            return;
        }

        World world = plugin.getServer().getWorlds().get(0);
        Location location = LocationFinder.inBiome(world.getSpawnLocation(), Biome.SNOWY_PLAINS);

        if (location == null) {
            location = world.getSpawnLocation().clone().add(0.5, 1, 0.5);
        }

        Wolf obelix = world.spawn(location, Wolf.class);
        obelix.setVariant(Wolf.Variant.SNOWY);
        obelix.customName(Component.text("Mysterious Dog"));
        obelix.setCustomNameVisible(false);
        obelix.setPersistent(true);
        obelix.setRemoveWhenFarAway(false);
        obelix.getPersistentDataContainer().set(plugin.getKeys().obelixHomeLocationKey, PersistentDataType.INTEGER_ARRAY,
            new int[] { location.getBlockX(), location.getBlockY(), location.getBlockZ() });

        obelixEntity.markAsObelix(obelix);
        saveObelixUuid(obelix.getUniqueId());
        greatPyrenees.put(obelixUuid, obelix);

        plugin.getLogger().info("Spawned Obelix at location: " + location.toString());
    }

    /**
     * Saves the given UUID as Obelix's UUID.
     *
     * @param uuid The UUID to save for Obelix
     */
    private void saveObelixUuid(UUID uuid) {
        obelixUuid = uuid;
        plugin.getConfig().set(Configurations.OBELIX_UUID, uuid.toString());
        plugin.saveConfig();
    }

    /**
     * Returns Obelix's UUID if known.
     *
     * @return Obelix's UUID, or null if it isn't known
     */
    public UUID getObelixUuid() {
        return obelixUuid;
    }

    /**
     * Returns all currently loaded great pyrenees.
     *
     * @return A list of all currently loaded great pyrenees
     */
    public List<Wolf> getGreatPyrenees() {
        return greatPyrenees.values().stream().toList();
    }

    /**
     * Adds the given great pyrenees to the list of known great pyrenees.
     *
     * @param wolf The great pyrenees to add to the list
     */
    public void addGreatPyrenees(Wolf wolf) {
        if (greatPyrenees.values().stream().noneMatch(greatPyrenees -> greatPyrenees.getUniqueId().equals(wolf.getUniqueId()))) {
            greatPyrenees.put(wolf.getUniqueId(), wolf);
        }
    }

    /**
     * Cleans up the Everest behaviors and listeners.
     */
    public void cleanUp() {
        greatPyreneesCombat.stop();
        greatPyreneesBehavior.stop();
        greatPyreneesChunkLoader.stop();

        HandlerList.unregisterAll(greatPyreneesLoadedListener);
        HandlerList.unregisterAll(greatPyreneesDamageListener);
        HandlerList.unregisterAll(greatPyreneesEnrageListener);
        HandlerList.unregisterAll(greatPyreneesFeedingListener);
        HandlerList.unregisterAll(greatPyreneesBreedingListener);
        HandlerList.unregisterAll(greatPyreneesNamingListener);
        HandlerList.unregisterAll(greatPyreneesSittingListener);
        HandlerList.unregisterAll(everestTargetListener);
        HandlerList.unregisterAll(summoningHornInteractionListener);

        plugin.getLogger().info("Everest manager disabled");
    }
}
