package ca.itwillbeapps.minecraft.cathysadventure.companion.grogu;

import org.bukkit.Chunk;
import org.bukkit.entity.Allay;
import org.bukkit.scheduler.BukkitTask;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class GroguChunkLoader {
    private final CathysAdventure plugin;
    private final GroguManager groguManager;

    private BukkitTask groguChunkLoaderTask;
    private Chunk grogusChunk;

    public GroguChunkLoader(CathysAdventure plugin, GroguManager groguManager) {
        this.plugin = plugin;
        this.groguManager = groguManager;
    }

    /**
     * Starts Grogu's chunk loader loop.
     */
    public void start() {
        if (groguChunkLoaderTask != null && !groguChunkLoaderTask.isCancelled()) {
            return;
        }

        groguChunkLoaderTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1l, 5l);

        plugin.getLogger().info("Grogu chunk loader loop started");
    }

    /**
     * Processes one chunk loader tick for Grogu.
     */
    private void tick() {
        Allay grogu = groguManager.getGrogu();

        if (grogu == null || grogu.isDead() || !grogu.isValid()) {
            return;
        }

        Chunk grogusCurrentChunk = grogu.getChunk();

        if (grogusChunk == null) {
            grogusCurrentChunk.addPluginChunkTicket(plugin);
            grogusChunk = grogusCurrentChunk;

            return;
        }

        if (!chunksAreEqual(grogusChunk, grogusCurrentChunk)) {
            grogusChunk.removePluginChunkTicket(plugin);
            grogusCurrentChunk.addPluginChunkTicket(plugin);
            grogusChunk = grogusCurrentChunk;
        }
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
     * Stops Grogu's chunk loader loop and clears the chunk ticket.
     */
    public void stop() {
        if (groguChunkLoaderTask != null) {
            groguChunkLoaderTask.cancel();
            groguChunkLoaderTask = null;
        }

        if (grogusChunk != null) {
            grogusChunk.removePluginChunkTicket(plugin);
            grogusChunk = null;
        }

        plugin.getLogger().info("Grogu chunk loader loop stopped");
    }
}
