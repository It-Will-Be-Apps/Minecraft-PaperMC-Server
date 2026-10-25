package ca.itwillbeapps.minecraft.cathysadventure.companion.grogu;

import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class GroguEntity {
    private static final String GROGU_ENTITY_ID = "GROGU";

    private final CathysAdventure plugin;

    public GroguEntity(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Marks an entity as Grogu.
     *
     * @param entity The entity to mark as Grogu
     */
    public void markAsGrogu(Entity entity) {
        entity.getPersistentDataContainer().set(plugin.getKeys().entityIdKey, PersistentDataType.STRING, GROGU_ENTITY_ID);
    }

    /**
     * Verifies if the given entity is Grogu.
     *
     * @param entity The entity to verify
     * @return Whether the entity is Grogu
     */
    public boolean isGrogu(Entity entity) {
        if (entity == null) {
            return false;
        }

        return GROGU_ENTITY_ID.equals(entity.getPersistentDataContainer().get(plugin.getKeys().entityIdKey, PersistentDataType.STRING));
    }
}
