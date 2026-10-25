package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.obelix;

import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class ObelixEntity {
    private static final String OBELIX_ENTITY_ID = "OBELIX";

    private final CathysAdventure plugin;

    public ObelixEntity(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Marks an entity as Obélix.
     *
     * @param entity The entity to mark as Obélix
     */
    public void markAsObelix(Entity entity) {
        entity.getPersistentDataContainer().set(plugin.getKeys().entityIdKey, PersistentDataType.STRING, OBELIX_ENTITY_ID);
    }

    /**
     * Verifies if the given entity is Obélix.
     *
     * @param entity The entity to verify
     * @return Whether the entity is Obélix
     */
    public boolean isObelix(Entity entity) {
        if (entity == null) {
            return false;
        }

        return OBELIX_ENTITY_ID.equals(entity.getPersistentDataContainer().get(plugin.getKeys().entityIdKey, PersistentDataType.STRING));
    }
}
