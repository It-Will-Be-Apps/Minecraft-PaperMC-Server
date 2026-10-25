package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class GreatPyreneesChunkLoader {
    private final CathysAdventure plugin;
    private final GreatPyreneesManager greatPyreneesManager;

    private BukkitTask greatPyreneesChunkLoaderTask;
    private List<Chunk> chunksWithTickets;

    public GreatPyreneesChunkLoader(CathysAdventure plugin, GreatPyreneesManager greatPyreneesManager) {
        this.plugin = plugin;
        this.greatPyreneesManager = greatPyreneesManager;
    }

    /**
     * Starts the great pyrenees' chunk loader loop.
     */
    public void start() {
        if (greatPyreneesChunkLoaderTask != null && !greatPyreneesChunkLoaderTask.isCancelled()) {
            return;
        }
        
        chunksWithTickets = new ArrayList<>();
        greatPyreneesChunkLoaderTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1l, 5l);

        plugin.getLogger().info("Great Pyrenees chunk loader loop started");
    }

    /**
     * Processes one chunk loader tick for the great pyrenees.
     */
    private void tick() {
        Set<Chunk> chunksWithGreatPyrenees = greatPyreneesManager.getGreatPyrenees().stream()
            .filter(wolf -> !wolf.isDead() && wolf.isValid())
            .map(Entity::getChunk)
            .collect(Collectors.toSet());

        List<Chunk> chunksToRemove = chunksWithTickets.stream()
            .filter(chunkWithTicket -> chunksWithGreatPyrenees.stream().noneMatch(chunkWithGreatPyrenees -> chunksAreEqual(chunkWithTicket, chunkWithGreatPyrenees)))
            .toList();
        chunksToRemove.forEach(chunk -> chunk.removePluginChunkTicket(plugin));

        List<Chunk> chunksToAdd = chunksWithGreatPyrenees.stream()
            .filter(chunkWithGreatPyrenees -> chunksWithTickets.stream().noneMatch(chunkWithTicket -> chunksAreEqual(chunkWithGreatPyrenees, chunkWithTicket)))
            .toList();
        chunksToAdd.forEach(chunk -> chunk.addPluginChunkTicket(plugin));

        chunksWithTickets.removeAll(chunksToRemove);
        chunksWithTickets.addAll(chunksToAdd);
    }

    /**
     * Checks whether two chunk objects represent the same chunk.
     *
     * @param firstChunk The first chunk object
     * @param secondChunk The second chunk object
     * @return Whether both objects refer to the same chunk
     */
    private boolean chunksAreEqual(Chunk firstChunk, Chunk secondChunk) {
        return firstChunk.getWorld().getUID().equals(secondChunk.getWorld().getUID())
            && firstChunk.getChunkKey() == secondChunk.getChunkKey();
    }

    /**
     * Stops the great pyrenees' chunk loader loop and clears all chunk
     * tickets.
     */
    public void stop() {
        if (greatPyreneesChunkLoaderTask != null) {
            greatPyreneesChunkLoaderTask.cancel();
            greatPyreneesChunkLoaderTask = null;
        }

        chunksWithTickets.forEach(chunk -> chunk.removePluginChunkTicket(plugin));
        chunksWithTickets.clear();

        plugin.getLogger().info("Great Pyrenees chunk loader loop stopped");
    }
}
