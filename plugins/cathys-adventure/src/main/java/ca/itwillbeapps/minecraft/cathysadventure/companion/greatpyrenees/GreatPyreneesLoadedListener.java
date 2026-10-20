package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.everest.EverestEntity;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.obelix.ObelixEntity;

public class GreatPyreneesLoadedListener implements Listener {
    private final GreatPyreneesManager greatPyreneesManager;
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;

    public GreatPyreneesLoadedListener(GreatPyreneesManager greatPyreneesManager, EverestEntity everestEntity, ObelixEntity obelixEntity) {
        this.greatPyreneesManager = greatPyreneesManager;
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
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

        greatPyreneesManager.getGreatPyreneesUuids().forEach(uuid -> {
            for (Wolf wolf : wolvesInLoadedChunk) {
                if (!wolf.getUniqueId().equals(uuid)) {
                    continue;
                }

                if (uuid.equals(greatPyreneesManager.getEverestUuid())) {
                    everestEntity.markAsEverest(wolf);
                } else if (uuid.equals(greatPyreneesManager.getObelixUuid())) {
                    obelixEntity.markAsObelix(wolf);
                } else {
                    // TODO Mark as a GP puppy
                }

                greatPyreneesManager.addGreatPyrenees(wolf);
            }
        });
    }
}
