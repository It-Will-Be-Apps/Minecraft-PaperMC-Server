package ca.itwillbeapps.minecraft.cathysadventure.items.fetchball;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class FetchBallItem {
    private static final String ITEM_ID = "FETCH_BALL";

    private final CathysAdventure plugin;

    public FetchBallItem(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a fetch ball.
     *
     * @return The created fetch ball item
     */
    public ItemStack create() {
        ItemStack fetchBall = ItemStack.of(Settings.FETCH_BALL_BASE_MATERIAL);

        fetchBall.editMeta(meta -> {
            meta.displayName(Component.text("Fetch Ball", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(Component.text("A dog would have a great time with this.", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(plugin.getKeys().itemIdKey, PersistentDataType.STRING, ITEM_ID);
            meta.getPersistentDataContainer().set(plugin.getKeys().soulboundItemKey, PersistentDataType.BOOLEAN, true);
        });

        return fetchBall;
    }

    /**
     * Verifies if the given item is a fetch ball.
     *
     * @param item The item to verify
     * @return Whether the item is a fetch ball
     */
    public boolean isFetchBall(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        if (item.getType() != Settings.FETCH_BALL_BASE_MATERIAL) {
            return false;
        }

        return ITEM_ID.equals(item.getPersistentDataContainer().get(plugin.getKeys().itemIdKey, PersistentDataType.STRING));
    }
}
