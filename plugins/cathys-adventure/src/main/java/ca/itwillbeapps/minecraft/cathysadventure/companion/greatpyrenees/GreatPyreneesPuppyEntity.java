package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class GreatPyreneesPuppyEntity {
    private static final String GP_PUPPY_ENTITY_ID = "GP_PUPPY";

    private final CathysAdventure plugin;

    public GreatPyreneesPuppyEntity(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Marks an entity as a great pyrenees puppy.
     *
     * @param entity The entity to mark as a great pyrenees puppy
     */
    public void markAsGreatPyreneesPuppy(Entity entity) {
        entity.getPersistentDataContainer().set(plugin.getKeys().entityIdKey, PersistentDataType.STRING, GP_PUPPY_ENTITY_ID);
    }

    /**
     * Verifies if the given entity is a great pyrenees puppy.
     *
     * @param entity The entity to verify
     * @return Whether the entity is a great pyrenees puppy
     */
    public boolean isGreatPyreneesPuppy(Entity entity) {
        if (entity == null) {
            return false;
        }

        return GP_PUPPY_ENTITY_ID.equals(entity.getPersistentDataContainer().get(plugin.getKeys().entityIdKey, PersistentDataType.STRING));
    }
}
