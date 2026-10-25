package ca.itwillbeapps.minecraft.cathysadventure.companion.grogu;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Allay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class GroguLoadedListener implements Listener {
    private final GroguManager groguManager;
    private final GroguEntity groguEntity;

    public GroguLoadedListener(GroguManager groguManager, GroguEntity groguEntity) {
        this.groguManager = groguManager;
        this.groguEntity = groguEntity;
    }

    /**
     * Listens to chunks loading to find Grogu.
     *
     * @param event The chunk loading event
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        List<Allay> allaysInLoadedChunk = Arrays.stream(event.getChunk().getEntities())
            .filter(entity -> entity instanceof Allay allay && !allay.isDead() && allay.isValid())
            .map(entity -> (Allay) entity)
            .toList();

        for (Allay allay : allaysInLoadedChunk) {
            UUID uuid = allay.getUniqueId();

            if (uuid.equals(groguManager.getGroguUuid())) {
                groguEntity.markAsGrogu(allay);
                groguManager.setGrogu(allay);
            }

            break;
        }
    }
}
