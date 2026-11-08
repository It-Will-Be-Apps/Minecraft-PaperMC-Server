package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class GreatPyreneesLoadedListener implements Listener {
    private final GreatPyreneesManager greatPyreneesManager;
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;
    private final GreatPyreneesPuppyEntity greatPyreneesPuppyEntity;

    public GreatPyreneesLoadedListener(GreatPyreneesManager greatPyreneesManager, EverestEntity everestEntity, ObelixEntity obelixEntity,
            GreatPyreneesPuppyEntity greatPyreneesPuppyEntity) {

        this.greatPyreneesManager = greatPyreneesManager;
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
        this.greatPyreneesPuppyEntity = greatPyreneesPuppyEntity;
    }

    /**
     * Listens to chunks loading to find great pyrenees.
     *
     * @param event The chunk loading event
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        List<Wolf> wolvesInLoadedChunk = Arrays.stream(event.getChunk().getEntities())
            .filter(entity -> entity instanceof Wolf wolf && !wolf.isDead() && wolf.isValid())
            .map(entity -> (Wolf) entity)
            .toList();

        for (Wolf wolf : wolvesInLoadedChunk) {
            UUID uuid = wolf.getUniqueId();

            if (uuid.equals(greatPyreneesManager.getEverestUuid())) {
                everestEntity.markAsEverest(wolf);
                greatPyreneesManager.addGreatPyrenees(wolf);
            } else if (uuid.equals(greatPyreneesManager.getObelixUuid())) {
                obelixEntity.markAsObelix(wolf);
                greatPyreneesManager.addGreatPyrenees(wolf);
            } else if (greatPyreneesManager.getPuppiesUuids().contains(uuid)) {
                greatPyreneesPuppyEntity.markAsGreatPyreneesPuppy(wolf);
                greatPyreneesManager.addGreatPyrenees(wolf);
            }
        }
    }
}
