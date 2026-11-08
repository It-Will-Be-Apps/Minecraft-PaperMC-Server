
package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import ca.itwillbeapps.minecraft.cathysadventure.bootstrap.CathysAdventureBootstrap;
import ca.itwillbeapps.minecraft.cathysadventure.companion.CompanionMovement;
import ca.itwillbeapps.minecraft.cathysadventure.companion.grogu.GroguManager;
import ca.itwillbeapps.minecraft.cathysadventure.config.Configurations;
import ca.itwillbeapps.minecraft.cathysadventure.items.cheese.CheeseItem;
import ca.itwillbeapps.minecraft.cathysadventure.items.maps.GroguMapItem;
import ca.itwillbeapps.minecraft.cathysadventure.items.maps.ObelixMapItem;
import ca.itwillbeapps.minecraft.cathysadventure.items.summoninghorn.SummoningHornInteractionListener;
import ca.itwillbeapps.minecraft.cathysadventure.items.summoninghorn.SummoningHornItem;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;
import ca.itwillbeapps.minecraft.cathysadventure.util.LocationFinder;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;

public class GreatPyreneesManager {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;
    private final GroguManager groguManager;
    private final CheeseItem cheeseItem;
    private final ObelixMapItem obelixMapItem;
    private final GroguMapItem groguMapItem;
    private final CompanionMovement companionMovement;

    private SummoningHornItem summoningHornItem;
    private EverestEntity everestEntity;
    private ObelixEntity obelixEntity;
    private GreatPyreneesPuppyEntity greatPyreneesPuppyEntity;
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
    private Set<UUID> puppiesUuids;

    private Map<UUID, Wolf> greatPyrenees;

    public GreatPyreneesManager(CathysAdventure plugin, PlayerManager playerManager, GroguManager groguManager, CheeseItem cheeseItem,
            ObelixMapItem obelixMapItem, GroguMapItem groguMapItem, CompanionMovement companionMovement) {

        this.plugin = plugin;
        this.playerManager = playerManager;
        this.groguManager = groguManager;
        this.cheeseItem = cheeseItem;
        this.obelixMapItem = obelixMapItem;
        this.groguMapItem = groguMapItem;
        this.companionMovement = companionMovement;
    }

    /**
     * Initializes the great pyrenees entities, behaviors and listeners and spawns
     * Everest and Obélix in a proper location if required.
     */
    public void initialize() {
        puppiesUuids = new HashSet<>();
        greatPyrenees = new HashMap<>();

        everestEntity = new EverestEntity(plugin);
        ensureEverestExists();

        obelixEntity = new ObelixEntity(plugin);
        ensureObelixExists();

        greatPyreneesPuppyEntity = new GreatPyreneesPuppyEntity(plugin);
        loadPuppies();

        summoningHornItem = new SummoningHornItem(plugin);

        greatPyreneesCombat = new GreatPyreneesCombat(plugin, this);
        greatPyreneesCombat.start();

        greatPyreneesBehavior = new GreatPyreneesBehavior(plugin, playerManager, cheeseItem, everestEntity, obelixEntity, this, greatPyreneesCombat, companionMovement);
        greatPyreneesBehavior.start();

        greatPyreneesChunkLoader = new GreatPyreneesChunkLoader(plugin, this);
        greatPyreneesChunkLoader.start();

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        greatPyreneesLoadedListener = new GreatPyreneesLoadedListener(this, everestEntity, obelixEntity, greatPyreneesPuppyEntity);
        pluginManager.registerEvents(greatPyreneesLoadedListener, plugin);

        greatPyreneesDamageListener = new GreatPyreneesDamageListener(everestEntity, obelixEntity, greatPyreneesPuppyEntity);
        pluginManager.registerEvents(greatPyreneesDamageListener, plugin);

        greatPyreneesEnrageListener = new GreatPyreneesEnrageListener(plugin, playerManager, greatPyreneesCombat);
        pluginManager.registerEvents(greatPyreneesEnrageListener, plugin);
        
        greatPyreneesFeedingListener = new GreatPyreneesFeedingListener(playerManager, this, groguManager, cheeseItem, summoningHornItem, obelixMapItem, groguMapItem,
            groguManager.getMetalBeadItem(), everestEntity, obelixEntity, greatPyreneesPuppyEntity);
        pluginManager.registerEvents(greatPyreneesFeedingListener, plugin);
        
        greatPyreneesBreedingListener = new GreatPyreneesBreedingListener(playerManager, this, everestEntity, obelixEntity);
        pluginManager.registerEvents(greatPyreneesBreedingListener, plugin);
        
        greatPyreneesNamingListener = new GreatPyreneesNamingListener(playerManager, everestEntity, obelixEntity, greatPyreneesPuppyEntity);
        pluginManager.registerEvents(greatPyreneesNamingListener, plugin);

        greatPyreneesSittingListener = new GreatPyreneesSittingListener(everestEntity, obelixEntity, greatPyreneesPuppyEntity);
        pluginManager.registerEvents(greatPyreneesSittingListener, plugin);

        everestTargetListener = new EverestTargetListener(everestEntity);
        pluginManager.registerEvents(everestTargetListener, plugin);

        summoningHornInteractionListener = new SummoningHornInteractionListener(plugin, playerManager, this, companionMovement, groguManager, summoningHornItem);
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
        Location location = LocationFinder.nearLocation(world.getSpawnLocation(), 0, Settings.LOCATION_HEIGHT_RANGE);

        if (location == null) {
            location = world.getSpawnLocation().clone().add(0.5, 1, 0.5);
        }

        Wolf everest = world.spawn(location, Wolf.class);
        everest.customName(Component.text("Mysterious Dog"));
        everest.setCustomNameVisible(false);
        everest.setPersistent(true);
        everest.setRemoveWhenFarAway(false);

        Wolf.Variant femaleGreatPyreneesVariant = RegistryAccess.registryAccess().getRegistry(RegistryKey.WOLF_VARIANT).get(NamespacedKey.fromString(CathysAdventureBootstrap.FEMALE_GP_VARIANT));
        if (femaleGreatPyreneesVariant != null) {
            everest.setVariant(femaleGreatPyreneesVariant);
        }

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
        obelix.customName(Component.text("Mysterious Dog"));
        obelix.setCustomNameVisible(false);
        obelix.setPersistent(true);
        obelix.setRemoveWhenFarAway(false);
        obelix.getPersistentDataContainer().set(plugin.getKeys().obelixHomeLocationKey, PersistentDataType.INTEGER_ARRAY,
            new int[] { location.getBlockX(), location.getBlockY(), location.getBlockZ() });

        Wolf.Variant maleGreatPyreneesVariant = RegistryAccess.registryAccess().getRegistry(RegistryKey.WOLF_VARIANT).get(NamespacedKey.fromString(CathysAdventureBootstrap.MALE_GP_VARIANT));
        if (maleGreatPyreneesVariant != null) {
            obelix.setVariant(maleGreatPyreneesVariant);
        }

        obelixEntity.markAsObelix(obelix);
        saveObelixUuid(obelix.getUniqueId());
        greatPyrenees.put(obelixUuid, obelix);

        plugin.getLogger().info("Spawned Obélix at location: " + location.toString());
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
     * Loads any existing puppies.
     */
    private void loadPuppies() {
        List<String> savedUuids = plugin.getConfig().getStringList(Configurations.PUPPIES_UUIDS);

        if (savedUuids == null || savedUuids.isEmpty()) {
            return;
        }

        for (String savedUuid : savedUuids) {
            try {
                UUID uuid = UUID.fromString(savedUuid);
                puppiesUuids.add(uuid);

                Entity savedEntity = plugin.getServer().getEntity(uuid);
                if (savedEntity == null) {
                    plugin.getLogger().info("Puppy is not currently loaded, waiting for his saved entity to load");
                    return;
                }

                if (savedEntity instanceof Wolf wolf) {
                    greatPyrenees.put(uuid, wolf);
                    greatPyreneesPuppyEntity.markAsGreatPyreneesPuppy(wolf);
                    plugin.getLogger().info("Found existing puppy");
                    return;
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Puppy's saved UUID is invalid");
            }
        }
    }

    /**
     * Verifies if an additional puppy can spawn.
     *
     * @return Whether a new puppy can spawn
     */
    public boolean canPuppySpawn() {
        return puppiesUuids.size() < Settings.GP_MAX_PUPPIES;
    }

    /**
     * Registers the given wolf as a great pyrenees puppy.
     *
     * @param puppy The new puppy to register
     */
    public void registerPuppy(Wolf puppy) {
        greatPyreneesPuppyEntity.markAsGreatPyreneesPuppy(puppy);

        puppy.setAgeLock(true);
        puppy.setPersistent(true);
        puppy.setRemoveWhenFarAway(false);
        puppy.customName(Component.text("Puppy"));
        puppy.setCustomNameVisible(false);

        UUID puppyUuid = puppy.getUniqueId();
        savePuppyUuid(puppyUuid);
        greatPyrenees.put(puppyUuid, puppy);

        plugin.getLogger().info("Registered new great pyrenees puppy");
    }

    /**
     * Saves the given UUID as a puppy's UUID.
     *
     * @param uuid The UUID to save for the puppy
     */
    private void savePuppyUuid(UUID uuid) {
        puppiesUuids.add(uuid);
        List<String> puppiesUuidsAsStrings = puppiesUuids.stream().map(UUID::toString).collect(Collectors.toList());
        plugin.getConfig().set(Configurations.PUPPIES_UUIDS, puppiesUuidsAsStrings);
        plugin.saveConfig();
    }

    /**
     * Returns the puppies' UUIDs if known.
     *
     * @return The puppies' UUIDs
     */
    public Set<UUID> getPuppiesUuids() {
        return puppiesUuids;
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
     * Cleans up the great pyrenees behaviors and listeners.
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
