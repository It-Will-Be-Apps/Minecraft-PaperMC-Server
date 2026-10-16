package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitTask;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class EverestChunkLoader {
    private final CathysAdventure plugin;
    private final EverestManager everestManager;

    private BukkitTask everestChunkLoaderTask;
    private List<Chunk> chunksWithTickets;

    public EverestChunkLoader(CathysAdventure plugin, EverestManager everestManager) {
        this.plugin = plugin;
        this.everestManager = everestManager;
    }

    /**
     * Starts Everest's behavior loop.
     */
    public void start() {
        if (everestChunkLoaderTask != null && !everestChunkLoaderTask.isCancelled()) {
            return;
        }
        
        chunksWithTickets = new ArrayList<>();
        everestChunkLoaderTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1l, 5l);

        plugin.getLogger().info("Everest chunk loader loop started");
    }

    /**
     * Processes one chunk loader tick for Everest.
     */
    private void tick() {
        Wolf everest = everestManager.getEverest(); // TODO replace with get list of wolves, iterate over the list
        
        if (everest == null || everest.isDead() || !everest.isValid()) {
            chunksWithTickets.forEach(chunk -> chunk.removePluginChunkTicket(plugin));
            return;
        }

        Chunk everestChunk = everest.getChunk();

        chunksWithTickets.stream().filter(chunk -> {
            return !chunk.getWorld().getUID().equals(everestChunk.getWorld().getUID())
                || chunk.getChunkKey() != everestChunk.getChunkKey();
        }).forEach(chunk -> chunk.removePluginChunkTicket(plugin));

        chunksWithTickets.add(everestChunk);
        everestChunk.addPluginChunkTicket(plugin);
    }

    /**
     * Stops Everest's chunk loader loop and clears all chunk tickets.
     */
    public void stop() {
        if (everestChunkLoaderTask != null) {
            everestChunkLoaderTask.cancel();
            everestChunkLoaderTask = null;
        }

        plugin.getLogger().info("Everest chunk loader loop stopped");
    }
}
