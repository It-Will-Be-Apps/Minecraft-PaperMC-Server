package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.everest;

import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class EverestEntity {
    private static final String EVEREST_ENTITY_ID = "EVEREST";

    private final CathysAdventure plugin;

    public EverestEntity(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Marks an entity as Everest.
     *
     * @param entity The entity to mark as Everest
     */
    public void markAsEverest(Entity entity) {
        entity.getPersistentDataContainer().set(plugin.getKeys().entityIdKey, PersistentDataType.STRING, EVEREST_ENTITY_ID);
    }

    /**
     * Verifies if the given entity is Everest.
     *
     * @param entity The entity to verify
     * @return Whether the entity is Everest
     */
    public boolean isEverest(Entity entity) {
        if (entity == null) {
            return false;
        }

        return EVEREST_ENTITY_ID.equals(entity.getPersistentDataContainer().get(plugin.getKeys().entityIdKey, PersistentDataType.STRING));
    }
}
