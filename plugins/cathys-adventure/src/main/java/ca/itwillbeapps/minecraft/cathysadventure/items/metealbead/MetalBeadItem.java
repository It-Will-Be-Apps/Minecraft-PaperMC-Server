package ca.itwillbeapps.minecraft.cathysadventure.items.metealbead;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MetalBeadItem {
    private static final String ITEM_ID = "METAL_BEAD";

    private final CathysAdventure plugin;

    public MetalBeadItem(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a metal bead.
     *
     * @return The created metal bead item
     */
    public ItemStack create() {
        ItemStack metalBead = ItemStack.of(Settings.METAL_BEAD_BASE_MATERIAL);

        metalBead.editMeta(meta -> {
            meta.displayName(Component.text("Metal Bead", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(Component.text("A curious metallic bead. What could it be for?", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(plugin.getKeys().itemIdKey, PersistentDataType.STRING, ITEM_ID);
            meta.getPersistentDataContainer().set(plugin.getKeys().soulboundItemKey, PersistentDataType.BOOLEAN, true);
        });

        return metalBead;
    }

    /**
     * Verifies if the given item is a metal bead.
     *
     * @param item The item to verify
     * @return Whether the item is a metal bead
     */
    public boolean isMetalBead(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        if (item.getType() != Settings.METAL_BEAD_BASE_MATERIAL) {
            return false;
        }

        return ITEM_ID.equals(item.getPersistentDataContainer().get(plugin.getKeys().itemIdKey, PersistentDataType.STRING));
    }
}
